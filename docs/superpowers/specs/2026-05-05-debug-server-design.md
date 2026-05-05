# debug-server 设计文档

## Context

App 部署在 Android TV 上后，缺少远程调试手段：无法实时查看日志、监控播放状态、远程执行操作。当前仅靠 `e.printStackTrace()` 输出到 logcat，需连接 adb 才能看到，对已部署设备不友好。

目标：在项目根目录新建 `debug-server/`，App 端新增 `DebugClient`，实现局域网内远程调试和运维。

## 方案

纯 REST HTTP 双向通信。App 用现有 OkHttp 上报日志/状态、轮询拉取命令。服务端用 Node.js + Express。

选择理由：代码量最小，复用现有 OkHttp 依赖，3-5 秒轮询延迟对调试场景足够。

## 整体架构

```
┌─────────────────┐         HTTP          ┌──────────────────┐
│   Android TV     │  ──── POST /log ────→ │  debug-server     │
│   App            │  ──── POST /status ──→ │  (Node.js+Express)│
│                  │  ←── GET /command ──── │                   │
│  DebugClient     │                        │  REST API         │
│  (Kotlin+OkHttp) │                        │  + Web Dashboard  │
└─────────────────┘                        └──────────────────┘
```

## API 端点

| 方法 | 路径 | 调用方 | 用途 |
|------|------|--------|------|
| POST | `/api/log` | App | 上报日志 |
| POST | `/api/status` | App | 上报设备状态 + 心跳 |
| GET | `/api/command` | App | 拉取待执行命令 |
| POST | `/api/command/:id/ack` | App | 确认命令已执行 |
| GET | `/api/devices` | Dashboard | 查看在线设备 |
| POST | `/api/command` | Dashboard | 下发命令 |
| GET | `/api/logs` | Dashboard | 查看日志 |
| GET | `/api/sessions` | Dashboard | 查看历史会话列表 |

## 数据模型

### 日志条目
```json
{
  "timestamp": "2026-05-05T14:30:22.123",
  "level": "ERROR",
  "tag": "PlayerManager",
  "message": "播放失败: source url unreachable"
}
```

level 枚举：`ERROR`, `WARN`, `INFO`

### 设备状态（心跳）
```json
{
  "deviceId": "android-tv-192.168.1.100",
  "sessionId": "2026-05-05_14-30-22",
  "appStartTime": "2026-05-05T14:30:22",
  "currentChannel": "CCTV-1",
  "playbackState": "PLAYING",
  "decoderMode": "AUTO",
  "memoryUsedMB": 85,
  "networkType": "WIFI"
}
```

### 命令
```json
{
  "commandId": "cmd-001",
  "type": "RELOAD_SOURCES",
  "params": {},
  "createdAt": "2026-05-05T14:35:00"
}
```

deviceId 生成规则：`android-tv-{IP地址后段}`，如 `android-tv-1-100`（来自 192.168.1.100）。若 IP 不可用则用 `android-tv-{Build.SERIAL}`。

命令确认机制：App 拉取命令并执行后，POST `/api/command/{commandId}/ack` 确认。服务端收到确认后不再重复下发该命令。超时 60 秒未确认则重新下发。

命令类型枚举：

| type | 说明 | params |
|------|------|--------|
| `RELOAD_SOURCES` | 重新加载源 | 无 |
| `SWITCH_CHANNEL` | 切换频道 | `{ channelNumber: 1 }` |
| `SWITCH_DECODER` | 切换解码模式 | `{ mode: "SOFTWARE" }` |
| `GET_LOGCAT` | 抓取 logcat 并上报 | 无 |
| `RESTART_APP` | 重启 App | 无 |

## 会话持久化

日志按会话持久化到磁盘，每个会话以 App 启动时间命名。

**存储位置**：`debug-server/sessions/`

**文件格式**：`{启动时间}.json`，如 `2026-05-05_14-30-22.json`

**Session 文件结构**：
```json
{
  "sessionId": "2026-05-05_14-30-22",
  "deviceInfo": {
    "model": "Mi Box S",
    "androidVersion": "9",
    "ip": "192.168.1.100",
    "appId": "com.tvlive.app",
    "appVersion": "1.0.0"
  },
  "startTime": "2026-05-05T14:30:22",
  "lastHeartbeat": "2026-05-05T14:45:10",
  "logs": [
    { "timestamp": "...", "level": "ERROR", "tag": "PlayerManager", "message": "..." }
  ]
}
```

**清理策略**：保留最近 10 个会话文件，超出自删除最早的。

## debug-server 目录结构

```
debug-server/
├── package.json
├── server.js          # 入口，Express 启动
├── routes/
│   ├── log.js         # POST /api/log
│   ├── status.js      # POST /api/status
│   ├── command.js     # GET/POST /api/command
│   └── dashboard.js   # Dashboard 页面路由
├── services/
│   ├── session.js     # 会话管理（创建、查找、清理）
│   └── device.js      # 设备在线状态管理
├── public/            # Dashboard 静态文件
│   └── index.html
└── sessions/          # 持久化目录（gitignore）
```

端口：9753（可通过 `node server.js --port 8080` 指定）

## App 端 DebugClient

**包名**：`com.tvlive.app.debug`

**服务器发现流程**：
1. 读取 SharedPreferences 中缓存的地址
2. 尝试 HEAD 请求，成功则使用
3. 失败则扫描局域网同网段 9753 端口
4. 找到则缓存，未找到则 30 秒后重试

**上报策略**：
- 日志：错误发生时立即 POST `/api/log`
- 状态/心跳：每 30 秒 POST `/api/status`
- 命令轮询：每 5 秒 GET `/api/command`

**集成点**：
- `LivePlayerActivity.onCreate()` → `DebugClient.start(context)`
- `LivePlayerActivity.onDestroy()` → `DebugClient.stop()`
- 替换 `e.printStackTrace()` → `DebugClient.logError(tag, e)`

**关键约束**：
- minSdk 19 兼容，用 Thread + Handler，不用协程
- 仅 `BuildConfig.DEBUG` 时启用，release 不编译
- 网络失败不阻塞主线程，静默重试

## Web Dashboard

简单 HTML 页面，无前端框架：
- 左侧：在线设备列表 + 历史会话列表
- 右侧：选中设备的日志流 + 状态信息 + 命令下发按钮
- 自动刷新（meta refresh 或 setInterval 拉取）

## 验证方案

1. 启动 debug-server：`cd debug-server && node server.js`
2. 浏览器访问 `http://localhost:9753`，确认 Dashboard 可打开
3. App 连接电视，启动 App，确认 Dashboard 显示设备在线
4. App 中触发错误，确认 Dashboard 实时显示日志
5. Dashboard 下发 `RELOAD_SOURCES` 命令，确认 App 执行
6. 重启 App，确认新 session 创建，旧 session 保留
7. 创建 11 个 session，确认最早的被自动删除
