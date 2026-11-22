package com.vknxworks.chatctrl

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import com.vknxworks.chatctrl.matrix.AppGraph
import com.vknxworks.chatctrl.matrix.MediaFetcher
import com.vknxworks.chatctrl.matrix.MediaFiles
import com.vknxworks.chatctrl.notify.Notifier
import kotlinx.coroutines.launch
import org.matrix.rustcomponents.sdk.LogLevel
import org.matrix.rustcomponents.sdk.TracingConfiguration
import org.matrix.rustcomponents.sdk.initPlatform

class ChatCtrlApp : Application(), SingletonImageLoader.Factory {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        // The restart helper process only relaunches the app; nothing else may start there.
        if (AppRestart.isRestartProcess()) return
        instance = this
        Strings.init(this)
        // Native SDK first: it needs the Context for TLS checks, then logging/runtime setup.
        runCatching { org.matrix.rustsdk.Android.init(this) }
        runCatching {
            initPlatform(
                config = TracingConfiguration(
                    logLevel = LogLevel.ERROR,
                    traceLogPacks = emptyList(),
                    extraTargets = emptyList(),
                    // The SDK's logs never go to logcat (they can name rooms, users, servers and media URLs),
                    // the same as Element X's release builds.
                    writeToStdoutOrSystem = false,
                    writeToFiles = null,
                    sentryConfig = null,
                ),
                useLightweightTokioRuntime = false,
            )
        }

        graph = AppGraph(this)
        // Decrypted attachments only live in the cache for a day.
        graph.scope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { MediaFiles.purgeOld(this@ChatCtrlApp) } }
        // The index of unsent drafts ("Draft: …" in the chat list): read and decrypted off the main thread.
        graph.scope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { graph.drafts.warm() } }
        // The list of custom servers signed in to before: encrypt one an older version left in plain text.
        graph.scope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { graph.prefs.encryptRecentServers() } }
        // The connection log was removed: delete the file an older version kept.
        graph.scope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { java.io.File(filesDir, "connection.log").delete() } }
        Notifier.createChannels(this)
        com.vknxworks.chatctrl.call.CallNotifications.createChannels(this)
        // Live location sharing was removed in 2.3.1: drop its notification channel from Android's settings.
        runCatching { getSystemService(android.app.NotificationManager::class.java)?.deleteNotificationChannel("live_location") }
        graph.boot()

        // Sync while the app is open; the background worker takes over (if enabled) when it isn't.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                com.vknxworks.chatctrl.ui.lock.AppLock.onForeground(graph.lockEnabled, graph.lock.lockTimeout.millis)
                // The session's sync orchestrator starts syncing (or restarts it if it died meanwhile).
                graph.session?.inForeground = true
            }
            override fun onStop(owner: LifecycleOwner) {
                com.vknxworks.chatctrl.ui.lock.AppLock.onBackground(graph.lockEnabled, graph.lock.lockTimeout.millis)
                graph.session?.inForeground = false
                Notifier.schedule(
                    this@ChatCtrlApp,
                    // Also with push through ntfy: the 15-minute check catches anything a push didn't bring.
                    graph.prefs.notifications.value && graph.session != null,
                )
            }
        })
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(MediaFetcher.Factory { graph.session })
                add(MediaFetcher.keyer)
                // Plays animated GIFs and stickers.
                add(coil3.gif.AnimatedImageDecoder.Factory())
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.2).build() }
            // Pictures (often decrypted from encrypted rooms) are only ever kept in memory, never written to disk.
            .diskCache { null }
            .build()

    companion object {
        lateinit var instance: ChatCtrlApp
            private set
        val graph: AppGraph get() = instance.graph
    }
}
