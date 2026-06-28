package com.example.todoalarm.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todoalarm.R
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.data.Todo
import com.example.todoalarm.data.TodoRepository
import com.example.todoalarm.notification.TodoNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Compose 屏背后的业务编排。
 *
 * 阶段 2：CRUD。
 * 阶段 3：暴露 notifyTodo() 立即发通知。
 * 阶段 4：addTodo() 接受 alarmAt，保存后调 AlarmScheduler.schedule()。
 * 阶段 4+：delete() 联动取消闹钟。
 */
class TodoViewModel(
    private val repo: TodoRepository,
    private val appContext: Context,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    val todos: StateFlow<List<Todo>> = repo.observePending()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * 阶段 4：添加待办，可选 alarmAt。
     * 保存后若 alarmAt 在未来，立即调 AlarmScheduler.schedule()。
     */
    fun addTodo(
        title: String,
        note: String? = null,
        alarmAt: Long? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val todo = Todo(
                title = title.trim(),
                note = note?.trim()?.ifBlank { null },
                alarmAt = alarmAt?.takeIf { it > System.currentTimeMillis() }
            )
            val id = repo.insert(todo)
            if (todo.alarmAt != null) {
                scheduler.schedule(todo.copy(id = id))
            }
        }
    }

    fun update(todo: Todo) {
        viewModelScope.launch {
            repo.update(todo)
            // alarmAt 变了 → 重新调度
            if (todo.alarmAt != null && todo.alarmAt > System.currentTimeMillis()) {
                scheduler.schedule(todo)
            } else {
                scheduler.cancel(todo.id)
            }
        }
    }

    fun complete(id: Long) {
        viewModelScope.launch {
            repo.markCompleted(id)
            scheduler.cancel(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repo.delete(id)
            scheduler.cancel(id)
        }
    }

    /** 阶段 3：立即发通知（演示用） */
    fun notifyTodo(todo: Todo) {
        TodoNotifier(appContext).show(todo)
    }

    /**
     * 阶段 4：给某条 todo 设定一个 1 分钟后的闹钟（测试入口）。
     * 写库 + 调度。
     */
    fun scheduleInOneMinute(todo: Todo) {
        val targetTs = System.currentTimeMillis() + 60_000L
        viewModelScope.launch {
            val updated = todo.copy(alarmAt = targetTs)
            repo.update(updated)
            scheduler.schedule(updated)
        }
    }

    /**
     * 快捷闹铃：插入一条标题为 "{N} 分钟后提醒" 的待办，并在 N 分钟后响铃。
     * 复用 addTodo 的「insert + schedule」原子语义；触发时间已过则由 AlarmScheduler.schedule 静默自取消。
     */
    fun quickAddAlarm(minutesFromNow: Int) {
        val triggerAt = System.currentTimeMillis() + minutesFromNow * 60_000L
        viewModelScope.launch {
            val todo = Todo(
                title = appContext.getString(R.string.quick_alarm_title_fmt, minutesFromNow),
                alarmAt = triggerAt.takeIf { it > System.currentTimeMillis() }
            )
            val id = repo.insert(todo)
            if (todo.alarmAt != null) {
                scheduler.schedule(todo.copy(id = id))
            }
        }
    }

    /**
     * 阶段 6+：开机/启动时全量重排。
     */
    fun rescheduleAll() {
        viewModelScope.launch {
            val list = repo.getAllWithAlarm()
            scheduler.rescheduleAll(list)
        }
    }

    /**
     * 阶段 6+：带返回值版本，给 UI 显示"重排了 N 条"用。
     * 返回值是"成功调度的条数"（含原本就过期的被 cancel 掉的也算 0）。
     */
    suspend fun rescheduleAllWithCount(): Int {
        val list = repo.getAllWithAlarm()
        scheduler.rescheduleAll(list)
        return list.size
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    return TodoViewModel(
                        TodoRepository.get(appContext),
                        appContext,
                        AlarmScheduler(appContext)
                    ) as T
                }
            }
    }
}
