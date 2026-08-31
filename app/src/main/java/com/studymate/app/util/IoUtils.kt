package com.studymate.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object IoUtils {

    /** Resolve a SAF/Uri display name, falling back to the last path segment. */
    fun displayName(context: Context, uri: Uri): String {
        var name: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) name = c.getString(0)
            }
        return name ?: uri.lastPathSegment ?: "document"
    }

    /** Mime type from the resolver, falling back to an extension-based guess. */
    fun mimeType(context: Context, uri: Uri): String {
        var mime = context.contentResolver.getType(uri)
        if (mime.isNullOrEmpty()) {
            mime = when (displayName(context, uri).substringAfterLast('.', "").lowercase()) {
                "pdf" -> "application/pdf"
                "txt", "text" -> "text/plain"
                "jpg", "jpeg", "png" -> "image/*"
                else -> "*/*"
            }
        }
        return mime
    }

    /** Ensure the app's private "models" directory exists and return it. */
    fun ensureModelsDir(context: Context): File =
        File(context.filesDir, "models").apply { if (!exists()) mkdirs() }
}
