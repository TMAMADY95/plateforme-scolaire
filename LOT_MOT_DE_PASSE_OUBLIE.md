# Lot « mot de passe oublié » par e-mail

Backend
- `POST /api/auth/mot-de-passe-oublie` {email} -> toujours 204 (on ne révèle pas si l'adresse existe).
- `POST /api/auth/reinitialisation` {jeton, nouveauMotDePasse} -> 204, ou 400 « lien invalide ou expiré ».
- Jeton aléatoire 256 bits, valable 30 min, à usage unique ; seule son empreinte SHA-256 est stockée (migration V10).
  Une demande tous les 2 min par compte ; un seul lien valide à la fois.
- Le nouveau mot de passe révoque les sessions existantes, lève le verrouillage et l'obligation de changement.
- Audit : DEMANDE_REINITIALISATION_MDP, MOT_DE_PASSE_REINITIALISE_PAR_EMAIL.
- Dépendance : spring-boot-starter-mail. `mvn clean verify` (10 tests dans ReinitialisationMotDePasseServiceTest).

Configuration (variables d'environnement)
- Sans serveur SMTP : rien n'est envoyé. En dev (profil dev), le message avec son lien est écrit dans la console du
  serveur (cherchez « E-MAIL NON ENVOYÉ »).
- Pour envoyer de vrais e-mails : SPRING_MAIL_HOST, SPRING_MAIL_PORT (587), SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD,
  SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true, SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true.
- MAIL_DE (expéditeur) et FRONT_URL (adresse publique du site web, utilisée dans le lien ; défaut http://localhost:5173).

Web : liens « Mot de passe oublié ? » sur la connexion, pages /mot-de-passe-oublie et /reinitialisation?jeton=...
`npm run build`.

Mobile : bouton « Mot de passe oublié ? » -> demande du lien ; le lien s'ouvre sur le site web. `flutter analyze` / `flutter test`.
