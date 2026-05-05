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

## 3. 当前已知阻塞（非 SDK 问题）

执行 `gradle testDebugUnitTest` 时，当前失败点为 IJK 依赖解析失败：

- `tv.danmaku.ijk.media:ijkplayer-java:0.8.8`
- `tv.danmaku.ijk.media:ijkplayer-armv7a:0.8.8`
- `tv.danmaku.ijk.media:ijkplayer-exo:0.8.8`

根因：依赖仓库中不可解析（`google()` + `jcenter()` 当前不可用/不稳定）。

## 4. 后续会话执行顺序（推荐）

1. 先执行 2.1，确认 `gradle -v` 显示 Gradle 5.6.4 + JVM 1.8
2. 执行 `gradle tasks` 确认可构建
3. 执行 `gradle testDebugUnitTest` 复现/验证当前阻塞
4. 处理 IJK 依赖来源（改可用仓库或本地化依赖）后重跑测试

## 5. 备注

- 本项目是老电视兼容路线，核心版本基线：`minSdk 19`、`compileSdk/targetSdk 28`。
- 不要将本项目升级到新版本 Android SDK 基线（如 API 35）作为默认方案。