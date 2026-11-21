package com.vknxworks.chatctrl.matrix

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.matrix.rustcomponents.sdk.Room

/** One sticker from an image pack: where the picture is (a plain mxc:// address) and how to describe it. */
data class Sticker(
    val shortcode: String,
    val url: String,
    val body: String,
    /** The image's `info` object as the pack has it (size, type), or null. */
    val infoJson: String?,
)

data class StickerPack(
    /** "mine" for your own pack, otherwise room ID + state key. */
    val id: String,
    val name: String,
    val mine: Boolean,
    val stickers: List<Sticker>,
)

/**
 * Sticker packs as other Matrix apps (FluffyChat, Cinny, Nheko) store them: "image packs" from MSC2545, under
 * their unstable names. Your own pack is the account data event `im.ponies.user_emotes`; a room's packs are
 * `im.ponies.room_emotes` state events; `im.ponies.emote_rooms` in your account data lists the room packs you
 * turned on everywhere. A sticker is sent as an `m.sticker` event that points at the pack's picture, so nothing
 * is uploaded when sending. Pack pictures are ordinary, unencrypted media on the homeserver.
 */
class StickerService(private val session: MatrixSession) {

    /** Your own pack, the packs you enabled from other rooms, and this room's default pack. */
    suspend fun packs(roomId: String): List<StickerPack> = withContext(Dispatchers.IO) {
        val out = ArrayList<StickerPack>()
        val mine = userPack()
        out += StickerPack("mine", Strings.str(R.string.stickers_my_pack), true, parseImages(mine))
        // Room packs: the ones enabled in account data, then this room's default pack ("" state key).
        val wanted = LinkedHashSet<Pair<String, String>>()
        runCatching {
            session.client.accountData(EMOTE_ROOMS)?.let { JSONObject(it) }?.optJSONObject("rooms")?.let { rooms ->
                for (id in rooms.keys()) {
                    val keys = rooms.optJSONObject(id) ?: continue
                    for (k in keys.keys()) wanted += id to k
                }
            }
        }
        wanted += roomId to ""
        for ((id, key) in wanted.take(MAX_ROOM_PACKS)) {
            val content = runCatching {
                val (code, json) = MatrixHttp.request(
                    session, "GET",
                    "/_matrix/client/v3/rooms/${MatrixHttp.enc(id)}/state/$ROOM_EMOTES/${MatrixHttp.enc(key)}",
                    returnErrors = true,
                )
                if (code in 200..299) json else null
            }.getOrNull() ?: continue
            val stickers = parseImages(content)
            if (stickers.isEmpty()) continue
            val name = content.optJSONObject("pack")?.optString("display_name")?.takeIf { it.isNotBlank() }
                ?: session.rooms.value.firstOrNull { it.roomId == id }?.name
                ?: key.ifBlank { Strings.str(R.string.stickers_room_pack) }
            out += StickerPack("$id|$key", name, false, stickers)
        }
        out
    }

    private suspend fun userPack(): JSONObject =
        runCatching { session.client.accountData(USER_EMOTES)?.let { JSONObject(it) } }.getOrNull() ?: JSONObject()

    /** The pictures of a pack that may be used as stickers (a pack or picture can be limited to emoticons). */
    private fun parseImages(pack: JSONObject): List<Sticker> {
        val images = pack.optJSONObject("images") ?: return emptyList()
        val packUsage = pack.optJSONObject("pack")?.optJSONArray("usage").strings()
        val out = ArrayList<Sticker>()
        for (code in images.keys()) {
            val img = images.optJSONObject(code) ?: continue
            val url = img.optString("url").takeIf { it.startsWith("mxc://") && it.length <= 512 } ?: continue
            val usage = img.optJSONArray("usage").strings().ifEmpty { packUsage }
            if (usage.isNotEmpty() && "sticker" !in usage) continue
            out += Sticker(
                shortcode = code,
                url = url,
                body = img.optString("body").ifBlank { code },
                infoJson = img.optJSONObject("info")?.toString(),
            )
            if (out.size >= MAX_STICKERS_PER_PACK) break
        }
        return out
    }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }

    /** Sends [sticker] to [room] (encrypted like any other message when the room is encrypted). */
    suspend fun send(room: Room, sticker: Sticker, replyTo: String?, threadRoot: String?) = withContext(Dispatchers.IO) {
        val content = JSONObject()
            .put("body", sticker.body)
            .put("url", sticker.url)
            .put("info", sticker.infoJson?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject())
        when {
            threadRoot != null -> content.put(
                "m.relates_to",
                JSONObject()
                    .put("rel_type", "m.thread")
                    .put("event_id", threadRoot)
                    .put("is_falling_back", replyTo == null)
                    .put("m.in_reply_to", JSONObject().put("event_id", replyTo ?: threadRoot)),
            )
            replyTo != null -> content.put("m.relates_to", JSONObject().put("m.in_reply_to", JSONObject().put("event_id", replyTo)))
        }
        room.sendRaw("m.sticker", content.toString())
        Unit
    }

    /** Adds a picture that is already on a homeserver (a sticker someone sent) to your own pack. */
    suspend fun addToMine(url: String, body: String, width: Int, height: Int, mime: String?) = withContext(Dispatchers.IO) {
        if (!url.startsWith("mxc://")) error(Strings.str(R.string.stickers_add_failed))
        val pack = userPack()
        val images = pack.optJSONObject("images") ?: JSONObject().also { pack.put("images", it) }
        for (code in images.keys()) {
            if (images.optJSONObject(code)?.optString("url") == url) return@withContext
        }
        val info = JSONObject()
        if (width > 0) info.put("w", width)
        if (height > 0) info.put("h", height)
        if (!mime.isNullOrBlank()) info.put("mimetype", mime)
        val image = JSONObject().put("url", url).put("body", body.take(100)).put("info", info).put("usage", JSONArray().put("sticker"))
        images.put(freeShortcode(images, body), image)
        session.client.setAccountData(USER_EMOTES, pack.toString())
    }

    /** Uploads a picture from the phone (not encrypted: pack pictures never are) and adds it to your own pack. */
    suspend fun addFromFile(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        if (!com.vknxworks.chatctrl.ui.isForeignContentUri(context, uri)) error(Strings.str(R.string.this_file_can_t_be_sent))
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: error(Strings.str(R.string.stickers_not_a_picture))
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }?.substringBeforeLast('.').orEmpty().ifBlank { "sticker" }
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                if (out.size() + n > MAX_STICKER_BYTES) error(Strings.str(R.string.stickers_too_big))
                out.write(buf, 0, n)
            }
            out.toByteArray()
        } ?: error(Strings.str(R.string.couldn_t_read_the_file))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val url = session.client.uploadMedia(mime, bytes, null)
        addToMine(url, name, bounds.outWidth, bounds.outHeight, mime)
    }

    /** Takes a sticker out of your own pack. The picture itself stays on the homeserver. */
    suspend fun removeFromMine(shortcode: String) = withContext(Dispatchers.IO) {
        val pack = userPack()
        val images = pack.optJSONObject("images") ?: return@withContext
        if (!images.has(shortcode)) return@withContext
        images.remove(shortcode)
        session.client.setAccountData(USER_EMOTES, pack.toString())
    }

    /** A shortcode other apps accept (letters, digits, "-" and "_") that the pack doesn't use yet. */
    private fun freeShortcode(images: JSONObject, body: String): String {
        val base = body.lowercase().replace(Regex("[^a-z0-9_-]+"), "_").trim('_').take(40).ifBlank { "sticker" }
        if (!images.has(base)) return base
        var n = 2
        while (images.has("${base}_$n")) n++
        return "${base}_$n"
    }

    private companion object {
        const val USER_EMOTES = "im.ponies.user_emotes"
        const val ROOM_EMOTES = "im.ponies.room_emotes"
        const val EMOTE_ROOMS = "im.ponies.emote_rooms"
        const val MAX_ROOM_PACKS = 30
        const val MAX_STICKERS_PER_PACK = 500
        const val MAX_STICKER_BYTES = 2 * 1024 * 1024
    }
}
