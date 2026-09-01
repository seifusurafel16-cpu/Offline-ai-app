package com.studymate.app.llm

import android.content.Context
import com.studymate.app.util.IoUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Resolves the LLM model file to an absolute path that MediaPipe's LlmInference can load.
 *
 * MediaPipe LlmInference requires a *filesystem path*, not an AAPT-processed asset path.
 * We therefore:
 *  1. Look first in the app's private `filesDir/models/` directory — this is where the
 *     README instructs users to sideload the model via `adb push` (recommended for large
 *     models that would otherwise blow past the 500MB APK limit).
 *  2. Fall back to a model shipped in `assets/` (copied to filesDir on first use). This
 *     path is used when a small quantized model is bundled directly.
 *
 * @return the absolute path to a usable model file, or null if none is present (the UI
 *         shows a friendly error in that case).
 */
object ModelLoader {

    /**
     * Candidate model file names, in priority order. The first one found is used.
     * Users only need to provide ONE of these.
     */
    private val CANDIDATES = listOf(
        "TinyLlama-1.1B-Chat-v1.0.Q4_K_M.tflite",        // TinyLlama 1.1B, 4-bit, ~670MB
        "TinyLlama-1.1B-Chat-v1.0.Q4_K_M.bin",            // alternate extension
        "qwen2.5-1.5b-instruct-q4.tflite",                // Qwen2.5 1.5B, 4-bit
        "gemma-2b-it-gpu-int4.tflite"                     // Gemma 2B int4 (fallback)
    )

    fun resolveModelPath(context: Context): String? {
        val modelsDir = IoUtils.ensureModelsDir(context)

        // 1) Sideloaded model in filesDir/models (recommended path for large models).
        CANDIDATES.firstOrNull { File(modelsDir, it).exists() && File(modelsDir, it).length() > 0 }
            ?.let { return File(modelsDir, it).absolutePath }

        // 2) Model shipped in assets/ — copy out on first use.
        val assetName = CANDIDATES.firstOrNull { assetExists(context, it) } ?: return null
        val outFile = File(modelsDir, assetName)
        if (!outFile.exists() || outFile.length() == 0L) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        }
        return outFile.absolutePath
    }

    private fun assetExists(context: Context, name: String): Boolean {
        // assets.list("") returns the top-level asset entries.
        return context.assets.list("")?.contains(name) == true
    }
}
