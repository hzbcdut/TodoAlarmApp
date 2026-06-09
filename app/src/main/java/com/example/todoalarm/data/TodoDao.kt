package com.example.todoalarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    /** 观察所有"待办中"——按"有提醒时间的在前 + 时间早的优先"排序 */
    @Query(
        """
        SELECT * FROM todos
        WHERE status = 'PENDING'
        ORDER BY
            CASE WHEN alarmAt IS NULL THEN 1 ELSE 0 END,
            alarmAt ASC,
            createdAt DESC
        """
    )
    fun observePending(): Flow<List<Todo>>

    /** 取单条 */
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): Todo?

    /** 启动恢复 / 全量重排时使用：所有设置了提醒时间的待办 */
    @Query("SELECT * FROM todos WHERE status = 'PENDING' AND alarmAt IS NOT NULL")
    suspend fun getAllWithAlarm(): List<Todo>

    /** 插入新待办，返回 id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: Todo): Long

    /** 整体更新（编辑时用） */
    @Update
    suspend fun update(todo: Todo)

    /** 标记完成（不删除，方便做历史记录） */
    @Query("UPDATE todos SET status = 'COMPLETED' WHERE id = :id")
    suspend fun markCompleted(id: Long)

    /** 物理删除（列表长按删除时用） */
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: Long)
}
