#!/usr/bin/env bash
# Envoie un message d'alerte : ./alerte.sh "texte"
# Canaux (au choix, dans l'environnement ou deploiement/.env) :
#   ALERTE_WEBHOOK_URL  adresse d'un webhook (Slack, Mattermost, Discord, n8n…) : reçoit {"text": "...", "content": "..."}
#   ALERTE_EMAIL        adresse e-mail (nécessite la commande « mail » sur le serveur)
# Sans canal configuré, le message est seulement écrit sur la sortie d'erreur (visible dans le journal de cron).
set -uo pipefail
MESSAGE="[$(hostname)] $1"
[ -f "$(dirname "$0")/.env" ] && set -a && . "$(dirname "$0")/.env" && set +a
echo "$MESSAGE" >&2
if [ -n "${ALERTE_WEBHOOK_URL:-}" ]; then
  CORPS=$(printf '%s' "$MESSAGE" | sed 's/\\/\\\\/g; s/"/\\"/g')
  curl -fsS -m 10 -X POST -H 'Content-Type: application/json' \
    -d "{\"text\":\"$CORPS\",\"content\":\"$CORPS\"}" "$ALERTE_WEBHOOK_URL" >/dev/null || echo "Webhook injoignable" >&2
fi
if [ -n "${ALERTE_EMAIL:-}" ] && command -v mail >/dev/null; then
  printf '%s\n' "$MESSAGE" | mail -s "Alerte plateforme scolaire" "$ALERTE_EMAIL" || echo "E-mail non envoyé" >&2
fi
exit 0
