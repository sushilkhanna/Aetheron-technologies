const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// ──────────────────────────────────────────────────────────────────────
// This Express server is NO LONGER used for auth or data.
// All API calls now go directly to the Java Spring Boot backend (port 8080).
// This file is kept only as a fallback/reference.
// ──────────────────────────────────────────────────────────────────────

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Express proxy server running. Auth handled by Java backend on port 8080.' });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
});

app.listen(PORT, () => {
  console.log(`🚀 BikePooling Express server running on port ${PORT}`);
  console.log(`ℹ️  Auth & data served by Java backend on port 8080`);
});
