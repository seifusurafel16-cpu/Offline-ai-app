package com.studymate.app.ui.study

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studymate.app.data.DocumentEntity

/**
 * The "Study Assistant" (RAG) screen.
 *
 * Layout: file picker + document list (top) → question composer → answer + sources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: StudyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var question by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    // SAF file picker — no storage permission required.
    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.indexFile(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(color = MaterialTheme.colorScheme.primary) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Study Assistant (RAG)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Upload button
            item {
                Button(
                    onClick = { pickFile.launch(arrayOf("application/pdf", "text/plain")) },
                    enabled = !state.isIndexing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Select PDF / TXT file")
                }
            }

            // Indexing progress
            if (state.isIndexing) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Processing…", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.size(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.size(8.dp))
                            Text(state.indexStage, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Error
            state.error?.let { err ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.dismissError() }) { Text("Dismiss") }
                        }
                    }
                }
            }

            // Document list
            item {
                Text(
                    "Indexed documents",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.documents.isEmpty()) {
                item {
                    Text(
                        "No documents yet. Select a PDF or TXT file above, then tap “Read & Learn”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.documents, key = { it.id }) { doc ->
                DocumentRow(
                    doc = doc,
                    selected = doc.id == state.selectedDocumentId,
                    onSelect = { viewModel.selectDocument(doc.id) },
                    onDelete = { viewModel.deleteDocument(doc) }
                )
            }

            // Selected document: "Read & Learn" already happens on upload, but we expose a
            // re-index affordance and the Q&A box.
            state.selectedDocumentId?.let { _ ->
                item {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ask about the document") },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Ask),
                        keyboardActions = KeyboardActions(onAsk = {
                            viewModel.askQuestion(question)
                            keyboard?.hide()
                        })
                    )
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            viewModel.askQuestion(question)
                            keyboard?.hide()
                        },
                        enabled = !state.isAnswering && question.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        if (state.isAnswering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(if (state.isAnswering) "Generating answer…" else "Read & Learn (Ask)")
                    }
                }

                // Answer
                if (state.answer.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Answer", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.size(4.dp))
                                Text(state.answer, style = MaterialTheme.typography.bodyMedium)
                                if (state.showSources && state.sources.isNotEmpty()) {
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        "Sources (${state.sources.size}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    state.sources.take(4).forEachIndexed { i, src ->
                                        Text(
                                            "• ${src.take(160)}${if (src.length > 160) "…" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentRow(
    doc: DocumentEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    val onContainer = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    doc.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = onContainer
                )
                Text(
                    "${doc.chunkCount} chunks · ${doc.charCount} chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete document")
            }
        }
    }
}
