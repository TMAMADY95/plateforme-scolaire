-- =====================================================================================================
-- Complément du jeu de démonstration : fonctionnalités d'examens des migrations V37 à V49
-- (notes par épreuve, listes validées, personnel et préparation des centres, incidents, plis et rapports de
-- surveillance, commissions de correction, banque de sujets, diplômes, calendrier prévisionnel, chefs de centre).
-- À exécuter UNE SEULE FOIS, APRÈS demo_donnees.sql :
--   psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f demo_examens_complements.sql
-- Ajoute l'examen « CEE blanc 6e année 2026 » (4 centres, élèves de 6e année des écoles GN-0001, GN-0004, GN-0007 et
-- GN-0011, résultats proclamés) et des comptes de chefs de centre (@demo.gn, mot de passe Demo!2026). Corrige aussi les
-- comptes de directions préfectorales et communales créés avec l'ancien rôle (ils deviennent DPE/DCE).
-- =====================================================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM examen_national WHERE libelle = 'CEE blanc 6e année 2026') THEN
        RAISE EXCEPTION 'Le complément d''examens a déjà été chargé.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'admin@demo.gn') THEN
        RAISE EXCEPTION 'Chargez d''abord demo_donnees.sql.';
    END IF;
END $$;

-- ---------- 1. Rôles : DPE/DCE et DGECS ----------
UPDATE affectation_role SET role = 'DIRECTION_LOCALE'
WHERE role = 'DIRECTION_REGIONALE' AND zone_id IN (SELECT id FROM zone_geographique WHERE type_zone IN ('PREFECTURE', 'COMMUNE'))
  AND utilisateur_id IN (SELECT id FROM utilisateur WHERE lower(email) LIKE '%@demo.gn');
INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom)
SELECT 'dgecs.demo@demo.gn', mot_de_passe_hash, 'Baldé', 'Thierno' FROM utilisateur WHERE email = 'admin@demo.gn'
ON CONFLICT DO NOTHING;
INSERT INTO affectation_role (utilisateur_id, role, perimetre)
SELECT u.id, 'DGECS', 'NATIONAL' FROM utilisateur u
WHERE u.email = 'dgecs.demo@demo.gn' AND NOT EXISTS (SELECT 1 FROM affectation_role a WHERE a.utilisateur_id = u.id);

-- ---------- 2. Calendrier prévisionnel des examens existants (retards visibles pour 2026) ----------
UPDATE examen_national SET correction_debut_prevue = date_fin + 3, correction_fin_prevue = date_fin + 17,
                           deliberation_prevue = date_fin + 24, proclamation_prevue = date_fin + 31
WHERE libelle IN ('CEE 2026', 'BEPC 2026', 'Baccalauréat 2026', 'CEE 2027', 'BEPC 2027', 'Baccalauréat 2027') AND date_fin IS NOT NULL;

-- ---------- 3. L'examen blanc : modèle, épreuves (avec coefficients), centres ----------
INSERT INTO examen_national (libelle, annee_scolaire_id, cycle_id, niveau_id, niveau_organisation, date_debut, date_fin,
                             statut_resultats, date_deliberation, date_proclamation,
                             correction_debut_prevue, correction_fin_prevue, deliberation_prevue, proclamation_prevue, bareme)
SELECT 'CEE blanc 6e année 2026', a.id, c.id, n.id, 'PREFECTURE', date '2026-09-14', date '2026-09-15',
       'PROCLAME', date '2026-10-06', date '2026-10-13',
       date '2026-09-18', date '2026-09-30', date '2026-10-05', date '2026-10-12', 10
FROM annee_scolaire a, cycle_scolaire c, niveau n
WHERE a.libelle = '2026-2027' AND c.code = 'PRIMAIRE' AND n.code = 'GN-06';

CREATE TEMP TABLE c_ex AS SELECT id FROM examen_national WHERE libelle = 'CEE blanc 6e année 2026';
INSERT INTO epreuve_examen (examen_id, matiere_id, date_epreuve, heure_debut, heure_fin, coefficient, created_at)
SELECT (SELECT id FROM c_ex), m.id, v.jour, v.debut::time, v.fin::time, v.coef, now()
FROM (VALUES ('FRANCAIS', date '2026-09-14', '08:00', '10:00', 2), ('MATHEMATIQUES', date '2026-09-14', '10:30', '12:30', 2),
             ('EVEIL_SCIENTIFIQUE', date '2026-09-15', '08:00', '10:00', 1)) v(matiere, jour, debut, fin, coef)
JOIN matiere m ON m.code = v.matiere;

-- Un centre par école de l'échantillon (l'établissement lui-même)
CREATE TEMP TABLE c_etab AS
SELECT e.id AS etab_id, e.code_officiel AS code, e.nom, e.adresse, e.zone_id, row_number() OVER (ORDER BY e.code_officiel) AS rang
FROM etablissement e WHERE e.code_officiel IN ('GN-0001', 'GN-0004', 'GN-0007', 'GN-0011');
INSERT INTO centre_examen (examen_id, zone_id, nom, adresse, date_debut, created_at, etablissement_id)
SELECT (SELECT id FROM c_ex), zone_id, nom, adresse, date '2026-09-14', now(), etab_id FROM c_etab;
CREATE TEMP TABLE c_centre AS
SELECT ce.id AS centre_id, t.etab_id, t.rang, t.code FROM centre_examen ce JOIN c_etab t ON t.etab_id = ce.etablissement_id
WHERE ce.examen_id = (SELECT id FROM c_ex);

-- ---------- 4. Candidats : numéros de PV, salle (30 par salle au plus) et table ----------
INSERT INTO candidat_examen (examen_id, eleve_id, etablissement_id, classe_id, centre_id, created_at)
SELECT (SELECT id FROM c_ex), i.eleve_id, i.etablissement_id, i.classe_id, ct.centre_id, timestamptz '2026-08-20 09:00+00'
FROM inscription i
JOIN classe cl ON cl.id = i.classe_id
JOIN niveau n ON n.id = cl.niveau_id AND n.code = 'GN-06'
JOIN annee_scolaire a ON a.id = cl.annee_scolaire_id AND a.libelle = '2026-2027'
JOIN c_centre ct ON ct.etab_id = i.etablissement_id
WHERE i.statut = 'ACCEPTEE';

UPDATE candidat_examen c SET numero = 'CB-' || lpad(c.id::text, 6, '0'),
       numero_pv = 'PVB-' || lpad(r.rang_global::text, 5, '0'), salle = 1 + (r.rang_centre - 1) / 30, table_numero = 1 + (r.rang_centre - 1) % 30
FROM (SELECT id, row_number() OVER (PARTITION BY centre_id ORDER BY id) AS rang_centre, row_number() OVER (ORDER BY centre_id, id) AS rang_global
      FROM candidat_examen WHERE examen_id = (SELECT id FROM c_ex)) r
WHERE r.id = c.id;

-- Listes de candidats validées par la DGECS, dossiers complets
INSERT INTO liste_candidats (examen_id, etablissement_id, statut, transmise_le, validee_region_le, validee_le)
SELECT (SELECT id FROM c_ex), etab_id, 'VALIDEE', timestamptz '2026-08-25 10:00+00', timestamptz '2026-08-28 10:00+00', timestamptz '2026-09-01 10:00+00'
FROM c_etab;
INSERT INTO piece_candidature (candidat_id, type, reference, fournie_le)
SELECT c.id, t.type, CASE t.type WHEN 'ACTE_NAISSANCE' THEN 'ACTE-' || c.id ELSE NULL END, date '2026-08-18'
FROM candidat_examen c, (VALUES ('DEMANDE'), ('ACTE_NAISSANCE'), ('ATTESTATION')) t(type)
WHERE c.examen_id = (SELECT id FROM c_ex);

-- ---------- 5. Déroulement : statuts, notes par épreuve, moyennes et décisions ----------
-- Un candidat éliminé pour fraude par centre (le premier) ; environ 5 % d'absents ; les autres sont présents
UPDATE candidat_examen c SET statut_epreuves = CASE
    WHEN c.id = (SELECT min(id) FROM candidat_examen x WHERE x.centre_id = c.centre_id AND x.examen_id = c.examen_id) THEN 'ELIMINE'
    WHEN abs(hashtext(c.id::text || 's')) % 100 < 5 THEN 'ABSENT' ELSE 'PRESENT' END
WHERE c.examen_id = (SELECT id FROM c_ex);
INSERT INTO note_examen (candidat_id, epreuve_id, note)
SELECT c.id, e.id, round(2 + (abs(hashtext(c.id::text || '-' || e.id::text)) % 700) / 100.0, 2)
FROM candidat_examen c JOIN epreuve_examen e ON e.examen_id = c.examen_id
WHERE c.examen_id = (SELECT id FROM c_ex) AND c.statut_epreuves = 'PRESENT';
UPDATE candidat_examen c SET moyenne = m.moy, decision = CASE WHEN m.moy >= 5 THEN 'ADMIS' ELSE 'NON_ADMIS' END
FROM (SELECT n.candidat_id, round(sum(n.note * e.coefficient) / sum(e.coefficient), 2) AS moy
      FROM note_examen n JOIN epreuve_examen e ON e.id = n.epreuve_id WHERE e.examen_id = (SELECT id FROM c_ex) GROUP BY n.candidat_id) m
WHERE m.candidat_id = c.id;
UPDATE candidat_examen SET decision = 'NON_ADMIS' WHERE examen_id = (SELECT id FROM c_ex) AND statut_epreuves = 'ELIMINE';

-- Résultats consolidés par école (comme le fait l'application à partir des candidats)
INSERT INTO resultat_examen (examen_id, etablissement_id, presentes, admis, absents, elimines, presentes_filles, admis_filles)
SELECT c.examen_id, c.etablissement_id,
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'),
       count(*) FILTER (WHERE c.decision = 'ADMIS'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ABSENT'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ELIMINE'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND e.sexe = 'F'),
       count(*) FILTER (WHERE c.decision = 'ADMIS' AND e.sexe = 'F')
FROM candidat_examen c JOIN eleve e ON e.id = c.eleve_id
WHERE c.examen_id = (SELECT id FROM c_ex) GROUP BY c.examen_id, c.etablissement_id;

-- ---------- 6. Centres : personnel, préparation, incidents, plis et rapports de surveillance ----------
CREATE TEMP TABLE c_nom (n int, nom text);
INSERT INTO c_nom VALUES (1, 'Mamadou Sylla'), (2, 'Fanta Camara'), (3, 'Ibrahima Barry'), (4, 'Aminata Diallo'), (5, 'Sékou Touré'),
    (6, 'Mariama Bah'), (7, 'Ousmane Condé'), (8, 'Hawa Keita'), (9, 'Alpha Soumah'), (10, 'Kadiatou Bangoura'),
    (11, 'Moussa Kourouma'), (12, 'Fatoumata Sow'), (13, 'Lansana Fofana'), (14, 'Aïssatou Sidibé'), (15, 'Boubacar Traoré'), (16, 'Namory Doumbouya');
CREATE TEMP TABLE c_salle AS
SELECT centre_id, salle FROM candidat_examen WHERE examen_id = (SELECT id FROM c_ex) AND centre_id IS NOT NULL GROUP BY centre_id, salle;

INSERT INTO personnel_centre (centre_id, role, nom, telephone, salle)
SELECT ct.centre_id, v.role, (SELECT nom FROM c_nom WHERE n = 1 + ((ct.rang * 4 + v.k) % 16)), '+224 62' || ct.rang || ' 00 0' || v.k, NULL
FROM c_centre ct, (VALUES ('PRESIDENT', 1), ('CHEF_CENTRE', 2), ('SECRETAIRE', 3), ('SECURITE', 4), ('SANTE', 5)) v(role, k);
INSERT INTO personnel_centre (centre_id, role, nom, salle)
SELECT s.centre_id, v.role, (SELECT nom FROM c_nom WHERE n = 1 + ((s.centre_id * 3 + s.salle * 5 + v.k) % 16)), s.salle
FROM c_salle s, (VALUES ('CHEF_SALLE', 1), ('SURVEILLANT', 2), ('SURVEILLANT', 3)) v(role, k);

INSERT INTO preparation_centre (centre_id, element, fait, observation, fait_le, fait_par)
SELECT ct.centre_id, el, TRUE, NULL, timestamptz '2026-09-07 15:00+00', 'chef.centre' || ct.rang || '@demo.gn'
FROM c_centre ct, unnest(ARRAY['SALLES', 'TABLES_BANCS', 'AFFICHAGE', 'SECRETARIAT', 'SECURITE', 'PERSONNEL', 'MATERIEL']) el;

INSERT INTO incident_examen (examen_id, centre_id, candidat_id, epreuve_id, type, description, survenu_le, signale_par, statut, decision, elimine)
SELECT (SELECT id FROM c_ex), c.centre_id, c.id, (SELECT min(id) FROM epreuve_examen WHERE examen_id = c.examen_id), 'FRAUDE',
       'Antisèche découverte sur le candidat pendant l''épreuve de français.', timestamptz '2026-09-14 09:10+01', 'Chef de salle', 'TRAITE',
       'Procès-verbal de fraude établi ; le candidat est éliminé de l''examen.', TRUE
FROM candidat_examen c WHERE c.examen_id = (SELECT id FROM c_ex) AND c.statut_epreuves = 'ELIMINE';
INSERT INTO incident_examen (examen_id, centre_id, type, description, survenu_le, signale_par, statut, decision)
SELECT (SELECT id FROM c_ex), ct.centre_id, v.type, v.descr, timestamptz '2026-09-14 08:20+01', 'Secrétaire du centre', v.statut, v.decision
FROM c_centre ct, (VALUES ('RETARD', 'Plusieurs candidats arrivés après l''ouverture des portes.', 'TRAITE', 'Admis en salle, temps de composition inchangé.'),
                          ('MALAISE', 'Un candidat a eu un malaise ; pris en charge par l''infirmier du centre.', 'TRAITE', 'Candidat revenu en salle après repos.')) v(type, descr, statut, decision)
WHERE ct.rang <= 2 OR v.type = 'RETARD';

INSERT INTO epreuve_centre (centre_id, epreuve_id, nb_exemplaires, statut_pli, conditionne_le, expedie_le, recu_dpe_le, recu_president_le,
                            ouvert_le, ouvert_par, temoin1, temoin2, composition_debut, composition_fin, presents, absents, elimines, copies_ramassees,
                            observations, rapport_clos)
SELECT ct.centre_id, e.id,
       (SELECT count(*) FROM candidat_examen c WHERE c.centre_id = ct.centre_id)::int + 5, 'OUVERT',
       e.date_epreuve - 5 + time '09:00', e.date_epreuve - 4 + time '09:00', e.date_epreuve - 2 + time '10:00', e.date_epreuve - 1 + time '16:00',
       e.date_epreuve + e.heure_debut - interval '30 minutes', 'Président du centre', 'Témoin 1 (délégué)', 'Témoin 2 (enseignant)',
       e.date_epreuve + e.heure_debut, e.date_epreuve + e.heure_fin,
       (SELECT count(*) FROM candidat_examen c WHERE c.centre_id = ct.centre_id AND c.statut_epreuves = 'PRESENT')::int,
       (SELECT count(*) FROM candidat_examen c WHERE c.centre_id = ct.centre_id AND c.statut_epreuves = 'ABSENT')::int,
       (SELECT count(*) FROM candidat_examen c WHERE c.centre_id = ct.centre_id AND c.statut_epreuves = 'ELIMINE')::int,
       (SELECT count(*) FROM candidat_examen c WHERE c.centre_id = ct.centre_id AND c.statut_epreuves = 'PRESENT')::int,
       'RAS', TRUE
FROM c_centre ct JOIN epreuve_examen e ON e.examen_id = (SELECT id FROM c_ex);

-- ---------- 7. Correction, sujets, diplômes ----------
INSERT INTO commission_correction (examen_id, epreuve_id, nom, lieu, nb_copies, nb_copies_corrigees, cloturee)
SELECT e.examen_id, e.id, 'Commission ' || COALESCE(m.libelle, 'épreuve'), 'DPE de Conakry',
       (SELECT count(*) FROM candidat_examen c WHERE c.examen_id = e.examen_id AND c.statut_epreuves = 'PRESENT')::int,
       (SELECT count(*) FROM candidat_examen c WHERE c.examen_id = e.examen_id AND c.statut_epreuves = 'PRESENT')::int, TRUE
FROM epreuve_examen e LEFT JOIN matiere m ON m.id = e.matiere_id WHERE e.examen_id = (SELECT id FROM c_ex);
INSERT INTO membre_commission_correction (commission_id, role, nom)
SELECT cc.id, v.role, (SELECT nom FROM c_nom WHERE n = 1 + ((cc.id * 3 + v.k) % 16))
FROM commission_correction cc, (VALUES ('PRESIDENT', 1), ('SECRETAIRE', 2), ('CORRECTEUR', 3), ('CORRECTEUR', 4)) v(role, k)
WHERE cc.examen_id = (SELECT id FROM c_ex);

INSERT INTO sujet_examen (epreuve_id, variante, intitule, auteurs, statut, valide_le, tire_le)
SELECT e.id, v.variante, 'Sujet ' || v.variante || ' : ' || COALESCE(m.libelle, 'épreuve'), 'Commission d''élaboration',
       CASE WHEN v.variante = 'A' THEN 'TIRE' ELSE 'VALIDE' END, timestamptz '2026-09-01 10:00+00',
       CASE WHEN v.variante = 'A' THEN e.date_epreuve + e.heure_debut - interval '1 hour' END
FROM epreuve_examen e LEFT JOIN matiere m ON m.id = e.matiere_id, (VALUES ('A'), ('B'), ('C')) v(variante)
WHERE e.examen_id = (SELECT id FROM c_ex);

INSERT INTO diplome_examen (candidat_id, numero, code, mention, delivre_le)
SELECT c.id, 'DIP-CB26-' || lpad(c.id::text, 6, '0'), upper(substr(md5('diplome' || c.id), 1, 10)),
       CASE WHEN c.moyenne >= 8 THEN 'TRES_BIEN' WHEN c.moyenne >= 7 THEN 'BIEN' WHEN c.moyenne >= 6 THEN 'ASSEZ_BIEN' ELSE 'PASSABLE' END,
       date '2026-10-14'
FROM candidat_examen c WHERE c.examen_id = (SELECT id FROM c_ex) AND c.decision = 'ADMIS';

-- ---------- 8. Chefs de centre (un compte par centre, accès limité à son centre) ----------
INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom)
SELECT 'chef.centre' || ct.rang || '@demo.gn', (SELECT mot_de_passe_hash FROM utilisateur WHERE email = 'admin@demo.gn'),
       split_part((SELECT nom FROM c_nom WHERE n = 1 + ((ct.rang * 4 + 2) % 16)), ' ', 2),
       split_part((SELECT nom FROM c_nom WHERE n = 1 + ((ct.rang * 4 + 2) % 16)), ' ', 1)
FROM c_centre ct;
INSERT INTO affectation_role (utilisateur_id, role, perimetre)
SELECT u.id, 'CHEF_CENTRE', 'PERSONNEL' FROM c_centre ct JOIN utilisateur u ON u.email = 'chef.centre' || ct.rang || '@demo.gn';
INSERT INTO acces_centre (utilisateur_id, centre_id)
SELECT u.id, ct.centre_id FROM c_centre ct JOIN utilisateur u ON u.email = 'chef.centre' || ct.rang || '@demo.gn';

DO $$
DECLARE r text;
BEGIN
    SELECT format('%s candidats, %s notes, %s admis, %s diplômes, %s incidents, %s plis, %s chefs de centre',
        (SELECT count(*) FROM candidat_examen WHERE examen_id = (SELECT id FROM c_ex)),
        (SELECT count(*) FROM note_examen n JOIN epreuve_examen e ON e.id = n.epreuve_id WHERE e.examen_id = (SELECT id FROM c_ex)),
        (SELECT count(*) FROM candidat_examen WHERE examen_id = (SELECT id FROM c_ex) AND decision = 'ADMIS'),
        (SELECT count(*) FROM diplome_examen), (SELECT count(*) FROM incident_examen WHERE examen_id = (SELECT id FROM c_ex)),
        (SELECT count(*) FROM epreuve_centre), (SELECT count(*) FROM acces_centre)) INTO r;
    RAISE NOTICE 'Complément chargé : %', r;
END $$;
