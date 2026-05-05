const express = require('express');

module.exports = function(deviceService) {
  const router = express.Router();

  // GET /api/command — App 拉取待执行命令
  router.get('/', (req, res) => {
    const { deviceId } = req.query;
    if (!deviceId) {
      return res.status(400).json({ error: '缺少 deviceId' });
    }

    const command = deviceService.popCommand(deviceId);
    if (!command) {
      return res.json({ command: null });
    }
    res.json({ command });
  });

  // POST /api/command — Dashboard 下发命令
  router.post('/', (req, res) => {
    const { deviceId, type, params } = req.body;
    if (!deviceId || !type) {
      return res.status(400).json({ error: '缺少必要字段: deviceId, type' });
    }

    if (!deviceService.isOnline(deviceId)) {
      return res.status(404).json({ error: '设备不在线' });
    }

    const command = {
      commandId: `cmd-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      type,
      params: params || {},
      createdAt: new Date().toISOString()
    };

    deviceService.pushCommand(deviceId, command);
    res.json({ ok: true, command });
  });

  // POST /api/command/:id/ack — App 确认命令已执行
  router.post('/:id/ack', (req, res) => {
    const { deviceId } = req.body;
    if (!deviceId) {
      return res.status(400).json({ error: '缺少 deviceId' });
    }
    deviceService.ackCommand(deviceId, req.params.id);
    res.json({ ok: true });
  });

  return router;
};
