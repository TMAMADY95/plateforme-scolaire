# Lot 10b — journal d'audit lisible

Copier `backend/` et `frontend/` par-dessus le projet (à appliquer après le lot 10).

Chaque entrée affiche :
- l'auteur (nom et e-mail, ou « Système ») ;
- une phrase lisible, par exemple « Note modifiée : 14,00 → 16,00 », avec le code d'action en dessous ;
- « Concerne » : élève (avec matricule), établissement (avec code), classe, évaluation, facture, frais, compte ou zone ;
- l'adresse (« Ce poste (local) » pour localhost) ;
- un bouton « Données brutes » qui ouvre le JSON d'origine.

Les noms sont relus au moment de l'affichage : un objet supprimé depuis est simplement omis. Les actions sans phrase prévue retombent sur un libellé lisible.
Nouvelle classe `ResolveurContexteAudit` + test `ResolveurContexteAuditTest` ; `ConsultationAuditService` prend maintenant le résolveur en paramètre.

Limite : les inscriptions et les présences ne sont pas encore journalisées côté serveur, donc absentes du journal.
