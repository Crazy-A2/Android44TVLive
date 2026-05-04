# Android 4.4.2 电视直播应用设计文档

## Context

为中老年用户开发智能电视直播应用。核心诉求：打开即看、遥控器换台、极简操作。后台自动管理直播源（抓取、比对、失效删除、拉取更新），用户无需关心源的问题。最低兼容 Android 4.4.2 (API 19)。

---

## 一、核心交互模型

**开机即全屏播放**——没有首页、没有频道浏览页，启动直接全屏播放上次收看的频道。

### 遥控器按键映射

| 按键 | 功能 |
|------|------|
| ⬆ / ⬇ | 上一个 / 下一个频道 |
| ⬅ / ➡ | 减小 / 增大音量 |
| OK / 确认 | 打开频道列表覆盖层 |
| 菜单 / Menu | 打开设置覆盖层 |
| 返回 / Back | 关闭当前覆盖层（不退出应用） |
| 数字键 0-9 | 直接输入频道号换台 |
| 长按 OK | 添加/取消收藏 |
| 静音键 | 切换静音 |

### OSD 覆盖层状态机

```
IDLE
├── [换台/调音量] ──> CHANNEL_INFO_SHOWING ── [3秒超时] ──> IDLE
├── [OK键] ──> CHANNEL_LIST_OPEN ── [Back/选中/5秒无操作] ──> IDLE
├── [菜单键] ──> SETTINGS_OPEN ── [Back] ──> IDLE
└── [数字键] ──> NUMBER_INPUTTING ── [2秒无输入确认换台/Back取消] ──> IDLE
```

互斥：打开新覆盖层前先关闭当前。Back 键优先关闭覆盖层，不退出 Activity。

---

## 二、架构：MVP + 单 Activity

```
LivePlayerActivity（唯一 Activity）
├── SurfaceView（全屏播放）
├── OSD 覆盖层管理器
│   ├── ChannelInfoBar（顶部频道信息条）
│   ├── ChannelListOverlay（右侧频道列表）
│   ├── SettingsOverlay（居中设置面板）
│   ├── VolumeBar（底部音量条）
│   └── ChannelNumberInput（频道号输入显示）
├── LivePlayerPresenter（核心调度）
└── PlayerManager（IJKPlayer 封装）

Service 层
├── SourceUpdateService（IntentService，源抓取更新）
└── SourceHealthChecker（AlarmManager 定时 + 即时触发）

数据层
├── ChannelRepository / SourceRepository / SourceUpdateRepository
├── M3uParser / JsonSourceParser
├── AppDatabase (Room 1.x)
└── SharedPreferences
```

---

## 三、数据库设计

### channels（频道表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, autoGenerate | 自增主键 |
| channel_number | Int | UNIQUE, NOT NULL | 频道号 |
| name | String | NOT NULL | 频道名称 |
| category | String | NOT NULL, DEFAULT "其他" | 分类（央视/卫视/地方） |
| logo_url | String | nullable | 台标 URL |
| epg_id | String | nullable, UNIQUE | 跨源去重标识 |
| sort_order | Int | NOT NULL, DEFAULT 0 | 排序权重 |
| is_visible | Boolean | NOT NULL, DEFAULT true | 是否可见 |
| created_at | Long | NOT NULL | 创建时间 |
| updated_at | Long | NOT NULL | 更新时间 |

### sources（源表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, autoGenerate | 自增主键 |
| channel_id | Long | FK→channels.id, NOT NULL | 所属频道 |
| url | String | NOT NULL | 播放流地址 |
| stream_type | String | NOT NULL, DEFAULT "hls" | hls/rtmp/rtsp/http-flv/other |
| quality | String | nullable | sd/hd/fhd |
| provider | String | nullable | 来源标注 |
| priority | Int | NOT NULL, DEFAULT 100 | 越小越优先，动态调整 |
| status | Int | NOT NULL, DEFAULT 0 | 0=正常, 1=缓慢, 2=失败, 3=检测中 |
| fail_count | Int | NOT NULL, DEFAULT 0 | 连续失败次数 |
| last_check_time | Long | nullable | 上次检测时间 |
| last_success_time | Long | nullable | 上次成功时间 |
| response_time_ms | Int | nullable | 响应耗时(ms) |
| source_config_id | Long | nullable, FK→source_config.id | 来自哪个源配置 |
| created_at | Long | NOT NULL | 创建时间 |
| updated_at | Long | NOT NULL | 更新时间 |

索引：`(channel_id ASC, priority ASC)` — 换台时最频繁的查询路径。

### favorites（收藏表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, autoGenerate | 自增主键 |
| channel_id | Long | FK→channels.id, UNIQUE, NOT NULL | 收藏频道 |
| created_at | Long | NOT NULL | 收藏时间 |

### history（观看历史表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, autoGenerate | 自增主键 |
| channel_id | Long | FK→channels.id, NOT NULL | 观看频道 |
| watched_at | Long | NOT NULL | 观看时间 |
| duration_sec | Long | nullable | 观看时长(秒) |

最多保留 100 条，超出删除最旧。

### source_config（源配置表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, autoGenerate | 自增主键 |
| name | String | NOT NULL | 配置名称 |
| url | String | NOT NULL | 源文件下载地址 |
| format | String | NOT NULL | m3u / json |
| is_builtin | Boolean | NOT NULL, DEFAULT false | 是否内置 |
| is_enabled | Boolean | NOT NULL, DEFAULT true | 是否启用 |
| last_update_time | Long | nullable | 上次更新时间 |
| update_interval_ms | Long | NOT NULL, DEFAULT 21600000 | 更新间隔(默认6h) |
| etag | String | nullable | HTTP ETag |
| created_at | Long | NOT NULL | 创建时间 |

---

## 四、核心流程

### 4.1 应用启动

```
TvliveApp.onCreate() → 初始化 DB / HttpClient / IJKPlayer
LivePlayerActivity.onCreate() → 全屏 + SurfaceView + PlayerManager + OsdManager
LivePlayerPresenter.start()
  → 频道表空？触发全量源更新，显示 loading
  → 读上次频道 ID，获取最佳源
  → PlayerManager.play(url)
  → 后台检查是否有源配置需要更新
```

### 4.2 换台（含源自动切换）

```
用户按 ⬆/⬇ → 计算目标频道
→ 立即显示频道信息条
→ getBestSource(channelId)
  → 有可用源 → play(url)
    → 成功 → reportSourceSuccess, 更新优先级
    → 失败 → 进入源自动切换
  → 无可用源 → 提示"暂无可用源" → 立即触发源更新

源自动切换:
  reportSourceFailed(sourceId) → fail_count++, >=3则status=FAILED, priority+=50
  → getNextAvailableSource(channelId, excludeFailedSourceId)
    → 有 → play(新源)
    → 无 → 提示不可用 + 触发源更新 + 5分钟后自动重试
```

### 4.3 源更新（抓取→解析→比对→入库）

```
触发: 启动时检查 / 手动触发 / 源全部失效时
SourceUpdateService (IntentService)
  → 遍历启用的 source_config
  → OkHttp 下载源文件（ETag 增量判断）
  → M3uParser 或 JsonSourceParser 解析
  → 比对入库:
    - epg_id 匹配现有频道 → 更新 name/logo/category
    - URL 已存在 → 更新 metadata
    - URL 不存在 → 插入新源
    - 新 epg_id → 插入新频道+源
  → 清理: 删除该配置下不在新结果中的旧源（不删用户手动添加的源）
  → LocalBroadcast 通知完成
```

### 4.4 源健康检测与自动修复

```
定时检测 (每6h, AlarmManager):
  → 查询 status=FAILED 的源
  → HTTP HEAD 探测（3秒超时）
  → HEAD成功 → 恢复NORMAL
  → HEAD失败 + 超过7天未成功 + fail_count>=5 → 删除
  → 查询"所有源都失效"的频道 → 触发源更新

播放失败即时检测:
  → reportSourceFailed → 切换备用源
  → 无可用源 → 删除失效源 → 立即触发源更新 → 新源入库后自动尝试播放

优先级动态调整:
  → 播放成功: priority恢复(-10), 记录response_time_ms
  → 响应<500ms: priority再减5 (快速源奖励)
  → 响应>3000ms: priority加10 (慢速源惩罚)
  → priority下限保护: 不低于1
```

---

## 五、直播源解析

### M3U/M3U8

```
#EXTM3U
#EXTINF:-1 tvg-id="cctv1" tvg-name="CCTV-1" tvg-logo="..." group-title="央视",CCTV-1 综合
http://live.example.com/cctv1/index.m3u8
```

- 按 `tvg-id` 合并同频道多源
- 缺少属性时容错：name 取逗号后文本，epg_id 可为 null
- URL 后缀推断 stream_type
- 编码容错：UTF-8 失败尝试 GBK

### JSON

```json
{
  "channels": [{
    "name": "CCTV-1 综合",
    "epg_id": "cctv1",
    "logo": "http://...",
    "category": "央视",
    "sources": [{
      "url": "http://...",
      "type": "hls",
      "quality": "hd",
      "provider": "主源"
    }]
  }]
}
```

- 使用 `org.json`（Android 原生，无需额外依赖）
- 单个频道解析失败跳过，继续后续
- type 缺失时按 URL 推断

### 解析中间模型

```kotlin
data class ParseResult(val channels: List<ParsedChannel>, val parseTimeMs: Long)
data class ParsedChannel(val name: String, val epgId: String?, val logoUrl: String?,
    val category: String, val sources: List<ParsedSource>)
data class ParsedSource(val url: String, val streamType: String,
    val quality: String?, val provider: String?)
```

---

## 六、技术栈与兼容性

| 项目 | 版本 | 原因 |
|------|------|------|
| AGP | 3.6.x | 最后支持 API 19 |
| Gradle | 5.6.4 | 配合 AGP 3.6 |
| JDK | 8 | AGP 3.6 要求 |
| Kotlin | 1.3.72 | 兼容旧版 AGP |
| compileSdkVersion | 28 | 不超过 29 |
| minSdkVersion | 19 | Android 4.4 |
| targetSdkVersion | 28 | 避免新限制适配 |
| Room | 1.1.1 | 兼容 API 14+ |
| OkHttp | 3.12.x | 最后支持 API 14+ |
| Glide | 4.11.0 | 兼容 API 14+ |
| IJKPlayer | armeabi-v7a only | 减少 APK 体积 |

### API 19 关键约束

- 权限：安装时授予，无需动态申请
- 后台服务：IntentService 正常工作
- 全屏：`SYSTEM_UI_FLAG_IMMERSIVE_STICKY`（API 19 引入）
- HTTPS：需手动启用 TLS 1.2
- 协程：不引入，用 AsyncTask + Handler
- 播放器：SurfaceView 优于 TextureView（API 19 渲染效率）
- 内存：目标设备 RAM 512MB，DB/HTTPClient 单例，Glide 限制内存缓存

### IJKPlayer 优化参数

```
analyzeduration = 1000000  (1秒分析)
probesize = 1048576        (1MB探测)
start-on-prepared = 1
framedrop = 1
skip_loop_filter = 48
```

硬解失败自动降级软解（监听 MEDIA_CODEC_ERROR）。

---

## 七、功能模块清单

### 第一版（基础完整）

1. 全屏直播播放（IJKPlayer + SurfaceView）
2. 遥控器换台（上下切频道、左右调音量、数字键直输）
3. OSD 覆盖层（频道信息条、频道列表、音量条、频道号输入、设置面板）
4. 源自动管理（启动检查 + 手动触发、M3U/JSON 解析、多源排序、失效删除+拉取更新）
5. 收藏频道
6. 观看历史（最近 100 条）
7. 设置中心（解码模式、网络检测、源管理、关于）

### 不做

- EPG 电子节目指南
- 回看/时移
- 录制功能
- 多用户/账号系统
