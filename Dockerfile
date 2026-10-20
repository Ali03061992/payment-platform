# Production : frontend Angular servi par nginx (VM OVH + CI).
FROM node:22-alpine AS build
WORKDIR /app
COPY payment-platform-ui/package.json payment-platform-ui/package-lock.json ./
# Cache BuildKit npm + install déterministe (lockfile).
# Cypress n'est pas nécessaire au build de production : on évite le téléchargement du binaire.
ENV CYPRESS_INSTALL_BINARY=0
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund --legacy-peer-deps
# Seuls les fichiers utiles au build (tests/cypress/dist exclus aussi via .dockerignore).
COPY payment-platform-ui/angular.json payment-platform-ui/tsconfig.json ./
COPY payment-platform-ui/tsconfig.app.json payment-platform-ui/ngsw-config.json ./
COPY payment-platform-ui/src ./src
RUN npx ng build --configuration production

FROM nginx:1.27-alpine
COPY --from=build /app/dist/payment-platform-ui /usr/share/nginx/html
COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
