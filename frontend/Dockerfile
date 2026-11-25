# ==============================================================================
# STAGE 1: Build Vue 3 with Vite & Node.js 22 Alpine
# ==============================================================================
FROM node:22-alpine AS builder

WORKDIR /app

# Copy dependency definitions and install
COPY package*.json ./
RUN npm ci

# Copy source code and build production bundle
COPY . .
RUN npm run build

# ==============================================================================
# STAGE 2: Lightweight Production Web Server (Nginx Alpine < 25MB)
# ==============================================================================
FROM nginx:1.27-alpine

# Copy built SPA static assets to Nginx html root
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration with SPA routing and API reverse proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
