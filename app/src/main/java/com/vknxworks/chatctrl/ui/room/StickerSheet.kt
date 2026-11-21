package com.vknxworks.chatctrl.ui.room

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.matrix.MatrixSession
import com.vknxworks.chatctrl.matrix.MxcImage
import com.vknxworks.chatctrl.matrix.Sticker
import com.vknxworks.chatctrl.matrix.StickerPack
import com.vknxworks.chatctrl.matrix.StickerService
import com.vknxworks.chatctrl.matrix.userMessage
import com.vknxworks.chatctrl.ui.ConfirmDialog
import com.vknxworks.chatctrl.ui.CtrlShapes
import com.vknxworks.chatctrl.ui.components.CtrlLabel
import com.vknxworks.chatctrl.ui.components.CtrlSheet
import com.vknxworks.chatctrl.ui.lock.AppLock
import kotlinx.coroutines.launch

/**
 * The sticker picker: your own pack first, then the packs you enabled from rooms and this room's pack. Tap a
 * sticker to send it; hold one of your own to remove it; "Add" puts a picture from the phone in your pack.
 */
@Composable
fun StickerSheet(
    session: MatrixSession,
    roomId: String,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    onPick: (Sticker) -> Unit,
) {
    val service = remember { StickerService(session) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var packs by remember { mutableStateOf<List<StickerPack>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Sticker?>(null) }
    LaunchedEffect(reload) {
        packs = runCatching { service.packs(roomId) }.getOrElse { onError(it.userMessage()); emptyList() }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        adding = true
        scope.launch {
            runCatching { service.addFromFile(context, uri) }
                .onSuccess { reload++ }
                .onFailure { onError(it.userMessage().ifBlank { context.getString(R.string.stickers_add_failed) }) }
            adding = false
        }
    }

    CtrlSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.stickers), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (adding) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            TextButton(enabled = !adding, onClick = {
                AppLock.expectReturn()
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Text(stringResource(R.string.stickers_add), modifier = Modifier.padding(start = 6.dp))
            }
        }
        val loaded = packs
        when {
            loaded == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            loaded.all { it.stickers.isEmpty() } -> Text(
                stringResource(R.string.stickers_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                modifier = Modifier.heightIn(max = 420.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
            ) {
                loaded.filter { it.stickers.isNotEmpty() }.forEach { pack ->
                    item(key = "pack:" + pack.id, span = { GridItemSpan(maxLineSpan) }) {
                        CtrlLabel(pack.name, Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp))
                    }
                    items(pack.stickers, key = { pack.id + "/" + it.shortcode }) { s ->
                        AsyncImage(
                            model = remember(s.url) { MxcImage(url = s.url, size = 192) },
                            contentDescription = s.body,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clip(CtrlShapes.Control)
                                .combinedClickable(
                                    onClick = { onPick(s) },
                                    onLongClick = if (pack.mine) ({ removing = s }) else null,
                                ),
                        )
                    }
                }
            }
        }
        Text(
            stringResource(R.string.stickers_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        )
    }

    removing?.let { s ->
        ConfirmDialog(
            stringResource(R.string.stickers_remove_title),
            stringResource(R.string.stickers_remove_body),
            stringResource(R.string.remove),
            destructive = true,
            onDismiss = { removing = null },
            onConfirm = {
                scope.launch {
                    runCatching { service.removeFromMine(s.shortcode) }
                        .onSuccess { reload++ }
                        .onFailure { onError(it.userMessage()) }
                }
            },
        )
    }
}
