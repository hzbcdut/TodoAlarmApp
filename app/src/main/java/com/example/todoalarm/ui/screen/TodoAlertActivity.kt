package com.example.todoalarm.ui.screen

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.todoalarm.alarm.AlarmRingService
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.data.Todo
import com.example.todoalarm.data.TodoRepository
import com.example.todoalarm.notification.TodoNotifier
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 锁屏/关屏状态下弹出的全屏 Activity。
 *
 * 在 [AndroidManifest.xml] 中配置了：
 *  - showOnLockScreen="true" / showWhenLocked="true" / turnScreenOn="true"
 *  - excludeFromRecents="true" / taskAffinity=""
 *
 * 进入流程：
 *   1. AlarmFireReceiver 触发 TodoNotifier.showUrgent
 *   2. 通知的 setFullScreenIntent 启动本 Activity
 *   3. Activity 点亮屏幕、显示在锁屏之上
 *   4. 用户点"完成"或"10 分钟后提醒" → 调对应逻辑 → finish
 */
class TodoAlertActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var todo: Todo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 锁屏三件套：可见 + 点亮 + 保持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        // 解锁后保持屏幕常亮 1 分钟（避免用户读完就熄屏）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId <= 0) {
            Log.w(TAG, "No todo id, finish")
            finish()
            return
        }

        // 2. 加载 todo 数据
        loadTodo(todoId)

        // 3. 尝试解锁（不强制，用户可拒绝）
        requestDismissKeyguardIfPossible()

        // 4. 设置 Compose 内容
        setContent {
            TodoAlarmTheme {
                TodoAlertScreen(
                    todo = todo,
                    onComplete = ::handleComplete,
                    onSnooze = ::handleSnooze
                )
            }
        }
    }

    override fun onDestroy() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        super.onDestroy()
    }

    // ---- 内部 ----

    private fun loadTodo(todoId: Long) {
        scope.launch {
            val t = withContext(Dispatchers.IO) {
                TodoRepository.get(applicationContext).getById(todoId)
            }
            if (t == null) {
                Log.w(TAG, "Todo $todoId not found")
                finish()
                return@launch
            }
            todo = t
        }
    }

    private fun requestDismissKeyguardIfPossible() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // 仅当当前是锁屏态才请求
            if (km.isKeyguardLocked) {
                km.requestDismissKeyguard(this, null)
            }
        } else {
            @Suppress("DEPRECATION")
            km.requestDismissKeyguard(this, null)
        }
    }

    private fun handleComplete() {
        val t = todo ?: run { finish(); return }
        scope.launch {
            withContext(Dispatchers.IO) {
                TodoRepository.get(applicationContext).markCompleted(t.id)
                TodoNotifier(applicationContext).cancel(t.id)
                AlarmRingService.stop(applicationContext)
            }
            finish()
        }
    }

    private fun handleSnooze() {
        val t = todo ?: run { finish(); return }
        scope.launch {
            withContext(Dispatchers.IO) {
                val next = System.currentTimeMillis() + 10 * 60_000L
                val updated = t.copy(alarmAt = next)
                TodoRepository.get(applicationContext).update(updated)
                AlarmScheduler(applicationContext).schedule(updated)
                AlarmRingService.stop(applicationContext)
                // 关掉全屏弹窗的锁屏通知，避免再触发全屏
                TodoNotifier(applicationContext).cancel(t.id)
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
        private const val TAG = "TodoAlertActivity"
    }
}
