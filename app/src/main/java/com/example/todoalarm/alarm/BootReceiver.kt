package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.todoalarm.data.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机 / 应用更新 / 时区变更 时重排所有闹钟。
 *
 * 为什么必须做这件事？
 *  - AlarmManager 的闹钟存于系统层，**重启后丢失**
 *  - Room 数据库在应用沙盒里，重启后还在
 *  - 所以要在收到开机广播后，把 DB 里"待办中 + 有 alarmAt"的全部重新注册
 *
 * 监听的事件：
 *  - BOOT_COMPLETED：用户首次解锁后
 *  - LOCKED_BOOT_COMPLETED：直接启动（要求 receiver directBootAware，阶段 6 暂不启用）
 *  - MY_PACKAGE_REPLACED：App 被覆盖安装（升级场景）
 *  - TIMEZONE_CHANGED / TIME_SET：用户改了时区或时间（防 alarmAt 漂移）
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!HANDLED_ACTIONS.contains(action)) return
        Log.i(TAG, "onReceive: $action")

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TodoRepository.get(appContext)
                val todos = repo.getAllWithAlarm()
                AlarmScheduler(appContext).rescheduleAll(todos)
                Log.i(TAG, "Rescheduled ${todos.size} alarms after $action")
            } catch (e: Exception) {
                Log.e(TAG, "Reschedule failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
    }
}
