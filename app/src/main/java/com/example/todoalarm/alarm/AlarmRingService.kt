package com.example.todoalarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.todoalarm.R
import com.example.todoalarm.notification.NotificationHelper

/**
 * 闹钟响铃服务：前台服务 + MediaPlayer 循环播铃声。
 *
 * 铃声优先级：
 *   1. assets/alarm.mp3（用户放了就用这个）
 *   2. RingtoneManager.getDefaultUri(TYPE_ALARM) 系统默认闹钟声
 *   3. TYPE_NOTIFICATION / TYPE_RINGTONE 兜底
 *
 * 停止方式：
 *   - 用户点通知的 "完成" → CompleteReceiver.markCompleted() → 调 stop(this)
 *   - 阶段 5+ 接入 TodoAlertActivity 的"完成"按钮也会调
 */
class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startRingtone()
        return START_NOT_STICKY  // 不需要重启
    }

    override fun onDestroy() {
        stopRingtone()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---- 内部 ----

    private fun startInForeground() {
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_TODO)
            .setSmallIcon(R.drawable.ic_todo)
            .setContentTitle("正在响铃")
            .setContentText("点击进入 App")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)  // 前台服务通道用 LOW，不打扰
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRingtone() {
        // 1) 优先：assets/alarm.mp3
        try {
            val afd = assets.openFd(ASSETS_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    true
                }
                prepareAsync()
            }
            return
        } catch (e: Exception) {
            Log.w(TAG, "assets/$ASSETS_RINGTONE not available, fallback to system ringtone", e)
        }

        // 2) 兜底：系统默认铃声
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return

        try {
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
                isLooping = true
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play system ringtone", e)
        }
    }

    private fun stopRingtone() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_LABEL = "label"
        private const val ASSETS_RINGTONE = "alarm.mp3"
        private const val NOTIFICATION_ID = 9000
        private const val TAG = "AlarmRingService"

        /** 外部调用：停止响铃 */
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingService::class.java))
        }
    }
}
