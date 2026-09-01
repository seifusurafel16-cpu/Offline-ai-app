package com.studymate.app.rag

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts plain text from PDF / TXT / image files fully on-device.
 *
 * - PDF: rendered page-by-page to a Bitmap via the platform [PdfRenderer], then OCR'd
 *   with Google ML Kit Text Recognition. We render one page at a time and recycle the
 *   bitmap immediately to keep peak memory tiny on 1GB-RAM devices.
 * - TXT: read directly as UTF-8 (fastest path; no OCR needed).
 * - Image (PNG/JPG): a single ML Kit OCR pass.
 *
 * ML Kit is the required text-extraction stack; PdfRenderer is used only to rasterize
 * PDF pages into something ML Kit can read (ML Kit has no native PDF parser).
 */
class TextExtractor(private val context: Context) {

    // Created on first OCR use (inside Dispatchers.IO), never at process start. ML Kit
    // initialization pulls in native libraries; deferring it keeps app launch crash-free.
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * @param onProgress optional callback receiving (currentPage, totalPages) for PDFs.
     * @return extracted plain text.
     */
    suspend fun extract(uri: Uri, mimeType: String, onProgress: ((Int, Int) -> Unit)? = null): String =
        withContext(Dispatchers.IO) {
            when {
                mimeType.contains("pdf") -> extractPdf(uri, onProgress)
                mimeType.startsWith("text") -> readTextFile(uri)
                mimeType.startsWith("image") -> ocrImage(uri)
                else -> {
                    // Unknown extension: try plain text first, then PDF.
                    readTextFile(uri).ifBlank { extractPdf(uri, onProgress) }
                }
            }
        }

    private fun readTextFile(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: ""

    private suspend fun ocrImage(uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return recognizer.process(image).await().text
    }

    /**
     * Render each PDF page to a bitmap and OCR it. Page bitmaps are created and recycled
     * one at a time; we render at ~150 DPI (scale 2.0 of the 72-DPI PDF default) which is
     * a good accuracy/memory trade-off for OCR. The ParcelFileDescriptor must stay open
     * for the entire lifetime of the PdfRenderer, so we manage it in a try/finally.
     */
    private suspend fun extractPdf(uri: Uri, onProgress: ((Int, Int) -> Unit)?): String {
        // Copy the stream to a temp file because PdfRenderer requires a seekable fd.
        val tempFile = File(context.cacheDir, "studymate_input.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        } ?: return ""

        val pfd = android.os.ParcelFileDescriptor.open(
            tempFile,
            android.os.ParcelFileDescriptor.MODE_READ_ONLY
        )
        val renderer = try {
            PdfRenderer(pfd)
        } catch (e: Exception) {
            pfd.close()
            tempFile.delete()
            throw e
        }

        val pageCount = renderer.pageCount
        val fullText = StringBuilder()
        val renderScale = 2.0f
        try {
            for (i in 0 until pageCount) {
                renderer.openPage(i).use { page ->
                    val width = (page.width * renderScale).toInt().coerceAtLeast(1)
                    val height = (page.height * renderScale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // PdfRenderer requires a white background (transparent renders as black).
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val pageText = recognizer.process(image).await().text
                    if (pageText.isNotBlank()) {
                        fullText.append(pageText).append("\n\n")
                    }
                    bitmap.recycle()
                }
                onProgress?.invoke(i + 1, pageCount)
            }
        } finally {
            renderer.close()
            pfd.close()
            tempFile.delete()
        }
        return fullText.toString()
    }
}
