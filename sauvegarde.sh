#!/usr/bin/env bash
# Sauvegarde quotidienne (cron : 0 2 * * * /chemin/sauvegarde.sh). Conserve 14 jours.
set -euo pipefail
DOSSIER="${DOSSIER_SAUVEGARDES:-/var/sauvegardes/scolaire}"
mkdir -p "$DOSSIER"
FICHIER="$DOSSIER/scolaire-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose -f "$(dirname "$0")/docker-compose.prod.yml" --env-file "$(dirname "$0")/.env" \
  exec -T db pg_dump -U scolaire --no-owner scolaire | gzip > "$FICHIER"
test -s "$FICHIER"
find "$DOSSIER" -name 'scolaire-*.sql.gz' -mtime +14 -delete
echo "Sauvegarde : $FICHIER"
# À faire en plus : copier $DOSSIER vers un autre serveur ou un stockage distant.
