#!/bin/sh
# Backup immédiat des 4 bases applicatives (dump + gzip + rotation).
# Usage : backup-now.sh [tag]   (tag défaut : manual)
# Auth via MYSQL_PWD (fourni par le service db-backup). Rétention :
# supprime les *.sql.gz de plus de BACKUP_RETENTION_DAYS (défaut 3 j).
set -eu
TAG=${1:-manual}
TS=$(date +%Y%m%d-%H%M%S)
OUT="/backups/${TS}-${TAG}.sql.gz"
mkdir -p /backups
mysqldump -h "${MYSQL_HOST:-mysql}" -uroot \
  --single-transaction --routines --events \
  --databases identity_db organization_db payment_db notification_db \
  | gzip > "$OUT"
find /backups -maxdepth 1 -name '*.sql.gz' -mtime +"${BACKUP_RETENTION_DAYS:-3}" -delete
ls -lh "$OUT"
echo "Backups conservés :"
ls -lh /backups/*.sql.gz
