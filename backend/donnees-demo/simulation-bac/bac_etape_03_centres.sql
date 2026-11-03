-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 3/9 : CENTRES D'EXAMEN, AFFECTATION ET PERSONNEL (avril à mai 2026)
-- Sept lycées sont désignés centres d'examen et caractérisés (état, accessibilité, risques). Les candidats sont affectés à un centre,
-- reçoivent leur numéro de PV, leur salle et leur table. Les dossiers incomplets sont régularisés et toutes les listes sont validées.
-- Les centres constituent leur personnel (président, chef de centre, secrétaire, sécurité, santé, chefs de salle, surveillants),
-- la DGECS enregistre une partie du personnel national (jurys, superviseurs, inspecteurs, informatique) et les chefs de centre
-- reçoivent un compte. À exécuter APRÈS l'étape 2. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM candidat_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 2.';
    END IF;
    IF EXISTS (SELECT 1 FROM centre_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'L''étape 3 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';
CREATE TEMP TABLE s_nom AS
SELECT n, (ARRAY['Mamadou', 'Fanta', 'Ibrahima', 'Aminata', 'Sékou', 'Mariama', 'Ousmane', 'Hawa', 'Alpha', 'Kadiatou'])[1 + (n - 1) % 10] || ' ' ||
       (ARRAY['Sylla', 'Camara', 'Barry', 'Diallo', 'Touré', 'Bah', 'Condé', 'Keita', 'Soumah', 'Bangoura'])[1 + (n - 1) / 10] AS nom
FROM generate_series(1, 100) n;

-- ---------- 1. Les sept centres (un lycée par centre) ----------
INSERT INTO centre_examen (examen_id, zone_id, nom, adresse, date_debut, created_at, etablissement_id)
SELECT (SELECT id FROM s_ex), e.zone_id, 'Centre ' || e.nom, e.adresse, date '2026-06-29', timestamptz '2026-04-06 09:00+00', e.id
FROM etablissement e WHERE e.code_officiel IN ('GN-0003', 'GN-0006', 'GN-0013', 'GN-0017', 'GN-0019', 'GN-0023', 'GN-0027');
CREATE TEMP TABLE s_centre AS
SELECT ce.id AS centre_id, e.code_officiel AS code, row_number() OVER (ORDER BY e.code_officiel) AS rang
FROM centre_examen ce JOIN etablissement e ON e.id = ce.etablissement_id WHERE ce.examen_id = (SELECT id FROM s_ex);

-- Caractéristiques : Labé enclavé, Nzérékoré éloigné et exposé aux inondations, Kindia aux toilettes moyennes, Ratoma à une salle de réserve
INSERT INTO caracteristiques_centre (centre_id, salles_reserve, etat_tables_bancs, etat_toilettes, etat_eclairage, etat_ventilation, accessible,
                                     distance_max_km, temps_max_min, risque_inondation, risque_enclavement, risque_insecurite, observation, mis_a_jour_le)
SELECT centre_id, CASE rang WHEN 2 THEN 1 WHEN 1 THEN 1 ELSE 0 END,
       CASE rang WHEN 5 THEN 'MOYEN' ELSE 'BON' END, CASE rang WHEN 4 THEN 'MOYEN' WHEN 7 THEN 'MAUVAIS' ELSE 'BON' END,
       'BON', CASE rang WHEN 6 THEN 'MOYEN' ELSE 'BON' END, rang <> 5,
       CASE rang WHEN 1 THEN 4 WHEN 2 THEN 6 WHEN 3 THEN 12 WHEN 4 THEN 20 WHEN 5 THEN 55 WHEN 6 THEN 28 ELSE 62 END,
       CASE rang WHEN 1 THEN 15 WHEN 2 THEN 20 WHEN 3 THEN 35 WHEN 4 THEN 50 WHEN 5 THEN 150 WHEN 6 THEN 70 ELSE 180 END,
       rang = 7, rang = 5, FALSE,
       CASE rang WHEN 5 THEN 'Route coupée en cas de fortes pluies : prévoir l''acheminement des plis plusieurs jours avant.'
                 WHEN 7 THEN 'Toilettes à réparer ; pistes inondables en saison des pluies.' END,
       timestamptz '2026-04-20 10:00+00'
FROM s_centre;

-- ---------- 2. Affectation des candidats : centre le plus proche, PV, salle (30 par salle) et table ----------
CREATE TEMP TABLE s_carte (etab text, centre text);
INSERT INTO s_carte VALUES ('GN-0003', 'GN-0003'), ('GN-0010', 'GN-0003'), ('GN-0006', 'GN-0006'), ('GN-0007', 'GN-0006'),
    ('GN-0013', 'GN-0013'), ('GN-0017', 'GN-0017'), ('GN-0019', 'GN-0019'), ('GN-0023', 'GN-0023'), ('GN-0027', 'GN-0027');
UPDATE candidat_examen c SET centre_id = sc.centre_id
FROM etablissement e, s_carte m, s_centre sc
WHERE c.examen_id = (SELECT id FROM s_ex) AND e.id = c.etablissement_id AND m.etab = e.code_officiel AND sc.code = m.centre;
UPDATE candidat_examen c SET numero_pv = 'PVS-' || lpad(r.rang_global::text, 5, '0'), salle = 1 + (r.rang_centre - 1) / 30, table_numero = 1 + (r.rang_centre - 1) % 30
FROM (SELECT id, row_number() OVER (PARTITION BY centre_id ORDER BY etablissement_id, id) AS rang_centre,
             row_number() OVER (ORDER BY centre_id, etablissement_id, id) AS rang_global
      FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex)) r
WHERE r.id = c.id;

-- ---------- 3. Dossiers régularisés et listes toutes validées par la DGECS ----------
INSERT INTO piece_candidature (candidat_id, type, reference, fournie_le)
SELECT c.id, t.type, CASE t.type WHEN 'ACTE_NAISSANCE' THEN 'ACTE-' || c.id WHEN 'DIPLOME' THEN 'DIP-' || c.id END, date '2026-04-10' + (abs(hashtext(c.id::text || t.type)) % 20)
FROM candidat_examen c CROSS JOIN (VALUES ('DEMANDE'), ('ACTE_NAISSANCE'), ('DIPLOME'), ('ATTESTATION')) t(type)
WHERE c.examen_id = (SELECT id FROM s_ex) AND NOT (t.type = 'ATTESTATION' AND c.libre)
  AND NOT EXISTS (SELECT 1 FROM piece_candidature p WHERE p.candidat_id = c.id AND p.type = t.type);
UPDATE liste_candidats SET statut = 'VALIDEE', motif_rejet = NULL,
       transmise_le = coalesce(transmise_le, timestamptz '2026-04-15 10:00+00'),
       validee_region_le = coalesce(validee_region_le, timestamptz '2026-04-22 10:00+00'),
       validee_le = coalesce(validee_le, timestamptz '2026-04-28 10:00+00')
WHERE examen_id = (SELECT id FROM s_ex);

-- ---------- 4. Personnel des centres : président, chef de centre, secrétaire, sécurité, santé ----------
INSERT INTO personnel_centre (centre_id, role, nom, telephone, salle)
SELECT sc.centre_id, v.role, (SELECT nom FROM s_nom WHERE n = 1 + ((sc.rang * 10 + v.k) * 37) % 100), '+224 62' || sc.rang || ' 10 0' || v.k, NULL
FROM s_centre sc, (VALUES ('PRESIDENT', 1), ('CHEF_CENTRE', 2), ('SECRETAIRE', 3), ('SECURITE', 4), ('SECURITE', 5), ('SANTE', 6), ('SANTE', 7)) v(role, k);
-- Chefs de salle et surveillants (deux par salle)
CREATE TEMP TABLE s_salle AS
SELECT centre_id, salle FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex) GROUP BY centre_id, salle;
INSERT INTO personnel_centre (centre_id, role, nom, telephone, salle)
SELECT s.centre_id, v.role, (SELECT nom FROM s_nom WHERE n = 1 + ((s.centre_id * 7 + s.salle * 11 + v.k * 3) * 37) % 100), '+224 66' || s.salle || ' 20 0' || v.k, s.salle
FROM s_salle s, (VALUES ('CHEF_SALLE', 1), ('SURVEILLANT', 2), ('SURVEILLANT', 3)) v(role, k);
-- Une salle du centre de Nzérékoré n'a qu'un seul surveillant : l'étape 4 le complétera
DELETE FROM personnel_centre WHERE id = (SELECT max(p.id) FROM personnel_centre p JOIN s_centre sc ON sc.centre_id = p.centre_id
                                         WHERE sc.rang = 7 AND p.role = 'SURVEILLANT');

-- Préparation matérielle : quatre centres sont prêts, trois ont encore des éléments à terminer
INSERT INTO preparation_centre (centre_id, element, fait, observation, fait_le, fait_par)
SELECT sc.centre_id, el, (sc.rang <= 4 OR el IN ('SALLES', 'TABLES_BANCS', 'AFFICHAGE')),
       CASE WHEN sc.rang > 4 AND el NOT IN ('SALLES', 'TABLES_BANCS', 'AFFICHAGE') THEN 'En cours' END,
       CASE WHEN sc.rang <= 4 OR el IN ('SALLES', 'TABLES_BANCS', 'AFFICHAGE') THEN timestamptz '2026-05-20 15:00+00' END,
       CASE WHEN sc.rang <= 4 OR el IN ('SALLES', 'TABLES_BANCS', 'AFFICHAGE') THEN 'chef.bac' || sc.rang || '@demo.gn' END
FROM s_centre sc, unnest(ARRAY['SALLES', 'TABLES_BANCS', 'AFFICHAGE', 'SECRETARIAT', 'SECURITE', 'PERSONNEL', 'MATERIEL']) el;

-- ---------- 5. Personnel national enregistré par la DGECS (le reste suivra à l'étape 4) ----------
INSERT INTO personnel_examen (examen_id, fonction, nom, telephone, created_at)
SELECT (SELECT id FROM s_ex), v.fonction, (SELECT nom FROM s_nom WHERE n = 1 + ((v.base + g) * 37) % 100), '+224 64' || (v.base + g) || ' 30 00', timestamptz '2026-05-11 10:00+00'
FROM (VALUES ('JURY', 100, 9), ('SUPERVISEUR', 200, 8), ('INSPECTEUR', 300, 5), ('INFORMATIQUE', 400, 4)) v(fonction, base, nb)
CROSS JOIN LATERAL generate_series(1, v.nb) g;

-- ---------- 6. Comptes des chefs de centre (accès limité à leur centre) ----------
INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom)
SELECT 'chef.bac' || sc.rang || '@demo.gn', (SELECT mot_de_passe_hash FROM utilisateur WHERE email = 'admin@demo.gn'),
       split_part(p.nom, ' ', 2), split_part(p.nom, ' ', 1)
FROM s_centre sc JOIN personnel_centre p ON p.centre_id = sc.centre_id AND p.role = 'CHEF_CENTRE';
INSERT INTO affectation_role (utilisateur_id, role, perimetre)
SELECT u.id, 'CHEF_CENTRE', 'PERSONNEL' FROM s_centre sc JOIN utilisateur u ON u.email = 'chef.bac' || sc.rang || '@demo.gn';
INSERT INTO acces_centre (utilisateur_id, centre_id)
SELECT u.id, sc.centre_id FROM s_centre sc JOIN utilisateur u ON u.email = 'chef.bac' || sc.rang || '@demo.gn';

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 3 chargée : % centres, % candidats affectés, % membres du personnel des centres, % du personnel national, % comptes de chefs de centre',
        (SELECT count(*) FROM centre_examen WHERE examen_id = x), (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND centre_id IS NOT NULL),
        (SELECT count(*) FROM personnel_centre p JOIN centre_examen ce ON ce.id = p.centre_id WHERE ce.examen_id = x),
        (SELECT count(*) FROM personnel_examen WHERE examen_id = x), (SELECT count(*) FROM acces_centre a JOIN centre_examen ce ON ce.id = a.centre_id WHERE ce.examen_id = x);
END $$;
