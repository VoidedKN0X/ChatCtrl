package com.vknxworks.chatctrl.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.vknxworks.chatctrl.ChatCtrlApp
import com.vknxworks.chatctrl.MainActivity
import com.vknxworks.chatctrl.R
import com.vknxworks.chatctrl.matrix.ConnectionLog
import com.vknxworks.chatctrl.ui.location.GeoSpot
import com.vknxworks.chatctrl.ui.location.Locator
import com.vknxworks.chatctrl.ui.location.toGeoUri
import com.vknxworks.chatctrl.ui.timeOfDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps sharing your live location while ChatCtrl is in the background or the screen is off: a foreground service
 * of type "location" with an ongoing notification ("Sharing your live location", with a Stop button). It asks
 * Android's own location service for a position about every 15 seconds (no Google Play services) and hands each
 * one to [LiveLocation.send]. It keeps the Matrix sync running while it shares, like a call does, and stops itself
 * when the last share ends.
 */
class LiveLocationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + com.vknxworks.chatctrl.matrix.LogAndContinue)
    private var listener: LocationListener? = null
    private var syncHold: Job? = null
    private var started = false
    /** Start requests whose "sharing until …" event is still being sent. */
    private var pending = 0
    private val positions = Channel<String>(Channel.CONFLATED)
    private var lastSentAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // One sender, so a slow connection can't pile up positions: only the newest waiting one is sent.
        scope.launch {
            for (geo in positions) {
                LiveLocation.send(geo)
                lastSentAt = SystemClock.elapsedRealtime()
            }
        }
        // The notification follows the list of shares; when the last one is gone, the service ends.
        scope.launch {
            LiveLocation.shares.collect { shares ->
                if (shares.isEmpty()) {
                    // Only once a start request has made this a foreground service (see onStartCommand).
                    if (pending == 0 && started) finish()
                } else if (started) {
                    notifyOngoing(shares.values)
                }
            }
        }
        // Shares end by themselves at their end time.
        scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val shares = LiveLocation.shares.value.values
                shares.filter { it.endsAt <= now }.forEach { LiveLocation.stop(it.roomId) }
                val next = LiveLocation.shares.value.values.minOfOrNull { it.endsAt }
                delay(((next ?: (now + 30_000L)) - now).coerceIn(1_000L, 30_000L))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val roomId = intent.getStringExtra(EXTRA_ROOM_ID)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                if (!goForeground()) {
                    LiveLocation.report(getString(R.string.live_location_start_failed, getString(R.string.live_location_no_foreground)))
                    if (!LiveLocation.active) finish()
                    return START_NOT_STICKY
                }
                if (roomId.isNullOrBlank() || duration <= 0L) {
                    if (!LiveLocation.active) finish()
                    return START_NOT_STICKY
                }
                pending++
                scope.launch {
                    val ok = LiveLocation.begin(roomId, duration)
                    pending--
                    if (ok) {
                        holdSync()
                        startUpdates()
                        notifyOngoing(LiveLocation.shares.value.values)
                        // The first position right away, instead of waiting for the next update.
                        Locator.current(this@LiveLocationService)?.let { positions.trySend(it.toGeoUri()) }
                    } else if (!LiveLocation.active && pending == 0) {
                        finish()
                    }
                }
            }
            ACTION_STOP_ALL -> scope.launch {
                LiveLocation.stopAll()
                if (!LiveLocation.active && pending == 0) finish()
            }
            else -> if (!LiveLocation.active && pending == 0) finish()
        }
        return START_NOT_STICKY
    }

    private fun goForeground(): Boolean {
        if (started) return true
        val shares = LiveLocation.shares.value.values
        started = runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, ongoing(shares), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        }.onFailure { ConnectionLog.log("live location: foreground service refused (${it.javaClass.simpleName})") }.isSuccess
        return started
    }

    /** Syncing keeps running while sharing (the SDK needs the room's current share to send positions). */
    private fun holdSync() {
        if (syncHold?.isActive == true) return
        val session = ChatCtrlApp.graph.session ?: return
        syncHold = scope.launch { session.withBackgroundSync { awaitCancellation() } }
    }

    @Suppress("MissingPermission")
    private fun startUpdates() {
        if (listener != null || !Locator.hasAny(this)) return
        val lm = getSystemService(LocationManager::class.java) ?: return
        val precise = Locator.hasPrecise(this)
        val request = LocationRequest.Builder(INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            .setQuality(if (precise) LocationRequest.QUALITY_HIGH_ACCURACY else LocationRequest.QUALITY_BALANCED_POWER_ACCURACY)
            .build()
        val l = LocationListener { loc -> onPosition(loc) }
        // Android's own fused provider first, then satellite (needs precise location), then network.
        val candidates = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in candidates) {
            if (provider == LocationManager.GPS_PROVIDER && !precise) continue
            val ok = runCatching {
                if (!lm.hasProvider(provider) || !lm.isProviderEnabled(provider)) error("unavailable")
                lm.requestLocationUpdates(provider, request, mainExecutor, l)
            }.isSuccess
            if (ok) {
                listener = l
                ConnectionLog.log("live location: updates from $provider")
                return
            }
        }
        ConnectionLog.log("live location: no location provider available")
        LiveLocation.report(getString(R.string.location_off))
    }

    private fun onPosition(loc: Location) {
        if (SystemClock.elapsedRealtime() - lastSentAt < MIN_INTERVAL_MS) return
        positions.trySend(loc.toGeoUri())
    }

    private fun Location.toGeoUri(): String = GeoSpot(latitude, longitude, if (hasAccuracy()) accuracy else null).toGeoUri()

    private fun notifyOngoing(shares: Collection<LiveLocation.Share>) {
        if (!started) return
        runCatching { getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, ongoing(shares)) }
    }

    /** Names no chat, so it's safe on the lock screen. */
    private fun ongoing(shares: Collection<LiveLocation.Share>): Notification {
        val open = PendingIntent.getActivity(
            this, 0x10C1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 0x10C2,
            Intent(this, LiveLocationService::class.java).setAction(ACTION_STOP_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val until = shares.maxOfOrNull { it.endsAt }
        val text = if (shares.isEmpty() || until == null) getString(R.string.live_location_notification_starting)
        else resources.getQuantityString(R.plurals.live_location_notification_text, shares.size, shares.size, timeOfDay(until))
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(getString(R.string.live_location_notification_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_LOCATION_SHARING)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.live_location_stop), stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun finish() {
        listener?.let { l -> runCatching { getSystemService(LocationManager::class.java)?.removeUpdates(l) } }
        listener = null
        syncHold?.cancel()
        syncHold = null
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        started = false
        stopSelf()
        if (LiveLocation.sealWhenDone) {
            LiveLocation.sealWhenDone = false
            // The app locked while sharing: shut the session down now, as it would have been.
            if (!com.vknxworks.chatctrl.ui.lock.AppLock.unlocked) ChatCtrlApp.graph.onLocked()
        }
    }

    override fun onDestroy() {
        listener?.let { l -> runCatching { getSystemService(LocationManager::class.java)?.removeUpdates(l) } }
        listener = null
        syncHold?.cancel()
        positions.close()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.vknxworks.chatctrl.location.START"
        const val ACTION_STOP_ALL = "com.vknxworks.chatctrl.location.STOP_ALL"
        const val EXTRA_ROOM_ID = "room_id"
        const val EXTRA_DURATION = "duration_ms"
        const val CHANNEL = "live_location"
        private const val NOTIFICATION_ID = 0x10C0
        private const val INTERVAL_MS = 15_000L
        private const val MIN_INTERVAL_MS = 10_000L

        fun createChannel(context: Context) {
            runCatching {
                context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                    NotificationChannel(CHANNEL, context.getString(R.string.channel_live_location), NotificationManager.IMPORTANCE_LOW).apply {
                        setSound(null, null)
                    },
                )
            }
        }
    }
}
