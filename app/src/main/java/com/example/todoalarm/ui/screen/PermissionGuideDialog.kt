package com.example.todoalarm.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.todoalarm.R
import com.example.todoalarm.util.RomUtils

/**
 * 权限检查对话框：显示当前 ROM 和各项关键设置状态。
 */
@Composable
fun PermissionGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val rom = RomUtils.current()
    val canFullScreen = RomUtils.canUseFullScreenIntent(context)
    val notifEnabled = RomUtils.areNotificationsEnabled(context)
    val batteryIgnored = RomUtils.isIgnoringBatteryOptimizations(context)

    val items = buildList {
        add(
            GuideItem(
                title = stringResource(R.string.guide_item_notification),
                statusOk = notifEnabled,
                actionLabel = stringResource(R.string.guide_btn_goto),
                onAction = { RomUtils.openAppNotificationSettings(context) }
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(
                GuideItem(
                    title = stringResource(R.string.guide_item_fullscreen),
                    statusOk = canFullScreen,
                    actionLabel = stringResource(R.string.guide_btn_goto),
                    onAction = { RomUtils.openFullScreenIntentSettings(context) }
                )
            )
        }
        add(
            GuideItem(
                title = stringResource(R.string.guide_item_battery),
                statusOk = batteryIgnored,
                actionLabel = stringResource(R.string.guide_btn_goto),
                onAction = { RomUtils.openIgnoreBatteryOptimization(context) }
            )
        )
        add(
            GuideItem(
                title = stringResource(R.string.guide_item_autostart),
                statusOk = null,
                statusLabel = if (rom == RomUtils.Rom.AOSP)
                    stringResource(R.string.guide_status_aosp_ok)
                else
                    stringResource(R.string.guide_status_manual),
                actionLabel = stringResource(R.string.guide_btn_goto),
                onAction = { RomUtils.openAutoStartSettings(context) }
            )
        )
        add(
            GuideItem(
                title = stringResource(R.string.guide_item_app_details),
                statusOk = null,
                statusLabel = null,
                actionLabel = stringResource(R.string.guide_btn_open),
                onAction = { RomUtils.openAppDetailsSettings(context) }
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.guide_dialog_title, rom.displayName))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item -> GuideRow(item) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.guide_btn_close))
            }
        }
    )
}

private data class GuideItem(
    val title: String,
    val statusOk: Boolean?,
    val statusLabel: String? = null,
    val actionLabel: String,
    val onAction: () -> Unit
)

@Composable
private fun GuideRow(item: GuideItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            val (icon, tint) = when (item.statusOk) {
                true -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                false -> Icons.Default.Warning to Color(0xFFFF9800)
                null -> Icons.Default.Warning to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                val label = item.statusLabel ?: when (item.statusOk) {
                    true -> stringResource(R.string.guide_status_on)
                    false -> stringResource(R.string.guide_status_off)
                    null -> stringResource(R.string.guide_status_unknown)
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = item.onAction) { Text(item.actionLabel) }
    }
}
