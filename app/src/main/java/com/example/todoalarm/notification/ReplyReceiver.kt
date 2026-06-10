package com.example.todoalarm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.todoalarm.data.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 锁屏/通知栏 "备注" Action 接收器（RemoteInput 回调）。
 *
 * 行为：
 *   1. 从 intent 取 RemoteInput 结果
 *   2. 读出原 todo，把 note 字段更新为新输入
 *   3. Repository.update(...) 写回 DB
 *   4. 重发通知（带新备注内容，cancel + notify 同一 id）
 */
class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPLY) return
        val id = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (id <= 0) {
            Log.w(TAG, "ReplyReceiver: invalid todo id")
            return
        }

        // 优先：从 RemoteInput 结果取（锁屏通知 UI 输入）
        val results = runCatching { RemoteInput.getResultsFromIntent(intent) }.getOrNull()
        var newNote = results?.getCharSequence(REMOTE_INPUT_KEY)?.toString()

        // 兜底：从普通 String extra 取（adb 测试 / 未来其他调用方）
        if (newNote.isNullOrBlank()) {
            newNote = intent.getStringExtra(REMOTE_INPUT_KEY)
        }
        Log.i(TAG, "ReplyReceiver id=$id newNote=${newNote?.take(50)}")

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!newNote.isNullOrBlank()) {
                    val repo = TodoRepository.get(appContext)
                    val todo = repo.getById(id)
                    if (todo != null) {
                        val updated = todo.copy(note = newNote.trim())
                        repo.update(updated)
                        // 重新发通知，让用户看到备注已更新
                        TodoNotifier(appContext).show(updated)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ReplyReceiver failed for id=$id", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.example.todoalarm.REPLY"
        const val EXTRA_TODO_ID = "todo_id"
        const val REMOTE_INPUT_KEY = "reply_text"
        private const val TAG = "ReplyReceiver"
    }
}
