package com.studymate.app.rag

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import com.studymate.app.util.IoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * On-device text embedding model wrapper using MediaPipe's [TextEmbedder].
 *
 * Default model: a quantized Universal Sentence Encoder (USE) `.tflite` (~25 MB) shipped
 * in `assets/` (see README for the exact file). The embedder is small and cheap enough to
 * keep resident for the lifetime of a RAG session; we still close it when the Study tab
 * is left to honor the 1GB-RAM budget.
 *
 * Vectors are L2-normalized on output so cosine similarity reduces to a dot product.
 */
class EmbeddingManager(private val context: Context) {

    @Volatile
    private var embedder: TextEmbedder? = null

    /** Lazy-load the embedder the first time it is needed. */
    private fun ensureLoaded() {
        if (embedder != null) return
        synchronized(this) {
            if (embedder != null) return
            val assetPath = resolveModelAsset()
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(assetPath)
                .build()
            val options = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                // Quantized model produces int8 vectors; MediaPipe handles dequantization
                // and (optionally) L2 normalization for us.
                .setL2Normalize(true)
                .build()
            embedder = TextEmbedder.createFromOptions(context, options)
        }
    }

    /**
     * Resolve the embedding model path. We copy the asset to filesDir on first run because
     * MediaPipe loads models from a filesystem path (not directly from the APK assets in
     * all cases). Subsequent runs reuse the cached copy.
     */
    private fun resolveModelAsset(): String {
        val assetName = EMBEDDING_MODEL_NAME
        val outDir = IoUtils.ensureModelsDir(context)
        val outFile = File(outDir, assetName)
        if (!outFile.exists() || outFile.length() == 0L) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        }
        return outFile.absolutePath
    }

    /** Embed a single piece of text. Returns null if the model produced no embedding. */
    suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.IO) {
        ensureLoaded()
        val result = embedder?.embed(text)?.embeddingResult() ?: return@withContext null
        result.embeddings().firstOrNull()?.let { emb ->
            emb.floatEmbedding().takeIf { it.isNotEmpty() }?.toFloatArray()
        }
    }

    fun close() {
        synchronized(this) {
            embedder?.close()
            embedder = null
        }
    }

    companion object {
        // Quantized USE sentence-encoder tflite shipped in assets/ (see README §3).
        const val EMBEDDING_MODEL_NAME = "universal_sentence_encoder_quantized.tflite"
    }
}
