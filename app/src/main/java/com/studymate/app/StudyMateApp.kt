package com.studymate.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.studymate.app.data.DocumentRepository
import com.studymate.app.llm.LlmManager
import com.studymate.app.rag.EmbeddingManager
import com.studymate.app.rag.RagService
import com.studymate.app.rag.TextExtractor

/**
 * Application entry point.
 *
 * IMPORTANT (1GB-RAM / crash-safety): `onCreate` does as little as possible. We deliberately
 * do NOT construct the Room database, the ML Kit recognizer, the embedder, or the RAG
 * service here — any of those can pull in native libraries or 3rd-party initializers that
 * may fail on exotic/low-end devices, and a failure in `Application.onCreate` (or in a
 * ContentProvider that runs before it) crashes the app before the UI ever appears.
 *
 * Instead every heavy component is created lazily on first USE (inside `Dispatchers.IO`,
 * from the ViewModels), and each creation is wrapped so a failure becomes a UI-visible
 * error rather than a hard crash.
 *
 * We still register a process-lifecycle observer that **unloads the LLM engine when the app
 * goes to the background** — the key OOM-prevention strategy for the 1GB-RAM constraint: a
 * 4-bit 1.1B model can occupy ~700MB of RAM, so we release it the moment the user leaves.
 */
class StudyMateApp : Application() {

    /** Lazily created on first access; never built at process start. */
    val repository: DocumentRepository by lazy { DocumentRepository(this) }
    val llmManager: LlmManager by lazy { LlmManager(this).also { llmLoaded = true } }
    val embeddingManager: EmbeddingManager by lazy { EmbeddingManager(this).also { embLoaded = true } }
    val ragService: RagService by lazy {
        RagService(repository, TextExtractor(this), embeddingManager, llmManager)
    }

    @Volatile private var llmLoaded = false
    @Volatile private var embLoaded = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            // Unload heavy models when the whole process is backgrounded. We only touch a
            // component if it was actually created, so backgrounding never forces lazy init.
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    try {
                        if (llmLoaded) {
                            Log.i(TAG, "Process backgrounded — unloading LLM + embedding models")
                            llmManager.unload()
                            embeddingManager.close()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unloading models on background", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register lifecycle observer", e)
        }
    }

    companion object {
        private const val TAG = "StudyMateApp"
        @Volatile
        lateinit var instance: StudyMateApp
            private set
    }
}
