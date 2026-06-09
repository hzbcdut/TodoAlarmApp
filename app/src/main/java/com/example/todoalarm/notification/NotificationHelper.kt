package com.example.todoalarm.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * 通知渠道集中管理。
 *
 * ⚠️ 重要：渠道一旦创建，重要等级（importance）/ 锁屏可见性等**不可再改**。
 *    后续阶段如要调等级，必须先 uninstall / 修改 channel id。
 */
object NotificationHelper {

    /** 普通待办：HIGH，锁屏 VISIBILITY_PUBLIC */
    const val CHANNEL_TODO = "todo_channel"

    /** 重要待办：MAX，锁屏全屏弹窗，阶段 5 使用 */
    const val CHANNEL_URGENT = "todo_urgent_channel"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val todo = NotificationChannel(
            CHANNEL_TODO,
            "待办提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "待办事项的提醒通知"
            setShowBadge(true)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val urgent = NotificationChannel(
            CHANNEL_URGENT,
            "重要待办",
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = "重要待办的强提醒通知（可触发锁屏全屏弹窗）"
            setShowBadge(true)
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }

        nm.createNotificationChannels(listOf(todo, urgent))
    }
}
