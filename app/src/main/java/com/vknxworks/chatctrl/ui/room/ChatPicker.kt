package com.vknxworks.chatctrl.ui.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.matrix.MatrixSession
import com.vknxworks.chatctrl.matrix.RoomSummary
import com.vknxworks.chatctrl.ui.Avatar
import com.vknxworks.chatctrl.ui.components.CtrlSheet
import com.vknxworks.chatctrl.ui.components.CtrlTextField

/** A sheet listing the chats you are in (no invites, no spaces), with a search box, to pick one. */
@Composable
fun ChatPickerSheet(
    session: MatrixSession,
    title: String,
    /** A chat to leave out (the one you are in). */
    exceptRoomId: String? = null,
    onDismiss: () -> Unit,
    onPick: (RoomSummary) -> Unit,
) {
    val rooms by session.rooms.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val shown = remember(rooms, query) {
        val q = query.trim().lowercase()
        rooms.filter { !it.isSpace && !it.isInvite && it.roomId != exceptRoomId && (q.isEmpty() || q in it.searchText) }
    }
    CtrlSheet(onDismissRequest = onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        CtrlTextField(
            query, { query = it },
            placeholder = { Text(stringResource(R.string.search_chats_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        )
        if (shown.isEmpty()) {
            Text(
                stringResource(R.string.no_chats_found),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            )
        }
        LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(shown, key = { it.roomId }) { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(r) }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(r.name, r.avatarUrl, r.dmUserId ?: r.roomId, size = 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        (r.canonicalAlias ?: r.dmUserId)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
