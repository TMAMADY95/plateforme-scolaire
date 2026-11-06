#!/usr/bin/env bash
# Usage : ./scripts/demo/installer.sh [--vider] [--jusqu-a N]
#   --vider      exécute d'abord 00_vider_donnees.sql (supprime toutes les données existantes)
#   --jusqu-a N  s'arrête après le script N (défaut : 6). 1 = établissements et élèves ; 2 = + examens planifiés ;
#                3 = + inscriptions ; 4 = + centres et préparation ; 5 = + déroulement et correction ; 6 = tout.
# Variables (défauts de application.yml) : DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
set -euo pipefail
HOTE="${DB_HOST:-localhost}"; PORT="${DB_PORT:-5433}"; BASE="${DB_NAME:-scolaire}"
UTIL="${DB_USER:-scolaire}"; export PGPASSWORD="${DB_PASSWORD:-scolaire}"
DIR="$(cd "$(dirname "$0")" && pwd)"
VIDER=0; JUSQUA=6
while [ $# -gt 0 ]; do
  case "$1" in
    --vider) VIDER=1 ;;
    --jusqu-a) shift; JUSQUA="$1" ;;
    *) echo "Option inconnue : $1"; exit 1 ;;
  esac
  shift
done
psql_f() { psql -v ON_ERROR_STOP=1 -q -h "$HOTE" -p "$PORT" -U "$UTIL" -d "$BASE" -f "$1"; }
if [ "$VIDER" = 1 ]; then echo "→ 0 : suppression des données"; psql_f "$DIR/00_vider_donnees.sql"; fi
for n in 1 2 3 4 5 6; do
  [ "$n" -le "$JUSQUA" ] || break
  fichier=$(ls "$DIR"/0"$n"_*.sql)
  echo "→ $n/6 : $(basename "$fichier")"
  psql_f "$fichier"
done
echo "✅ Terminé. Comptes : ministere@example.org, dgecs@example.org… (mot de passe Demo!2026). Voir LISEZMOI.md"
