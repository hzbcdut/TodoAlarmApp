package com.example.todoalarm

import android.app.Application
import com.example.todoalarm.notification.NotificationHelper

/**
 * Application 入口。
 *
 * 阶段 1：仅做空 Application。
 * 阶段 3：创建两个通知渠道。
 * 阶段 4+：AlarmScheduler.preload(this) 开机恢复。
 * 阶段 7+：RomUtils.detect() 决定是否弹引导。
 */
class TodoAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 必须在第一次发通知前调用；幂等，可多次调用
        NotificationHelper.createChannels(this)
    }
}
