package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.todoalarm.data.TodoRepository
import com.example.todoalarm.notification.TodoNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 闹钟到点时的广播接收器。
 *
 * 行为：
 *   1. 读 todo_id
 *   2. 启动前台服务 [AlarmRingService] 循环响铃
 *   3. 发锁屏通知 [TodoNotifier.show]
 *   4. 若该 todo 是一次性（!isRepeating），保持 PENDING 状态等用户在通知里"完成"
 *      阶段 5+ 接入"完成"后会自动停服务
 */
class AlarmFireReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (id <= 0) {
            Log.w(TAG, "AlarmFireReceiver: invalid todo id")
            return
        }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TodoRepository.get(appContext)
                val todo = repo.getById(id)
                if (todo == null) {
                    Log.w(TAG, "Todo $id not found, skip")
                    return@launch
                }

                // 1. 启动前台服务响铃
                val serviceIntent = Intent(appContext, AlarmRingService::class.java).apply {
                    putExtra(AlarmRingService.EXTRA_TODO_ID, id)
                    putExtra(AlarmRingService.EXTRA_LABEL, todo.title)
                }
                ContextCompat.startForegroundService(appContext, serviceIntent)

                // 2. 阶段 5：发"重要待办"通知（MAX 渠道 + setFullScreenIntent）
                //    锁屏/关屏场景会直接弹 TodoAlertActivity
                TodoNotifier(appContext).showUrgent(todo)
            } catch (e: Exception) {
                Log.e(TAG, "AlarmFireReceiver failed for id=$id", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.example.todoalarm.ALARM_FIRE"
        const val EXTRA_TODO_ID = "todo_id"
        private const val TAG = "AlarmFireReceiver"
    }
}
