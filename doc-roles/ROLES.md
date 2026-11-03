# Rôles et structures administratives

## Hiérarchie

Établissements → **DPE/DCE** (direction préfectorale ou communale de l'éducation) → **IRE** (inspection régionale) → **ministère**.
La **DGECS** (direction générale des examens et du contrôle scolaire) assure la partie technique nationale des examens.

| Rôle (code) | Périmètre | Rôle dans l'application |
|---|---|---|
| Administrateur technique (`ADMIN_TECHNIQUE`) | national | Comptes et paramétrage technique |
| Ministère (`MINISTERE`) | national | Fixe le calendrier (période, épreuves, création et suppression d'examen), les types d'examen et les critères de centres ; **proclame** les résultats ; gouverne les comptes institutionnels (IRE, DGECS) ; **consulte** tout le reste |
| DGECS (`DGECS`) | national | Centres, banque de sujets, correction, jury et réouverture de la délibération, saisie et import des résultats, validation définitive des listes de candidats, diplômes, rejet d'une liste vérifiée |
| Direction régionale, IRE (`DIRECTION_REGIONALE`) | zone (région) | Supervise sa région ; crée et gère les comptes DPE/DCE de sa région ; calendrier et saisie des examens de sa zone |
| DPE/DCE (`DIRECTION_LOCALE`) | zone (préfecture ou commune) | Vérifie les listes de candidats de ses établissements avant la DGECS ; calendrier et saisie des examens de sa zone |
| Chef de centre (`CHEF_CENTRE`) | personnel + centres désignés (`acces_centre`) | Personnel du centre, incidents, avancement et ouverture des plis, rapport de surveillance, préparation matérielle, uniquement pour **ses** centres |
| Direction / personnel d'établissement, enseignant, parent, élève | établissement ou famille | Inscriptions, candidatures, cartes, résultats… |

Circuit des listes de candidats : établissement (transmet) → DPE/DCE ou IRE (vérifie) → DGECS (valide définitivement) ; le rejet motivé est possible à l'étape de vérification.
L'authentification à deux facteurs est exigée pour MINISTERE, DGECS, DIRECTION_REGIONALE et DIRECTION_LOCALE.

## Où c'est codé

- `security/DroitsExamen` : constantes `@PreAuthorize` (`CONSULTATION`, `GESTION`, `TERRAIN`, `LECTURE_TERRAIN`, `NATIONAL`, `MINISTERE`) et helpers.
- `security/AccesCentreService` (`@accesCentre.peut(#centreId)`) : ministère et DGECS voient tous les centres ; IRE et DPE/DCE ceux de leur zone ; un chef de centre ses centres.
- `security/ZonesAdminService` (`@zonesAdmin`) : qui peut administrer une zone ou un compte local.
- Création des comptes : `POST /api/ministere/comptes-dgecs`, comptes IRE depuis le référentiel des régions, `POST /api/zones/{id}/direction-locale`
  (préfecture ou commune), `/api/region/comptes-locaux` (l'IRE gère les DPE/DCE de sa région), `/api/examens/{e}/centres/{c}/chefs` (chefs de centre).
- Frontend : `auth/roles.ts` (`droitsExamens`) et le hook `useDroitsExamens()` masquent les actions non permises ; le contrôle réel reste côté API.

## Vision des résultats

Les résultats d'un examen ne sont visibles par l'élève et ses parents (`/api/eleves/{id}/resultats-examens`) qu'après la proclamation par le ministère.
