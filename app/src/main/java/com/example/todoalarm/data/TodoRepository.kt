package com.example.todoalarm.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 单一仓库，封装所有数据访问。Compose 层只通过它读写。
 * 阶段 1-2 是 Room 的薄封装；阶段 4+ 在此添加"写完即调度闹钟 / 删完即取消闹钟"等副作用。
 */
class TodoRepository(private val dao: TodoDao) {

    fun observePending(): Flow<List<Todo>> = dao.observePending()

    suspend fun getById(id: Long): Todo? = dao.getById(id)

    suspend fun getAllWithAlarm(): List<Todo> = dao.getAllWithAlarm()

    suspend fun insert(todo: Todo): Long = dao.insert(todo)

    suspend fun update(todo: Todo) = dao.update(todo)

    suspend fun markCompleted(id: Long) = dao.markCompleted(id)

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        @Volatile
        private var INSTANCE: TodoRepository? = null

        fun get(context: Context): TodoRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: TodoRepository(AppDatabase.get(context).todoDao())
                .also { INSTANCE = it }
        }
    }
}
