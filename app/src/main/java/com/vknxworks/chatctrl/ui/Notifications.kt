package com.vknxworks.chatctrl.ui

import com.vknxworks.chatctrl.ChatCtrlApp
import com.vknxworks.chatctrl.notify.Notifier

/** Small bridge so screens can dismiss a room's notification when it is opened. */
object Notifications {
    fun clear(roomId: String) = Notifier.clear(ChatCtrlApp.instance, roomId)
}
