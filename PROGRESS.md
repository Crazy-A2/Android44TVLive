# 电视直播应用 — 进度追踪

**项目**：Android 4.4.2 电视直播应用  
**状态**：开发中  

---

## 已完成

### ✅ 阶段 1：项目骨架搭建
- 步骤 1.1 — 创建 Android 项目（build.gradle、依赖配置、AndroidManifest）
- 步骤 1.2 — 实现数据库层（Entity、DAO、AppDatabase、TvliveApp）
- 步骤 1.3 — 实现 LivePlayerActivity 基本布局（全屏横屏、SurfaceView、状态栏隐藏）

### ✅ 阶段 2：播放核心
- 步骤 2.1 — 集成 IJKPlayer，实现 PlayerManager（含回调接口、解码模式、优化参数）
- 步骤 2.2 — 实现内置源加载与启动播放（assets/builtin_sources.m3u、M3uParser、首次启动自动播放）
- 步骤 2.3 — 实现遥控器换台与音量调节（LivePlayerPresenter、ChannelNumberInput、按键分发）
- 步骤 2.4 — 实现源自动切换（SourceRepository、播放失败自动切换备用源、优先级动态调整）

### 测试
- StreamTypeDetectorTest（JVM）：9 个用例
- M3uParserTest（JVM）：10 个用例
- SourceRepositoryTest（JVM）：14 个用例
- ChannelDaoTest（Android）：15 个用例

---

## 进行中

### 🔄 步骤 4.4：联通源健康检测与自动修复完整流程
- 待开始

---

## 待完成

### 阶段 2 剩余
- 步骤 2.4 — 源自动切换（SourceRepository、播放失败自动切换备用源）

### 阶段 3：OSD 覆盖层
- 步骤 3.1 — OsdManager + ChannelInfoBar **[已完成]**
- 步骤 3.2 — ChannelListOverlay（右侧频道列表，分类浏览、收藏）**[已完成]**
- 步骤 3.3 — VolumeBar + ChannelNumberInput（音量条、频道号输入）**[ChannelNumberInput 已完成]**
- 步骤 3.4 — SettingsOverlay（解码模式、源管理、关于）**[已完成]**

### 阶段 4：源管理
- 步骤 4.1 — JsonSourceParser **[已完成]**
- 步骤 4.2 — SourceUpdateService **[已完成]**
- 步骤 4.3 — SourceHealthChecker（AlarmManager 定时检测、HEAD 探活、失效删除）**[已完成]**
- 步骤 4.4 — 联通源健康检测与自动修复完整流程 **[已完成]**

### 阶段 5：收藏、历史与收尾
- 步骤 5.1 — 收藏功能（长按 OK 添加/取消收藏）**[已完成]**
- 步骤 5.2 — 观看历史（自动记录、最多 100 条）**[已完成]**
- 步骤 5.3 — 性能优化（RecyclerView 稳定 ID/禁用动画、PlayerManager 表面释放）**[已完成]**
- 步骤 5.4 — 真机测试

---

## 技术栈

| 项目 | 版本 |
|------|------|
| AGP | 3.6.4 |
| Gradle | 5.6.4 |
| Kotlin | 1.3.72 |
| minSdk | 19 (Android 4.4) |
| compileSdk | 28 |
| Room | 1.1.1 |
| OkHttp | 3.12.13 |
| Glide | 4.11.0 |
| IJKPlayer | 0.8.8 (armv7a) |
