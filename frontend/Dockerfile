FROM node:18-alpine AS build
WORKDIR /app
COPY payment-platform-ui/package.json payment-platform-ui/package-lock.json ./
RUN npm ci
COPY payment-platform-ui/ ./
RUN npx ng build --configuration production

FROM nginx:1.27-alpine
COPY --from=build /app/dist/payment-platform-ui /usr/share/nginx/html
COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
