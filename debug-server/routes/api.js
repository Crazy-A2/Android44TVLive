const express = require('express');

module.exports = function(sessionService, deviceService) {
  const router = express.Router();

  // GET /api/devices — 在线设备列表
  router.get('/devices', (req, res) => {
    res.json(deviceService.getOnlineDevices());
  });

  // GET /api/sessions — 历史会话列表
  router.get('/sessions', (req, res) => {
    res.json(sessionService.listSessions());
  });

  // GET /api/logs — 获取指定会话的日志
  router.get('/logs', (req, res) => {
    const { sessionId, offset, limit } = req.query;
    if (!sessionId) {
      return res.status(400).json({ error: '缺少 sessionId' });
    }
    const logs = sessionService.getLogs(
      sessionId,
      parseInt(offset) || 0,
      parseInt(limit) || 100
    );
    res.json(logs);
  });

  return router;
};
