const express = require('express');

module.exports = function(sessionService) {
  const router = express.Router();

  // POST /api/log — App 上报日志
  router.post('/', (req, res) => {
    const { sessionId, level, tag, message } = req.body;
    if (!sessionId || !level || !message) {
      return res.status(400).json({ error: '缺少必要字段: sessionId, level, message' });
    }

    const logEntry = {
      timestamp: new Date().toISOString(),
      level: level.toUpperCase(),
      tag: tag || '',
      message
    };

    sessionService.appendLog(sessionId, logEntry);
    res.json({ ok: true });
  });

  return router;
};
