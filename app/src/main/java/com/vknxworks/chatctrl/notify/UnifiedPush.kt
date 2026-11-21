package com.vknxworks.chatctrl.notify

import android.app.BroadcastOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.security.KeyStore
import java.util.UUID

/**
 * The phone side of push through a UnifiedPush app ("distributor"): ntfy unless another one was chosen in
 * Settings. The push app keeps one connection to its server and hands ChatCtrl what your homeserver sends to its
 * endpoint. This speaks the UnifiedPush Android protocol (https://unifiedpush.org/developers/spec/android/)
 * directly with that one app, without a library.
 */
object UnifiedPush {
    /** ntfy's package name: the push app ChatCtrl uses unless another one is chosen. */
    const val NTFY = "io.heckel.ntfy"
    const val NTFY_DOWNLOAD = "https://f-droid.org/packages/io.heckel.ntfy/"

    private const val ACTION_REGISTER = "org.unifiedpush.android.distributor.REGISTER"
    private const val ACTION_UNREGISTER = "org.unifiedpush.android.distributor.UNREGISTER"
    private const val ACTION_MESSAGE_ACK = "org.unifiedpush.android.distributor.MESSAGE_ACK"
    const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
    const val ACTION_REGISTRATION_FAILED = "org.unifiedpush.android.connector.REGISTRATION_FAILED"
    const val ACTION_UNREGISTERED = "org.unifiedpush.android.connector.UNREGISTERED"
    const val ACTION_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"
    const val ACTION_TEMP_UNAVAILABLE = "org.unifiedpush.android.connector.TEMP_UNAVAILABLE"
    private const val FEATURE_BYTES_MESSAGE = "org.unifiedpush.android.distributor.feature.BYTES_MESSAGE"

    const val EXTRA_TOKEN = "token"
    const val EXTRA_ENDPOINT = "endpoint"
    const val EXTRA_BYTES_MESSAGE = "bytesMessage"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_ID = "id"
    const val EXTRA_REASON = "reason"

    private const val PREFS = "unifiedpush"
    private const val KEY_TOKEN = "token"
    private const val KEY_DISTRIBUTOR = "distributor"

    /** A UnifiedPush app installed on the phone. */
    data class PushApp(val pkg: String, val label: String)

    /** The push app registrations go to: the one chosen in Settings, or ntfy when none was chosen. */
    fun distributor(context: Context): String = prefs(context).getString(KEY_DISTRIBUTOR, null)?.takeIf { it.isNotBlank() } ?: NTFY

    /** Remembers the chosen push app. Unregister from the old one first (see Push.switchApp). */
    fun setDistributor(context: Context, pkg: String) {
        prefs(context).edit().putString(KEY_DISTRIBUTOR, pkg).commit()
    }

    /** The name Android shows for a push app; "ntfy" for ntfy, the package name when it isn't installed. */
    fun label(context: Context, pkg: String = distributor(context)): String {
        if (pkg == NTFY) return "ntfy"
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))).toString()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: pkg
    }

    /** Every installed app that takes UnifiedPush registrations, ntfy first. */
    @Suppress("DEPRECATION")
    fun installedDistributors(context: Context): List<PushApp> = runCatching {
        context.packageManager.queryBroadcastReceivers(Intent(ACTION_REGISTER), 0)
            .mapNotNull { it.activityInfo?.packageName }
            .filter { it != context.packageName }
            .distinct()
            .map { PushApp(it, label(context, it)) }
            .sortedWith(compareByDescending<PushApp> { it.pkg == NTFY }.thenBy { it.label.lowercase() })
    }.getOrDefault(emptyList())

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Whether the chosen push app (ntfy by default) is installed and can take registrations. */
    @Suppress("DEPRECATION")
    fun isPushAppInstalled(context: Context): Boolean = runCatching {
        context.packageManager.queryBroadcastReceivers(Intent(ACTION_REGISTER).setPackage(distributor(context)), 0).isNotEmpty()
    }.getOrDefault(false)

    /** Whether a package is installed at all (to tell "ntfy uninstalled" from "never had it"). */
    fun isInstalled(context: Context, pkg: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)); true
    }.getOrDefault(false)

    /** The random secret that proves a broadcast really comes from the push app for our registration. */
    fun token(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)

    /** Asks the push app for an endpoint. The answer (NEW_ENDPOINT or REGISTRATION_FAILED) arrives in [PushReceiver]. */
    fun register(context: Context, description: String): Boolean {
        if (!isPushAppInstalled(context)) return false
        val p = prefs(context)
        val token = p.getString(KEY_TOKEN, null) ?: UUID.randomUUID().toString().also { p.edit().putString(KEY_TOKEN, it).commit() }
        send(
            context,
            Intent(ACTION_REGISTER).apply {
                `package` = distributor(context)
                putExtra(EXTRA_TOKEN, token)
                putExtra("features", arrayOf(FEATURE_BYTES_MESSAGE))
                putExtra("application", context.packageName)
                putExtra(EXTRA_MESSAGE, description)
            },
        )
        return true
    }

    /** Tells the push app to drop our registration and forgets the token, so the old endpoint can never reach the app again. */
    fun unregister(context: Context) {
        val token = token(context)
        prefs(context).edit().remove(KEY_TOKEN).commit()
        if (token != null && isPushAppInstalled(context)) sendUnregister(context, distributor(context), token)
    }

    /** After REGISTRATION_FAILED the next registration must use a new token (spec). */
    fun dropToken(context: Context) {
        prefs(context).edit().remove(KEY_TOKEN).commit()
    }

    fun ack(context: Context, token: String, id: String?) {
        if (id.isNullOrEmpty()) return
        runCatching {
            context.sendBroadcast(
                Intent(ACTION_MESSAGE_ACK).apply {
                    `package` = distributor(context)
                    putExtra(EXTRA_TOKEN, token)
                    putExtra(EXTRA_ID, id)
                },
            )
        }
    }

    private fun sendUnregister(context: Context, pkg: String, token: String) {
        send(context, Intent(ACTION_UNREGISTER).apply { `package` = pkg; putExtra(EXTRA_TOKEN, token) })
    }

    private fun send(context: Context, intent: Intent) {
        // Tells the distributor which app is asking: a pending intent before Android 14, the sender's identity after.
        val pi = PendingIntent.getBroadcast(context, 0, Intent("org.unifiedpush.dummy_app"), PendingIntent.FLAG_IMMUTABLE)
        intent.putExtra("pi", pi)
        runCatching {
            context.sendBroadcast(intent, null, BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle())
        }
    }

    /**
     * Takes over a registration made by an earlier version (which used a push library). With ntfy the token is
     * kept, so the endpoint and your homeserver's pusher stay the same. A registration with any other push app is
     * removed there. Returns the push app that was used before, or null if there was nothing to take over.
     */
    fun migrateFromLibrary(context: Context): String? {
        val old = context.getSharedPreferences("unifiedpush.connector", Context.MODE_PRIVATE)
        if (old.all.isEmpty()) return null
        val distributor = old.getString("unifiedpush.distributor", null)
        val oldToken = old.getString("default/unifiedpush.connector", null)
        if (oldToken != null && distributor != null) {
            if (distributor == NTFY) {
                prefs(context).edit().putString(KEY_TOKEN, oldToken).commit()
            } else {
                sendUnregister(context, distributor, oldToken)
            }
        }
        old.edit().clear().commit()
        // The library's Web Push key, unused now.
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("UnifiedPush") }
        return distributor
    }
}
