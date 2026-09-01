package com.studymate.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    suspend fun getAll(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<ChunkEntity>): List<Long>

    /**
     * Load every chunk (text + embedding) for a document.
     * Retrieval is performed in-memory with cosine similarity, which is O(n) per query.
     * For typical study documents (a few thousand chunks) this is fast and uses very
     * little peak RAM — far less than a native FAISS build and fully offline.
     */
    @Query("SELECT * FROM chunks WHERE documentId = :documentId ORDER BY ordinal ASC")
    suspend fun getForDocument(documentId: Long): List<ChunkEntity>

    @Query("SELECT COUNT(*) FROM chunks WHERE documentId = :documentId")
    suspend fun countForDocument(documentId: Long): Int

    @Query("DELETE FROM chunks WHERE documentId = :documentId")
    suspend fun deleteForDocument(documentId: Long)

    @Transaction
    suspend fun replaceForDocument(documentId: Long, chunks: List<ChunkEntity>) {
        deleteForDocument(documentId)
        insertAll(chunks)
    }
}
