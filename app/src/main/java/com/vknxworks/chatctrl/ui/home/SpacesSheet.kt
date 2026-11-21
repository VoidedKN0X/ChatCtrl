package com.vknxworks.chatctrl.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.matrix.SpaceBrowser
import com.vknxworks.chatctrl.matrix.SpaceChild
import com.vknxworks.chatctrl.matrix.SpaceEntry
import com.vknxworks.chatctrl.matrix.userMessage
import com.vknxworks.chatctrl.ui.Avatar
import com.vknxworks.chatctrl.ui.components.CtrlSheet
import kotlinx.coroutines.launch

/**
 * Pick a space to show only its chats in the chat list ("All chats" clears it). The globe button next to a
 * space lists everything in it, including rooms you haven't joined yet.
 */
@Composable
fun SpacesSheet(
    spaces: List<SpaceEntry>,
    selectedId: String?,
    /** How many chats of the list are in each space, by space ID. */
    chatCounts: Map<String, Int>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onBrowse: (SpaceEntry) -> Unit,
) {
    CtrlSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.spaces), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item("all") {
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.spaces_all_chats), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (selectedId == null) Icon(Icons.Outlined.Check, stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
                }
            }
            items(spaces, key = { it.level.toString() + it.id }) { s ->
                val count = chatCounts[s.id] ?: 0
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(s.id) }
                        .padding(start = if (s.level > 0) 48.dp else 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(s.name, s.avatarUrl, s.id, size = if (s.level > 0) 32.dp else 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (s.id == selectedId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            pluralStringResource(R.plurals.hero_chats, count, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (s.id == selectedId) Icon(Icons.Outlined.Check, stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { onBrowse(s) }) {
                        Icon(Icons.Outlined.TravelExplore, stringResource(R.string.spaces_browse), Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Everything directly inside a space, as the homeserver lists it: open what you're in, join what you're not. */
@Composable
fun SpaceContentsSheet(
    browser: SpaceBrowser,
    space: SpaceEntry,
    /** Rooms you are in, to tell "open" from "join" even before the server's list catches up. */
    joinedRoomIds: Set<String>,
    onDismiss: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenSpace: (SpaceEntry) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var children by remember(space.id) { mutableStateOf<List<SpaceChild>?>(null) }
    var failed by remember(space.id) { mutableStateOf<String?>(null) }
    var joining by remember(space.id) { mutableStateOf<String?>(null) }
    var reload by remember(space.id) { mutableIntStateOf(0) }
    LaunchedEffect(space.id, reload) {
        failed = null
        runCatching { browser.children(space.id) }
            .onSuccess { children = it }
            .onFailure { children = emptyList(); failed = it.userMessage().ifBlank { context.getString(R.string.spaces_load_failed) } }
    }
    CtrlSheet(onDismissRequest = onDismiss) {
        Text(space.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        val list = children
        when {
            list == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            failed != null -> Text(failed.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            list.isEmpty() -> Text(stringResource(R.string.spaces_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            else -> LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(list, key = { it.roomId }) { c ->
                    val joined = c.joined || c.roomId in joinedRoomIds
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(enabled = joined) {
                                if (c.isSpace) onOpenSpace(SpaceEntry(c.roomId, c.name, c.avatarUrl, 1, emptySet())) else onOpenRoom(c.roomId)
                            }
                            .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(c.name, c.avatarUrl, c.roomId, size = 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val members = pluralStringResource(R.plurals.members_count, c.members.toInt(), c.members.toInt())
                            Text(
                                if (c.isSpace) stringResource(R.string.spaces_space_line, members) else (c.topic?.let { "$members · $it" } ?: members),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        when {
                            joining == c.roomId -> CircularProgressIndicator(Modifier.padding(horizontal = 16.dp).size(20.dp), strokeWidth = 2.dp)
                            joined -> Text(stringResource(R.string.spaces_joined), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
                            else -> TextButton(enabled = joining == null, onClick = {
                                joining = c.roomId
                                scope.launch {
                                    runCatching { browser.join(c) }
                                        .onSuccess { reload++ }
                                        .onFailure { Toast.makeText(context, it.userMessage().ifBlank { context.getString(R.string.spaces_join_failed) }, Toast.LENGTH_LONG).show() }
                                    joining = null
                                }
                            }) { Text(stringResource(R.string.spaces_join)) }
                        }
                    }
                }
            }
        }
        Text(
            stringResource(R.string.spaces_contents_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        )
    }
}
