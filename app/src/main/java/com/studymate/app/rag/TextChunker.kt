package com.studymate.app.rag

/**
 * A chunk produced by [TextChunker] before embedding.
 */
data class Chunk(val ordinal: Int, val text: String)

/**
 * Sentence-aware fixed-size chunker tuned for low-RAM devices.
 *
 * Strategy: split the document into sentences, then greedily pack sentences into
 * chunks of ~[targetChars] characters while keeping a [overlapChars] overlap between
 * consecutive chunks so context is not lost at chunk boundaries.
 *
 * Why not a giant buffer? Each chunk is embedded + stored independently, so we never
 * need to hold the whole document in memory as one structure — we stream chunks out
 * one at a time via [chunkSequence].
 */
class TextChunker(
    private val targetChars: Int = 500,
    private val overlapChars: Int = 100,
    private val maxChunkChars: Int = 1200
) {

    /** Greedy, low-allocation chunker. Emits chunks lazily via a Sequence. */
    fun chunkSequence(rawText: String): Sequence<Chunk> = sequence {
        if (rawText.isBlank()) return@sequence
        val sentences = splitSentences(rawText)
        var ordinal = 0
        val current = StringBuilder(targetChars + 64)
        var overlapCarry: String? = null

        for (sentence in sentences) {
            // If a single "sentence" is larger than the max, hard-split it by characters.
            if (sentence.length > maxChunkChars) {
                if (current.isNotBlank()) {
                    yield(Chunk(ordinal++, current.toString().trim()))
                    current.clear()
                }
                overlapCarry = null
                for (piece in splitByChars(sentence, targetChars, overlapChars)) {
                    yield(Chunk(ordinal++, piece))
                }
                continue
            }
            if (current.length + sentence.length + 1 > targetChars && current.isNotEmpty()) {
                // Emit current chunk; carry its tail as overlap into the next.
                overlapCarry = tailOverlap(current.toString(), overlapChars)
                yield(Chunk(ordinal++, current.toString().trim()))
                current.clear()
                overlapCarry?.let { current.append(it).append(' ') }
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        if (current.isNotBlank()) yield(Chunk(ordinal++, current.toString().trim()))
    }

    /** Convenience: materialize all chunks into a list (only for small docs). */
    fun chunk(rawText: String): List<Chunk> = chunkSequence(rawText).toList()

    /** Split into sentences on common terminators, preserving the punctuation. */
    private fun splitSentences(text: String): List<String> {
        val cleaned = text.replace("\r", "\n").replace(Regex("\\u00A0"), " ")
        // Split keeping delimiters; also treat newlines as hard boundaries.
        val regex = Regex("(?<=[.!?።፨])\\s+|\\n+")
        return cleaned.split(regex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun splitByChars(text: String, size: Int, overlap: Int): List<String> {
        val result = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            val end = minOf(i + size, text.length)
            result.add(text.substring(i, end).trim())
            if (end == text.length) break
            i += (size - overlap).coerceAtLeast(1)
        }
        return result
    }

    private fun tailOverlap(text: String, overlap: Int): String {
        if (text.length <= overlap) return text
        val cut = text.length - overlap
        val space = text.indexOf(' ', cut)
        return text.substring(if (space >= 0) space + 1 else cut).trim()
    }
}
