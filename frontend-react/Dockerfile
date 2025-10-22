# === React Dockerfile ===
FROM node:20-alpine AS build
WORKDIR /app

# Copy package.json first for caching
COPY package*.json ./
RUN npm install

# Copy all source files
COPY . .

# Run build (ignores TS errors if configured in tsconfig or package.json script)
RUN npm run build

# === Serve stage (optional, if using nginx) ===
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
