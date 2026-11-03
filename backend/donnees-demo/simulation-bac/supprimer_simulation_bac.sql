-- =====================================================================================================
-- Supprime toute la simulation « Baccalauréat simulé 2026 » (quel que soit le nombre d'étapes chargées) :
-- l'examen et, en cascade, ses épreuves, centres, candidats, notes, incidents, plis, jury, diplômes, réclamations, budget, etc.
-- ainsi que les comptes chef.bac1@demo.gn … chef.bac7@demo.gn, les vérifications de diplômes et la situation statistique provisoire créées à l'étape 9.
--   psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f supprimer_simulation_bac.sql
-- =====================================================================================================
DELETE FROM verification_diplome
WHERE code_saisi LIKE 'SIM%'
   OR code_saisi IN (SELECT d.code FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id
                     WHERE c.examen_id IN (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'));
DELETE FROM situation_statistique WHERE commentaire = 'Situation provisoire établie après le baccalauréat simulé';
DELETE FROM acces_centre WHERE utilisateur_id IN (SELECT id FROM utilisateur WHERE email ~ '^chef\.bac[0-9]+@demo\.gn$');
DELETE FROM affectation_role WHERE utilisateur_id IN (SELECT id FROM utilisateur WHERE email ~ '^chef\.bac[0-9]+@demo\.gn$');
DELETE FROM utilisateur WHERE email ~ '^chef\.bac[0-9]+@demo\.gn$';
DELETE FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';
