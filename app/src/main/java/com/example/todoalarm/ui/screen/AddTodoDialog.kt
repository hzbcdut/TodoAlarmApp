package com.example.todoalarm.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.todoalarm.R
import java.util.Calendar

/**
 * 阶段 4：新建/编辑待办对话框。
 *
 * - 标题（必填）
 * - 备注（可选）
 * - 提醒时间（可选）：点 chip 弹 TimePicker，默认 5 分钟后；可清除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, note: String?, alarmAt: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var alarmAt by remember { mutableStateOf<Long?>(defaultFiveMinutesLater()) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    alarmAt = todayAt(state.hour, state.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            text = { TimePicker(state = state) }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_new_todo_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_note)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = {
                            Text(
                                alarmAt?.let { stringResource(R.string.chip_reminder_set, formatHm(it)) }
                                    ?: stringResource(R.string.chip_no_reminder)
                            )
                        }
                    )
                    if (alarmAt != null) {
                        TextButton(onClick = { alarmAt = null }) {
                            Text(stringResource(R.string.btn_clear))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(title, note, alarmAt) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

private fun defaultFiveMinutesLater(): Long {
    val c = Calendar.getInstance()
    c.add(Calendar.MINUTE, 5)
    return c.timeInMillis
}

/** 把"今天 HH:MM"组装成时间戳；若已过则顺延到明天同时分 */
private fun todayAt(hour: Int, minute: Int): Long {
    val c = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (c.timeInMillis <= System.currentTimeMillis()) {
        c.add(Calendar.DAY_OF_YEAR, 1)
    }
    return c.timeInMillis
}

private fun formatHm(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return "%02d:%02d".format(
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE)
    )
}
