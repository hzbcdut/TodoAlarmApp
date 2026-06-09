package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办实体。
 *
 * 设计要点：
 *  - id 自增主键
 *  - status 用 String 存储（见 [TodoStatus]）
 *  - alarmAt 时刻到点 → 阶段 4 触发 AlarmManager.setAlarmClock
 *  - repeatDays 用逗号分隔的星期数字（"1,3,5" 表示周日/周二/周四）
 *    对应 [java.util.Calendar.SUNDAY]=1 .. [Calendar.SATURDAY]=7
 */
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String? = null,
    val dueAt: Long? = null,                 // 截止时间戳
    val alarmAt: Long? = null,               // 提醒时间戳
    val isRepeating: Boolean = false,
    val repeatDays: String? = null,          // "1,3,5" 形式
    val ringtoneUri: String? = null,         // null = 系统默认
    val vibrate: Boolean = true,
    val status: String = TodoStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    val statusEnum: TodoStatus get() = TodoStatus.fromString(status)
}
