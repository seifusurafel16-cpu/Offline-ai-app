package com.studymate.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.app.StudyMateApp
import com.studymate.app.llm.LlmManager
import com.studymate.app.rag.PromptBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the "Chat" (Normal Mode) tab.
 *
 * Drives a standard chat list: appends the user's message, streams the LLM's reply token
 * by token, and supports clearing the conversation. Uses the shared [LlmManager] from the
 * [StudyMateApp], which handles lazy load/unload for the 1GB-RAM constraint.
 */
class ChatViewModel(
    private val llm: LlmManager = StudyMateApp.instance.llmManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _modelAvailable = MutableStateFlow(llm.isModelAvailable())
    val modelAvailable: StateFlow<Boolean> = _modelAvailable.asStateFlow()

    fun sendQuestion(question: String) {
        val q = question.trim()
        if (q.isEmpty() || _isGenerating.value) return

        if (!_modelAvailable.value) {
            _modelAvailable.value = llm.isModelAvailable()
            if (!_modelAvailable.value) {
                _messages.update { it + ChatMessage(ChatMessage.nextId(), ChatMessage.Role.ASSISTANT,
                    "No LLM model found. Add a quantized model file as described in the README, then reopen the app.",
                    isError = true) }
                return
            }
        }

        // Append user message + a placeholder assistant message we will stream into.
        val userMsg = ChatMessage(ChatMessage.nextId(), ChatMessage.Role.USER, q)
        val assistantMsg = ChatMessage(
            id = ChatMessage.nextId(),
            role = ChatMessage.Role.ASSISTANT,
            content = "",
            isStreaming = true
        )
        _messages.update { it + listOf(userMsg, assistantMsg) }
        _isGenerating.value = true

        viewModelScope.launch {
            try {
                val prompt = PromptBuilder.chatPrompt(q)
                llm.generateStream(prompt) { token ->
                    appendToLast(token)
                }
                finalizeLast(isError = false)
            } catch (e: Exception) {
                replaceLast("Error: ${e.message ?: "generation failed"}", isError = true)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearChat() {
        if (_isGenerating.value) return
        _messages.value = emptyList()
    }

    /** Refresh model availability (e.g. after user adds a model via adb). */
    fun refreshModelAvailability() {
        _modelAvailable.value = llm.isModelAvailable()
    }

    private fun appendToLast(token: String) {
        _messages.update { list ->
            list.toMutableList().also { mutable ->
                val last = mutable.lastOrNull() ?: return@also
                mutable[mutable.lastIndex] = last.copy(content = last.content + token)
            }
        }
    }

    private fun finalizeLast(isError: Boolean) {
        _messages.update { list ->
            list.toMutableList().also { mutable ->
                val last = mutable.lastOrNull() ?: return@also
                mutable[mutable.lastIndex] = last.copy(isStreaming = false, isError = isError)
            }
        }
    }

    private fun replaceLast(text: String, isError: Boolean) {
        _messages.update { list ->
            list.toMutableList().also { mutable ->
                val last = mutable.lastOrNull() ?: return@also
                mutable[mutable.lastIndex] = last.copy(
                    content = text, isStreaming = false, isError = isError
                )
            }
        }
    }
}
