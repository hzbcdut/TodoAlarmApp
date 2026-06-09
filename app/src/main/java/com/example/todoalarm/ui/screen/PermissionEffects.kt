package com.example.todoalarm.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 通知权限工具：
 *  - Android 13+ 需运行时申请 POST_NOTIFICATIONS
 *  - 阶段 3 首次进入主页时自动检查并申请
 */
@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    val needsRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var granted by remember {
        mutableStateOf(
            !needsRequest || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    // 第一次进入时若未授权，自动发起申请
    LaunchedEffect(Unit) {
        if (needsRequest && !granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    return remember(granted) { NotificationPermissionState(granted) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) } }
}

class NotificationPermissionState(
    val granted: Boolean,
    val request: () -> Unit
)
