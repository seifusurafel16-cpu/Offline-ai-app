package com.studymate.app.ui.study

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.app.StudyMateApp
import com.studymate.app.data.DocumentEntity
import com.studymate.app.data.DocumentRepository
import com.studymate.app.rag.RagService
import com.studymate.app.util.IoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the "Study Assistant" (RAG) tab.
 *
 * Owns the RAG workflow: file selection → indexing → Q&A. All long-running work runs in
 * [viewModelScope] on Dispatchers.IO (inside [RagService]); this VM only maps results to
 * [StudyUiState] and never blocks the main thread.
 */
class StudyViewModel(
    private val repository: DocumentRepository = StudyMateApp.instance.repository,
    private val ragService: RagService = StudyMateApp.instance.ragService
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    init { loadDocuments() }

    fun loadDocuments() {
        viewModelScope.launch {
            val docs = repository.getAllDocuments()
            _state.update {
                it.copy(
                    documents = docs,
                    selectedDocumentId = it.selectedDocumentId ?: docs.firstOrNull()?.id
                )
            }
        }
    }

    fun selectDocument(id: Long) {
        _state.update { it.copy(selectedDocumentId = id, answer = "", sources = emptyList(), showSources = false) }
    }

    /**
     * Index a freshly picked file. The URI is read once for display name + mime type,
     * then handed to [RagService.indexDocument] which extracts, chunks, embeds, and stores.
     */
    fun indexFile(uri: Uri) {
        val context = StudyMateApp.instance
        val name = IoUtils.displayName(context, uri)
        val mime = IoUtils.mimeType(context, uri)

        viewModelScope.launch {
            _state.update { it.copy(isIndexing = true, indexStage = "Starting…", error = null) }
            try {
                val id = ragService.indexDocument(uri, name, mime) { progress ->
                    _state.update {
                        it.copy(indexStage = "${progress.stage} (${progress.value}/${progress.total})")
                    }
                }
                val docs = repository.getAllDocuments()
                _state.update {
                    it.copy(
                        documents = docs,
                        selectedDocumentId = id,
                        isIndexing = false,
                        indexStage = "Done: ${docs.firstOrNull { d -> d.id == id }?.chunkCount ?: 0} chunks indexed."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isIndexing = false, indexStage = "", error = e.message ?: "Indexing failed")
                }
            }
        }
    }

    /** Ask a question about the currently selected document. */
    fun askQuestion(question: String) {
        val q = question.trim()
        if (q.isEmpty()) return
        val docId = _state.value.selectedDocumentId ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(isAnswering = true, answer = "", sources = emptyList(), showSources = false, error = null)
            }
            try {
                val result = ragService.answerQuestion(docId, q)
                _state.update {
                    it.copy(
                        isAnswering = false,
                        answer = result.answer,
                        sources = result.sources.map { c -> c.text },
                        showSources = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isAnswering = false, error = e.message ?: "Could not generate an answer")
                }
            }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            loadDocuments()
            if (_state.value.selectedDocumentId == doc.id) {
                _state.update { it.copy(selectedDocumentId = null, answer = "", sources = emptyList()) }
            }
        }
    }

    fun clearAnswer() {
        _state.update { it.copy(answer = "", sources = emptyList(), showSources = false) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
