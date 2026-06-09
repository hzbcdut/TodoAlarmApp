package com.example.todoalarm.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoalarm.R
import com.example.todoalarm.data.Todo
import com.example.todoalarm.ui.TodoViewModel
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import com.example.todoalarm.util.RomGuideHelper
import com.example.todoalarm.util.RomUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    vm: TodoViewModel = viewModel(
        factory = TodoViewModel.factory(LocalContext.current)
    )
) {
    val todos by vm.todos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permState = rememberNotificationPermissionState()
    var showAddDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 阶段 4：进入主页时全量重排
    LaunchedEffect(Unit) { vm.rescheduleAll() }

    // 阶段 7：首次启动 + 国产 ROM → 自动弹引导
    LaunchedEffect(Unit) {
        if (RomGuideHelper.shouldShow(context)) {
            showGuideDialog = true
            RomGuideHelper.markShown(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_main)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more))
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_reschedule)) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val count = vm.rescheduleAllWithCount()
                                    snackbarHost.showSnackbar(
                                        context.getString(R.string.snack_rescheduled, count)
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_permission_check)) },
                            onClick = {
                                menuOpen = false
                                showGuideDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.PrivacyTip, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_notification_settings)) },
                            onClick = {
                                menuOpen = false
                                RomUtils.openAppNotificationSettings(context)
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_autostart_settings)) },
                            onClick = {
                                menuOpen = false
                                RomUtils.openAutoStartSettings(context)
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
            }
        }
    ) { padding ->
        if (todos.isEmpty()) {
            EmptyState(padding)
        } else {
            TodoList(
                todos = todos,
                padding = padding,
                onComplete = { vm.complete(it) },
                onDelete = { vm.delete(it) },
                onNotify = { todo ->
                    if (!permState.granted) {
                        Toast.makeText(context, R.string.toast_no_notification_perm, Toast.LENGTH_SHORT).show()
                        permState.request()
                    } else {
                        vm.notifyTodo(todo)
                        Toast.makeText(context, R.string.toast_notify_sent, Toast.LENGTH_SHORT).show()
                    }
                },
                onSetAlarmIn1Min = { todo ->
                    vm.scheduleInOneMinute(todo)
                    Toast.makeText(context, R.string.toast_alarm_1min, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, note, alarmAt ->
                vm.addTodo(title, note, alarmAt)
                showAddDialog = false
            }
        )
    }

    if (showGuideDialog) {
        PermissionGuideDialog(onDismiss = { showGuideDialog = false })
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodoList(
    todos: List<Todo>,
    padding: PaddingValues,
    onComplete: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onNotify: (Todo) -> Unit,
    onSetAlarmIn1Min: (Todo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = todos, key = { it.id }) { todo ->
            TodoCard(
                todo = todo,
                onClick = { onComplete(todo.id) },
                onDelete = { onDelete(todo.id) },
                onNotify = { onNotify(todo) },
                onSetAlarmIn1Min = { onSetAlarmIn1Min(todo) }
            )
        }
    }
}

@Composable
private fun TodoCard(
    todo: Todo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onNotify: () -> Unit,
    onSetAlarmIn1Min: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(todo.title, style = MaterialTheme.typography.titleMedium)
                if (!todo.note.isNullOrBlank()) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        todo.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                todo.alarmAt?.let { ts ->
                    Spacer(Modifier.size(4.dp))
                    Text(
                        stringResource(R.string.label_reminder_at, formatFull(ts)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onSetAlarmIn1Min) {
                Icon(Icons.Default.Alarm, contentDescription = stringResource(R.string.cd_alarm_1min))
            }
            IconButton(onClick = onNotify) {
                Icon(Icons.Default.NotificationsActive, contentDescription = stringResource(R.string.cd_test_notification))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete))
            }
        }
    }
}

private fun formatFull(ts: Long): String {
    val df = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return df.format(java.util.Date(ts))
}

@Preview(showBackground = true)
@Composable
private fun TodoListScreenPreview() {
    TodoAlarmTheme {
        Text("TodoListScreen Preview", modifier = Modifier.padding(16.dp))
    }
}
