package com.vknxworks.chatctrl.ui.room

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.matrix.AttachmentPlan
import com.vknxworks.chatctrl.ui.components.CtrlDialog
import com.vknxworks.chatctrl.ui.components.CtrlTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the staging strip shows for a picked or shared file: its name, size (when the other app says) and type. */
internal data class StagedInfo(val name: String, val size: Long?, val mime: String)

/** Asks the app that owns [uri] for the file's name, size and type. Only metadata: the file itself isn't read. */
internal suspend fun loadStagedInfo(context: Context, uri: Uri): StagedInfo = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mime = runCatching { resolver.getType(uri) }.getOrNull().orEmpty()
    var name: String? = null
    var size: Long? = null
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val s = c.getColumnIndex(OpenableColumns.SIZE)
                if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s).takeIf { it >= 0 }
            }
        }
    }
    StagedInfo(name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "?", size, mime)
}

/** A small still of a video: the provider's own thumbnail, or a frame read through the file descriptor. */
private suspend fun videoThumbnail(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching { context.contentResolver.loadThumbnail(uri, android.util.Size(320, 320), null) }.getOrNull()
        ?: runCatching {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                r.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 320)
            }
        }.getOrNull()
}

/** A picture, a still of a video, or an icon for any other file. */
@Composable
private fun StagedThumb(uri: Uri, mime: String, modifier: Modifier) {
    val context = LocalContext.current
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when {
            mime.startsWith("image/") -> coil3.compose.AsyncImage(
                model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
            mime.startsWith("video/") -> {
                val still by produceState<Bitmap?>(null, uri) { value = videoThumbnail(context, uri) }
                still?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                Icon(Icons.Outlined.PlayCircle, null, Modifier.size(32.dp), tint = Color.White)
            }
            mime.startsWith("audio/") -> Icon(Icons.Outlined.AudioFile, null, Modifier.size(36.dp))
            else -> Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null, Modifier.size(36.dp))
        }
    }
}

/**
 * The check before sending picked or shared files: a strip of thumbnails (or a large preview for one file), each
 * with its name and size and a button to take it out, an optional caption, and Send with the count. Files bigger
 * than the server allows ([maxUploadSize]) are marked and block Send until they are taken out.
 * Nothing is copied or read here: the files are only read when they are sent.
 */
@Composable
internal fun AttachmentStagingDialog(
    uris: List<Uri>,
    maxUploadSize: Long?,
    replying: Boolean,
    onRemove: (Int) -> Unit,
    onCancel: () -> Unit,
    onSend: (caption: String) -> Unit,
) {
    val context = LocalContext.current
    val infos = remember { mutableStateMapOf<Uri, StagedInfo>() }
    LaunchedEffect(uris) {
        uris.forEach { u -> if (u !in infos) infos[u] = loadStagedInfo(context, u) }
    }
    var caption by rememberSaveable { mutableStateOf("") }
    val count = uris.size
    val tooBig = uris.count { AttachmentPlan.tooBig(infos[it]?.size, maxUploadSize) }

    CtrlDialog(
        onDismissRequest = onCancel,
        title = {
            Text(if (count == 1) stringResource(R.string.send_attachment) else pluralStringResource(R.plurals.send_files_title, count, count))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (count == 1) {
                    val u = uris.first()
                    val info = infos[u]
                    val mime = info?.mime.orEmpty()
                    if (mime.startsWith("image/") || mime.startsWith("video/")) {
                        StagedThumb(u, mime, Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp).clip(RoundedCornerShape(12.dp)))
                    }
                    Text(
                        listOfNotNull(info?.name ?: u.lastPathSegment, info?.size?.let(AttachmentPlan::formatSize)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(uris, key = { _, u -> u.toString() }) { i, u ->
                            val info = infos[u]
                            val big = AttachmentPlan.tooBig(info?.size, maxUploadSize)
                            Column(Modifier.width(96.dp)) {
                                Box {
                                    StagedThumb(
                                        u, info?.mime.orEmpty(),
                                        Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)).then(
                                            if (big) Modifier.border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp)) else Modifier
                                        ),
                                    )
                                    IconButton(
                                        onClick = { onRemove(i) },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                    ) {
                                        Icon(Icons.Outlined.Close, stringResource(R.string.staging_remove, info?.name ?: ""), Modifier.size(16.dp), tint = Color.White)
                                    }
                                }
                                Text(info?.name.orEmpty(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (big) stringResource(R.string.staging_too_big) else info?.size?.let(AttachmentPlan::formatSize).orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (big) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    Text(stringResource(R.string.send_files_body), style = MaterialTheme.typography.bodySmall)
                }
                if (tooBig > 0 && maxUploadSize != null) {
                    Text(
                        pluralStringResource(R.plurals.staging_too_big_note, tooBig, tooBig, AttachmentPlan.formatSize(maxUploadSize)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    )
                }
                CtrlTextField(caption, { caption = it }, label = { Text(stringResource(R.string.caption_optional)) }, modifier = Modifier.fillMaxWidth())
                if (count > 1) {
                    Text(
                        stringResource(if (replying) R.string.staging_first_gets_caption_and_reply else R.string.staging_first_gets_caption),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(caption) }, enabled = count > 0 && tooBig == 0) {
                Text(if (count == 1) stringResource(R.string.send) else pluralStringResource(R.plurals.staging_send_n, count, count))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.lock_cancel)) } },
    )
}
