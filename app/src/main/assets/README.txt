把这个目录下放一个 `alarm.mp3` 文件作为内置闹钟铃声。

如果没有放置 mp3，AlarmRingService 会自动 fallback 到
RingtoneManager.getDefaultUri(TYPE_ALARM) 播放系统默认闹钟声，
App 仍然能正常响铃（只是用系统铃声而非自定义）。

建议：
- 时长 5~15 秒
- 单调循环无爆音（铃声会循环播放）
- 体积 < 1 MB
- 任意合法 mp3 格式均可

放置完后需重新 build → install。
