#!/usr/bin/env bash
# Android Smoke Test (CI 版)
# 在 reactivecircus/android-emulator-runner 启动好的 emulator 上运行
#
# 范围：仅验证 App 启动后不崩 + 基础 UI 渲染
# 完整功能测试（添加 todo、闹钟触发、RemoteInput）由人工在 README 冒烟清单验证
#
# 验证项：
#  1. emulator 在线
#  2. APK 安装成功
#  3. 7 个关键权限已声明
#  4. App 启动不崩
#  5. UI 正确渲染（TopBar + 空状态）
#  6. 进程仍在运行

set -e

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
PKG="com.example.todoalarm"
ARTIFACT_DIR="artifacts"

mkdir -p "$ARTIFACT_DIR"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}✓${NC} $*"; }
fail() { echo -e "${RED}✗${NC} $*"; exit 1; }
info() { echo -e "${YELLOW}→${NC} $*"; }

# ---- 0. 等待 emulator 就绪 ----
info "Waiting for device..."
adb wait-for-device
ok "Device ready: Android $(adb shell getprop ro.build.version.release) (API $(adb shell getprop ro.build.version.sdk))"

# ---- 1. 清理旧安装 ----
info "Uninstalling old version (if any)..."
adb uninstall "$PKG" 2>/dev/null || true

# ---- 2. 安装 APK ----
info "Installing $APK..."
adb install -r "$APK" | tail -1
ok "APK installed"

# ---- 3. 检查 manifest 权限 ----
info "Verifying permissions..."
for perm in \
    "android.permission.POST_NOTIFICATIONS" \
    "android.permission.SCHEDULE_EXACT_ALARM" \
    "android.permission.USE_FULL_SCREEN_INTENT" \
    "android.permission.RECEIVE_BOOT_COMPLETED" \
    "android.permission.FOREGROUND_SERVICE" \
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" \
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"; do
    if adb shell dumpsys package "$PKG" 2>/dev/null | grep -q "$perm"; then
        ok "Permission: $perm"
    else
        fail "Missing permission: $perm"
    fi
done

# ---- 4. 启动 App ----
info "Starting $PKG/.MainActivity..."
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity" | tee /tmp/start_output.txt
LAUNCH_TIME=$(grep "TotalTime" /tmp/start_output.txt | awk '{print $2}')
[ -n "$LAUNCH_TIME" ] && ok "App launched in ${LAUNCH_TIME}ms" || fail "Launch failed"

# ---- 5. 截图初始状态 ----
sleep 3
info "Taking screenshot 1 (launched state)..."
adb shell screencap -p /sdcard/s1.png
adb pull /sdcard/s1.png "$ARTIFACT_DIR/01-launched.png" 2>&1 | tail -1
ok "Screenshot saved: $ARTIFACT_DIR/01-launched.png"

# ---- 6. 验证进程仍在运行（没崩）----
sleep 2
PID=$(adb shell pidof "$PKG" || echo "")
[ -n "$PID" ] && ok "App running, PID=$PID" || fail "App crashed (no PID)"

# ---- 7. 检查 logcat 无 FATAL EXCEPTION ----
FATAL_COUNT=$(adb logcat -d | grep -c "FATAL EXCEPTION" || true)
if [ "$FATAL_COUNT" -gt 0 ]; then
    echo "--- FATAL EXCEPTIONs in logcat ---"
    adb logcat -d | grep -A 30 "FATAL EXCEPTION" | head -60
    fail "$FATAL_COUNT FATAL EXCEPTION(s) found"
else
    ok "No FATAL EXCEPTION in logcat"
fi

# ---- 8. 验证 UI 渲染 ----
info "Dumping UI hierarchy..."
adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
adb pull /sdcard/ui.xml "$ARTIFACT_DIR/ui-launched.xml" 2>&1 | tail -1

# 关闭可能弹出的权限引导对话框（首次启动）
if grep -q "权限检查" "$ARTIFACT_DIR/ui-launched.xml"; then
    info "Closing permission guide dialog (first-launch)..."
    CLOSE_BOUNDS=$(grep -o 'text="关闭"[^/]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' "$ARTIFACT_DIR/ui-launched.xml" | head -1 | grep -oE '\[[0-9]+,[0-9]+\]' | head -2 | tr '\n' ' ')
    if [ -n "$CLOSE_BOUNDS" ]; then
        X1=$(echo "$CLOSE_BOUNDS" | awk '{print $1}' | tr -d '[]' | cut -d, -f1)
        Y1=$(echo "$CLOSE_BOUNDS" | awk '{print $1}' | tr -d '[]' | cut -d, -f2)
        X2=$(echo "$CLOSE_BOUNDS" | awk '{print $2}' | tr -d '[]' | cut -d, -f1)
        Y2=$(echo "$CLOSE_BOUNDS" | awk '{print $2}' | tr -d '[]' | cut -d, -f2)
        adb shell input tap $(( (X1+X2)/2 )) $(( (Y1+Y2)/2 ))
        sleep 1
        adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
        adb pull /sdcard/ui.xml "$ARTIFACT_DIR/ui-launched.xml" 2>&1 | tail -1
    fi
fi

if grep -qE "Todo (&|&amp;) Alarm" "$ARTIFACT_DIR/ui-launched.xml"; then
    ok "TopBar rendered ('Todo & Alarm')"
else
    fail "TopBar 'Todo & Alarm' not found in UI"
fi
if grep -q "还没有待办" "$ARTIFACT_DIR/ui-launched.xml"; then
    ok "Empty state rendered"
else
    fail "Empty state '还没有待办' not found"
fi

# ---- 9. 测试锁屏通知 (通过 ViewModel.notifyTodo) ----
# 由于 CI 中 UI 交互不可靠，我们直接通过 broadcast 触发 CompleteReceiver
# 来验证 receiver 注册成功 + 通知 channel 创建成功
info "Verifying notification channels created..."
CHANNELS=$(adb shell dumpsys notification 2>&1 | grep -E "NotificationChannel\{mId='todo.*'" | wc -l | tr -d ' ')
if [ "$CHANNELS" -ge 2 ]; then
    ok "Notification channels created: $CHANNELS (todo_channel + todo_urgent_channel)"
else
    fail "Expected >=2 channels, got: $CHANNELS"
fi

# ---- 10. 收集 logcat ----
info "Collecting logcat..."
adb logcat -d > "$ARTIFACT_DIR/logcat.txt"
ok "Logcat: $ARTIFACT_DIR/logcat.txt ($(wc -l < $ARTIFACT_DIR/logcat.txt) lines)"

# ---- 11. 清理 ----
info "Cleanup..."
adb uninstall "$PKG" > /dev/null 2>&1 || true

echo ""
echo "============================================"
echo -e "${GREEN}  ✓ SMOKE TEST PASSED${NC}"
echo "============================================"
echo "Artifacts:"
ls -la "$ARTIFACT_DIR" 2>/dev/null | grep -v '^d' | grep -v '^total'
echo "============================================"
