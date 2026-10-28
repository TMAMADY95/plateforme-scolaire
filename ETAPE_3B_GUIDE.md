# Étape 3b — Paiements (Module 7, partie 2)

À fusionner après `lot7-direction-regionale.zip`. **Nouvelle migration : `V7__paiements.sql`** (table `paiement`).
Aucune donnée bancaire n'est reçue ni stockée : seulement nos références et celles du prestataire.

## Ce qui est livré
| Fonction | Qui | Route |
|---|---|---|
| Historique des paiements d'une facture | direction, personnel admin | `GET /api/factures/{id}/paiements` |
| Encaissement espèces / virement / chèque | direction, personnel admin | `POST /api/factures/{id}/paiements` |
| Paiement en ligne (initiation) | élève, parent (ses factures seulement) | `POST /api/me/factures/{id}/paiements` |
| Confirmation du prestataire | prestataire (signée) | `POST /api/paiements/notification` |
| Faux prestataire (dev) | profil dev | `POST /api/dev/paiements/{ref}/simuler?succes=` |

## Règles de sécurité
- **Rien n'est validé sur la parole du navigateur.** Le paiement en ligne reste `EN_ATTENTE` jusqu'à une notification dont la signature HMAC-SHA256 est correcte.
- Signature = HMAC(secret, `reference|statut|montant à 2 décimales|referenceExterne`), comparée en temps constant.
- Notification **idempotente** (rejouée = sans effet), montant vérifié (écart ⇒ `ECHEC` + audit), référence externe vérifiée.
- La facture est **verrouillée** pendant chaque opération : impossible de dépasser le montant dû, même avec deux paiements simultanés.
- Un paiement en ligne ouvert (30 min) réserve son montant et empêche l'annulation / l'exonération de la facture.
- Une référence de chèque, virement ou transaction ne peut pas être saisie deux fois pour un même mode.
- Un parent ne peut payer que les factures de ses enfants ; sinon la réponse est identique à « facture introuvable ».

## Configuration
| Variable | Rôle |
|---|---|
| `PAIEMENT_FOURNISSEUR` | vide = paiement en ligne **désactivé** (503) ; `simule` = faux prestataire (profil dev, déjà configuré) |
| `PAIEMENT_WEBHOOK_SECRET` | secret partagé avec le prestataire, obligatoire en production |

## Tester en local
1. Direction : onglet Finances → bouton **Paiements** sur une facture → encaisser un montant partiel : statut `PARTIELLE`, puis le solde : `PAYEE`.
2. Parent ou élève : bouton **Payer** → page « Prestataire de paiement (simulation) » → « Simuler un succès » : la facture passe à `PAYEE`.
3. Essayer un montant supérieur au reste, ou le même numéro de chèque deux fois : refus.

## Brancher un vrai Orange Money (à faire avec ton compte marchand)
Je n'ai pas branché d'API réelle : chaque pays a son propre contrat d'API Orange Money (adresses, jetons, formats) et il faut un compte marchand et sa documentation. Le code est prêt pour cela :
1. Écrire une classe `PasserelleOrangeMoney implements PasserelleDePaiement` : créer la transaction chez Orange, renvoyer `ResultatInitiation(referenceExterne, urlPaiement)`.
2. L'activer par `PAIEMENT_FOURNISSEUR=orange` (`@ConditionalOnProperty`).
3. Recevoir la notification d'Orange et la convertir en `NotificationPaiement` signée, **ou** (recommandé) lui faire appeler un petit contrôleur qui interroge l'API de statut d'Orange avant de valider : on ne fait confiance qu'à une réponse obtenue directement auprès d'eux.
4. En production : HTTPS obligatoire, secret long et aléatoire, restriction des adresses IP d'Orange si possible.

## Autres changements
- `FacturationService` lit désormais les paiements par le repository (une seule requête pour toute une liste de factures) ; le contournement « la table existe-t-elle ? » disparaît.
- `GestionErreurs` inchangé ; `SecurityConfig` : `/api/paiements/notification` est ouvert (protégé par la signature).

## Tests
`PaiementServiceTest` (13 tests) ; `FacturationGenerationTest` mis à jour (nouveau paramètre du constructeur).

## Limites connues
- Pas de remboursement ni d'annulation d'un paiement validé (à traiter comme une écriture de correction, à concevoir).
- Pas de reçu PDF.
- Les paiements en ligne restés `EN_ATTENTE` ne sont pas nettoyés : ils cessent simplement de réserver le montant après 30 minutes.
