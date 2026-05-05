# 构建与测试操作指南（会话复用）

适用项目：Android 4.4.2 电视直播应用（AGP 3.6.4 / Gradle 5.6.4 / Kotlin 1.3.72）

## 1. 结论摘要（本次已验证）

### 1.1 Android SDK 组件
已确认以下组件已安装并满足项目要求：

- `platforms;android-28`
- `build-tools;28.0.3`
- `platform-tools`
- `cmdline-tools;latest`

`sdk.dir` 当前路径：`C:\Users\qwe45\AppData\Local\Android\Sdk`

### 1.2 Gradle 与 JDK
- 可用 Gradle：`C:\Software\gradle\gradle-5.6.4\bin\gradle`
- 项目构建使用：JDK 8
  - `C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot`
- `sdkmanager` 运行需要 JDK 17+
  - 本机可用：`C:\Software\Java\25`

### 1.3 已修复配置
`gradle.properties` 已补充：

- `android.useAndroidX=true`
- `android.enableJetifier=true`

## 2. 固定命令模板

> 说明：以下为 Git Bash 下命令模板。

### 2.1 构建/测试前环境变量（JDK 8 + Gradle 5.6.4）

已预置在 `~/.bashrc`，新终端自动加载。手动 source 用：

```bash
source ~/.bashrc
```

配置内容：

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot"
export ANDROID_HOME="/c/Users/qwe45/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_HOME="/c/Software/gradle/gradle-5.6.4"
export PATH="$GRADLE_HOME/bin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

### 2.2 SDK 管理命令前环境变量（JDK 17+）

```bash
export JAVA_HOME="/c/Software/Java/25"
export PATH="$JAVA_HOME/bin:$PATH"
```

### 2.3 验证 SDK 已安装项

```bash
"/c/Users/qwe45/AppData/Local/Android/Sdk/cmdline-tools/latest/bin/sdkmanager.bat" --list_installed
```

预期关键项包含：

- `build-tools;28.0.3`
- `platforms;android-28`
- `platform-tools`
- `cmdline-tools;latest`

### 2.4 验证项目构建链路

```bash
gradle tasks
```

预期：`BUILD SUCCESSFUL`

### 2.5 执行 JVM 单元测试

```bash
gradle testDebugUnitTest
```

## 3. 当前状态

**`gradle testDebugUnitTest` 已通过** — 107 tests completed, BUILD SUCCESSFUL ✅（2026-05-05 验证）

## 4. 已解决问题记录

### 4.1 IJKPlayer 依赖（jcenter 已下线）

将 IJKPlayer 从远程依赖改为本地源码模块（从 `ijkplayer-master.zip` 提取）：

| 模块 | 原远程依赖 | 现本地模块 |
|------|-----------|-----------|
| ijkplayer-java | `tv.danmaku.ijk.media:ijkplayer-java:0.8.8` | `:ijkplayer-java` |
| ijkplayer-armv7a | `tv.danmaku.ijk.media:ijkplayer-armv7a:0.8.8` | `:ijkplayer-armv7a` |

- `ijkplayer-exo` 未提取（依赖 ExoPlayer r1.5.11，同样不可解析）
- 本地模块 build.gradle 硬编码 `compileSdkVersion 28`、`buildToolsVersion "28.0.3"`
- `settings.gradle` 已添加 `include ':ijkplayer-java'`, `':ijkplayer-armv7a'`

### 4.2 Room 注解（AndroidX 迁移）

Room 使用 AndroidX 注解路径，不是旧的 `android.arch.persistence.room.*`：

```kotlin
import androidx.room.Entity
import androidx.room.Dao
import androidx.room.Database
// 等
```

涉及 11 个文件（entity、dao、database）。`gradle.properties` 已开启 `android.useAndroidX=true`。

### 4.3 LocalBroadcastManager

使用 AndroidX 版本：
```kotlin
import androidx.localbroadcastmanager.content.LocalBroadcastManager
```
而不是 `android.support.v4.content.LocalBroadcastManager`。

### 4.4 生产代码 Bug 修复

- **M3uParser**：`addSourceToChannel` 中当频道尚未被 `pushCurrent()` 创建时，自动在 map 中创建频道
- **StreamTypeDetector**：检测前先去除 URL 查询字符串（`?query=...`），避免 `.m3u8?token=x` 被误判

### 4.5 测试基础设施

| 问题 | 解决 |
|------|------|
| PreferenceHelper 是 final 类 | 添加 `test/resources/mockito-extensions/org.mockito.plugins.MockMaker` → `mock-maker-inline` |
| `org.json.JSONObject` JVM 测试中不存在 | 添加 `testImplementation 'org.json:json:20231013'` |
| `any(Foo::class.java)` 返回 null，Kotlin 非空类型拒绝 | 使用 `(any(Foo::class.java) as? Foo) ?: Foo()` 模式 |
| `as X` 转型在 Kotlin 中检查 nullability 抛出 TypeCastException | 改用 `as?` 安全转型 + `?:` 默认实例 |

### 4.6 布局 & 资源

- `app/src/main/res/mipmap-hdpi/ic_launcher.png` — 补充缺失的启动图标
- 布局中 `45%` / `60%` 的 `layout_width` 改为固定 `480dp` / `640dp`（百分比在部分版本不兼容）
- `PlayerManager` 中 `OnVideoSizeChangedListener` SAM 接口有 5 个参数（含 sar_num, sar_den）

## 5. 后续会话执行顺序（推荐）

1. （新终端已自动加载 `~/.bashrc`，否则 `source ~/.bashrc`）确认 `gradle -v` 显示 Gradle 5.6.4 + JVM 1.8
2. `gradle tasks` 确认可构建
3. `gradle testDebugUnitTest` 验证 107 测试通过
4. `gradle assembleDebug` 构建 APK

## 6. 2026-05-05 新增（第二轮开发）

### 6.1 新增功能
- **频道 Logo 显示**：ChannelListAdapter + ChannelInfoBar 用 Glide 加载 logoUrl（圆形裁剪）
- **EPG 文本**：ChannelInfoBar 新增 `channel_epg` TextView
- **响应时间统计**：LivePlayerPresenter 记录播放启动时间节点，onPlaybackPrepared 时计算耗时替代硬编码 0
- **默认源预置**：`assets/default_source_configs.json`，首次启动自动导入

### 6.2 新增测试
- 新增 `LivePlayerPresenterTest`（23 用例）
- 测试总数：84 → **107**

### 6.3 可测化重构
- LivePlayerPresenter 新构造参数 `sourceRepository`、`handler`（均带默认值，向后兼容）
- `channels`、`currentIndex`、`isReady`、`currentSourceId` 改为 `internal`

### 6.4 资源抽取
- 新增 `colors.xml`（19 色值）、`dimens.xml`（15 维度），4 个布局 XML 已引用

### 6.5 CI
- `.github/workflows/ci.yml`：JDK 8 + Android SDK 28

### 6.6 清理
- 删除 `ijkplayer-exo/` 模块

## 7. Bugreport 日志分析（快速参考）

本节供后续会话拿到模拟器 bugreport 时快速定位问题用。

### 7.1 关键文件

| 文件 | 内容 | 用途 |
|------|------|------|
| `bugreport/bugreport-*.txt` | 主报告（dumpsys + kernel log + logcat） | **核心分析入口** |
| `bugreport/dumpstate_log.txt` | dumpstate 过程记录 | 确认 logcat 是否抓取成功 |
| `avd_details.txt` | AVD 配置（API 级别、CPU、内存） | 确认环境参数 |

### 7.2 快速分析命令（Git Bash）

```bash
DIR="bugreport 目录路径"

# 1. 确认 logcat 是否成功
grep -i "logcat.*fail\|logcat.*error" "$DIR/bugreport/dumpstate_log.txt"

# 2. 查 FATAL / ANR / tombstone（全文件）
grep -n -i "FATAL\|ANR\|tombstone" "$DIR/bugreport/bugreport-*.txt" | head -30

# 3. 查 com.tvlive.app 所有异常（本项目包名）
grep -n "com.tvlive.app" "$DIR/bugreport/bugreport-*.txt" | grep -i "crash\|fatal\|error\|exception" | head -20

# 4. 查 Activity 生命周期事件（am_* 协议缓冲区行）
grep -n "am_crash\|am_create_activity\|am_finish_activity" "$DIR/bugreport/bugreport-*.txt"

# 5. 查进程死亡
grep -n "process.*died\|FATAL EXCEPTION" "$DIR/bugreport/bugreport-*.txt" | head -20

# 6. 查内存状态
grep -n "MemTotal\|MemFree\|MemAvailable" "$DIR/bugreport/bugreport-*.txt"
```

### 7.3 阅读理解顺序

1. `dumpstate_log.txt` 第 27 行 → 确认 `logcat read failure` 有无
2. 搜索 `FATAL EXCEPTION` → 定位崩溃进程、异常类型、堆栈
3. 搜索 `am_crash` → 确认崩溃的 Activity 和异常消息（protocol buffer 格式，更简洁）
4. 搜索 `ANR` → 确认是否有无响应
5. 查看 `MemTotal` / `MemFree` → 排除 OOM
6. 找到崩溃栈顶 `at com.tvlive.app.xxx(Xxx.kt:行号)` → **直接读源码对应行**

### 7.4 本次实战案例

**Bug**：`com.tvlive.app` 进程反复崩溃，每次启动即死。

**从 bugreport 定位到的根因**（am_crash 行）：
```
am_crash: [4192,0,com.tvlive.app,...,kotlin.UninitializedPropertyAccessException,
            lateinit property playerManager has not been initialized,LivePlayerActivity.kt,134]
```

**对应源码**：[LivePlayerActivity.kt:134](app/src/main/java/com/tvlive/app/ui/activity/LivePlayerActivity.kt#L134)
```kotlin
presenter = LivePlayerPresenter(this, playerManager, prefs)  // playerManager 未初始化
```

`initPlayer()`（第 309 行）定义了 `playerManager = PlayerManager()` 但 `onCreate` 中从未调用。

**修复**：第 134 行前插入 `initPlayer()`，确保 `playerManager` 在使用前已初始化。

### 7.5 常见异常速查

| 异常类型 | 含义 | 典型原因 |
|---------|------|---------|
| `kotlin.UninitializedPropertyAccessException` | lateinit 属性未初始化 | 忘记调用初始化方法 |
| `NullPointerException` | 空指针 | 同上，或异步回调时 View 已销毁 |
| `IllegalStateException` | 非法状态 | SurfaceView 在 Surface 创建前使用 |
| `android.database.sqlite.SQLiteCantOpenDatabaseException` | 数据库打不开 | Room 初始化失败或路径问题 |
| `WindowManager$BadTokenException` | 无效 Window Token | Activity 已销毁但仍尝试显示 Dialog |

## 8. 备注

- 本项目是老电视兼容路线，核心版本基线：`minSdk 19`、`compileSdk/targetSdk 28`。
- 不要将本项目升级到新版本 Android SDK 基线（如 API 35）作为默认方案。
- Room 1.x DAO 查询是同步的，必须在后台线程调用（无协程，用 `Thread` + `Handler`）。