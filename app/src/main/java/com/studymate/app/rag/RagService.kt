package com.studymate.app.rag

import android.net.Uri
import com.studymate.app.data.ChunkEntity
import com.studymate.app.data.DocumentEntity
import com.studymate.app.data.DocumentRepository
import com.studymate.app.llm.LlmManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates the full RAG pipeline:
 *
 *   index:  extract text → chunk → embed → persist (Room vector store)
 *   answer: embed query → retrieve top-k chunks → build prompt → LLM generate
 *
 * Progress callbacks keep the UI informed during the (potentially long) indexing phase.
 */
class RagService(
    private val repository: DocumentRepository,
    private val extractor: TextExtractor,
    private val embedder: EmbeddingManager,
    private val llm: LlmManager
) {
    private val chunker = TextChunker()

    data class IndexProgress(val stage: String, val value: Int, val total: Int)

    /**
     * Index a document end-to-end. Returns the created [DocumentEntity] id.
     */
    suspend fun indexDocument(
        uri: Uri,
        displayName: String,
        mimeType: String,
        onProgress: (IndexProgress) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        // 1) Create a stub document row so we have an id to attach chunks to.
        val doc = DocumentEntity(displayName = displayName, uri = uri.toString(), mimeType = mimeType)
        val docId = repository.insertDocument(doc)

        try {
            // 2) Extract text.
            onProgress(IndexProgress("Extracting text", 0, 0))
            val text = extractor.extract(uri, mimeType) { page, total ->
                onProgress(IndexProgress("Reading PDF page", page, total))
            }
            if (text.isBlank()) {
                throw IllegalStateException("No text could be extracted from this file.")
            }

            // 3) Chunk.
            val chunks = chunker.chunk(text)
            if (chunks.isEmpty()) throw IllegalStateException("Text could not be split into chunks.")

            // 4) Embed each chunk and build entities.
            val entities = ArrayList<ChunkEntity>(chunks.size)
            chunks.forEachIndexed { index, chunk ->
                val embedding = embedder.embed(chunk.text)
                    ?: FloatArray(0) // store empty if embedding failed; retrieval will skip.
                entities.add(
                    ChunkEntity(
                        documentId = docId,
                        ordinal = chunk.ordinal,
                        text = chunk.text,
                        embedding = embedding
                    )
                )
                onProgress(IndexProgress("Embedding chunks", index + 1, chunks.size))
            }

            // 5) Persist.
            repository.replaceChunks(docId, entities)

            // 6) Update document stats.
            repository.updateDocument(
                doc.copy(id = docId, charCount = text.length, chunkCount = entities.size)
            )
            docId
        } catch (e: Exception) {
            // On failure, remove the stub so the UI doesn't show a half-indexed doc.
            repository.getDocument(docId)?.let { repository.deleteDocument(it) }
            throw e
        }
    }

    /**
     * Answer a question against an indexed document. Returns the LLM answer and the
     * list of source chunks used (so the UI can show citations).
     */
    suspend fun answerQuestion(documentId: Long, question: String): RagAnswer =
        withContext(Dispatchers.IO) {
            val chunks = repository.loadChunks(documentId)
            if (chunks.isEmpty()) {
                return@withContext RagAnswer(
                    answer = "This document has not been indexed yet. Tap \"Read & Learn\" first.",
                    sources = emptyList()
                )
            }

            // Embed the query and retrieve top-k.
            val queryEmbedding = embedder.embed(question)
            val sources: List<RetrievedChunk> = if (queryEmbedding != null && queryEmbedding.isNotEmpty()) {
                VectorRetriever(chunks).retrieve(queryEmbedding, k = 4)
            } else {
                // Embedding unavailable (model missing): fall back to first chunks as context.
                chunks.take(4)
            }

            val prompt = PromptBuilder.ragPrompt(question, sources.map { it.text })
            val answer = llm.generate(prompt)
            RagAnswer(answer = answer.trim(), sources = sources)
        }

    data class RagAnswer(val answer: String, val sources: List<RetrievedChunk>)
}
