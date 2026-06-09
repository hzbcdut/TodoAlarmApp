package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.todoalarm.MainActivity
import com.example.todoalarm.data.Todo
import com.example.todoalarm.notification.TodoNotifier

/**
 * 闹钟调度器：封装 AlarmManager.setAlarmClock。
 *
 * 关键性质：
 *  - 不需要 SCHEDULE_EXACT_ALARM 权限
 *  - 即使 App 被 force-stop / Doze 模式也会准时触发
 *  - 状态栏会显示 ⏰ 图标，锁屏显示"关闭闹钟"按钮
 */
class AlarmScheduler(private val context: Context) {

    private val am = context.getSystemService(AlarmManager::class.java)

    /**
     * 调度一条待办的闹钟。
     * - alarmAt 必须在未来
     * - 同一 todo 多次 schedule 会被替换（PendingIntent FLAG_UPDATE_CURRENT）
     */
    fun schedule(todo: Todo) {
        val triggerAt = todo.alarmAt ?: return
        if (triggerAt <= System.currentTimeMillis()) {
            cancel(todo.id)
            return
        }
        val info = AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent(todo))
        am.setAlarmClock(info, firePendingIntent(todo))
    }

    /**
     * 取消一条闹钟。必须用与 schedule 相同的 PendingIntent 才能匹配。
     */
    fun cancel(todoId: Long) {
        am.cancel(firePendingIntentForCancel(todoId))
    }

    /**
     * 开机后 / 启动时全量重排：把数据库里所有"待办中 + alarmAt 在未来"的全部重新注册。
     * 阶段 6 (BootReceiver) 会调这个。
     */
    fun rescheduleAll(todos: List<Todo>) {
        val now = System.currentTimeMillis()
        todos.filter { (it.alarmAt ?: 0L) > now }
            .forEach { schedule(it) }
    }

    // ---- PendingIntent 构建 ----

    /** 点状态栏 ⏰ 图标/锁屏关闭按钮跳转的页面 */
    private fun showPendingIntent(todo: Todo): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(TodoNotifier.EXTRA_TODO_ID, todo.id)
        }
        return PendingIntent.getActivity(
            context,
            REQ_SHOW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 闹钟到点时系统发广播给 AlarmFireReceiver */
    private fun firePendingIntent(todo: Todo): PendingIntent {
        val intent = Intent(context, AlarmFireReceiver::class.java).apply {
            action = AlarmFireReceiver.ACTION_FIRE
            putExtra(AlarmFireReceiver.EXTRA_TODO_ID, todo.id)
        }
        return PendingIntent.getBroadcast(
            context,
            reqCodeFor(todo.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** cancel 用：只关心 requestCode 和 component/action 一致即可 */
    private fun firePendingIntentForCancel(todoId: Long): PendingIntent {
        val intent = Intent(context, AlarmFireReceiver::class.java).apply {
            action = AlarmFireReceiver.ACTION_FIRE
            putExtra(AlarmFireReceiver.EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            reqCodeFor(todoId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 把 todoId 映射到安全的 requestCode。
     * 避免和其他 PendingIntent（通知/Receiver）冲突。
     */
    private fun reqCodeFor(todoId: Long): Int {
        val raw = (todoId + 2_000_000).toInt()
        return raw
    }

    companion object {
        private const val REQ_SHOW = 0x1A1A_0001
    }
}
