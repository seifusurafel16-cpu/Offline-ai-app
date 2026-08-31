package com.studymate.app.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.ErrorListener
import com.google.mediapipe.tasks.core.OutputHandler.ProgressListener
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/**
 * Thin wrapper around MediaPipe GenAI [LlmInference] (tasks-genai 0.10.14) for on-device,
 * 4-bit-quantized LLM generation (e.g. TinyLlama-1.1B-Chat Q4_K_M).
 *
 * Memory strategy for the 1GB-RAM constraint:
 * - The engine is created **lazily** on first [generate] / [generateStream] call, never at
 *   app start. This keeps cold-start RAM low.
 * - [unload] releases the engine when the app is backgrounded (see [com.studymate.app.StudyMateApp]'s
 *   ProcessLifecycle observer). The next query re-loads it transparently.
 *
 * API notes (tasks-genai 0.10.14):
 * - There is no session class in this version; generation params (temperature, topK, seed)
 *   are set on the engine options, and a [ProgressListener] is registered on the options to
 *   receive streaming tokens from `generateResponseAsync`.
 * - `generateResponse(prompt)` returns the full string synchronously.
 * - `generateResponseAsync(prompt)` returns void; tokens arrive via the registered listener.
 */
class LlmManager(private val context: Context) {

    @Volatile
    private var llm: LlmInference? = null
    private val loadMutex = Mutex()

    /**
     * Holder for the "current" streaming callback. The MediaPipe options' resultListener is
     * fixed at engine creation, so we route every token through this reference and swap the
     * target per query. Guarded by [streamLock].
     */
    private val streamLock = Any()
    private val activeStream = AtomicReference<StreamTarget?>(null)

    private inner class StreamTarget(val onToken: (String) -> Unit) {
        val sb = StringBuilder()
        val done = CompletableDeferred<Unit>()
        fun append(token: String) {
            sb.append(token)
            onToken(token)
        }
    }

    /** True if a model file is present on disk (sideloaded or in assets). */
    fun isModelAvailable(): Boolean = ModelLoader.resolveModelPath(context) != null

    private suspend fun ensureLoaded() {
        if (llm != null) return
        loadMutex.withLock {
            if (llm != null) return
            val path = ModelLoader.resolveModelPath(context)
                ?: error("No LLM model found. See README section 3 to add one.")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                // Cap total tokens (prompt + output) to keep memory predictable on 1GB devices.
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .setTopK(40)
                .setRandomSeed(101)
                // Streaming listener: fires (partialResult, done) per token. Registered once;
                // we forward to whichever StreamTarget is active (or drop if none).
                .setResultListener { partialResult, done ->
                    val target = activeStream.get()
                    if (target != null && partialResult.isNotEmpty()) {
                        target.append(partialResult)
                    }
                    if (done) {
                        target?.done?.complete(Unit)
                    }
                }
                .setErrorListener(ErrorListener { e ->
                    Log.e(TAG, "LLM generation error", e)
                    activeStream.get()?.done?.completeExceptionally(e)
                })
                .build()
            llm = LlmInference.createFromOptions(context, options)
            Log.i(TAG, "LLM engine loaded: $path")
        }
    }

    /**
     * Synchronous full generation (used for RAG answers where we want the complete text).
     * MediaPipe 0.10.14: `generateResponse(prompt)` blocks and returns the full string.
     */
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        ensureLoaded()
        val engine = llm ?: error("LLM not loaded")
        engine.generateResponse(prompt)
    }

    /**
     * Streaming generation. Calls [onToken] for each partial output; returns the full text.
     *
     * MediaPipe 0.10.14 has no per-call callback or returned future for `generateResponseAsync`,
     * so we install an [activeStream] target, kick off the async call, and await a
     * [CompletableDeferred] that is completed by the resultListener when `done == true`.
     */
    suspend fun generateStream(prompt: String, onToken: (String) -> Unit): String =
        withContext(Dispatchers.IO) {
            ensureLoaded()
            val engine = llm ?: error("LLM not loaded")
            val target = StreamTarget(onToken)
            synchronized(streamLock) { activeStream.set(target) }
            try {
                // Fire-and-forget async; tokens arrive on the listener registered at creation.
                engine.generateResponseAsync(prompt)
                // Suspend until the listener signals completion.
                target.done.await()
                target.sb.toString()
            } finally {
                synchronized(streamLock) { activeStream.compareAndSet(target, null) }
            }
        }

    /** Release the engine. Safe to call repeatedly. */
    fun unload() {
        synchronized(this) {
            try {
                llm?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing LLM engine", e)
            }
            llm = null
            synchronized(streamLock) { activeStream.set(null) }
            Log.i(TAG, "LLM engine unloaded")
        }
    }

    companion object {
        private const val TAG = "StudyMate/LlmManager"
    }
}
