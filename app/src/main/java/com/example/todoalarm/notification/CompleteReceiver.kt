package com.example.todoalarm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.todoalarm.alarm.AlarmRingService
import com.example.todoalarm.data.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 锁屏/通知栏 "完成" Action 接收器。
 *
 * 行为：
 *   1. 读取 todo_id
 *   2. Repository.markCompleted(id)  → DB 状态变为 COMPLETED
 *   3. TodoNotifier.cancel(id)       → 通知消失
 *   4. AlarmScheduler.cancel(id)     → 取消未触发的闹钟
 *   5. AlarmRingService.stop(this)   → 停止正在响的前台服务
 *
 * 用 goAsync() 拿到 10 秒窗口做异步 IO。
 */
class CompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        val id = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (id <= 0) {
            Log.w(TAG, "CompleteReceiver: invalid todo id")
            return
        }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TodoRepository.get(appContext)
                repo.markCompleted(id)
                TodoNotifier(appContext).cancel(id)
                // 阶段 4 联动：取消闹钟 + 停响铃服务
                com.example.todoalarm.alarm.AlarmScheduler(appContext).cancel(id)
                AlarmRingService.stop(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "CompleteReceiver failed for id=$id", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.example.todoalarm.COMPLETE"
        const val EXTRA_TODO_ID = "todo_id"
        private const val TAG = "CompleteReceiver"
    }
}
