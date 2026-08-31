package com.studymate.app.data

import android.content.Context
import com.studymate.app.rag.RetrievedChunk

/**
 * Thin repository over Room. Keeps ViewModels free of direct DB access and gives a
 * single place to map entities <-> domain models.
 */
class DocumentRepository(context: Context) {

    private val db = StudyMateDatabase.get(context.applicationContext)
    private val documentDao = db.documentDao()
    private val chunkDao = db.chunkDao()

    suspend fun insertDocument(doc: DocumentEntity): Long =
        documentDao.insert(doc)

    suspend fun updateDocument(doc: DocumentEntity) =
        documentDao.update(doc)

    suspend fun getAllDocuments(): List<DocumentEntity> =
        documentDao.getAll()

    suspend fun getDocument(id: Long): DocumentEntity? =
        documentDao.getById(id)

    suspend fun deleteDocument(doc: DocumentEntity) =
        documentDao.delete(doc)

    suspend fun deleteAllDocuments() =
        documentDao.deleteAll()

    suspend fun replaceChunks(documentId: Long, chunks: List<ChunkEntity>) =
        chunkDao.replaceForDocument(documentId, chunks)

    /** Load chunks for a document as lightweight [RetrievedChunk] holders for retrieval. */
    suspend fun loadChunks(documentId: Long): List<RetrievedChunk> =
        chunkDao.getForDocument(documentId).map {
            RetrievedChunk(ordinal = it.ordinal, text = it.text, embedding = it.embedding)
        }

    suspend fun chunkCount(documentId: Long): Int =
        chunkDao.countForDocument(documentId)
}
