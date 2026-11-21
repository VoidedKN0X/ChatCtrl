package com.vknxworks.chatctrl.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.vknxworks.chatctrl.BuildConfig
import com.vknxworks.chatctrl.ChatCtrlApp
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.data.BubbleStyle
import com.vknxworks.chatctrl.data.SettingsBackup
import com.vknxworks.chatctrl.ui.ConfirmDialog
import com.vknxworks.chatctrl.ui.Group
import com.vknxworks.chatctrl.ui.GroupRow
import com.vknxworks.chatctrl.ui.lock.AppLock
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings › Back up and restore: ChatCtrl's own settings as a JSON file in a place the user picks. See
 * [SettingsBackup] for what is in it (and what never is). Restoring lists what will change and asks first.
 */
@Composable
fun BackupGroup() {
    val context = LocalContext.current
    val prefs = ChatCtrlApp.graph.prefs
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<List<SettingsBackup.Change>?>(null) }

    fun toast(id: Int) = Toast.makeText(context, id, Toast.LENGTH_LONG).show()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val text = SettingsBackup.export(prefs, BuildConfig.VERSION_NAME, OffsetDateTime.now().toString())
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("no stream")
                }
            }.isSuccess
            toast(if (ok) R.string.backup_saved else R.string.backup_save_failed)
        }
    }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            if (out.size() > SettingsBackup.MAX_BYTES) throw SettingsBackup.FormatException(newer = false)
                        }
                        out.toString(Charsets.UTF_8.name())
                    } ?: throw SettingsBackup.FormatException(newer = false)
                }
                SettingsBackup.changes(SettingsBackup.current(prefs), SettingsBackup.parse(text))
            }.onSuccess { changes ->
                if (changes.isEmpty()) toast(R.string.backup_nothing_to_change) else pending = changes
            }.onFailure { e ->
                toast(if (e is SettingsBackup.FormatException && e.newer) R.string.backup_too_new else R.string.backup_not_valid)
            }
        }
    }

    Group(stringResource(R.string.backup_title), footer = stringResource(R.string.backup_footer)) {
        GroupRow(
            stringResource(R.string.backup_save),
            stringResource(R.string.backup_save_sub),
            icon = Icons.Outlined.FileDownload,
            onClick = { AppLock.expectReturn(); save.launch("chatctrl-settings-${LocalDate.now()}.json") },
        )
        GroupRow(
            stringResource(R.string.backup_restore),
            stringResource(R.string.backup_restore_sub),
            icon = Icons.Outlined.FileUpload,
            onClick = { AppLock.expectReturn(); open.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
        )
    }

    pending?.let { changes ->
        val lines = changes.map { c -> stringResource(R.string.backup_change_line, settingName(c.key), shown(c.key, c.from), shown(c.key, c.to)) }
        ConfirmDialog(
            stringResource(R.string.backup_restore_title),
            stringResource(R.string.backup_restore_body, lines.joinToString("\n")),
            stringResource(R.string.backup_restore_confirm),
            onDismiss = { pending = null },
            onConfirm = {
                SettingsBackup.apply(prefs, changes)
                toast(R.string.backup_restored)
            },
        )
    }
}

@Composable
private fun settingName(key: String): String = stringResource(
    when (key) {
        SettingsBackup.BUBBLE_STYLE -> R.string.bubbles
        SettingsBackup.CHAT_AVATARS -> R.string.chat_avatars
        SettingsBackup.DOUBLE_TAP -> R.string.double_tap
        SettingsBackup.SHOW_READ_RECEIPTS -> R.string.read_receipts
        SettingsBackup.SHOW_ROOM_EVENTS -> R.string.room_events
        SettingsBackup.SEND_ON_ENTER -> R.string.enter_sends
        SettingsBackup.LINK_PREVIEWS -> R.string.link_previews
        SettingsBackup.LINK_PREVIEWS_ENCRYPTED -> R.string.link_previews_e2ee
        SettingsBackup.PRIVATE_READ_RECEIPTS -> R.string.private_read_receipts
        else -> R.string.send_typing
    },
)

/** A stored value the way Settings shows it: on/off, the bubble style's name, the emoji. */
@Composable
private fun shown(key: String, value: String): String = when (key) {
    SettingsBackup.BUBBLE_STYLE -> BubbleStyle.entries.firstOrNull { it.name == value }?.label ?: value
    SettingsBackup.DOUBLE_TAP -> value.ifEmpty { stringResource(R.string.backup_value_off) }
    else -> stringResource(if (value == "true") R.string.backup_value_on else R.string.backup_value_off)
}
