package com.vknxworks.chatctrl.location

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.vknxworks.chatctrl.ChatCtrlApp
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.Strings
import com.vknxworks.chatctrl.matrix.ConnectionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.LiveLocationException

/**
 * Your own live location shares (MSC3489, the same as Element X), through the Matrix Rust SDK:
 * `Room.startLiveLocationShare` sends the "sharing until …" state event, `Room.sendLiveLocation` sends each position
 * (encrypted like any message in an encrypted chat) and `Room.stopLiveLocationShare` ends it. The positions come
 * from [LiveLocationService], which runs as a foreground service with a notification while anything is shared.
 *
 * Which chats are being shared to is kept in memory only. If Android ends the app, the shares simply end by
 * themselves at their end time on everyone's screen, and your own live location in the chat offers "Stop sharing".
 */
object LiveLocation {
    /** A chat you are sharing your live location with, until [endsAt] (wall clock, ms). */
    data class Share(val roomId: String, val startedAt: Long, val endsAt: Long)

    private val _shares = MutableStateFlow<Map<String, Share>>(emptyMap())
    val shares: StateFlow<Map<String, Share>> = _shares.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Problems to show in the chat screen (the service has no screen of its own). */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    /** Set when the app locked while sharing: the session is shut down once sharing ends (see AppGraph.onLocked). */
    @Volatile var sealWhenDone = false

    val active: Boolean get() = _shares.value.isNotEmpty()

    private val sendMutex = Mutex()

    /** The choices offered for how long to share. */
    val DURATIONS_MS = longArrayOf(15 * 60_000L, 60 * 60_000L, 8 * 60 * 60_000L)

    /** Starts sharing in [roomId] for [durationMs]. Call while the chat is on screen (Android requires that). */
    fun start(context: Context, roomId: String, durationMs: Long) {
        val intent = Intent(context, LiveLocationService::class.java)
            .setAction(LiveLocationService.ACTION_START)
            .putExtra(LiveLocationService.EXTRA_ROOM_ID, roomId)
            .putExtra(LiveLocationService.EXTRA_DURATION, durationMs)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { report(Strings.str(R.string.live_location_start_failed, it.javaClass.simpleName)) }
    }

    /**
     * Stops sharing in [roomId]. Also works for a share this phone no longer updates (the app was ended while
     * sharing): it then only tells the chat that the share has stopped.
     */
    fun stopFromUi(roomId: String) {
        ChatCtrlApp.graph.scope.launch { stop(roomId) }
    }

    internal fun report(message: String) {
        _errors.tryEmit(message)
    }

    /** Sends the "sharing until …" event for [roomId]. Returns false (and reports why) when that didn't work. */
    internal suspend fun begin(roomId: String, durationMs: Long): Boolean {
        val session = ChatCtrlApp.graph.session
        if (session == null) {
            report(Strings.str(R.string.live_location_start_failed, Strings.str(R.string.live_location_not_signed_in)))
            return false
        }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val room = session.getRoom(roomId) ?: error(Strings.str(R.string.live_location_no_room))
                room.use { it.startLiveLocationShare(durationMs.toULong()) }
            }
        }
        return result.fold(
            onSuccess = {
                val now = System.currentTimeMillis()
                _shares.update { it + (roomId to Share(roomId, now, now + durationMs)) }
                ConnectionLog.log("live location: started (${durationMs / 60_000} min)")
                true
            },
            onFailure = {
                ConnectionLog.log("live location: start failed (${it.javaClass.simpleName})")
                report(Strings.str(R.string.live_location_start_failed, it.message?.takeIf { m -> m.isNotBlank() } ?: it.javaClass.simpleName))
                false
            },
        )
    }

    /** Ends the share in [roomId]: no more positions are sent, and the chat is told it stopped. */
    internal suspend fun stop(roomId: String) {
        _shares.update { it - roomId }
        val session = ChatCtrlApp.graph.session ?: return
        val result = withContext(Dispatchers.IO) {
            runCatching { session.getRoom(roomId)?.use { it.stopLiveLocationShare() } }
        }
        result.exceptionOrNull()?.let { e ->
            // Already over (ended, or stopped on another device): nothing left to do.
            if (e is LiveLocationException.NotLive || e is LiveLocationException.NotFound) return
            ConnectionLog.log("live location: stop failed (${e.javaClass.simpleName})")
            report(Strings.str(R.string.live_location_stop_failed))
        }
    }

    /** Forgets every share without telling the server (the session is already gone); the service then ends. */
    fun clearLocal() {
        _shares.value = emptyMap()
    }

    /** Stops every share (signing out, or "Stop" in the notification). */
    suspend fun stopAll() {
        _shares.value.keys.toList().forEach { stop(it) }
    }

    /**
     * Sends [geoUri] to every chat that is still shared to. A chat whose share was stopped elsewhere (another of
     * your devices) or has run out is dropped; right after starting, the SDK may not have the new share yet, so
     * those errors are ignored for the first minute.
     */
    internal suspend fun send(geoUri: String) = sendMutex.withLock {
        val session = ChatCtrlApp.graph.session ?: return@withLock
        val now = System.currentTimeMillis()
        _shares.value.values.filter { it.endsAt > now }.forEach { share ->
            val result = withContext(Dispatchers.IO) {
                runCatching { session.getRoom(share.roomId)?.use { it.sendLiveLocation(geoUri) } }
            }
            val e = result.exceptionOrNull() ?: return@forEach
            val gone = e is LiveLocationException.NotLive || e is LiveLocationException.NotFound ||
                e is LiveLocationException.Redacted || e is LiveLocationException.Stripped
            if (gone && now - share.startedAt > GRACE_MS) {
                ConnectionLog.log("live location: share ended elsewhere (${e.javaClass.simpleName})")
                _shares.update { it - share.roomId }
            } else if (!gone) {
                ConnectionLog.log("live location: send failed (${e.javaClass.simpleName})")
            }
        }
    }

    private const val GRACE_MS = 60_000L
}
