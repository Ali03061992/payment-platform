# Build + test Flutter sans rien installer (100% Docker).
# Image Flutter stable : SDK + Dart pre-installes.
FROM ghcr.io/cirruslabs/flutter:stable AS build
WORKDIR /app

# Deps d'abord (cache)
COPY pubspec.yaml ./
RUN flutter pub get

COPY . .
# Analyse (seules les erreurs bloquent) + tests
RUN flutter config --enable-web
RUN flutter analyze --no-fatal-infos --no-fatal-warnings
RUN flutter test

# Build web : par defaut same-origin (API_BASE_URL vide -> nginx proxy /api).
ARG API_BASE_URL=
RUN flutter create . --platforms web
RUN flutter build web --release --dart-define=API_BASE_URL=${API_BASE_URL}

# Serveur leger nginx
FROM nginx:alpine AS runtime
COPY --from=build /app/build/web /usr/share/nginx/html
COPY nginx-mobile.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
