package com.example.todoalarm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.R
import com.example.todoalarm.data.Todo
import com.example.todoalarm.ui.theme.TodoAlarmTheme

/**
 * 锁屏全屏弹窗的 Compose 内容。
 *
 * 由 [TodoAlertActivity] 包裹：
 *  - 锁屏可见（setShowWhenLocked + showOnLockScreen）
 *  - 点亮屏幕（setTurnScreenOn）
 *  - 请求解锁（不强制）
 *
 * 两个大号按钮：
 *  - 完成：markCompleted + 停响铃 + finish
 *  - 10 分钟后提醒：重新调度 10 min 后 + 停响铃 + finish
 */
@Composable
fun TodoAlertScreen(
    todo: Todo?,
    onComplete: () -> Unit,
    onSnooze: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.alert_title),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = todo?.title ?: stringResource(R.string.alert_loading),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            if (!todo?.note.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = todo!!.note!!,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    stringResource(R.string.btn_complete),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(stringResource(R.string.btn_snooze_10min), fontSize = 18.sp)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TodoAlertScreenPreview() {
    TodoAlarmTheme {
        TodoAlertScreen(
            todo = Todo(id = 1, title = "吃药", note = "维生素 C 2 片"),
            onComplete = {},
            onSnooze = {}
        )
    }
}
