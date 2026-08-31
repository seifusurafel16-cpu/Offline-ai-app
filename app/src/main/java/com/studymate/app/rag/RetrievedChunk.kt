package com.studymate.app.rag

/**
 * A retrieved chunk plus its similarity score to the query.
 */
data class RetrievedChunk(
    val ordinal: Int,
    val text: String,
    val embedding: FloatArray,
    val score: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RetrievedChunk) return false
        return ordinal == other.ordinal && text == other.text &&
            embedding.contentEquals(other.embedding) && score == other.score
    }

    override fun hashCode(): Int {
        var r = ordinal
        r = 31 * r + text.hashCode()
        r = 31 * r + embedding.contentHashCode()
        r = 31 * r + score.hashCode()
        return r
    }
}
