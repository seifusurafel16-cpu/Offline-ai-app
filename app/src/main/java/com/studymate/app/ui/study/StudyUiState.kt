package com.studymate.app.ui.study

import com.studymate.app.data.DocumentEntity

/**
 * Immutable UI state for the "Study Assistant" (RAG) tab.
 */
data class StudyUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val selectedDocumentId: Long? = null,
    val isIndexing: Boolean = false,
    val indexStage: String = "",
    val isAnswering: Boolean = false,
    val answer: String = "",
    val sources: List<String> = emptyList(),   // text of retrieved chunks (for citations)
    val showSources: Boolean = false,
    val error: String? = null
)
