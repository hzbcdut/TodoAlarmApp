package com.example.todoalarm.util

import android.content.Context

/**
 * 首次启动检测：决定是否弹 ROM 引导对话框。
 *
 * 只在"非 AOSP ROM"上 + "首次启动"时返回 true。
 * AOSP 通常自带标准通知/电池/自启动管控，无需引导。
 */
object RomGuideHelper {

    private const val PREFS = "rom_guide"
    private const val KEY_SHOWN = "shown_once"

    /** 是否应该弹引导 */
    fun shouldShow(context: Context): Boolean {
        val rom = RomUtils.current()
        // AOSP / 未知 ROM：跳引导
        if (rom == RomUtils.Rom.AOSP || rom == RomUtils.Rom.OTHER) {
            return false
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val shown = prefs.getBoolean(KEY_SHOWN, false)
        return !shown
    }

    /** 标记"已展示过引导" */
    fun markShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOWN, true)
            .apply()
    }
}
