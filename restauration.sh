#!/usr/bin/env bash
# Restauration : ./restauration.sh fichier.sql.gz  (À ESSAYER sur une machine de test avant l'ouverture.)
set -euo pipefail
[ $# -eq 1 ] || { echo "usage: $0 sauvegarde.sql.gz"; exit 1; }
read -r -p "Ceci REMPLACE la base actuelle. Taper OUI pour continuer : " r; [ "$r" = "OUI" ] || exit 1
C="docker compose -f $(dirname "$0")/docker-compose.prod.yml --env-file $(dirname "$0")/.env"
$C stop api
$C exec -T db psql -U scolaire -d postgres -c "DROP DATABASE scolaire" -c "CREATE DATABASE scolaire OWNER scolaire"
gunzip -c "$1" | $C exec -T db psql -U scolaire -d scolaire -v ON_ERROR_STOP=1
$C start api
