package com.example.todoalarm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.todoalarm.alarm.AlarmRingService

/**
 * 通知被滑动删除（swipe to dismiss）时的接收器。
 *
 * OS 在用户从通知抽屉划掉通知时会发这个 broadcast（通过 [androidx.core.app.NotificationCompat.Builder.setDeleteIntent]）。
 * 我们在这里只做一件事：停掉响铃服务。**不**标记 todo 完成（用户可能想保留 todo）。
 */
class DismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        Log.i(TAG, "Notification dismissed (todoId=$todoId), stopping ringtone")
        AlarmRingService.stop(context)
    }

    companion object {
        const val ACTION_DISMISS = "com.example.todoalarm.DISMISS"
        const val EXTRA_TODO_ID = "todo_id"
        private const val TAG = "DismissReceiver"
    }
}
