const fs = require('fs');
const path = require('path');

const MAX_SESSIONS = 10;

class SessionService {
  constructor(sessionsDir) {
    this.sessionsDir = sessionsDir;
    if (!fs.existsSync(sessionsDir)) {
      fs.mkdirSync(sessionsDir, { recursive: true });
    }
  }

  // 创建或获取会话
  getOrCreate(sessionId, deviceInfo) {
    const filePath = this._filePath(sessionId);
    if (fs.existsSync(filePath)) {
      return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    }
    const session = {
      sessionId,
      deviceInfo,
      startTime: new Date().toISOString(),
      lastHeartbeat: new Date().toISOString(),
      logs: []
    };
    fs.writeFileSync(filePath, JSON.stringify(session, null, 2), 'utf-8');
    this._cleanup();
    return session;
  }

  // 追加日志
  appendLog(sessionId, logEntry) {
    const filePath = this._filePath(sessionId);
    if (!fs.existsSync(filePath)) return;
    const session = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    session.logs.push(logEntry);
    session.lastHeartbeat = new Date().toISOString();
    fs.writeFileSync(filePath, JSON.stringify(session, null, 2), 'utf-8');
  }

  // 更新心跳
  updateHeartbeat(sessionId) {
    const filePath = this._filePath(sessionId);
    if (!fs.existsSync(filePath)) return;
    const session = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    session.lastHeartbeat = new Date().toISOString();
    fs.writeFileSync(filePath, JSON.stringify(session, null, 2), 'utf-8');
  }

  // 获取所有会话列表（按时间倒序）
  listSessions() {
    if (!fs.existsSync(this.sessionsDir)) return [];
    return fs.readdirSync(this.sessionsDir)
      .filter(f => f.endsWith('.json'))
      .sort()
      .reverse()
      .map(f => {
        const data = JSON.parse(fs.readFileSync(path.join(this.sessionsDir, f), 'utf-8'));
        return {
          sessionId: data.sessionId,
          deviceInfo: data.deviceInfo,
          startTime: data.startTime,
          lastHeartbeat: data.lastHeartbeat,
          logCount: data.logs.length
        };
      });
  }

  // 获取会话详情（含日志）
  getSession(sessionId) {
    const filePath = this._filePath(sessionId);
    if (!fs.existsSync(filePath)) return null;
    return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
  }

  // 获取会话日志（支持分页）
  getLogs(sessionId, offset = 0, limit = 100) {
    const session = this.getSession(sessionId);
    if (!session) return [];
    return session.logs.slice(offset, offset + limit);
  }

  _filePath(sessionId) {
    return path.join(this.sessionsDir, `${sessionId}.json`);
  }

  // 保留最近 MAX_SESSIONS 个，删除最早的
  _cleanup() {
    if (!fs.existsSync(this.sessionsDir)) return;
    const files = fs.readdirSync(this.sessionsDir)
      .filter(f => f.endsWith('.json'))
      .sort();
    while (files.length > MAX_SESSIONS) {
      const oldest = files.shift();
      fs.unlinkSync(path.join(this.sessionsDir, oldest));
    }
  }
}

module.exports = { SessionService };
