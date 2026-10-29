# Liste de contrôle avant la mise en ligne

## Variables d'environnement (profil `prod`)

| Variable | Rôle | Obligatoire |
|---|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Active la configuration de production (Swagger fermé, erreurs sans détail, arrêt propre) | oui |
| `JWT_SECRET` | Secret de signature des jetons, 32 caractères minimum, aléatoire | oui |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Base PostgreSQL | oui |
| `CORS_ORIGINS` | Adresses du site web autorisées (ex. `https://ecole.example.org`) | oui |
| `FRONT_URL` | Adresse du site web, utilisée dans les liens des e-mails | oui |
| `SPRING_MAIL_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD`, `MAIL_DE` | Envoi des e-mails (mot de passe oublié) | oui, sinon aucun e-mail ne part |
| `FORWARD_HEADERS_STRATEGY=native` | À activer derrière un proxy ou un répartiteur de charge (adresse IP réelle) | si proxy |
| `PAIEMENT_FOURNISSEUR`, `PAIEMENT_WEBHOOK_SECRET` | Paiement en ligne (vide = désactivé) | non |
| `PAIEMENT_EXPIRATION_HEURES` | Délai avant clôture d'un paiement resté en attente (24 par défaut) | non |

Ne jamais utiliser en production : `DEV_PASSWORD`, le profil `dev`, le fournisseur `simule`.

## Protections en place

- Mots de passe hachés (BCrypt), verrouillage du compte après des échecs répétés.
- Jetons révoqués au changement de mot de passe, compte désactivé ou verrouillé refusé immédiatement.
- Limitation de débit par adresse IP : connexion (60/minute), mot de passe oublié (10 par 10 minutes),
  réinitialisation (20 par 10 minutes). Réglable : `app.limite-debit.connexion`, `.mot-de-passe`, `.actif`.
- Contrôle de périmètre sur chaque route (établissement, classe, élève, facture).
- Journal d'audit des opérations sensibles.
- En production : Swagger fermé, aucun message d'erreur interne renvoyé au client.

## À faire côté hébergement

1. **HTTPS obligatoire** (certificat sur le proxy ou le répartiteur). Les jetons ne doivent jamais circuler en clair.
2. **Sauvegardes PostgreSQL** automatiques, avec un essai de restauration avant l'ouverture.
3. **Pare-feu** : la base n'est joignable que depuis le serveur d'application.
4. **Journaux** : conservés hors du serveur, sans mot de passe ni jeton.
5. **Un seul compte administrateur technique** créé à la main, mot de passe long, jamais partagé.
6. Taille des requêtes : le serveur refuse (413) les requêtes de plus de 1 Mo annoncées par Content-Length
   (`TAILLE_REQUETE_MAX`, ex. `2MB`). Fixer aussi une limite au niveau du proxy (nginx : `client_max_body_size 1m;`)
   pour couvrir les corps envoyés en morceaux.
7. Contrôle de santé : `GET /api/sante` renvoie 200 (serveur et base joignables) ou 503. À utiliser pour le
   répartiteur de charge ou l'orchestrateur.
8. Plusieurs instances du serveur : la limitation de débit est comptée par instance. Pour une limite globale,
   la faire aussi au niveau du proxy.

## Limites connues

- Les jetons sont stockés côté navigateur et application (pas encore en cookies `HttpOnly`).
- Un paiement dont la notification arrive plus de 24 h après la création est ignoré : à rapprocher à la main.
