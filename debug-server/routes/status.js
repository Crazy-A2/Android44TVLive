const express = require('express');

module.exports = function(sessionService, deviceService) {
  const router = express.Router();

  // POST /api/status — App 上报设备状态 + 心跳
  router.post('/', (req, res) => {
    const {
      deviceId, sessionId, appStartTime,
      currentChannel, playbackState, decoderMode,
      memoryUsedMB, networkType,
      deviceInfo
    } = req.body;

    if (!deviceId || !sessionId) {
      return res.status(400).json({ error: '缺少必要字段: deviceId, sessionId' });
    }

    // 创建或更新会话
    sessionService.getOrCreate(sessionId, deviceInfo || {});
    sessionService.updateHeartbeat(sessionId);

    // 更新设备在线状态
    deviceService.update(deviceId, {
      sessionId, appStartTime,
      currentChannel, playbackState, decoderMode,
      memoryUsedMB, networkType
    });

    res.json({ ok: true });
  });

  return router;
};
