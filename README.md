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

### 环境要求

- **JDK 8**（AGP 3.6.4 不支持 JDK 9+）
- **Android SDK**：API 28（compileSdk） + Build-Tools 28.0.3
- **Gradle**：5.6.4（项目未包含 `gradlew`，需本地安装或生成 wrapper）

```bash
# 生成 Gradle Wrapper（首次使用）
gradle wrapper --gradle-version 5.6.4

# Debug APK
./gradlew assembleDebug

# Release APK（ProGuard 混淆，规则见 app/proguard-rules.pro）
./gradlew assembleRelease
```

| 构建类型 | 输出路径 | 说明 |
|---------|---------|------|
| Debug | `app/build/outputs/apk/debug/` | 未混淆，可调试 |
| Release | `app/build/outputs/apk/release/` | ProGuard 混淆，需签名 |

> **Release 签名**：在 `app/build.gradle` 中配置 `signingConfigs`，或使用 Android Studio 的 Generate Signed Bundle 菜单。

### Gradle 依赖同步

修改 `build.gradle` 后新依赖需要同步：

```bash
./gradlew build --refresh-dependencies
```

## 测试

共 **101 个测试用例**，10 个测试类（9 个 JVM 单元测试 + 1 个 Android 仪器测试）。

### JVM 单元测试

纯逻辑测试，无需设备，秒级执行：

| 测试类 | 用例数 | 覆盖范围 |
|--------|-------|---------|
| `M3uParserTest` | 10 | M3U 格式解析（标准/异常/空输入） |
| `StreamTypeDetectorTest` | 10 | 流类型检测（HLS/RTMP/RTSP/HTTP-FLV） |
| `JsonSourceParserTest` | 9 | JSON 源格式解析 |
| `SourceRepositoryTest` | 14 | 源查询、优先级排序、失效切换 |
| `SourceUpdateRepositoryTest` | 7 | 源合并、去重、ETag 缓存逻辑 |
| `SourceHealthCheckerTest` | 5 | 健康检测探活、失效清理规则 |
| `OsdManagerTest` | 10 | OSD 状态机切换、超时恢复 |
| `ChannelListPresenterTest` | 8 | 频道列表分类、过滤、排序 |
| `SettingsPresenterTest` | 11 | 设置读写、解码模式切换 |

```bash
# 全部 JVM 测试
./gradlew test

# 运行指定测试类
./gradlew test --tests "com.tvlive.app.data.parser.M3uParserTest"

# 测试报告：app/build/reports/tests/
```

### Android 仪器测试

`ChannelDaoTest`（17 个用例）— Room DAO 的 CRUD、查询、索引约束验证。需要连接 Android 设备或启动模拟器（API 19+）。

```bash
# 连接设备后执行
./gradlew connectedAndroidTest

# 测试报告：app/build/reports/androidTests/
```

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

## 文档

| 文档 | 说明 |
|------|------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | MVP 架构、模块职责、关键约束 |
| [docs/PROGRESS.md](docs/PROGRESS.md) | 项目开发进度追踪 |
| [docs/superpowers/specs/2026-05-05-tv-live-design.md](docs/superpowers/specs/2026-05-05-tv-live-design.md) | 产品设计文档：交互模型、按键映射、技术方案 |

## License

MIT
