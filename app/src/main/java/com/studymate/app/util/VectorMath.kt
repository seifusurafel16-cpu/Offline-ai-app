package com.studymate.app.util

import kotlin.math.sqrt

/**
 * Small, dependency-free vector math used for cosine-similarity retrieval.
 */
object VectorMath {

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            val av = a[i]
            val bv = b[i]
            dot += av * bv
            normA += av * av
            normB += bv * bv
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom <= 0.0) 0f else (dot / denom).toFloat()
    }

    /** In-place L2 normalization; returns the same array for convenience. */
    fun l2Normalize(a: FloatArray): FloatArray {
        var sum = 0.0
        for (v in a) sum += (v * v)
        val norm = sqrt(sum)
        if (norm > 0.0) {
            val inv = (1.0 / norm).toFloat()
            for (i in a.indices) a[i] *= inv
        }
        return a
    }
}
