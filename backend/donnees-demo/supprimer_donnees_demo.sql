-- =====================================================================================================
-- Supprime le jeu de données de démonstration (établissements GN-xxxx, comptes @demo.gn, élèves DEMO-xxxx,
-- zones « Guinée », examens de démonstration). Les référentiels (années, niveaux GN-xx, matières) restent.
--   psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f supprimer_donnees_demo.sql
-- =====================================================================================================
CREATE TEMP TABLE c_etab AS SELECT id FROM etablissement WHERE code_officiel LIKE 'GN-%';
CREATE TEMP TABLE c_eleve AS SELECT id FROM eleve WHERE matricule LIKE 'DEMO-%';
CREATE TEMP TABLE c_user AS SELECT id FROM utilisateur WHERE lower(email) LIKE '%@demo.gn';

DELETE FROM journal_audit WHERE utilisateur_id IN (SELECT id FROM c_user) OR details ->> 'origine' = 'jeu de démonstration';
DELETE FROM alerte_envoyee WHERE destinataire LIKE '%@demo.gn';
DELETE FROM examen_national WHERE libelle IN ('CEE 2026', 'BEPC 2026', 'Baccalauréat 2026', 'Évaluation de rentrée 6e année', 'CEE 2027',
                                              'BEPC 2027', 'Baccalauréat 2027', 'Évaluation communale 6e année', 'CEE franco-arabe 2027');
DELETE FROM paiement WHERE facture_id IN (SELECT id FROM facture WHERE etablissement_id IN (SELECT id FROM c_etab));
DELETE FROM facture WHERE etablissement_id IN (SELECT id FROM c_etab);
DELETE FROM devoir_fait WHERE eleve_id IN (SELECT id FROM c_eleve);
DELETE FROM conversation WHERE eleve_id IN (SELECT id FROM c_eleve);
DELETE FROM inscription WHERE eleve_id IN (SELECT id FROM c_eleve);
DELETE FROM eleve WHERE id IN (SELECT id FROM c_eleve);
DELETE FROM classe WHERE etablissement_id IN (SELECT id FROM c_etab);
DELETE FROM affectation_role WHERE utilisateur_id IN (SELECT id FROM c_user);
DELETE FROM enseignant WHERE utilisateur_id IN (SELECT id FROM c_user);
DELETE FROM etablissement WHERE id IN (SELECT id FROM c_etab);
DELETE FROM utilisateur WHERE id IN (SELECT id FROM c_user);
-- Zones : de la plus fine à la plus large
DELETE FROM zone_geographique WHERE type_zone = 'COMMUNE' AND parent_id IN (SELECT id FROM zone_geographique WHERE type_zone = 'PREFECTURE' AND parent_id IN
    (SELECT id FROM zone_geographique WHERE type_zone = 'REGION' AND parent_id IN (SELECT id FROM zone_geographique WHERE nom = 'Guinée' AND type_zone = 'PAYS')));
DELETE FROM zone_geographique WHERE type_zone = 'PREFECTURE' AND parent_id IN (SELECT id FROM zone_geographique WHERE type_zone = 'REGION' AND parent_id IN
    (SELECT id FROM zone_geographique WHERE nom = 'Guinée' AND type_zone = 'PAYS'));
DELETE FROM zone_geographique WHERE type_zone = 'REGION' AND parent_id IN (SELECT id FROM zone_geographique WHERE nom = 'Guinée' AND type_zone = 'PAYS');
DELETE FROM zone_geographique WHERE nom = 'Guinée' AND type_zone = 'PAYS';
UPDATE critere_centre_examen SET secteur_admis = 'TOUS', autorisation_requise = FALSE, salles_minimum = 0, capacite_minimum = 0, updated_at = now() WHERE id = 1;
