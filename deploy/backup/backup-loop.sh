#!/bin/sh
# Boucle du service db-backup : un backup toutes les BACKUP_INTERVAL_SECONDS
# (défaut 8 h = 28800 s), rétention BACKUP_RETENTION_DAYS (défaut 3 j).
set -eu
echo "db-backup: 1er backup immédiat, puis toutes les ${BACKUP_INTERVAL_SECONDS:-28800}s (rétention ${BACKUP_RETENTION_DAYS:-3}j)"
/scripts/backup-now.sh scheduled
while true; do
  sleep "${BACKUP_INTERVAL_SECONDS:-28800}"
  /scripts/backup-now.sh scheduled
done
