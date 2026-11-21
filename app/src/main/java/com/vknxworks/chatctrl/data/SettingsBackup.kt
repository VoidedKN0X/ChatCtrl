package com.vknxworks.chatctrl.data

import org.json.JSONObject

/**
 * A backup of ChatCtrl's own settings as a small JSON file the user keeps: how chats look and behave, and the
 * privacy switches. Nothing about the account is in it: no sign-in, tokens, keys, PIN, server names, drafts
 * or push registration. The app lock's settings (and "Hide in screenshots" / "Hide in recent apps") stay out
 * too, so a file can never weaken the lock, and so does "Verified devices only" and whether notifications
 * are on (that one decides how the saved sign-in is protected while the app is locked).
 *
 * The privacy switches only ever move to the more private side on import: a file can turn private read
 * receipts on, but not off.
 */
object SettingsBackup {
    const val FORMAT = "chatctrl-settings"
    const val VERSION = 1
    /** A settings file is well under a kilobyte; anything far bigger isn't one. */
    const val MAX_BYTES = 64 * 1024

    /** Thrown for a file that isn't a ChatCtrl settings backup, or one from a newer, incompatible version. */
    class FormatException(val newer: Boolean) : Exception()

    /** One setting an import would change: its key, and the old and new value as stored. */
    data class Change(val key: String, val from: String, val to: String)

    const val BUBBLE_STYLE = "bubbleStyle"
    const val CHAT_AVATARS = "chatAvatars"
    const val DOUBLE_TAP = "doubleTapReaction"
    const val SHOW_READ_RECEIPTS = "showReadReceipts"
    const val SHOW_ROOM_EVENTS = "showRoomEvents"
    const val SEND_ON_ENTER = "sendOnEnter"
    const val LINK_PREVIEWS = "linkPreviews"
    const val LINK_PREVIEWS_ENCRYPTED = "linkPreviewsEncrypted"
    const val PRIVATE_READ_RECEIPTS = "privateReadReceipts"
    const val SEND_TYPING = "sendTyping"

    /** The settings as they are now, by key. Booleans as "true"/"false". */
    fun current(prefs: Prefs): LinkedHashMap<String, String> = linkedMapOf(
        BUBBLE_STYLE to prefs.bubbleStyle.value.name,
        CHAT_AVATARS to prefs.chatAvatars.value.toString(),
        DOUBLE_TAP to prefs.doubleTapReaction.value,
        SHOW_READ_RECEIPTS to prefs.showReadReceipts.value.toString(),
        SHOW_ROOM_EVENTS to prefs.showStateEvents.value.toString(),
        SEND_ON_ENTER to prefs.sendOnEnter.value.toString(),
        LINK_PREVIEWS to prefs.linkPreviews.value.toString(),
        LINK_PREVIEWS_ENCRYPTED to prefs.linkPreviewsEncrypted.value.toString(),
        PRIVATE_READ_RECEIPTS to prefs.privateReadReceipts.value.toString(),
        SEND_TYPING to prefs.sendTyping.value.toString(),
    )

    private val booleans = setOf(
        CHAT_AVATARS, SHOW_READ_RECEIPTS, SHOW_ROOM_EVENTS, SEND_ON_ENTER, LINK_PREVIEWS,
        LINK_PREVIEWS_ENCRYPTED, PRIVATE_READ_RECEIPTS, SEND_TYPING,
    )

    /** The value a privacy switch may be imported to (its more private side); other values are ignored. */
    private val onlyTowards = mapOf(
        LINK_PREVIEWS_ENCRYPTED to "false",
        PRIVATE_READ_RECEIPTS to "true",
        SEND_TYPING to "false",
    )

    fun export(prefs: Prefs, appVersion: String, createdAt: String): String {
        val settings = JSONObject()
        current(prefs).forEach { (k, v) -> if (k in booleans) settings.put(k, v == "true") else settings.put(k, v) }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("appVersion", appVersion)
            .put("createdAt", createdAt)
            .put("settings", settings)
            .toString(2)
    }

    /** Reads a backup file into the settings it holds (only keys and values this version knows). */
    fun parse(text: String): Map<String, String> {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: throw FormatException(newer = false)
        if (root.optString("format") != FORMAT) throw FormatException(newer = false)
        if (root.optInt("version", 0) > VERSION) throw FormatException(newer = true)
        val s = root.optJSONObject("settings") ?: throw FormatException(newer = false)
        val out = LinkedHashMap<String, String>()
        for (key in booleans) {
            when (val v = s.opt(key)) {
                true -> out[key] = "true"
                false -> out[key] = "false"
                else -> Unit
            }
        }
        (s.opt(BUBBLE_STYLE) as? String)?.let { name ->
            if (BubbleStyle.entries.any { it.name == name }) out[BUBBLE_STYLE] = name
        }
        // An emoji or nothing (double-tap off): short, and no control characters.
        (s.opt(DOUBLE_TAP) as? String)?.let { emoji ->
            if (emoji.length <= 16 && emoji.none { it.isISOControl() || it.isWhitespace() }) out[DOUBLE_TAP] = emoji
        }
        return out
    }

    /** What applying [file] would change, in the order of [current]. Privacy switches only move one way. */
    fun changes(now: Map<String, String>, file: Map<String, String>): List<Change> = now.mapNotNull { (key, old) ->
        val new = file[key] ?: return@mapNotNull null
        if (new == old) return@mapNotNull null
        val allowed = onlyTowards[key]
        if (allowed != null && new != allowed) return@mapNotNull null
        Change(key, old, new)
    }

    fun apply(prefs: Prefs, changes: List<Change>) {
        for (c in changes) {
            when (c.key) {
                BUBBLE_STYLE -> BubbleStyle.entries.firstOrNull { it.name == c.to }?.let(prefs::setBubbleStyle)
                CHAT_AVATARS -> prefs.setChatAvatars(c.to == "true")
                DOUBLE_TAP -> prefs.setDoubleTapReaction(c.to)
                SHOW_READ_RECEIPTS -> prefs.setShowReadReceipts(c.to == "true")
                SHOW_ROOM_EVENTS -> prefs.setShowStateEvents(c.to == "true")
                SEND_ON_ENTER -> prefs.setSendOnEnter(c.to == "true")
                LINK_PREVIEWS -> prefs.setLinkPreviews(c.to == "true")
                LINK_PREVIEWS_ENCRYPTED -> if (c.to == "false") prefs.setLinkPreviewsEncrypted(false)
                PRIVATE_READ_RECEIPTS -> if (c.to == "true") prefs.setPrivateReadReceipts(true)
                SEND_TYPING -> if (c.to == "false") prefs.setSendTyping(false)
            }
        }
    }
}
