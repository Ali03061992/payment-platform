#!/bin/sh
# Raccourcis de backup (sur la VM, depuis la racine du repo).
# Usage :
#   sh deploy/backup/backup.sh db                  → backup database 'date-heure' (tag db-AAAAMMJJ-HHMMSS)
#   sh deploy/backup/backup.sh release 1.1.0       → backup release (tag release-1.1.0)
set -eu
MODE=${1:-}
case "$MODE" in
  db)
    TAG="db-$(date +%Y%m%d-%H%M%S)"
    ;;
  release)
    VERSION=${2:?"Usage : backup.sh release <version>  (ex. backup.sh release 1.1.0)"}
    TAG="release-$VERSION"
    ;;
  *)
    echo "Usage :"
    echo "  sh deploy/backup/backup.sh db"
    echo "  sh deploy/backup/backup.sh release <version>"
    exit 1
    ;;
esac
docker compose -f deploy/docker-compose.yml --profile backup up -d db-backup
docker compose -f deploy/docker-compose.yml exec -T db-backup /scripts/backup-now.sh "$TAG"
