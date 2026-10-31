#!/usr/bin/env bash
# Sauvegarde quotidienne (cron : 0 2 * * * /chemin/sauvegarde.sh). Conserve 14 jours.
# Variables facultatives (environnement ou .env) :
#   DOSSIER_SAUVEGARDES  où ranger les fichiers (défaut /var/sauvegardes/scolaire)
#   COPIE_DISTANTE       destination d'une copie hors serveur, au format rsync : utilisateur@hote:/chemin/
# Toute erreur déclenche alerte.sh.
set -euo pipefail
ICI="$(cd "$(dirname "$0")" && pwd)"
[ -f "$ICI/.env" ] && set -a && . "$ICI/.env" && set +a
DOSSIER="${DOSSIER_SAUVEGARDES:-/var/sauvegardes/scolaire}"
FICHIER="$DOSSIER/scolaire-$(date +%Y%m%d-%H%M%S).sql.gz"
echec() { "$ICI/alerte.sh" "SAUVEGARDE EN ÉCHEC ($1). Rien n'a été sauvegardé cette nuit."; rm -f "$FICHIER"; exit 1; }
trap 'echec "erreur ligne $LINENO"' ERR

mkdir -p "$DOSSIER"
docker compose -f "$ICI/docker-compose.prod.yml" --env-file "$ICI/.env" \
  exec -T db pg_dump -U scolaire --no-owner scolaire | gzip > "$FICHIER"
test -s "$FICHIER"
gzip -t "$FICHIER"                       # l'archive est lisible
# Une sauvegarde de la base sans aucune table serait vide de sens.
[ "$(gunzip -c "$FICHIER" | grep -c 'CREATE TABLE')" -gt 0 ] || echec "sauvegarde sans table"

find "$DOSSIER" -name 'scolaire-*.sql.gz' -mtime +14 -delete
if [ -n "${COPIE_DISTANTE:-}" ]; then
  rsync -a --timeout=120 "$FICHIER" "$COPIE_DISTANTE" || echec "copie hors serveur impossible"
fi
echo "Sauvegarde : $FICHIER"
