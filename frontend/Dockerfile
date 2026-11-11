# =====================================================================
# Imagen del frontend en dos etapas:
#  1) Node compila React + TypeScript a archivos estáticos (dist/)
#  2) Nginx sirve esos archivos y reenvía /api al backend
# =====================================================================
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
