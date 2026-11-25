package com.lowspin.app.ui.screens.folders

import com.lowspin.app.ui.theme.LowspinOnAccent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.data.scanner.ScanState
import com.lowspin.app.ui.components.SectionHeader
import com.lowspin.app.ui.components.VinylGlyph
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary

@Composable
fun FolderManagerScreen(
    viewModel: FolderManagerViewModel,
    onBack: () -> Unit
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val scanning = scanState is ScanState.Scanning || scanState is ScanState.Enriching

    val included = remember(rows) { rows.filter { !it.excluded } }
    val hidden = remember(rows) { rows.filter { it.excluded } }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            Text(
                text = "Manage folders",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = viewModel::refresh, enabled = !scanning) {
                if (scanning) {
                    VinylGlyph(modifier = Modifier.size(20.dp), rotating = true)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Rescan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Only turn on folders that contain music. Hidden folders are skipped on every scan \u2014 perfect for call recordings and voice notes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (rows.isEmpty() && !scanning) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOff,
                    contentDescription = null,
                    tint = LowspinTextMuted,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "No folders found",
                    style = MaterialTheme.typography.titleMedium,
                    color = LowspinTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Rescan the library after adding music to your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::refresh,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = LowspinTextPrimary
                    )
                ) {
                    Text("Rescan library")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (included.isNotEmpty()) {
                    item(key = "included-header", contentType = "header") {
                        SectionHeader(title = "Included")
                    }
                    items(
                        items = included,
                        key = { "inc-${it.folderPath}" },
                        contentType = { "folder" }
                    ) { row ->
                        FolderRowItem(
                            row = row,
                            onToggle = { viewModel.setExcluded(row, true) }
                        )
                    }
                }
                if (hidden.isNotEmpty()) {
                    item(key = "hidden-header", contentType = "header") {
                        SectionHeader(title = "Hidden")
                    }
                    items(
                        items = hidden,
                        key = { "exc-${it.folderPath}" },
                        contentType = { "folder" }
                    ) { row ->
                        FolderRowItem(
                            row = row,
                            onToggle = { viewModel.setExcluded(row, false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRowItem(row: FolderRow, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (row.excluded) Icons.Rounded.FolderOff else Icons.Rounded.Folder,
            contentDescription = null,
            tint = if (row.excluded) LowspinTextMuted else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = row.folderName,
                style = MaterialTheme.typography.titleSmall,
                color = if (row.excluded) MaterialTheme.colorScheme.onSurfaceVariant else LowspinTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(if (row.songCount == 1) "1 song" else "${row.songCount} songs")
                    if (row.folderPath.isNotBlank()) {
                        append(" \u00B7 ")
                        append(row.folderPath)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = LowspinTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = !row.excluded,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = LowspinOnAccent,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = LowspinTextMuted
            )
        )
    }
}
