# TodoAlarmApp —— 计划文档

> 一个最小可运行的 Android 示例 App，演示**闹钟 + 锁屏待办**两类核心能力。
> 目标：作为技术验证原型，所有功能用最少代码呈现，可直接装机跑通。

---

## 0. 已确认决策（2026-06-08）

| # | 决策项 | 选定方案 |
|---|---|---|
| 1 | 包名 / App 名 | `com.example.todoalarm` / "TodoAlarmApp"（暂用，可后续改） |
| 2 | UI 框架 | **Jetpack Compose** |
| 3 | 铃声方案 | **内置铃声**（`assets/alarm.mp3`），不接系统媒体库 |
| 4 | 测试范围 | **冒烟测试**（按 §11.1 清单），不追求覆盖率 |
| 5 | 演示视频 | **不做** |

---

## 实现状态：8 阶段全部完成 ✅

- [x] 阶段 1：项目骨架（Compose）
- [x] 阶段 2：数据层（Room）
- [x] 阶段 3：通知系统（锁屏可见）
- [x] 阶段 4：闹钟系统（setAlarmClock）
- [x] 阶段 5：全屏弹窗（锁屏）
- [x] 阶段 6：开机恢复
- [x] 阶段 7：国产 ROM 适配
- [x] 阶段 8：调优 + 文档（strings.xml 资源化 + 完整 README）

---

## 1. 项目目标

### 1.1 核心目标

用 **单一 App** 演示以下能力（在前文讨论中已确认技术可行）：

| 编号 | 能力 | 关键技术 |
|---|---|---|
| F1 | 创建"系统级"闹钟（无需 SCHEDULE_EXACT_ALARM 权限） | `AlarmManager.setAlarmClock()` |
| F2 | 调起系统时钟 App 创建闹钟 | `ACTION_SET_ALARM` Intent |
| F3 | 锁屏显示待办通知列表 | `NotificationCompat` + Channel + `VISIBILITY_PUBLIC` |
| F4 | 重要待办在锁屏之上全屏弹窗 | `setFullScreenIntent` + 全屏 Activity |
| F5 | 锁屏直接完成 / 回复待办 | `RemoteInput` + Action |
| F6 | 闹钟触发后启动服务响铃 | `BroadcastReceiver` + 前台服务 |
| F7 | 持久化待办 + 开机恢复闹钟 | Room + `BOOT_COMPLETED` |
| F8 | 国产 ROM 适配（MIUI/HarmonyOS/ColorOS/OriginOS） | RomUtils + 引导设置页 |

### 1.2 非目标

明确**不**做：
- 账户体系、登录、注册
- 复杂 UI（动画、自定义 View、Material 3 完整主题）
- 后端同步（Google Calendar / iCloud / WebDAV）
- 多语言（仅中文）
- 单元测试覆盖率指标（仅冒烟测试）

---

## 2. 技术栈

| 维度 | 选型 | 理由 |
|---|---|---|
| 语言 | **Kotlin** 1.9+ | Android 官方首选 |
| UI | **Jetpack Compose** | 声明式更简洁；项目复杂度上升后 ViewModel+State 链路更清晰 |
| 最低 SDK | **API 26 (Android 8.0)** | 覆盖 ~99% 在用设备；Channel 必须 |
| 目标 SDK | **API 34 (Android 14)** | 适配最新行为变更 |
| 数据库 | **Room 2.6+** | 持久化待办 |
| 异步 | **Coroutines + Flow** | 列表响应式刷新 |
| 依赖注入 | ❌ **不引入** | 示例项目，越简单越好 |
| 网络 | ❌ **不引入** | 纯本地 |

---

## 3. 架构

### 3.1 整体结构

```
┌──────────────────────────────────────────────┐
│           UI Layer (Compose Screens)         │
│  TodoListScreen · AddTodoScreen ·            │
│  TodoAlertScreen(全屏)                       │
│  承载：MainActivity · AddTodoActivity ·      │
│        TodoAlertActivity                     │
└─────────────┬────────────────────────────────┘
              │ ViewModel + StateFlow
┌─────────────▼────────────────────────────────┐
│            Domain / Application              │
│  TodoRepository · AlarmScheduler ·           │
│  TodoNotifier · RomUtils                     │
│  TodoViewModel                                │
└─────────────┬────────────────────────────────┘
              │
┌─────────────▼────────────────────────────────┐
│            Data + System Services            │
│  Room(TodoDao) · AlarmManager ·              │
│  NotificationManager · KeyguardManager       │
└──────────────────────────────────────────────┘

BroadcastReceivers (跨进程入口):
  AlarmFireReceiver     ← 闹钟触发
  BootReceiver          ← 开机恢复
  CompleteReceiver      ← 锁屏"完成"操作
  ReplyReceiver         ← 锁屏"备注"输入
```

### 3.2 包结构

```
com.example.todoalarm/
├── TodoAlarmApp.kt                 Application
├── data/
│   ├── Todo.kt                     @Entity
│   ├── TodoStatus.kt
│   ├── TodoDao.kt                  @Dao
│   ├── AppDatabase.kt
│   └── TodoRepository.kt
├── alarm/
│   ├── AlarmScheduler.kt           setAlarmClock 封装
│   ├── AlarmFireReceiver.kt        闹钟触发 → 响铃
│   ├── BootReceiver.kt             开机恢复
│   └── AlarmRingService.kt         前台服务，循环响铃
├── notification/
│   ├── NotificationHelper.kt       Channel + 重要等级
│   ├── TodoNotifier.kt             发通知（普通/分组/全屏）
│   ├── CompleteReceiver.kt         "完成"Action
│   └── ReplyReceiver.kt            RemoteInput 处理
├── ui/
│   ├── MainActivity.kt             承载 TodoListScreen
│   ├── AddTodoActivity.kt          承载 AddTodoScreen
│   ├── TodoAlertActivity.kt        承载 TodoAlertScreen（全屏）
│   ├── screen/
│   │   ├── TodoListScreen.kt       Composable
│   │   ├── AddTodoScreen.kt        Composable
│   │   └── TodoAlertScreen.kt      Composable（全屏）
│   ├── TodoViewModel.kt            StateFlow + 业务编排
│   └── theme/
│       ├── Theme.kt                MaterialTheme 包装
│       ├── Color.kt
│       └── Type.kt
└── util/
    ├── RomUtils.kt                 国产 ROM 识别 + 引导
    └── TimeUtils.kt                时间格式化
```

**Compose 配套依赖**（写入 `app/build.gradle.kts`）：

```kotlin
implementation(platform("androidx.compose:compose-bom:2024.06.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.9.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
debugImplementation("androidx.compose.ui:ui-tooling")
```

---

## 4. 数据模型

```kotlin
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String? = null,
    val dueAt: Long? = null,             // 截止时间戳
    val alarmAt: Long? = null,           // 提醒时间戳
    val isRepeating: Boolean = false,
    val repeatDays: String? = null,      // "1,3,5" (Calendar.SUNDAY=1..SATURDAY=7)
    val ringtoneUri: String? = null,     // null = 系统默认
    val vibrate: Boolean = true,
    val status: String = "PENDING",      // PENDING / COMPLETED / DISMISSED
    val notificationId: Int = 0,         // 通知 ID，便于 cancel
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 5. 关键模块设计

### 5.1 `AlarmScheduler` —— 闹钟编排

```kotlin
class AlarmScheduler(private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    /**
     * 调度单条待办的闹钟
     * 关键：setAlarmClock 不需要任何运行时权限
     */
    fun schedule(todo: Todo) {
        if (todo.alarmAt == null || todo.alarmAt < System.currentTimeMillis()) return

        val showPi = PendingIntent.getActivity(
            context, REQUEST_SHOW,
            Intent(context, MainActivity::class.java),
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        val firePi = buildFirePendingIntent(todo)
        val info = AlarmManager.AlarmClockInfo(todo.alarmAt, showPi)

        am.setAlarmClock(info, firePi)
    }

    fun cancel(todoId: Long) {
        am.cancel(buildFirePendingIntent(todoId))
    }

    fun rescheduleAll(todos: List<Todo>) {  // 开机后调用
        todos.filter { it.status == "PENDING" && it.alarmAt != null }
             .forEach { schedule(it) }
    }
}
```

### 5.2 `TodoNotifier` —— 通知中心

```kotlin
class TodoNotifier(private val context: Context) {

    fun show(todo: Todo) {
        // 普通锁屏通知：VISIBILITY_PUBLIC + RemoteInput + Action
        val notification = NotificationCompat.Builder(context, CHANNEL_TODO)
            .setSmallIcon(R.drawable.ic_todo)
            .setContentTitle(todo.title)
            .setContentText(todo.note)
            .setPriority(PRIORITY_HIGH)
            .setCategory(CATEGORY_REMINDER)
            .setVisibility(VISIBILITY_PUBLIC)
            .setContentIntent(buildOpenIntent(todo))
            .addAction(buildCompleteAction(todo))   // 锁屏"完成"
            .addAction(buildReplyAction(todo))      // 锁屏"备注"
            .build()
        nm.notify(todo.id.toInt(), notification)
    }

    fun showUrgent(todo: Todo) {
        // 重要待办：全屏弹窗
        // setFullScreenIntent + TodoAlertActivity (showWhenLocked)
    }

    fun showGroupSummary(todos: List<Todo>) {
        // 多条分组：InboxStyle 展示所有
    }
}
```

### 5.3 `AlarmFireReceiver` —— 触发入口

```kotlin
class AlarmFireReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1)
        // 启动前台服务响铃
        val serviceIntent = Intent(context, AlarmRingService::class.java)
            .putExtra(EXTRA_TODO_ID, todoId)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
```

### 5.4 `TodoAlertActivity` —— 全屏锁屏弹窗

```kotlin
class TodoAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 关键三件套：锁屏可见 + 唤醒 + 常亮
        if (Build.VERSION.SDK_INT >= O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(FLAG_SHOW_WHEN_LOCKED or FLAG_TURN_SCREEN_ON or FLAG_KEEP_SCREEN_ON)
        }

        // Compose 入口
        setContent {
            TodoAlarmTheme {
                TodoAlertScreen(
                    todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L),
                    onComplete = { /* 更新 DB + 关闭 */ },
                    onSnooze = { /* 10 分钟后再次响铃 */ }
                )
            }
        }

        // 请求解锁（不强制）
        getSystemService(KeyguardManager::class.java)
            .requestDismissKeyguard(this, null)
    }
}
```

### 5.5 `RomUtils` —— 国产 ROM 适配

```kotlin
object RomUtils {
    enum class Rom { MIUI, HARMONY, COLOR_OS, ORIGIN_OS, ONE_UI, AOSP, OTHER }

    fun current(): Rom = when {
        isMiui() -> Rom.MIUI
        isHarmony() -> Rom.HARMONY
        isColorOs() -> Rom.COLOR_OS
        isOriginOs() -> Rom.ORIGIN_OS
        isOneUi() -> Rom.ONE_UI
        else -> Rom.AOSP
    }

    /** 跳转到各 ROM 的"自启动 / 电池白名单"设置页 */
    fun openBatterySettings(context: Context) { /* try 多个 intent */ }
    fun openAutoStartSettings(context: Context) { /* try 多个 intent */ }
    fun openNotificationSettings(context: Context) { /* 通用 */ }
}
```

---

## 6. UI 设计（极简，Compose）

| Screen (Composable) | 内容 | 关键组件 |
|---|---|---|
| `TodoListScreen` | 待办列表 + 入口 | `LazyColumn` + `FloatingActionButton` + 卡片项 |
| `AddTodoScreen` | 新建/编辑 | `OutlinedTextField` + `TimePicker` 对话框 + 星期多选 `FilterChip` |
| `TodoAlertScreen` | 锁屏全屏 | 大标题 + 大号 [完成] [稍后提醒] 按钮（`Button`） |

不画复杂原型图，运行时通过截图迭代。

**ViewModel 链路**：

```kotlin
class TodoViewModel(private val repo: TodoRepository) : ViewModel() {
    val todos: StateFlow<List<Todo>> = repo.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTodo(todo: Todo) = viewModelScope.launch { repo.insert(todo) }
    fun complete(id: Long) = viewModelScope.launch { repo.markCompleted(id) }
    fun schedule(todo: Todo) = AlarmScheduler(context).schedule(todo)
}
```

**Composable 中使用**：

```kotlin
@Composable
fun TodoListScreen(vm: TodoViewModel = viewModel()) {
    val todos by vm.todos.collectAsStateWithLifecycle()
    Scaffold(floatingActionButton = { /* + */ }) { padding ->
        LazyColumn { items(todos) { TodoItem(it) } }
    }
}
```

---

## 7. 权限清单

```xml
<!-- 通知（Android 13+ 运行时） -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- 全屏 Intent（Android 14+ 普通权限，但用户可关） -->
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

<!-- 开机自启（用于恢复闹钟） -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- 响铃用前台服务 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<!-- 可选：如果要选本地音乐做铃声 -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

**注意**：`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` **不需要**（F1 用 setAlarmClock 规避）。

---

## 8. 实现步骤（分阶段）

### 阶段 1：项目骨架 ✅ 目标：能跑起来

- [ ] 创建 Android Studio 项目（Kotlin + **Compose** + minSdk 26 / target 34）
- [ ] 配置 `app/build.gradle.kts`（Kotlin、Compose BOM、Room、Coroutines）
- [ ] 写 `AndroidManifest.xml`（权限 + 四大组件注册）
- [ ] 创建 `TodoAlarmApp` Application
- [ ] 创建 `MainActivity` + 空 Composable `TodoListScreen`（只显示 "Hello Todo"）
- [ ] 创建 `theme/`（Theme / Color / Type 三件套，Android Studio 模板生成即可）

**完成标准**：`./gradlew assembleDebug` 通过；安装到模拟器能启动，屏幕显示 "Hello Todo"。

### 阶段 2：数据层 ✅ 目标：CRUD 可用

- [ ] 定义 `Todo` Entity + `TodoStatus`
- [ ] 写 `TodoDao`（插入/更新/删除/查询 PENDING）
- [ ] 写 `AppDatabase`（RoomDatabase）
- [ ] 写 `TodoRepository`（单例）
- [ ] `MainActivity` 接入 RecyclerView 展示列表

**完成标准**：能添加/勾选/删除待办，进程杀重启数据还在。

### 阶段 3：通知系统 ✅ 目标：锁屏可见

- [ ] `NotificationHelper` 创建两个 Channel：
  - `todo_channel`（普通，HIGH）
  - `todo_urgent_channel`（紧急，MAX）
- [ ] `TodoNotifier.show()` 实现普通通知 + RemoteInput + Action
- [ ] `CompleteReceiver` 实现"完成"操作（更新 DB + 取消通知）
- [ ] `ReplyReceiver` 实现 RemoteInput 处理（保存备注）
- [ ] 申请 POST_NOTIFICATIONS 权限

**完成标准**：锁屏上能看到待办通知；点"完成"通知消失且 DB 状态变更。

### 阶段 4：闹钟系统 ✅ 目标：到点触发

- [ ] `AlarmScheduler` 封装 `setAlarmClock`
- [ ] `AddTodoScreen` 加 `TimePicker` 对话框 + 触发 `schedule`
- [ ] `AlarmFireReceiver` 接收闹钟广播
- [ ] `AlarmRingService` 前台服务 + MediaPlayer 循环铃声
- [ ] 放一个内置铃声到 `app/src/main/assets/alarm.mp3`（随便找一段 ~10 秒的 mp3）

**内置铃声代码**：

```kotlin
class AlarmRingService : Service() {
    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)

        player = MediaPlayer().apply {
            val afd = assets.openFd("alarm.mp3")
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            isLooping = true
            prepare()
            start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        player?.stop(); player?.release()
        super.onDestroy()
    }
}
```

**完成标准**：设定 1 分钟后闹钟 → 到点响铃 → 通知出现。

### 阶段 5：全屏弹窗 ✅ 目标：锁屏立即可见

- [ ] `TodoAlertActivity` 全屏布局（showWhenLocked / turnScreenOn）
- [ ] `TodoNotifier.showUrgent()` 加 `setFullScreenIntent`
- [ ] 申请 `USE_FULL_SCREEN_INTENT` 权限
- [ ] 锁屏 / 关屏场景测试

**完成标准**：锁屏 + 关屏状态下到点 → 屏幕点亮 + 全屏 Activity 弹出。

### 阶段 6：开机恢复 ✅ 目标：重启不丢

- [ ] `BootReceiver` 注册 `BOOT_COMPLETED`
- [ ] 启动后调用 `AlarmScheduler.rescheduleAll()`
- [ ] 实测小米 / AOSP 模拟器重启场景

**完成标准**：设置闹钟后重启手机 → 闹钟仍然在原定时间触发。

### 阶段 7：国产 ROM 适配 ✅ 目标：国产机可用

- [ ] `RomUtils` 实现各 ROM 识别（Build.MANUFACTURER + 系统属性）
- [ ] 提供 `openBatterySettings` / `openAutoStartSettings`（try 多个 intent 兜底）
- [ ] `MainActivity` 添加"通知设置"按钮 → 跳系统设置
- [ ] 首次启动检测 ROM → 弹引导：开启通知 / 加入白名单

**完成标准**：在小米 / 华为真机上功能完整可用。

### 阶段 8：调优 + 文档 ✅ 目标：可交付

- [ ] README.md（截图 + 编译说明 + 真机测试清单）
- [ ] 关键代码加中文注释
- [ ] 真机测试（至少 2 台：小米 + AOSP）
- [ ] 录演示视频（可选）

---

## 9. 兼容性策略

### 9.1 Android 版本

| API | 关键处理 |
|---|---|
| 26 | 引入 NotificationChannel |
| 31 (12) | 行为变更：精确闹钟权限；本项目**不依赖**此权限（用 setAlarmClock 规避） |
| 33 (13) | 运行时申请 `POST_NOTIFICATIONS` |
| 34 (14) | `USE_FULL_SCREEN_INTENT` 普通权限但可被用户关；前台服务 type 必填 |

### 9.2 国产 ROM

| ROM | 必做的引导 |
|---|---|
| MIUI | 加入"自启动"白名单 + 关闭"电池优化" |
| HarmonyOS | 通知设为"重要" |
| ColorOS | 关闭"智能隐藏通知" |
| OriginOS | 关闭"智能唤醒"对通知的限制 |

策略：识别 ROM 后**一次性弹引导卡片**，用户点"去设置"跳对应页面（不强制）。

---

## 10. 关键技术决策记录

### 决策 1：为什么用 `setAlarmClock` 而非 `setExactAndAllowWhileIdle`？

- `setAlarmClock` **不需要** `SCHEDULE_EXACT_ALARM` 权限
- 锁屏 / 状态栏 / 系统层面体验一致
- 唯一代价是用户**能看见**（状态栏图标）—— 对"待办 App"是合理诉求

### 决策 2：UI 用 Jetpack Compose？

- **响应式更自然**：`StateFlow → collectAsStateWithLifecycle()` 自动驱动重组
- **锁屏全屏弹窗**用 Compose 写大号按钮、铺色背景比 XML 更直接
- **代价**：Compose BOM 多 ~10MB 体积；新人不熟悉；预览需 @Preview
- **取舍**：作为新项目默认推荐；ViewBinding 项目改 Compose 才有动力

### 决策 3：不用 DI 框架？

- 整个 App 不到 20 个类，手动注入足够
- 加 Hilt/Koin 会让示例失去焦点

### 决策 4：响铃实现用 MediaPlayer + 前台服务

- AlarmManager Receiver 仅有 10 秒，不能放循环铃声
- 前台服务（`type=mediaPlayback`）保证不被杀
- Android 14+ 必填 foregroundServiceType

### 决策 5：分组通知何时触发？

- 列表 ≥ 3 条且都未完成时显示汇总
- 单条紧急待办直接走 `showUrgent`
- 否则单条 `show`

---

## 11. 测试计划

### 11.1 冒烟测试清单

- [ ] 启动 App，列表为空
- [ ] 添加一条 1 分钟后的待办
- [ ] 关屏等待，1 分钟后**屏幕点亮** + **铃声响** + **通知出现**
- [ ] 锁屏状态下：通知**完整可见**
- [ ] 锁屏点"完成"：通知消失，DB 状态变更
- [ ] 锁屏点"备注"：弹出输入框，输入后保存
- [ ] 重启手机：闹钟仍在原定时间触发
- [ ] 关闭通知权限：通知不出现，但全屏 Intent 仍能弹（Android 14 行为）

### 11.2 真机验证矩阵

| 设备 | ROM | 重点验证 |
|---|---|---|
| Pixel / AVD | AOSP 14 | 基线行为 |
| 小米 13 / 14 | MIUI 14 | 自启动 / 通知重要性 / 锁屏 |
| 华为 P60 | HarmonyOS 4 | 重要通知分类 |
| 三星 S23 | OneUI 6 | 锁屏通知列表 |
| OPPO Find X | ColorOS 13 | 全屏 Intent 行为 |

### 11.3 已知限制（不需要修复，仅记录）

- 国产 ROM `EXTRA_SKIP_UI=true` 普遍被忽略 → 调起系统时钟一定需用户确认
- 通知渠道创建后**不能**改重要性 → 慎用
- Android 14+ 用户在系统设置里关掉 `USE_FULL_SCREEN_INTENT` 后全屏弹窗失效 → 降级为普通通知

---

## 12. 风险与待确认

| 风险 | 影响 | 缓解 |
|---|---|---|
| MIUI 14+ 对 setAlarmClock 列表写入不友好 | 时钟 App 看不到，仅状态栏有 | 文档说明；用户预期管理 |
| 国产 ROM 自启动被禁 | 开机恢复失败 | 引导用户加白名单 |
| Android 14 USE_FULL_SCREEN_INTENT 可被用户关 | 全屏弹窗失效 | 检测 + 引导开启 |
| 测试设备不足 | 国产 ROM 兼容性无法完整验证 | 至少保证 AOSP + 1 台小米通过 |

---

## 13. 交付物清单

- [ ] `app/` 完整可编译工程
- [ ] `README.md` 编译/运行/测试说明
- [ ] `PLAN.md` 本文档
- [ ] 关键截图（主列表、全屏弹窗、锁屏通知）
- [ ] （可选）演示视频

---

## 14. 时间估算（示例开发，Compose 栈）

| 阶段 | 估时 |
|---|---|
| 1. 项目骨架（含 Compose 主题三件套） | 1h |
| 2. 数据层 | 1h |
| 3. 通知系统 | 2h |
| 4. 闹钟系统 | 2h |
| 5. 全屏弹窗（Compose 全屏 Activity） | 1.5h |
| 6. 开机恢复 | 0.5h |
| 7. 国产 ROM 适配 | 2h |
| 8. 调优 + 文档 | 1h |
| **合计** | **~11h** |

---

## 15. 不在范围内（未来扩展）

- Google Calendar 双向同步
- 多端同步（自建后端 / Firebase）
- 协作待办
- 智能推荐提醒时间
- 语音添加待办
- Widget（桌面小组件）

---

**计划版本**：v1.0  
**最后更新**：2026-06-08
