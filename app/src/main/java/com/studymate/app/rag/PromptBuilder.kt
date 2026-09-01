package com.studymate.app.rag

/**
 * Builds prompts for the local LLM. Bilingual-aware (English + Amharic): the system
 * instruction tells the model to answer in the same language as the question.
 */
object PromptBuilder {

    /** System instruction shared by both modes. */
    const val SYSTEM_INSTRUCTION = """You are StudyMate, a helpful, concise educational assistant that runs fully on-device.
Rules:
- Answer in the SAME language as the user's question (English or Amharic / አማርኛ).
- Be accurate and brief. If you are unsure, say you don't know rather than guessing.
- Never invent sources or citations."""

    /**
     * RAG prompt: the system instruction is prepended (the current MediaPipe session API
     * has no setSystemPrompt), then the model is told to answer strictly from context. If
     * the context does not contain the answer, it must say so instead of hallucinating.
     */
    fun ragPrompt(question: String, contextChunks: List<String>): String {
        val context = contextChunks.joinToString("\n---\n") { it.trim() }
        return """$SYSTEM_INSTRUCTION

Use ONLY the context below to answer the question. If the context does not contain the answer, reply: "I couldn't find this in the document."

Context:
$context

Question: $question
Answer:"""
    }

    /** Plain chat prompt (no retrieved context). System instruction is prepended. */
    fun chatPrompt(question: String): String =
        "$SYSTEM_INSTRUCTION\n\nQuestion: $question\nAnswer:"
}
