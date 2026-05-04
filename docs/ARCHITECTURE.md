# 电视直播应用 — 架构参考

## 概述
Android 4.4.2 (API 19) 电视直播应用，面向老年用户：打开即播、全屏沉浸、遥控器操作。

## 关键约束
- **minSdk 19**：无协程（Kotlin 1.3.72），用 `Thread` + `Handler`；Room 1.x DAO 同步查询，必须在后台线程调用
- **单 Activity**：`LivePlayerActivity` 是唯一 Activity，所有 UI 切换通过 OSD 覆盖层实现
- **SurfaceView**：不用 TextureView（API 19 兼容性差）

## 架构：MVP
```
View (Activity + OSD 组件) ←→ Presenter ←→ Model (DB + Repository)
```
- Presenter 通过构造参数注入 DAO，不直接依赖 TvliveApp.db（除 LivePlayerPresenter）
- Activity 持有 Presenter 引用，Presenter 通过回调/方法调用操作 View

## 模块说明

### 播放
- `player/PlayerManager` — IJKPlayer 封装，支持 AUTO/SOFTWARE/HARDWARE 解码
- `player/PlayerCallback` — onPrepared/onError/onInfo/onVideoSizeChanged

### OSD 状态机
`OsdManager` 管理状态：`IDLE → {CHANNEL_INFO, CHANNEL_LIST, SETTINGS, NUMBER_INPUT, VOLUME} → IDLE`
- 每个状态有独立 Timer，到期自动回 IDLE
- Activity 通过 `onStateChanged` 回调响应状态切换

### 源管理管道
1. **获取**：`SourceFetcher` 带 ETag 缓存（304 跳过）
2. **解析**：`M3uParser` / `JsonSourceParser` → `ParseResult`
3. **合并**：`SourceUpdateRepository.mergeToDatabase` — 按 epgId 匹配频道，URL 去重，配置域清理
4. **健康检测**：`SourceHealthChecker` — AlarmManager 每 6h，HEAD 探活（3s 超时），7 天 + failCount>=5 删除
5. **失败切换**：`SourceRepository.getNextAvailableSource` — 播放失败自动切备用源，全部失效则触发更新 + 5min 重试

### 数据库（Room 1.x）
| 表 | 用途 |
|------|------|
| channels | 频道元数据（编号、名称、分类、EPG ID、Logo） |
| sources | 播放源地址（URL、流类型、优先级、状态、失败计数） |
| favorites | 收藏（channel_id, created_at） |
| history | 观看历史（channel_id, watched_at），保留最近 100 条 |
| source_configs | 源配置（名称、URL、格式、ETag、启用状态） |

## 测试
- JVM 测试：纯逻辑（Presenter、Parser、Repository、HealthChecker）
- Android 仪器测试：`ChannelDaoTest` — DAO 操作（需设备/模拟器运行）

## 遥控器按键

| 按键 | 功能 |
|------|------|
| 上/下 | 切换频道 |
| 左/右 | 调节音量 |
| 数字 0-9 | 频道号输入（2 秒确认） |
| OK/ENTER | 打开/关闭频道列表 |
| 长按 OK | 收藏/取消收藏 |
| MENU | 设置 |
| BACK | 返回 / 取消输入 |
