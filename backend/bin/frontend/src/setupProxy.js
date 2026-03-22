const { createProxyMiddleware } = require('http-proxy-middleware');

/**
 * setupProxy.js — forwards ALL /api requests (GET, POST, PUT, DELETE)
 * from React dev server (port 3000) to Spring Boot (port 5000).
 *
 * This replaces the simple "proxy" line in package.json which
 * doesn't reliably forward DELETE requests.
 *
 * After adding this file: restart React with npm start.
 * Also run: npm install http-proxy-middleware  (if not already installed)
 */
module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:5000',
      changeOrigin: true,
    })
  );
};
