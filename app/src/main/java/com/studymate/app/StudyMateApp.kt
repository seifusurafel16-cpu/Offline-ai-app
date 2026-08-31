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
 * Wires up the singletons (repository, RAG service, LLM manager) and registers a
 * process-lifecycle observer that **unloads the LLM engine when the app goes to the
 * background**. This is the key OOM-prevention strategy for the 1GB-RAM constraint: a
 * 4-bit 1.1B model can occupy ~700MB of RAM, so we must release it the moment the user
 * leaves the app and reload lazily on return.
 */
class StudyMateApp : Application() {

    lateinit var repository: DocumentRepository
        private set
    lateinit var llmManager: LlmManager
        private set
    lateinit var embeddingManager: EmbeddingManager
        private set
    lateinit var ragService: RagService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        repository = DocumentRepository(this)
        llmManager = LlmManager(this)
        embeddingManager = EmbeddingManager(this)
        val extractor = TextExtractor(this)
        ragService = RagService(repository, extractor, embeddingManager, llmManager)

        // Unload heavy models when the whole process is backgrounded.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                Log.i(TAG, "Process backgrounded — unloading LLM + embedding models")
                llmManager.unload()
                embeddingManager.close()
            }
        })
    }

    companion object {
        private const val TAG = "StudyMateApp"
        @Volatile
        lateinit var instance: StudyMateApp
            private set
    }
}
