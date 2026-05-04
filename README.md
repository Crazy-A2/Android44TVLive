# TVLive — 安卓电视直播

Android 4.4.2+ 电视直播应用，面向遥控器操作，开机即播。

## 功能

- **全屏直播** — IJKPlayer 内核，支持 HLS / RTMP / RTSP / HTTP-FLV
- **遥控器换台** — 上下键切换频道，数字键快速跳转（2 秒确认）
- **频道列表** — 右侧覆盖层，按分类浏览，支持收藏
- **音量控制** — 左右键 / 遥控器音量键调节
- **设置** — 解码模式切换（自动/软解/硬解）、源管理
- **源自动切换** — 播放失败自动切备用源，全部失效后触发源更新并定时重试
- **源健康检测** — AlarmManager 每 6 小时 HEAD 探活，自动清理失效源
- **收藏** — 长按 OK 键收藏/取消收藏
- **观看历史** — 自动记录，保留最近 100 条

## 技术栈

| 项目 | 版本 |
|------|------|
| minSdk / targetSdk | 19 (Android 4.4) / 28 |
| Kotlin | 1.3.72 |
| AGP / Gradle | 3.6.4 / 5.6.4 |
| Room | 1.1.1 |
| IJKPlayer | 0.8.8 (armv7a) |
| OkHttp | 3.12.13 |

## 构建

```bash
# Debug
./gradlew assembleDebug

# Release（ProGuard 混淆）
./gradlew assembleRelease
```

APK 输出：`app/build/outputs/apk/`

## 测试

```bash
# JVM 单元测试（Parser、Presenter、Repository 等）
./gradlew test

# Android 仪器测试（DAO）
./gradlew connectedAndroidTest
```

共 **107 个测试用例**，10 个测试类。

## 遥控器操作

| 按键 | 功能 |
|------|------|
| 上 / 下 | 切换频道 |
| 左 / 右 | 调节音量 |
| 数字 0–9 | 频道号输入 |
| OK / ENTER | 打开/关闭频道列表 |
| 长按 OK | 收藏/取消收藏 |
| MENU | 设置 |
| BACK | 返回 / 取消输入 |

## 项目结构

```
app/src/main/java/com/tvlive/app/
├── TvliveApp.kt              # Application 入口
├── data/
│   ├── db/                   # Room 数据库（5 张表）
│   ├── model/                # 解析模型
│   ├── net/                  # 网络请求（OkHttp）
│   ├── parser/               # M3U / JSON 源解析
│   └── repository/           # 源管理与更新逻辑
├── player/                   # IJKPlayer 封装
├── service/                  # 后台服务（源更新、健康检测）
└── ui/
    ├── activity/             # LivePlayerActivity
    ├── osd/                  # OSD 覆盖层组件
    └── presenter/            # MVP Presenter
```

## 架构

MVP 架构，详细说明见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## License

MIT
