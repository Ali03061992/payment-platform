#!/bin/sh
# Restauration d'un backup (sur la VM, depuis la racine du repo).
# Usage : sh deploy/backup/restore.sh <fichier.sql.gz>
# Le dump contient les 4 bases (--databases) : restauration complète.
# ATTENTION : écrase les données actuelles. Confirmation explicite requise.
set -eu
FILE=${1:?"Usage : sh deploy/backup/restore.sh <fichier.sql.gz>"}
if [ ! -f "$FILE" ]; then echo "Fichier introuvable : $FILE"; exit 1; fi
echo "!!! Restauration de $FILE dans MySQL (écrase les données)."
echo "Taper RESTORE en majuscules pour confirmer :"
read CONFIRM
[ "$CONFIRM" = "RESTORE" ] || { echo "Annulé."; exit 1; }
zcat "$FILE" | docker compose -f deploy/docker-compose.yml exec -T mysql \
  sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
echo "Restauration terminée : $FILE"
