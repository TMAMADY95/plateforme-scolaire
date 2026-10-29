# Lot audit des inscriptions et des présences (backend uniquement)

Nouvelles entrées du journal d'audit (visibles dans l'onglet « Journal d'audit » du ministère, filtre par action) :
- DEMANDE_INSCRIPTION, ACCEPTATION_INSCRIPTION, REFUS_INSCRIPTION, ANNULATION_INSCRIPTION
- SAISIE_APPEL (une entrée par appel réellement modifié : présents / absents / retards / corrections)
- CORRECTION_PRESENCE (une entrée par présence modifiée : avant → après). Renvoyer le même appel ne laisse aucune trace.

Le résolveur affiche l'élève, l'établissement et la classe pour chaque entrée.
Aucune migration. `mvn clean verify` (3 tests de présence + 1 de résolveur ajoutés ; les constructeurs de
InscriptionService et PresenceService reçoivent AuditService, les tests existants sont adaptés).
