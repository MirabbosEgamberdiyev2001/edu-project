# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Cache node_modules layer
COPY package*.json ./
RUN npm ci --silent

# Copy source
COPY . .

# Build args — Vite bakes these into the JS bundle at build time.
# Default is relative /api so nginx can proxy it regardless of domain/IP.
ARG VITE_API_BASE_URL=/api
ARG VITE_WS_URL=/ws
ARG VITE_GOOGLE_CLIENT_ID=237372892439-4bju17u6k3cjoil26p148m21ilmecd9s.apps.googleusercontent.com

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

RUN npm run build

# ── Stage 2: Serve ───────────────────────────────────────────────────────────
FROM nginx:1.25-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy built React app
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy our nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
