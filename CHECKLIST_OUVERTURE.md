# Checklist avant l'ouverture

## Revue de sécurité (faite)
- Toutes les routes exigent un jeton, sauf : connexion / mot de passe oublié / réinitialisation (limitées en débit),
  `/api/sante`, et la notification de paiement (protégée par signature HMAC, comparaison à temps constant).
- Swagger fermé et erreurs sans détail avec `SPRING_PROFILES_ACTIVE=prod` (positionné dans le Dockerfile).
- `JWT_SECRET` sans valeur par défaut en prod ; refus de démarrer sous 32 caractères.
- Conteneur serveur non-root ; base non exposée ; en-têtes de sécurité et CSP dans nginx.

## À faire par toi
1. [ ] Créer `deploiement/.env` depuis `.env.example` (secrets aléatoires, `SITE_URL` en https).
2. [ ] Mettre un HTTPS devant le service `web` (port 8081) ; transmettre `X-Forwarded-Proto`.
3. [ ] Lancer : `docker compose -f deploiement/docker-compose.prod.yml --env-file deploiement/.env up -d --build`.
4. [ ] Créer le compte administrateur technique à la main (mot de passe long), ne pas utiliser `DEV_PASSWORD`.
5. [ ] Planifier `sauvegarde.sh` (cron) + copie hors serveur ; **essayer `restauration.sh` sur une machine de test**.
6. [ ] Tester : connexion, mot de passe oublié (e-mail reçu, lien vers `SITE_URL`), un bulletin, une facture.
7. [ ] Surveiller `GET /api/sante` (alerte si ≠ 200).
8. [ ] Application mobile : pointer l'adresse de l'API vers le domaine https avant de publier.
9. [ ] Paiement : laisser `PAIEMENT_FOURNISSEUR` vide tant que l'adaptateur Orange Money n'existe pas.

## Limites connues
- Jetons en stockage navigateur (pas cookies HttpOnly) : la CSP limite le risque XSS ; à reconsidérer plus tard.
- Limitation de débit par instance (une seule instance prévue ici).
