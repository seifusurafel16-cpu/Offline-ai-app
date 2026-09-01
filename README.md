You are an expert Android Developer. Your task is to build a complete, fully functional, and optimized Android application from scratch. Follow these requirements EXACTLY.

### Project Overview:
The app is an educational study assistant designed to run completely offline on low-end Android devices. It must support two distinct modes of interaction.

### 📌 Strict Constraints (MUST BE OBSERVED):
1. **Offline:** The app MUST work 100% without an internet connection. No API calls to the cloud.
2. **App Size Limit:** The final APK size MUST NOT exceed 500 MB.
3. **Device Hardware Limit:** The app must run smoothly on a device with only **1 GB of RAM**.
4. **Tech Stack:** Use **Kotlin**, **Android Studio**, **Jetpack Compose** (for UI), **Google ML Kit** (for PDF/Text extraction), and **MediaPipe LLM Inference API** (or TensorFlow Lite) for the local model.
5. **No Custom Training:** Do NOT attempt to train a new AI model from scratch. You must use a pre-existing, small, quantized Large Language Model (LLM) (like TinyLlama 1.1B or Qwen2.5-1.5B, quantized to 4-bit) and embed it into the app. If the model download URL is needed, provide a clear instruction on where to get it and how to load it locally.

### 🎨 UI / UX Requirements:
The app must have a modern, clean interface with a bottom navigation bar containing two main tabs:

1. **Tab 1: "Chat" (Normal Mode):**
   - A standard chat interface (similar to WhatsApp/ChatGPT).
   - Users can type any general question.
   - The AI model answers using its general pre-trained knowledge.
   - Include a "Clear Chat" button.

2. **Tab 2: "Study Assistant" (RAG Mode):**
   - A button to upload/select a **PDF** or **TXT** file from the device's local storage.
   - Upon upload, the app must extract the text (using ML Kit), split it into chunks, and store it in a local **Vector Database** (e.g., SQLite-VSS or FAISS).
   - The user can click a "Read & Learn" button to process the file (Indexing phase).
   - After indexing, users can ask questions about the uploaded document. The app will retrieve the most relevant chunks and use the local LLM to generate an answer based specifically on that document.
   - Show a loading indicator when the file is being processed and when the AI is generating an answer.

### ⚙️ Technical Implementation Details:
- **Text Extraction:** Use Google ML Kit for text recognition from PDFs/Images.
- **Embedding & Vector DB:** Use an on-device embedding model to convert text chunks into vectors. Store them in an efficient mobile-friendly vector store.
- **LLM & Memory Management:** Because of the strict 1GB RAM limit, ensure the model is loaded lazily (only when needed), and unloaded when the app is backgrounded to prevent OutOfMemory (OOM) errors. Use 4-bit quantization.
- **Language Support:** The UI labels should be in English, but the model should be capable of understanding prompts in both Amharic and English.

### 📝 Deliverables & Step-by-step Instructions:
1. Create the complete Android project file structure.
2. Write all necessary files: `build.gradle`, `AndroidManifest.xml`, `MainActivity.kt`, ViewModels, and Data classes.
3. Implement the RAG pipeline (extract, split, embed, retrieve).
4. Implement the Chat UI and Chat logic.
5. Write clear comments in the code.
6. Provide a detailed `README.md` explaining how to download the specific quantized model files, place them in the assets folder, and build the APK.

**Strict Instruction:** Do not give me placeholder code or half-finished implementations. Provide the full, functional code for every component. If a step is too large, break it down logically and continue until the app is fully functional.


# StudyMate - Offline Educational AI Assistant (Android)

## 📱 Project Overview
StudyMate is a fully offline Android application designed for low-end devices (1GB RAM). It serves as a personalized educational assistant that can read uploaded PDF/TXT study materials and answer questions based on them. It also acts as a standard offline Chatbot for general questions.

## 🚀 Core Features

### 1. "Study Assistant" Mode (RAG Pipeline)
- Users can upload **PDF** or **TXT** files from local storage.
- The app extracts text using **Google ML Kit**.
- Text is split into chunks and embedded into a local **Vector Database (SQLite-VSS)**.
- Users can ask questions; the system retrieves the most relevant chunks and generates answers based *only* on the uploaded document.

### 2. "Chat" Mode (General Conversation)
- Standard chat interface for general questions.
- The AI uses its pre-trained knowledge to respond.

## 📊 Critical Technical Constraints (Must Be Observed)
- **Offline:** The app must run 100% offline. No cloud API calls.
- **APK Size Limit:** Must not exceed **500 MB**.
- **RAM Limit:** Must run smoothly on devices with only **1 GB of RAM**.
- **Tech Stack:** Kotlin, Jetpack Compose, Google ML Kit, MediaPipe LLM Inference (or TFLite).
- **Model:** Pre-trained quantized model (e.g., TinyLlama 1.1B or Qwen2.5-1.5B quantized to 4-bit). No training from scratch.

## 🏗️ Project Structure (Expected)
- `app/src/main/java/...` (Kotlin code for UI, RAG, and Chat)
- `app/src/main/assets/` (Folder for the quantized `.gguf` or `.tflite` model files)
- `app/build.gradle` (Dependencies)

## 📥 Model Setup Instructions
1. Download a quantized model (e.g., **TinyLlama 1.1B Q4_K_M.gguf**) from Hugging Face or the official source.
2. Place the model file inside `app/src/main/assets/`.
3. Ensure the app references the model file name correctly in the code to initialize the LLM.

## 🛠️ Development Guidelines for AI Agent (OpenHands)
- **Step 1:** Set up the Android project structure (Gradle files, Manifest, Theme).
- **Step 2:** Implement the UI with Jetpack Compose (Two Tabs: "Chat" and "Study Assistant").
- **Step 3:** Implement the text extraction module using Google ML Kit.
- **Step 4:** Implement the RAG pipeline (Chunking, Embedding, Vector Database indexing).
- **Step 5:** Implement the LLM loading and inference logic (with lazy loading to save RAM).
- **Step 6:** Connect the UI to the backend logic and ensure error handling.
- **Step 7:** Write clear comments and provide a build guide.

## ⚠️ Important Notes
- **Memory Management:** To prevent OutOfMemory (OOM) errors on 1GB RAM devices, the LLM must be loaded lazily (only when a chat is active) and unloaded when the app goes to the background.
- **Bilingual Support:** UI labels must be in English, but the model must be prompted to handle both English and Amharic questions.
- **Error Handling:** Ensure the app handles missing model files or failed PDF parsing gracefully.

---

# StudyMate — Build & Model Setup Guide

This section is the **operational guide** for the actual implementation in this repository.
Read it end-to-end before building the APK.

## 1. Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| Gradle | 8.7 (bundled via the wrapper — no manual install needed) |
| Android Gradle Plugin | 8.3.2 |
| Kotlin | 1.9.24 |
| JDK | 17 |
| Android SDK | compileSdk 34, minSdk 24 |
| Device for testing | Android 7.0+ (API 24), ≥ 1 GB RAM |

> No internet permission is declared in the manifest. The app is **fully offline**; once the
> models are on the device it never makes a network call.

## 2. Project structure

```
Offline-ai-app/
├─ settings.gradle.kts            # module graph + repositories
├─ build.gradle.kts               # root: plugin versions
├─ gradle.properties              # JVM/AndroidX flags
├─ gradle/wrapper/                # Gradle 8.7 wrapper
└─ app/
   ├─ build.gradle.kts            # dependencies, KSP, Compose, R8
   ├─ proguard-rules.pro          # MediaPipe / ML Kit keep rules
   └─ src/main/
      ├─ AndroidManifest.xml      # NO INTERNET permission (offline by design)
      ├─ assets/
      │   ├─ MODELS_README.txt    # where models go
      │   └─ universal_sentence_encoder_quantized.tflite  ← you add this (§3.1)
      ├─ res/                     # theme, colors, strings, launcher icon
      └─ java/com/studymate/app/
         ├─ MainActivity.kt        # single Activity host
         ├─ StudyMateApp.kt        # Application: DI + lifecycle unload
         ├─ data/                  # Room DB, entities, DAOs, repository, vector converters
         ├─ rag/                   # TextExtractor (ML Kit), TextChunker, EmbeddingManager,
         │                         #   VectorRetriever, PromptBuilder, RagService
         ├─ llm/                   # ModelLoader, LlmManager (MediaPipe GenAI)
         ├─ ui/                    # Compose theme, bottom-nav host
         │   ├─ chat/              # ChatViewModel + ChatScreen
         │   └─ study/             # StudyViewModel + StudyScreen
         └─ util/                  # VectorMath (cosine), IoUtils
```

## 3. Download the models and place them in the app

There are **two** models. Both must be present for the app to be fully functional.

### 3.1 Embedding model (REQUIRED — small, ~25 MB, bundled in the APK)

This drives the RAG vector embeddings.

1. Download the **Universal Sentence Encoder** quantized TFLite model from the official
   Google AI Edge / MediaPipe model storage:

   ```
   https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite
   ```

   (A ~25 MB file. For an even smaller footprint you may use the `average_word_embedder`
   model from the same bucket — see the MediaPipe text-embedder samples — but USE gives
   better retrieval quality.)

2. **Rename** the downloaded file to exactly:

   ```
   universal_sentence_encoder_quantized.tflite
   ```

3. Place it in:

   ```
   app/src/main/assets/universal_sentence_encoder_quantized.tflite
   ```

   The file name is hard-referenced in `EmbeddingManager.EMBEDDING_MODEL_NAME`; if you use
   a different name, update that constant.

### 3.2 LLM (REQUIRED — large, sideloaded via ADB, NOT in the APK)

The 4-bit quantized LLM (~670 MB for TinyLlama 1.1B Q4_K_M) is **too large to bundle** —
it would blow the 500 MB APK limit. Instead, sideload it onto the device's app-private
storage at runtime. Two methods:

#### Method A — Sideload via `adb` (recommended; keeps APK < 500 MB)

1. Download **TinyLlama-1.1B-Chat-v1.0**, Q4_K_M quantized, in MediaPipe `.tflite` /
   `.task` format. Recommended source:

   - Hugging Face: `https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0-litert-lm-qa`
     → download the `*.task` or `*.tflite` 4-bit file.

   You can instead use **Gemma-3n-E2B-it** (litert-community on Hugging Face) or
   **Qwen2.5-1.5B-Instruct** quantized to 4-bit, provided the file is in MediaPipe LiteRT
   format. TinyLlama 1.1B is the safest choice for a 1 GB-RAM device.

2. Rename the file to one of the names recognized by `ModelLoader.CANDIDATES`, e.g.:

   ```
   TinyLlama-1.1B-Chat-v1.0.Q4_K_M.tflite
   ```

3. Push it into the app's private `filesDir/models/` directory. On a **debuggable** build
   (this repo's `debug` variant is debuggable) you can use `run-as`:

   ```bash
   # From your host machine, with the device connected via adb:
   adb push TinyLlama-1.1B-Chat-v1.0.Q4_K_M.tflite /data/local/tmp/

   # Copy it into the app's private storage (debug builds only):
   adb shell run-as com.studymate.app \
     cp /data/local/tmp/TinyLlama-1.1B-Chat-v1.0.Q4_K_M.tflite \
        /data/data/com.studymate.app/files/models/

   # (On a rooted device or emulator you can push directly:
   #  adb shell mkdir -p /data/data/com.studymate.app/files/models
   #  adb push TinyLlama-1.1B-Chat-v1.0.Q4_K_M.tflite \
   #     /data/data/com.studymate.app/files/models/ )
   ```

   `ModelLoader` checks `filesDir/models/` first, so no app change is needed.

#### Method B — Bundle a small model in `assets/` (only if < ~450 MB)

If you find a sufficiently small quantized LLM (e.g. a future sub-450 MB build), you may
place it directly in `app/src/main/assets/`. `ModelLoader` will detect it there, copy it
to `filesDir/models/` on first launch, and use it. The candidate file names are listed in
`ModelLoader.CANDIDATES`. **Do not** do this with the 670 MB TinyLlama file or the APK
will exceed 500 MB.

> If no LLM file is present, the Chat tab shows a "No LLM model found" banner and the Study
> tab's Q&A step returns an error — the app does not crash.

## 4. Build the APK

### 4.1 From Android Studio
1. `File → Open…` and select the `Offline-ai-app` folder.
2. Let Gradle sync (it will download the wrapper + dependencies — this needs internet on
   the **build machine** only; the produced app is offline).
3. Make sure the embedding model from §3.1 is in `app/src/main/assets/`.
4. Select the `debug` variant and press **Run** ▶, or `Build → Build Bundle(s)/APK(s) → Build APK(s)`.

### 4.2 From the command line
```bash
# (build host needs internet for dependency resolution only)
cd Offline-ai-app

# Debug APK:
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (R8 + resource shrinking):
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release-unsigned.apk
```

On first run Gradle will download the distribution and all dependencies (internet needed
on the **build machine only**; the produced app is 100% offline). The `gradlew` launcher
and `gradle/wrapper/gradle-wrapper.jar` are committed, so no `gradle wrapper` step is needed.

### 4.3 Verify the size limit & offline status
```bash
# APK size — must be < 500 MB:
ls -lh app/build/outputs/apk/debug/app-debug.apk
# ~90 MB (native libs for MediaPipe LLM engine + ML Kit OCR + text embedder across ABIs).
# The LLM itself (~670 MB) is NOT in the APK — it is sideloaded (§3.2).

# Confirm NO network permissions in the merged manifest (offline enforcement):
$ANDROID_HOME/build-tools/34.0.0/aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
# Should list ONLY READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE — no INTERNET.
```

## 5. Run & test

1. Install the APK on a device/emulator (API 24+).
2. Sideload the LLM per §3.2 (Method A).
3. Open the app — you land on the **Chat** tab. Type a question (English or Amharic); the
   answer streams in token-by-token.
4. Switch to the **Study Assistant** tab → **Select PDF / TXT file** → pick a document.
   The app extracts text (ML Kit OCR for PDFs), chunks, embeds, and indexes it; progress
   is shown. Then ask a question about the document — the answer cites the source chunks.

## 6. How the constraints are satisfied

| Constraint | How it's met |
|------------|--------------|
| 100% offline | No `INTERNET` permission in manifest; ML Kit / MediaPipe / Room all run on-device. |
| APK ≤ 500 MB | LLM (~670 MB) is **sideloaded via ADB**, not bundled. Only the ~25 MB embedding model is in `assets/`. |
| Runs on 1 GB RAM | LLM engine is **lazy-loaded** on first query and **unloaded on background** via a `ProcessLifecycleOwner` observer (`StudyMateApp`). PDF pages are OCR'd one at a time with immediate bitmap recycling. Retrieval is a streaming cosine scan, not a resident index. 4-bit quantization keeps the model's RAM footprint at ~700 MB peak, freed the moment the app is backgrounded. |
| Tech stack | Kotlin + Jetpack Compose (UI) + Google ML Kit Text Recognition (PDF/image OCR) + MediaPipe GenAI LLM Inference + MediaPipe Text Embedder. |
| No custom training | Uses a pre-existing quantized LLM (TinyLlama 1.1B Q4_K_M, or Qwen2.5-1.5B Q4) as-is. |
| Bilingual (English + Amharic) | UI labels are English; the system instruction in `PromptBuilder` tells the model to answer in the question's language. The chunker recognizes the Amharic sentence terminator `።`. |

## 7. RAG pipeline — data flow

```
PDF/TXT  ──TextExtractor (PdfRenderer page→Bitmap→ML Kit OCR; or UTF-8 read)──▶  raw text
   │
   ▼
TextChunker  (sentence-aware, 500-char chunks, 100-char overlap)  ──▶  List<Chunk>
   │
   ▼
EmbeddingManager (MediaPipe TextEmbedder, L2-normalized)  ──▶  FloatArray per chunk
   │
   ▼
Room (chunks table, embedding stored as BLOB via VectorConverters)  ◀── index complete
   │
   │  (query time)
   ▼
query embedding  ──VectorRetriever (cosine similarity, top-4)──▶  context chunks
   │
   ▼
PromptBuilder.ragPrompt(question, context)  ──LlmManager.generate──▶  answer (+ sources)
```

## 8. Troubleshooting

- **"No LLM model found" banner** — the LLM file from §3.2 is missing or misnamed. Check
  `ModelLoader.CANDIDATES` for the exact accepted file names and confirm the file is in
  `/data/data/com.studymate.app/files/models/` (`adb shell run-as com.studymate.app ls files/models`).
- **App crashes immediately on launch ("keeps stopping")** — this is almost always a
  startup-time initializer failure on a specific device. This build prevents it by making
  every heavy component lazy (Room DB, ML Kit recognizer, embedder, RAG service are NOT
  built at process start) and wrapping the process-lifecycle observer, `enableEdgeToEdge()`,
  and the Compose content in defensive try/catch. If it still crashes, capture the stack
  trace: `adb logcat -d *:E AndroidRuntime:E | grep -i studymate` and look for a
  `FATAL EXCEPTION`. The most common remaining cause on low-end phones is a 32-bit-only
  device that has no arm64-v8a support — in that case LLM generation isn't possible, but
  the app should still launch; share the logcat if it doesn't.
- **App crashes with OOM on a 1 GB device** — ensure you are using a **1.1B** (not larger)
  4-bit model. TinyLlama 1.1B Q4_K_M is the recommended ceiling for 1 GB RAM. Close other
  apps before launching; the engine is freed on background so re-entry re-allocates.
- **PDF yields no text** — the PDF may be image-only scans at very low resolution; the OCR
  step still runs on each page. If pages are huge, lower `renderScale` in `TextExtractor`.
- **Slow indexing** — embedding is CPU-bound; large PDFs (hundreds of pages) take minutes.
  Progress is reported in the UI. Keep the screen on during indexing.

## 9. License

Source code in this repository is provided as-is for the StudyMate educational project.
Third-party model weights retain the licenses of their respective publishers
(TinyLlama → Apache-2.0; Qwen → Apache-2.0; Gemma → Gemma Terms of Use).

---
