package com.studymate.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single uploaded document that has been (or is being) indexed for RAG.
 */
@Entity(
    tableName = "documents",
    indices = [Index("uri"), Index("createdAt")]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Human-readable file name shown in the UI. */
    @ColumnInfo(name = "displayName")
    val displayName: String,

    /** Original content Uri (may be transient across reboots for SAF). Kept for reference. */
    @ColumnInfo(name = "uri")
    val uri: String,

    /** Mime type of the source file. */
    @ColumnInfo(name = "mimeType")
    val mimeType: String,

    /** Number of characters extracted (for stats). */
    @ColumnInfo(name = "charCount")
    val charCount: Int = 0,

    /** Number of chunks created. */
    @ColumnInfo(name = "chunkCount")
    val chunkCount: Int = 0,

    /** Epoch millis when the document was added. */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * One retrievable text chunk + its embedding vector.
 * Foreign key to [DocumentEntity] with cascade delete keeps storage tidy when a doc is removed.
 */
@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "documentId")
    val documentId: Long,

    /** 1-based ordinal of the chunk within the document. */
    @ColumnInfo(name = "ordinal")
    val ordinal: Int,

    /** The raw chunk text. */
    @ColumnInfo(name = "text")
    val text: String,

    /** The embedding vector (stored as a BLOB via [VectorConverters]). */
    @ColumnInfo(name = "embedding")
    val embedding: FloatArray
) {
    // FloatArray cannot define equals/hashCode by value automatically; implement for completeness.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkEntity) return false
        return id == other.id && documentId == other.documentId &&
            ordinal == other.ordinal && text == other.text &&
            embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + ordinal
        result = 31 * result + text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
