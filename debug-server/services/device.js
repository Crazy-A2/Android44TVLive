const HEARTBEAT_TIMEOUT_MS = 90000; // 90 秒

class DeviceService {
  constructor() {
    this.devices = new Map(); // deviceId -> { ...status, lastSeen, pendingCommands }
  }

  // 注册或更新设备状态
  update(deviceId, status) {
    const existing = this.devices.get(deviceId);
    this.devices.set(deviceId, {
      ...status,
      deviceId,
      lastSeen: Date.now(),
      pendingCommands: existing ? existing.pendingCommands : []
    });
  }

  // 判断设备是否在线
  isOnline(deviceId) {
    const device = this.devices.get(deviceId);
    if (!device) return false;
    return (Date.now() - device.lastSeen) < HEARTBEAT_TIMEOUT_MS;
  }

  // 获取在线设备列表
  getOnlineDevices() {
    const now = Date.now();
    const online = [];
    for (const [id, device] of this.devices) {
      if (now - device.lastSeen < HEARTBEAT_TIMEOUT_MS) {
        online.push({
          deviceId: id,
          sessionId: device.sessionId,
          currentChannel: device.currentChannel,
          playbackState: device.playbackState,
          decoderMode: device.decoderMode,
          memoryUsedMB: device.memoryUsedMB,
          networkType: device.networkType,
          lastSeen: new Date(device.lastSeen).toISOString()
        });
      }
    }
    return online;
  }

  // 下发命令
  pushCommand(deviceId, command) {
    const device = this.devices.get(deviceId);
    if (!device) return false;
    device.pendingCommands.push(command);
    return true;
  }

  // 拉取待执行命令
  popCommand(deviceId) {
    const device = this.devices.get(deviceId);
    if (!device || device.pendingCommands.length === 0) return null;
    return device.pendingCommands.shift();
  }

  // 确认命令已执行
  ackCommand(deviceId, commandId) {
    // 命令已在 popCommand 时移除，无需额外操作
    return true;
  }
}

module.exports = { DeviceService };
