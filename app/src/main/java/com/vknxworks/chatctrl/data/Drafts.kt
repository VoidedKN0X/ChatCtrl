package com.vknxworks.chatctrl.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Which chats have an unsent draft and its first line, so the chat list can show "Draft: …". The draft
 * itself is stored by the SDK in the account's encrypted database; this is only a local index.
 */
class Drafts(private val context: Context) {
    // Opened on first use, so building this at app start reads nothing from disk.
    private val sp by lazy { context.getSharedPreferences("drafts", Context.MODE_PRIVATE) }

    /**
     * Empty until the saved index is read: [warm] does that in the background right after the app starts, so the
     * Keystore decrypt isn't done on the main thread before the first frame. The chat list just shows its
     * "Draft: …" lines once it's in.
     */
    private val _all = MutableStateFlow<Map<String, String>>(emptyMap())
    val all: StateFlow<Map<String, String>> = _all.asStateFlow()

    private val lock = Any()
    /** The saved index is in [_all] (or was cleared): nothing saved can be overwritten or brought back any more. */
    private var loaded = false

    /** Reads the saved index, once. Slow the first time (a Keystore decrypt): meant for a background thread. */
    fun warm() {
        synchronized(lock) {
            if (loaded) return
            _all.value = load()
            loaded = true
        }
    }

    // The index holds the start of unsent messages, so it is encrypted with the Keystore key (SecureBox).
    private fun load(): Map<String, String> = runCatching {
        val raw = sp.getString(KEY_ENC, null)?.let { SecureBox.decrypt(it) }
            ?: sp.getString(KEY_PLAIN, null)
            ?: "{}"
        val o = JSONObject(raw)
        val map = o.keys().asSequence().associateWith { o.getString(it) }
        // Older versions stored the index in plain text: encrypt it once and drop the plain copy.
        if (sp.contains(KEY_PLAIN)) runCatching { save(map) }
        map
    }.getOrDefault(emptyMap())

    private fun save(map: Map<String, String>) {
        val json = JSONObject(map as Map<*, *>).toString()
        sp.edit().putString(KEY_ENC, SecureBox.encrypt(json)).remove(KEY_PLAIN).apply()
    }

    fun set(roomId: String, text: String?) {
        // Never before the saved index is in: saving a map without it would drop the other chats' draft lines.
        warm()
        val next = _all.value.toMutableMap()
        if (text.isNullOrBlank()) next.remove(roomId) else next[roomId] = text.lineSequence().first().take(120)
        if (next == _all.value) return
        _all.value = next
        runCatching { save(next) }
    }

    fun clearAll() {
        // After this a late [warm] must not bring the old index back.
        synchronized(lock) { loaded = true }
        _all.value = emptyMap()
        // Written straight away: this runs during sign-out, and the app may be closed right after.
        sp.edit().clear().commit()
    }

    private companion object {
        const val KEY_PLAIN = "index"
        const val KEY_ENC = "index_enc"
    }
}
