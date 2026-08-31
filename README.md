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
