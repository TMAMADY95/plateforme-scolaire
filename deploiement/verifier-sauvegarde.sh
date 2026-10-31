#!/usr/bin/env bash
# Prouve que la dernière sauvegarde se restaure : elle est rechargée dans une base temporaire, puis la base
# temporaire est supprimée. N'affecte PAS la base de production. À planifier chaque semaine :
#   0 4 * * 0 /chemin/verifier-sauvegarde.sh
set -euo pipefail
ICI="$(cd "$(dirname "$0")" && pwd)"
[ -f "$ICI/.env" ] && set -a && . "$ICI/.env" && set +a
DOSSIER="${DOSSIER_SAUVEGARDES:-/var/sauvegardes/scolaire}"
DERNIERE="$(ls -1t "$DOSSIER"/scolaire-*.sql.gz 2>/dev/null | head -1 || true)"
C="docker compose -f $ICI/docker-compose.prod.yml --env-file $ICI/.env"
TEMP="scolaire_verif_restauration"
nettoyer() { $C exec -T db psql -U scolaire -d postgres -c "DROP DATABASE IF EXISTS $TEMP" >/dev/null 2>&1 || true; }
echec() { "$ICI/alerte.sh" "VÉRIFICATION DE SAUVEGARDE EN ÉCHEC ($1) : la restauration n'est pas garantie."; nettoyer; exit 1; }

[ -n "$DERNIERE" ] || echec "aucune sauvegarde trouvée dans $DOSSIER"
nettoyer
$C exec -T db psql -U scolaire -d postgres -c "CREATE DATABASE $TEMP OWNER scolaire" >/dev/null || echec "base temporaire"
gunzip -c "$DERNIERE" | $C exec -T db psql -U scolaire -d "$TEMP" -v ON_ERROR_STOP=1 -q >/dev/null || echec "rechargement de $DERNIERE"
N=$($C exec -T db psql -U scolaire -d "$TEMP" -tA -c "SELECT count(*) FROM utilisateur") || echec "lecture de la base restaurée"
nettoyer
echo "Sauvegarde vérifiée : $DERNIERE ($N comptes)"
