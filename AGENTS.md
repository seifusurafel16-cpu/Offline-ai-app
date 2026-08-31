# AGENTS.md — StudyMate (Offline AI Study Assistant)

Repository-specific memory for OpenHands agents working on this Android app.

## What this project is
A fully-offline Android study assistant (Kotlin + Jetpack Compose) that runs on 1 GB-RAM
devices. Two tabs: **Chat** (general LLM Q&A) and **Study Assistant** (RAG over uploaded
PDF/TXT). No internet permission is declared.

## Tech stack & key versions
- Kotlin 1.9.24, AGP 8.3.2, Gradle 8.7, JDK 17, Compose compiler ext 1.5.14, Compose BOM 2024.06.00
- compileSdk/targetSdk 34, minSdk 24
- Room 2.6.1 (KSP), ML Kit text-recognition 16.0.1, MediaPipe tasks-text + tasks-genai 0.10.14
- Package: `com.studymate.app`

## Architecture map
- `StudyMateApp` (Application) = manual DI + ProcessLifecycleObserver that **unloads the LLM
  and embedder on background** (the core OOM-prevention strategy for 1 GB RAM).
- `llm/LlmManager` — MediaPipe GenAI `LlmInference` + session-based API. Lazy-loaded, mutex-guarded.
- `rag/TextExtractor` — PdfRenderer (page→Bitmap→ML Kit OCR, one page at a time) or UTF-8 read.
- `rag/EmbeddingManager` — MediaPipe `TextEmbedder` (USE tflite from assets).
- `rag/TextChunker` — sentence-aware, 500-char chunks, 100-char overlap; recognizes Amharic `።`.
- `rag/VectorRetriever` — in-memory cosine (no native FAISS); chunks persisted in Room as BLOB.
- `data/` — Room DB `studymate.db`, `documents` + `chunks` tables, `VectorConverters` (FloatArray↔BLOB).

## MediaPipe API gotchas (verified against official samples)
- **Session-based:** `llmInference.createSession(opts)` then `session.addQueryChunk(prompt)`,
  `session.generateResponse()` / `session.generateResponseAsync { token, done -> }`, `session.close()`.
- `LlmInferenceSessionOptions` has NO `setSystemPrompt` in 0.10.14 → system instruction is
  prepended to the prompt in `PromptBuilder`.
- `TextEmbedder`: call chain is `embedder.embed(text).embeddingResult().embeddings().first().floatEmbedding()`.
- Models load from a **filesystem path**, not assets — copy asset→filesDir/models/ first (see `ModelLoader`/`EmbeddingManager`).

## Models (NOT committed — too large)
- Embedding model `universal_sentence_encoder_quantized.tflite` (~25 MB) → `app/src/main/assets/`.
- LLM (TinyLlama-1.1B-Chat Q4_K_M, ~670 MB) → **sideloaded via adb** to
  `filesDir/models/` (keeps APK < 500 MB). Candidate names in `ModelLoader.CANDIDATES`.
- See README §3 for exact download URLs + adb `run-as` commands.

## Build
No Android SDK / JDK is available in this sandbox — **the app cannot be compiled here**.
Build on a machine with Android Studio: `./gradlew assembleDebug` (needs the wrapper jar,
generated on first Android Studio sync, or `gradle wrapper --gradle-version 8.7`).

## Conventions
- Single-activity + Compose; one ViewModel per tab (`ChatViewModel`, `StudyViewModel`).
- All heavy work on `Dispatchers.IO` via `withContext`; UI state as `StateFlow` collected with
  `collectAsStateWithLifecycle`.
- Comments only for non-obvious invariants (lazy load/unload, PFD lifetime, cosine retrieval choice).
