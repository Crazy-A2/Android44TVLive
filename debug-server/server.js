const express = require('express');
const path = require('path');
const { DeviceService } = require('./services/device');
const { SessionService } = require('./services/session');

const DEFAULT_PORT = 9753;
const port = parseInt(process.argv.find((a, i) => process.argv[i - 1] === '--port') || DEFAULT_PORT, 10);

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const sessionService = new SessionService(path.join(__dirname, 'sessions'));
const deviceService = new DeviceService();

// 路由
const logRouter = require('./routes/log')(sessionService);
const statusRouter = require('./routes/status')(sessionService, deviceService);
const commandRouter = require('./routes/command')(deviceService);
const apiRouter = require('./routes/api')(sessionService, deviceService);

app.use('/api/log', logRouter);
app.use('/api/status', statusRouter);
app.use('/api/command', commandRouter);
app.use('/api', apiRouter);

app.listen(port, '0.0.0.0', () => {
  console.log(`[debug-server] 监听端口: ${port}`);
  console.log(`[debug-server] Dashboard: http://localhost:${port}`);
});
