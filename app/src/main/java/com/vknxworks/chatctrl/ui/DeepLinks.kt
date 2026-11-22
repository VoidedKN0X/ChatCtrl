package com.vknxworks.chatctrl.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** Content another app shared into ChatCtrl, waiting for the user to pick a room. */
data class SharedContent(
    val text: String?,
    val uri: Uri?,
    val more: List<Uri> = emptyList(),
    /** Files left out because more than [MAX_FILES_AT_ONCE] were shared at once. */
    val leftOut: Int = 0,
) {
    /** Every shared file: [uri] first, then the rest when several were shared at once. */
    val uris: List<Uri> get() = listOfNotNull(uri) + more
}

/** The most files sent in one go (shared from another app or picked in a chat). */
const val MAX_FILES_AT_ONCE = 20

/** Things that arrive from outside the UI (intents, notifications) for the screens to act on. */
object DeepLinks {
    /** OAuth/SSO redirect back from the browser. */
    val authRedirect = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    /** A room to open, from a notification or a matrix.to link. */
    val openRoom = MutableStateFlow<String?>(null)
    /** A room ID or alias from a link that may need joining first. */
    val joinTarget = MutableStateFlow<Pair<String, List<String>>?>(null)
    /** A user from a link, to open their profile. */
    val openUser = MutableStateFlow<String?>(null)
    val share = MutableStateFlow<SharedContent?>(null)
}

/**
 * Whether a URI handed to us by another app (share sheet) is safe to read and upload. Only content: URIs from
 * other apps' providers are accepted: file: URIs and our own providers (FileProvider etc.) could otherwise be
 * used to make ChatCtrl read and send its own private files, such as the session databases.
 */
fun isForeignContentUri(context: Context, uri: Uri): Boolean {
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) return false
    // "content://0@<authority>" still reaches <authority> (the number is a user id), so look past it.
    val authority = uri.authority?.substringAfterLast('@')?.takeIf { it.isNotBlank() } ?: return false
    val own = context.packageName
    if (authority == own || authority.startsWith("$own.")) return false
    val provider = runCatching { context.packageManager.resolveContentProvider(authority, 0) }.getOrNull()
    if (provider != null && provider.packageName == own) return false
    // Reject paths that point back into this app's private storage through another provider (e.g. a file manager).
    val path = uri.path.orEmpty()
    val privateDirs = listOfNotNull(context.dataDir?.absolutePath, context.filesDir?.parentFile?.absolutePath, "/data/data/$own", "/data/user/0/$own")
    if (privateDirs.any { it.isNotBlank() && path.contains(it) }) return false
    return true
}

/**
 * The files another app shared, in the order it gave them: the EXTRA_STREAM extra (one for SEND, a list for
 * SEND_MULTIPLE) and, when that is missing, the intent's ClipData (some apps only fill that in). Pictures, videos
 * and other files can be mixed. Only [isForeignContentUri] files are kept, each once, at most [MAX_FILES_AT_ONCE];
 * the second value is how many were left out over that limit.
 */
fun sharedStreams(context: Context, intent: android.content.Intent): Pair<List<Uri>, Int> {
    val fromExtra: List<Uri> = if (intent.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
        androidx.core.content.IntentCompat.getParcelableArrayListExtra(intent, android.content.Intent.EXTRA_STREAM, Uri::class.java).orEmpty().filterNotNull()
    } else {
        listOfNotNull(androidx.core.content.IntentCompat.getParcelableExtra(intent, android.content.Intent.EXTRA_STREAM, Uri::class.java))
    }
    val all = fromExtra.ifEmpty {
        val clip = intent.clipData
        if (clip == null) emptyList() else (0 until clip.itemCount).mapNotNull { clip.getItemAt(it)?.uri }
    }
    return com.vknxworks.chatctrl.matrix.AttachmentPlan.take(all.filter { isForeignContentUri(context, it) }, MAX_FILES_AT_ONCE)
}
