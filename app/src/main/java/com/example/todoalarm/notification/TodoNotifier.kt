package com.example.todoalarm.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.todoalarm.MainActivity
import com.example.todoalarm.R
import com.example.todoalarm.data.Todo
import com.example.todoalarm.ui.screen.TodoAlertActivity

/**
 * 待办通知中心。
 *
 * 阶段 3：实现普通通知（VISIBILITY_PUBLIC + RemoteInput + Action）。
 * 阶段 5：实现 showUrgent() —— 全屏 Intent 弹 TodoAlertActivity。
 *         通知用 IMPORTANCE_MAX 渠道（CHANNEL_URGENT）+ setFullScreenIntent。
 */
class TodoNotifier(private val context: Context) {

    private val nm = NotificationManagerCompat.from(context)

    /**
     * 普通待办：VISIBILITY_PUBLIC + 完成/备注 Action。
     * 阶段 5+ 用于"非紧急"待办；阶段 4 时 AlarmFireReceiver 也调它，阶段 5 起改为调 showUrgent()。
     */
    fun show(todo: Todo) {
        val builder = baseBuilder(todo)
            .setChannelId(NotificationHelper.CHANNEL_TODO)
            .setContentIntent(openAppIntent(todo))
            .addAction(buildCompleteAction(todo))
            .addAction(buildReplyAction(todo))
            .setDeleteIntent(dismissIntent(todo))  // 滑动删除时停响铃

        if (canPostNotifications()) {
            nm.notify(todo.id.toInt(), builder.build())
        }
    }

    /**
     * 重要待办：全屏 Intent 弹 TodoAlertActivity。
     *
     * setFullScreenIntent 行为：
     *  - 用户使用 App 时：直接弹 Activity
     *  - 锁屏 / 关屏：点亮屏幕 + 弹 Activity（请求解锁）
     *  - Android 14+ 用户关掉 USE_FULL_SCREEN_INTENT：降级为普通通知
     */
    fun showUrgent(todo: Todo) {
        val builder = baseBuilder(todo)
            .setChannelId(NotificationHelper.CHANNEL_URGENT)  // MAX 渠道
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenIntent(todo), /* highPriority = */ true)
            // 主体点击：兜底进入 App（如果全屏 Intent 失败/被拒）
            .setContentIntent(openAppIntent(todo))
            // 不带 Action —— 锁屏上让用户直接进入全屏 Activity 操作
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDeleteIntent(dismissIntent(todo))  // 滑动删除时停响铃

        if (canPostNotifications()) {
            nm.notify(todo.id.toInt(), builder.build())
        }
    }

    fun cancel(todoId: Long) {
        nm.cancel(todoId.toInt())
    }

    // ---- 内部 ----

    private fun baseBuilder(todo: Todo): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NotificationHelper.CHANNEL_TODO)
            .setSmallIcon(R.drawable.ic_todo)
            .setContentTitle(todo.title)
            .setContentText(todo.note?.takeIf { it.isNotBlank() } ?: "点击查看详情")
            .setAutoCancel(true)
            .setShowWhen(true)
    }

    /** 点通知主体 / 兜底：打开 MainActivity */
    private fun openAppIntent(todo: Todo): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_TODO_ID, todo.id)
        }
        return PendingIntent.getActivity(
            context, REQ_OPEN, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 全屏 Intent：启动 TodoAlertActivity。
     * 注意：fullScreenIntent 的 PendingIntent 在 Android 14+ 上**必须 IMMUTABLE**。
     */
    private fun fullScreenIntent(todo: Todo): PendingIntent {
        val intent = Intent(context, TodoAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(TodoAlertActivity.EXTRA_TODO_ID, todo.id)
        }
        return PendingIntent.getActivity(
            context,
            reqCodeFullScreen(todo.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** "完成" Action */
    private fun buildCompleteAction(todo: Todo): NotificationCompat.Action {
        val intent = Intent(context, CompleteReceiver::class.java).apply {
            action = CompleteReceiver.ACTION_COMPLETE
            putExtra(CompleteReceiver.EXTRA_TODO_ID, todo.id)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            todo.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_todo, "完成", pi).build()
    }

    /** "备注" Action（RemoteInput） */
    private fun buildReplyAction(todo: Todo): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(ReplyReceiver.REMOTE_INPUT_KEY)
            .setLabel("输入备注")
            .build()
        val intent = Intent(context, ReplyReceiver::class.java).apply {
            action = ReplyReceiver.ACTION_REPLY
            putExtra(ReplyReceiver.EXTRA_TODO_ID, todo.id)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            todo.id.toInt() + 1_000_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_todo, "备注", pi)
            .addRemoteInput(remoteInput).build()
    }

    /**
     * 通知被滑动删除时触发的 DeleteIntent。
     * 调 [DismissReceiver] 停响铃服务，**不**标记 todo 完成。
     */
    private fun dismissIntent(todo: Todo): PendingIntent {
        val intent = Intent(context, DismissReceiver::class.java).apply {
            action = DismissReceiver.ACTION_DISMISS
            putExtra(DismissReceiver.EXTRA_TODO_ID, todo.id)
        }
        return PendingIntent.getBroadcast(
            context,
            todo.id.toInt() + 2_000_000,  // 与其他 reqCode 错开
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** fullScreenIntent 的 requestCode 必须独立 */
    private fun reqCodeFullScreen(todoId: Long): Int = (todoId + 3_000_000).toInt()

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
        private const val REQ_OPEN = 100
    }
}
