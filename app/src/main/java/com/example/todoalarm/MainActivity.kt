package com.example.todoalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.todoalarm.ui.screen.TodoListScreen
import com.example.todoalarm.ui.theme.TodoAlarmTheme

/**
 * 主 Activity：承载 [TodoListScreen]。
 *
 * 阶段 1+2：仅展示待办列表 + 添加/完成/删除。
 * 阶段 3+：在 Application 中预创建通知 Channel。
 * 阶段 5+：注册 TodoAlertActivity（已使用 setShowWhenLocked）。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoAlarmTheme {
                TodoListScreen()
            }
        }
    }
}
