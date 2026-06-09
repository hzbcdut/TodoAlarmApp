# TodoAlarmApp

Android 示例项目，演示 **闹钟 + 锁屏待办** 完整链路。

> 一个最小可运行 App：数据持久化 + 锁屏通知 + 闹钟调度 + 全屏弹窗 + 开机恢复 + 国产 ROM 适配。

---

## 已实现的 8 大能力

| 编号 | 能力 | 关键技术 | 阶段 |
|---|---|---|---|
| F1 | 数据持久化（待办 CRUD） | Room + Coroutines + StateFlow | 2 |
| F2 | 锁屏通知列表（多条） | `NotificationCompat` + `VISIBILITY_PUBLIC` | 3 |
| F3 | 锁屏直接"完成"/"备注" | PendingIntent + `goAsync()` + RemoteInput | 3 |
| F4 | 系统级闹钟（无需 SCHEDULE_EXACT_ALARM） | `AlarmManager.setAlarmClock` | 4 |
| F5 | 闹钟触发后响铃 + 发通知 | 前台服务（`type=mediaPlayback`）+ MediaPlayer | 4 |
| F6 | 锁屏全屏弹窗（关屏也能看到） | `setFullScreenIntent` + `setShowWhenLocked` | 5 |
| F7 | 开机 / 升级 / 时区变更 时重排 | `BootReceiver` 监听 5 个广播 | 6 |
| F8 | 国产 ROM 适配（MIUI/Harmony/ColorOS/OriginOS） | 系统属性识别 + 多 Intent 兜底 | 7 |

---

## 5 分钟快速验证

### 第 0 步：环境

- Android Studio Hedgehog (2023.1) 或更新
- JDK 17
- 模拟器或真机：API 26+（Android 8.0+）
- 推荐：Pixel 模拟器 + 一台国产真机（小米 / 华为 / OPPO / vivo）

### 第 1 步：打开工程（30 秒）

```bash
Android Studio → File → Open → ~/Desktop/TodoAlarmApp
```

等待首次 Gradle Sync（5-10 分钟下载依赖）。

### 第 2 步：跑通（5 秒）

点击 ▶️ Run，启动 App。预期看到 **"还没有待办"** 空状态。

### 第 3 步：加一条待办（10 秒）

1. 右下角 **+**
2. 标题："测试待办"
3. 备注："hello"
4. 默认时间已是"5 分钟后"，可点 chip 改时间
5. 点 **保存**

### 第 4 步：测试 4 个核心入口（5 分钟）

每张卡片上有 **3 个图标按钮**（从左到右）：

| 图标 | 行为 | 预期 |
|---|---|---|
| ⏰ | 设闹钟到 1 分钟后 | Toast "已设定 1 分钟后响铃" |
| 🔔 | 立即发锁屏通知 | 锁屏看到通知 + 2 个 Action |
| 🗑️ | 删除 | 列表少一条 |

#### 测试 1：锁屏通知（30 秒）

1. 点 **🔔**
2. **按电源键锁屏**
3. 看到通知 "测试待办 / hello"
4. 点 **"完成"** → 通知消失 + 解锁回 App 列表少一条
5. 再加一条 → 锁屏 → 点 **"备注"** → 输入文字 → 通知刷新

#### 测试 2：闹钟 + 响铃 + 全屏（1.5 分钟）

1. 加一条待办，点 **⏰**
2. **关屏**（不操作）
3. 等待 1 分钟
4. 屏幕**自动点亮** + 状态栏 ⏰ + **全屏紫色弹窗**
5. 弹窗上点 **"完成"** → 铃声停 + 弹窗消失
6. 再来一次 → 点 **"10 分钟后提醒"** → 弹窗消失，10 分钟后再次响

#### 测试 3：开机恢复（1 分钟）

```bash
adb shell am force-stop com.example.todoalarm
```

1. 加一条 5 分钟后的待办
2. 上面这条命令 force-stop
3. **重新打开 App**（必须）→ 看 logcat 或直接 ⋮ → "立即全量重排" 应显示 N
4. 等待 → 闹钟响

#### 测试 4：国产 ROM 适配（30 秒）

在小米 / 华为真机上：

1. 启动 App → **自动弹权限引导**
2. 引导里看 ROM 识别（"小米 MIUI" / "华为 HarmonyOS" 等）
3. 点 **"自启动设置"** → 跳到 MIUI 的"自启动管理"页
4. 开启 TodoAlarmApp 的自启动
5. 同样检查"通知 / 电池 / 全屏"项

---

## 编译运行

环境要求：

| 项 | 要求 |
|---|---|
| Android Studio | Hedgehog (2023.1) 或更新 |
| JDK | 17 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 34 (Android 14) |
| compileSdk | 34 |
| Kotlin | 1.9.24 |
| AGP | 8.5.2 |
| Compose BOM | 2024.06.00 |
| Room | 2.6.1 (KSP) |
| Gradle | 8.7 |

打开后会自动下载 Gradle Wrapper。

---

## 目录结构

```
TodoAlarmApp/
├── PLAN.md                            计划文档（含已确认决策）
├── README.md                          本文档
├── build.gradle.kts                   项目级
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── .gitignore
└── app/
    ├── build.gradle.kts               模块级
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/todoalarm/
        │   ├── TodoAlarmApp.kt        Application：建 Channel
        │   ├── MainActivity.kt        承载 TodoListScreen
        │   ├── data/                  [阶段 2] Room 层
        │   │   ├── Todo.kt
        │   │   ├── TodoStatus.kt
        │   │   ├── TodoDao.kt
        │   │   ├── AppDatabase.kt
        │   │   └── TodoRepository.kt
        │   ├── alarm/                 [阶段 4 + 6]
        │   │   ├── AlarmScheduler.kt      setAlarmClock 封装
        │   │   ├── AlarmFireReceiver.kt   闹钟触发
        │   │   ├── AlarmRingService.kt    前台服务 + 铃声
        │   │   └── BootReceiver.kt        开机重排
        │   ├── notification/          [阶段 3]
        │   │   ├── NotificationHelper.kt
        │   │   ├── TodoNotifier.kt        show / showUrgent
        │   │   ├── CompleteReceiver.kt
        │   │   └── ReplyReceiver.kt
        │   ├── ui/                    [阶段 2 + 4 + 7]
        │   │   ├── TodoViewModel.kt
        │   │   └── screen/
        │   │       ├── TodoListScreen.kt
        │   │       ├── AddTodoDialog.kt
        │   │       ├── TodoAlertActivity.kt   [阶段 5]
        │   │       ├── TodoAlertScreen.kt     [阶段 5]
        │   │       ├── PermissionGuideDialog.kt  [阶段 7]
        │   │       └── PermissionEffects.kt
        │   ├── util/                  [阶段 7]
        │   │   ├── RomUtils.kt             6 种 ROM 识别 + 跳转
        │   │   └── RomGuideHelper.kt
        │   └── ui/theme/              Material 3 主题
        ├── res/                       资源
        │   ├── values/{strings,themes,colors}.xml
        │   ├── drawable/ic_*.xml
        │   └── mipmap-anydpi-v26/
        └── assets/                    内置铃声
            ├── alarm.mp3              （可选；fallback 到系统默认）
            └── README.txt
```

---

## 完整冒烟测试清单

按顺序逐项验证：

- [ ] 1. App 启动后空状态显示
- [ ] 2. 添加一条待办（带时间）
- [ ] 3. 列表出现新条目 + 提醒时间显示
- [ ] 4. 杀进程重启 App，数据还在
- [ ] 5. 点卡片 → 标记完成，列表少一条
- [ ] 6. 点垃圾桶 → 删除，列表少一条
- [ ] 7. 点 🔔 → 锁屏看到通知
- [ ] 8. 锁屏点"完成" → 通知消失 + DB 状态变更
- [ ] 9. 锁屏点"备注" → 弹输入框 → 输入 → 通知刷新
- [ ] 10. 点 ⏰ → 设 1 分钟闹钟
- [ ] 11. 关屏等待 1 分钟 → 屏幕点亮
- [ ] 12. 看到全屏紫色弹窗（标题 + 两个按钮）
- [ ] 13. 弹窗点"完成" → 铃声停 + 弹窗消失
- [ ] 14. 弹窗点"10 分钟后提醒" → 10 分钟后再次响
- [ ] 15. ⋮ → 立即全量重排 → Snackbar 显示数字
- [ ] 16. ⋮ → 权限检查 → 弹对话框 + 显示当前 ROM
- [ ] 17. ⋮ → 通知设置 → 跳系统设置
- [ ] 18. ⋮ → 自启动设置 → 跳各 ROM 自启动页
- [ ] 19. force-stop 后重启 App → LaunchedEffect 触发重排
- [ ] 20. 真实重启手机 → BootReceiver 重排所有闹钟

---

## 关键设计决策

### 决策 1：为什么用 `setAlarmClock` 而非 `setExactAndAllowWhileIdle`？

- `setAlarmClock` **不需要** `SCHEDULE_EXACT_ALARM` 权限（Android 12+ 关键限制）
- 锁屏 / 状态栏 / 系统层面体验一致
- 唯一代价是用户**能看见**状态栏 ⏰ 图标——对"待办 App"是合理诉求

### 决策 2：UI 用 Jetpack Compose？

- `StateFlow → collectAsStateWithLifecycle()` 自动驱动重组
- 锁屏全屏弹窗用 Compose 写大号按钮比 XML 更直接
- 代价：BOM 多 ~10MB 体积；需熟悉新范式

### 决策 3：UI 单 Activity + 多 Screen？

- MainActivity 承载 `TodoListScreen`
- TodoAlertActivity 独立（launchMode=singleTask + taskAffinity=""），不进任务栈
- 不引入 Navigation Compose（屏少，直接 State 管理更简单）

### 决策 4：响铃实现用 MediaPlayer + 前台服务？

- AlarmManager Receiver 仅有 10 秒，不能放循环铃声
- 前台服务（`type=mediaPlayback`）保证不被系统杀
- Android 14+ 必填 foregroundServiceType

### 决策 5：铃声 fallback 策略？

- 优先 `assets/alarm.mp3`（用户可自定义）
- 失败时 fallback 到 `RingtoneManager.getDefaultUri(TYPE_ALARM)` 系统默认
- 零配置即可跑通

---

## 兼容性矩阵

### Android 版本

| API | 关键处理 |
|---|---|
| 26 (8.0) | 引入 NotificationChannel（minSdk） |
| 31 (12) | `SCHEDULE_EXACT_ALARM` 行为变更；本项目**不依赖**此权限 |
| 33 (13) | 运行时申请 `POST_NOTIFICATIONS` |
| 34 (14) | `USE_FULL_SCREEN_INTENT` 普通权限但用户可关；前台服务 type 必填 |

### 国产 ROM

| ROM | 自启动 | 通知 | 电池 | 全屏 | 引导 |
|---|---|---|---|---|---|
| MIUI | 需开启 | 默认 | 默认 | API 34+ 默认 | ✅ |
| HarmonyOS | 需开启 | 默认（设重要） | 默认 | API 34+ 默认 | ✅ |
| ColorOS | 需开启 | 关闭智能隐藏 | 默认 | API 34+ 默认 | ✅ |
| OriginOS | 需开启 | 默认 | 默认 | API 34+ 默认 | ✅ |
| OneUI | 默认 | 默认 | 默认 | API 34+ 默认 | 通常无需 |
| AOSP / Pixel | 默认 | 默认 | 默认 | API 34+ 默认 | 通常无需 |

---

## 已知限制

1. **国产 ROM `EXTRA_SKIP_UI=true` 普遍被忽略** —— 调起系统时钟需用户确认
2. **通知渠道创建后不能改 importance** —— 改渠道重要级需重装
3. **Android 14+ 全屏 Intent 权限可被关** —— `setFullScreenIntent` 会降级为普通通知
4. **MIUI 时钟 App 不显示 `setAlarmClock` 的闹钟** —— 仍能响铃，但不在"时钟"列表
5. **BootReceiver 在国产 ROM 默认被拦截** —— 用户需手动开启自启动
6. **MediaPlayer 在切歌/插拔耳机时偶发中断** —— `setOnErrorListener` 已兜底
7. **自启动 Intent 偶尔会改包名/类名** —— RomUtils 配 2-3 个候选 Intent 兜底

---

## 已确认决策（v1.0）

| # | 决策项 | 选定方案 |
|---|---|---|
| 1 | 包名 / App 名 | `com.example.todoalarm` / "TodoAlarmApp" |
| 2 | UI 框架 | Jetpack Compose |
| 3 | 铃声方案 | 内置铃声（`assets/alarm.mp3`） |
| 4 | 测试范围 | 冒烟测试 |
| 5 | 演示视频 | 不做 |

详细计划见 [PLAN.md](./PLAN.md)。

---

## License

示例项目，仅供学习。
