# React Dockerfile for production hosting
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies first (for better caching)
COPY package*.json ./
RUN npm ci --only=production=false

# Install missing dependencies including terser
# RUN npm install sonner @tanstack/react-table terser

# Copy source code
COPY . .

# Build with TypeScript errors ignored
RUN npm run build:docker || npx vite build --mode production

# Production stage
FROM nginx:alpine

# Copy built files
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy Nginx config from repo
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
