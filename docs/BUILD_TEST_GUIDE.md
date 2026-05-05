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

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot"
export PATH="/c/Software/gradle/gradle-5.6.4/bin:$JAVA_HOME/bin:$PATH"
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

**`gradle testDebugUnitTest` 已通过** — 84 tests completed, BUILD SUCCESSFUL ✅

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

1. 执行 2.1 环境变量，确认 `gradle -v` 显示 Gradle 5.6.4 + JVM 1.8
2. `gradle tasks` 确认可构建
3. `gradle testDebugUnitTest` 验证 84 测试通过
4. `gradle assembleDebug` 构建 APK

## 6. 备注

- 本项目是老电视兼容路线，核心版本基线：`minSdk 19`、`compileSdk/targetSdk 28`。
- 不要将本项目升级到新版本 Android SDK 基线（如 API 35）作为默认方案。
- Room 1.x DAO 查询是同步的，必须在后台线程调用（无协程，用 `Thread` + `Handler`）。