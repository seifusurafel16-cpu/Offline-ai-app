package com.studymate.app.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around MediaPipe GenAI [LlmInference] for on-device, 4-bit-quantized LLM
 * generation (e.g. TinyLlama-1.1B-Chat Q4_K_M).
 *
 * Memory strategy for the 1GB-RAM constraint:
 * - The engine is created **lazily** on first [generate] / [generateStream] call, never at
 *   app start. This keeps cold-start RAM low.
 * - [unload] releases the engine when the app is backgrounded (see [com.studymate.app.StudyMateApp]'s
 *   ProcessLifecycle observer). The next query re-loads it transparently.
 * - Sessions are short-lived: one session per query, closed in a `finally` block.
 *
 * The `maxTokens`, `temperature`, `topK`, and `randomSeed` defaults are tuned for a 1.1B
 * chat model on low-end hardware: short, deterministic-ish answers to save compute.
 */
class LlmManager(private val context: Context) {

    @Volatile
    private var llm: LlmInference? = null
    private val loadMutex = Mutex()

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
                .build()
            llm = LlmInference.createFromOptions(context, options)
            Log.i(TAG, "LLM engine loaded: $path")
        }
    }

    /** Synchronous full generation (simplest path; used for RAG answers). */
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        ensureLoaded()
        val session = createSession()
        try {
            session.addQueryChunk(prompt)
            session.generateResponse()
        } finally {
            safeCloseSession(session)
        }
    }

    /**
     * Streaming generation. Calls [onToken] for each partial output; returns the full text.
     * Uses the session's async callback API (`generateResponseAsync { token, done -> }`).
     *
     * Note: `generateResponseAsync` returns a [com.google.common.util.concurrent.ListenableFuture]
     * that completes only when generation is done. We block on it (we are already on
     * Dispatchers.IO) so the returned string is the complete response, not a partial one.
     */
    suspend fun generateStream(prompt: String, onToken: (String) -> Unit): String =
        withContext(Dispatchers.IO) {
            ensureLoaded()
            val session = createSession()
            try {
                session.addQueryChunk(prompt)
                val sb = StringBuilder()
                // The callback fires on each token; `done` marks completion.
                val future = session.generateResponseAsync { token, done ->
                    if (token.isNotEmpty()) {
                        sb.append(token)
                        onToken(token)
                    }
                }
                // Block until the future completes; the final string is also available from it,
                // but we use our accumulated buffer so [onToken] has already seen every token.
                future.get()
                sb.toString()
            } finally {
                safeCloseSession(session)
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
            Log.i(TAG, "LLM engine unloaded")
        }
    }

    private fun createSession(): LlmInferenceSession {
        // Only confirmed, broadly-available options are set here. (The system instruction
        // is injected into the prompt itself in PromptBuilder, since the current MediaPipe
        // session options do not expose setSystemPrompt.)
        val sessionOpts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(0.7f)
            .setTopK(40)
            .setRandomSeed(101)
            .build()
        val engine = llm ?: error("LLM not loaded")
        return engine.createSession(sessionOpts)
    }

    private fun safeCloseSession(session: LlmInferenceSession?) {
        try {
            session?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing session", e)
        }
    }

    companion object {
        private const val TAG = "StudyMate/LlmManager"
    }
}
