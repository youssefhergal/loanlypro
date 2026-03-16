# Stage 1 : build Angular
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2 : servir avec nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Supprimer les fichiers par défaut
RUN rm -rf /usr/share/nginx/html/*

# Copier le build Angular (Angular 19 : output dans dist/frontend/browser)
COPY --from=build /app/dist/frontend/browser .

# Config nginx (routing Angular + proxy /api vers le backend)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
