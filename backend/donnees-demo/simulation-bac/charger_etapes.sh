#!/usr/bin/env bash
# Charge une ou plusieurs étapes de la simulation du baccalauréat dans le conteneur Docker du projet.
#   ./charger_etapes.sh          étapes 1 à 9      ./charger_etapes.sh 1 3   étapes 1 à 3      ./charger_etapes.sh 4 4   étape 4 seule
set -euo pipefail
de="${1:-1}"; a="${2:-9}"; conteneur="${CONTENEUR:-scolaire-db}"
cd "$(dirname "$0")"
for i in $(seq "$de" "$a"); do
    fichier="$(ls bac_etape_$(printf '%02d' "$i")_*.sql | head -n 1)"
    echo "Étape $i : $fichier"
    docker cp "$fichier" "$conteneur:/tmp/$fichier"
    docker exec -e PGCLIENTENCODING=UTF8 "$conteneur" psql -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f "/tmp/$fichier"
done
