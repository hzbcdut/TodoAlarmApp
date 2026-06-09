package com.example.todoalarm.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * 国产 ROM 适配工具。
 *
 * 关键点：
 *  - 识别靠系统属性（最稳）+ MANUFACTURER / BRAND 兜底
 *  - 跳转靠"多个 Intent 顺序尝试"，直到能 resolveActivity
 *  - 检查类方法只读，不弹权限申请
 */
object RomUtils {

    enum class Rom(val displayName: String) {
        MIUI("小米 MIUI"),
        HARMONY("华为 HarmonyOS / EMUI"),
        COLOR_OS("OPPO ColorOS"),
        ORIGIN_OS("vivo OriginOS / FuntouchOS"),
        ONE_UI("三星 OneUI"),
        AOSP("AOSP / 原生"),
        OTHER("其他");
    }

    /** 当前 ROM 类型（带缓存） */
    fun current(): Rom {
        _currentCache?.let { return it }
        val detected = detect()
        _currentCache = detected
        return detected
    }
    private var _currentCache: Rom? = null

    @SuppressLint("PrivateApi")
    private fun detect(): Rom {
        val props = readSystemProps()

        return when {
            // MIUI
            props.any { it.startsWith("ro.miui.ui.version.name") } -> Rom.MIUI
            props.any { it.startsWith("ro.miui.ui.version.code") } -> Rom.MIUI
            getProp("ro.miui.ui.version.name") != null -> Rom.MIUI

            // HarmonyOS / EMUI（HMS 5+ 是 HarmonyOS，更早是 EMUI）
            getProp("ro.build.version.emui") != null -> Rom.HARMONY
            getProp("ro.huawei.os.build.version") != null -> Rom.HARMONY
            getProp("hw_sc.build.platform.version") != null -> Rom.HARMONY

            // ColorOS
            getProp("ro.build.version.opporom") != null -> Rom.COLOR_OS
            getProp("ro.oppo.theme.version") != null -> Rom.COLOR_OS

            // vivo OriginOS / FuntouchOS
            getProp("ro.vivo.product.version") != null -> Rom.ORIGIN_OS
            getProp("ro.vivo.os.version") != null -> Rom.ORIGIN_OS

            // OneUI（三星）
            getProp("ro.samsung.fingerprint") != null -> Rom.ONE_UI
            Build.MANUFACTURER.equals("samsung", ignoreCase = true) -> Rom.ONE_UI

            // 兜底按品牌
            else -> {
                val mf = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
                when {
                    "xiaomi" in mf || "redmi" in mf || "poco" in mf -> Rom.MIUI
                    "huawei" in mf || "honor" in mf -> Rom.HARMONY
                    "oppo" in mf || "realme" in mf || "oneplus" in mf -> Rom.COLOR_OS
                    "vivo" in mf || "iqoo" in mf -> Rom.ORIGIN_OS
                    "samsung" in mf -> Rom.ONE_UI
                    else -> if (Build.VERSION.SDK_INT >= 28) Rom.AOSP else Rom.OTHER
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun readSystemProps(): List<String> = runCatching {
        val systemPropsClass = Class.forName("android.os.SystemProperties")
        val getMethod = systemPropsClass.getMethod("get", String::class.java)
        // 读所有可能涉及 ROM 的 prop
        listOf(
            "ro.miui.ui.version.name",
            "ro.miui.ui.version.code",
            "ro.build.version.emui",
            "ro.build.version.harmony",
            "ro.huawei.os.build.version",
            "hw_sc.build.platform.version",
            "ro.build.version.opporom",
            "ro.oppo.theme.version",
            "ro.vivo.product.version",
            "ro.vivo.os.version",
            "ro.samsung.fingerprint"
        ).mapNotNull { propName ->
            runCatching { getMethod.invoke(null, propName) as? String }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private fun getProp(name: String): String? = runCatching {
        val c = Class.forName("android.os.SystemProperties")
        val m = c.getMethod("get", String::class.java)
        m.invoke(null, name) as? String
    }.getOrNull()

    // ============================================================
    // 状态检查
    // ============================================================

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return NotificationManagerCompat.from(context).canUseFullScreenIntent()
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // ============================================================
    // 跳转 Intent（每个方法都试多个候选，失败兜底到应用详情）
    // ============================================================

    /**
     * 通知设置（API 26+ 标准入口；个别 ROM 会跳到自己的通知中心）
     */
    fun openAppNotificationSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        // 1. 标准入口
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intents += Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        // 2. 应用详情（兜底）
        intents += Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))

        return tryStart(context, intents)
    }

    /**
     * 全屏 Intent 权限（API 34+）
     */
    fun openFullScreenIntentSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return openAppNotificationSettings(context)
        }
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            .setData(Uri.fromParts("package", context.packageName, null))
        return tryStart(context, listOf(intent))
    }

    /**
     * 电池优化：申请加入白名单
     */
    fun openIgnoreBatteryOptimization(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        return tryStart(context, listOf(intent))
    }

    /**
     * 电池优化设置列表（系统级，看所有 App）
     */
    fun openBatteryOptimizationList(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        return tryStart(context, listOf(intent))
    }

    /**
     * 自启动设置：跳各 ROM 的"自启动管理"页
     * 各家 ROM 的 Activity 名经常变，多 Intent 兜底
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val intents = when (current()) {
            Rom.MIUI -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                ),
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.appmanager.AppManagerMainActivity"
                    )
                )
            )
            Rom.HARMONY -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                ),
                Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                )
            )
            Rom.COLOR_OS -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                ),
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.privacypermissionsentry.PermissionTopActivity"
                    )
                ),
                Intent().setComponent(
                    ComponentName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                )
            )
            Rom.ORIGIN_OS -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                ),
                Intent().setComponent(
                    ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                    )
                )
            )
            Rom.ONE_UI -> listOf(
                // 三星没有专门"自启动"页，引导到电池页
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            )
            else -> listOf(
                // AOSP / 其他：跳应用详情
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            )
        }
        return tryStart(context, intents)
    }

    /**
     * 应用详情（兜底入口）
     */
    fun openAppDetailsSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
        return tryStart(context, listOf(intent))
    }

    private fun tryStart(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                runCatching {
                    context.startActivity(intent)
                    return true
                }.onFailure { /* 继续试下一个 */ }
            }
        }
        // 全部失败，兜底到应用详情
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(fallback)
            true
        }.getOrDefault(false)
    }
}
