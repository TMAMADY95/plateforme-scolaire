# Sauvegardes et supervision

## À planifier sur le serveur (`crontab -e`)

```
0 2 * * *    /chemin/deploiement/sauvegarde.sh            # sauvegarde chaque nuit à 2 h (14 jours conservés)
0 4 * * 0    /chemin/deploiement/verifier-sauvegarde.sh   # le dimanche : prouve que la dernière sauvegarde se restaure
*/5 * * * *  /chemin/deploiement/supervision.sh           # surveille le serveur, la sauvegarde et le disque
```

## Où recevoir les alertes (dans `deploiement/.env`)

| Variable | Rôle |
|---|---|
| `ALERTE_WEBHOOK_URL` | adresse d'un webhook Slack / Mattermost / Discord / n8n |
| `ALERTE_EMAIL` | adresse e-mail (le serveur doit avoir la commande `mail`) |
| `COPIE_DISTANTE` | copie de chaque sauvegarde hors du serveur, format rsync : `utilisateur@hote:/chemin/` |
| `DOSSIER_SAUVEGARDES` | dossier des sauvegardes (défaut `/var/sauvegardes/scolaire`) |
| `URL_SANTE` | adresse contrôlée (défaut `http://localhost:8081/api/sante`) |
| `SEUIL_DISQUE` | occupation du disque qui déclenche l'alerte (défaut 85) |

Sans canal configuré, les alertes sont seulement écrites dans le journal de cron : configurez-en au moins un.

## Ce qui est surveillé

- **Sauvegarde** : un échec, une archive illisible, vide ou sans tables, ou une copie hors serveur impossible déclenchent une alerte.
- **Vérification hebdomadaire** : la dernière sauvegarde est rechargée dans une base temporaire (la production n'est pas touchée).
- **Supervision** : serveur injoignable, aucune sauvegarde depuis 26 h, disque presque plein. Une alerte au début du problème, une au retour à la normale.

## Restauration en cas de sinistre

`./restauration.sh /var/sauvegardes/scolaire/scolaire-AAAAMMJJ-HHMMSS.sql.gz` (remplace la base actuelle, demande confirmation).
