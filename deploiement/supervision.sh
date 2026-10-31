#!/usr/bin/env bash
# Supervision : à planifier toutes les 5 minutes (cron : */5 * * * * /chemin/supervision.sh).
# Contrôle : le serveur répond (/api/sante), la dernière sauvegarde a moins de 26 h, le disque n'est pas plein.
# N'alerte qu'au changement d'état (panne, puis retour à la normale) : pas de message toutes les 5 minutes.
# Variables : URL_SANTE (défaut http://localhost:8081/api/sante), DOSSIER_SAUVEGARDES, SEUIL_DISQUE (défaut 85 %).
set -uo pipefail
ICI="$(cd "$(dirname "$0")" && pwd)"
[ -f "$ICI/.env" ] && set -a && . "$ICI/.env" && set +a
URL="${URL_SANTE:-http://localhost:8081/api/sante}"
DOSSIER="${DOSSIER_SAUVEGARDES:-/var/sauvegardes/scolaire}"
SEUIL="${SEUIL_DISQUE:-85}"
ETAT="${DOSSIER_ETAT_SUPERVISION:-/var/tmp/scolaire-supervision}"
mkdir -p "$ETAT"

verifier() {  # verifier <nom> <message d'échec> <commande de test…>
  local nom="$1" message="$2"; shift 2
  if "$@" >/dev/null 2>&1; then
    if [ -f "$ETAT/$nom" ]; then rm -f "$ETAT/$nom"; "$ICI/alerte.sh" "RÉTABLI : $nom"; fi
  else
    if [ ! -f "$ETAT/$nom" ]; then touch "$ETAT/$nom"; "$ICI/alerte.sh" "PROBLÈME : $message"; fi
  fi
}

sauvegarde_recente() { [ -n "$(find "$DOSSIER" -name 'scolaire-*.sql.gz' -mmin -1560 2>/dev/null | head -1)" ]; }
disque_ok() { local u; u=$(df --output=pcent "$DOSSIER" 2>/dev/null | tail -1 | tr -dc '0-9'); [ -n "$u" ] && [ "$u" -lt "$SEUIL" ]; }

verifier "serveur" "le serveur ne répond plus ($URL)" curl -fsS -m 10 "$URL"
verifier "sauvegarde" "aucune sauvegarde depuis plus de 26 h dans $DOSSIER" sauvegarde_recente
verifier "disque" "le disque de $DOSSIER dépasse $SEUIL % d'occupation" disque_ok
