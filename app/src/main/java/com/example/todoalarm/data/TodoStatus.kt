package com.example.todoalarm.data

/**
 * 待办状态。
 * 持久化用 String（DB 列）→ 运行时枚举映射，避免引入 TypeConverter。
 */
enum class TodoStatus {
    PENDING,      // 待办中
    COMPLETED,    // 已完成
    DISMISSED;    // 已忽略（未来用于"已过期未完成"等场景）

    companion object {
        fun fromString(s: String): TodoStatus =
            entries.firstOrNull { it.name == s } ?: PENDING
    }
}
