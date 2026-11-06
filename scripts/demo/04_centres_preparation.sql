-- =====================================================================================
-- DÉMONSTRATION 4/6 — Préparation : centres d'examen et chefs de centre, état des locaux, travaux préparatoires, répartition en
-- salles et numéros de PV, personnel, livraisons, convois, plis. À exécuter APRÈS le script 03.
-- =====================================================================================
BEGIN;

-- ------------------------------------------------------------------ Table commune aux scripts 02 à 06 : les examens simulés
-- stade : 0 planification · 1 inscriptions · 2 préparation · 3 déroulement · 4 correction · 5 délibéré · 6 proclamé
-- av (avancement de la préparation, stade 2) : 1 centres seulement · 2 salles, PV, logistique prévue · 3 plis conditionnés, convois en route
CREATE TEMP TABLE demo_examen (cle text, annee text, libelle text, cycle text, niveau text, orga text, types text[],
                               debut date, nbj int, gab text, fam text, stade int, av int);
INSERT INTO demo_examen VALUES
    -- Année 2026-2027 : examens à venir (aujourd'hui = novembre 2026)
    ('BAC-G-2027',  '2026-2027', 'Baccalauréat 2027 – Enseignement général',        'LYCEE',    'TERM', 'NATIONAL',   ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL'], '2027-06-07', 4, 'BAC',  'G',  2, 3),
    ('BAC-FA-2027', '2026-2027', 'Baccalauréat 2027 – Enseignement franco-arabe',   'LYCEE',    'TERM', 'NATIONAL',   ARRAY['FRANCO_ARABE'],                               '2027-06-07', 5, 'BAC',  'FA', 1, 0),
    ('BAC-T-2027',  '2026-2027', 'Baccalauréat 2027 – Enseignement technique',      'LYCEE',    'TERM', 'NATIONAL',   ARRAY['TECHNIQUE'],                                  '2027-06-07', 3, 'BACT', 'T',  0, 0),
    ('BEPC-G-2027', '2026-2027', 'BEPC 2027 – Enseignement général',                'COLLEGE',  '10A',  'REGION',     ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL'], '2027-05-10', 2, 'BEPC', 'G',  2, 1),
    ('BEPC-FA-2027','2026-2027', 'BEPC 2027 – Enseignement franco-arabe',           'COLLEGE',  '10A',  'REGION',     ARRAY['FRANCO_ARABE'],                               '2027-05-10', 3, 'BEPC', 'FA', 1, 0),
    ('BEPC-T-2027', '2026-2027', 'BEPC 2027 – Enseignement technique',              'COLLEGE',  '10A',  'REGION',     ARRAY['TECHNIQUE'],                                  '2027-05-10', 3, 'BEPC', 'T',  0, 0),
    ('CEE-G-2027',  '2026-2027', 'CEE 2027 – Enseignement général',                 'PRIMAIRE', '6A',   'PREFECTURE', ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL'], '2027-05-18', 1, 'CEE',  'G',  1, 0),
    ('CEE-FA-2027', '2026-2027', 'CEE 2027 – Enseignement franco-arabe',            'PRIMAIRE', '6A',   'PREFECTURE', ARRAY['FRANCO_ARABE'],                               '2027-05-18', 2, 'CEE',  'FA', 0, 0),
    ('COM9-2027',   '2026-2027', 'Examen communal de 9e année 2027',                'COLLEGE',  '9A',   'COMMUNE',    ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL', 'FRANCO_ARABE', 'TECHNIQUE'], '2027-05-03', 1, 'COM', 'G', 1, 0),
    -- Année 2025-2026 : examens passés, à différents états d'avancement
    ('BAC-G-2026',  '2025-2026', 'Baccalauréat 2026 – Enseignement général',        'LYCEE',    'TERM', 'NATIONAL',   ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL'], '2026-06-08', 4, 'BAC',  'G',  6, 3),
    ('BAC-FA-2026', '2025-2026', 'Baccalauréat 2026 – Enseignement franco-arabe',   'LYCEE',    'TERM', 'NATIONAL',   ARRAY['FRANCO_ARABE'],                               '2026-06-08', 5, 'BAC',  'FA', 5, 3),
    ('BAC-T-2026',  '2025-2026', 'Baccalauréat 2026 – Enseignement technique',      'LYCEE',    'TERM', 'NATIONAL',   ARRAY['TECHNIQUE'],                                  '2026-06-08', 3, 'BACT', 'T',  4, 3),
    ('BEPC-G-2026', '2025-2026', 'BEPC 2026 – Enseignement général',                'COLLEGE',  '10A',  'REGION',     ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL'], '2026-05-11', 2, 'BEPC', 'G',  6, 3),
    ('BEPC-FA-2026','2025-2026', 'BEPC 2026 – Enseignement franco-arabe',           'COLLEGE',  '10A',  'REGION',     ARRAY['FRANCO_ARABE'],                               '2026-05-11', 3, 'BEPC', 'FA', 3, 3),
    ('BEPC-T-2026', '2025-2026', 'BEPC 2026 – Enseignement technique',              'COLLEGE',  '10A',  'REGION',     ARRAY['TECHNIQUE'],                                  '2026-05-11', 3, 'BEPC', 'T',  5, 3),
    ('CEE-G-2026',  '2025-2026', 'CEE 2026 – Enseignement général',                 'PRIMAIRE', '6A',   'PREFECTURE', ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL'], '2026-05-18', 1, 'CEE',  'G',  6, 3),
    ('CEE-FA-2026', '2025-2026', 'CEE 2026 – Enseignement franco-arabe',            'PRIMAIRE', '6A',   'PREFECTURE', ARRAY['FRANCO_ARABE'],                               '2026-05-18', 2, 'CEE',  'FA', 4, 3),
    ('COM9-2026',   '2025-2026', 'Examen communal de 9e année 2026',                'COLLEGE',  '9A',   'COMMUNE',    ARRAY['CLASSIQUE', 'COMMUNAUTAIRE', 'CONFESSIONNEL', 'FRANCO_ARABE', 'TECHNIQUE'], '2026-05-04', 1, 'COM', 'G', 6, 3);

-- Noms de personnes fictifs (personnel, jurys, correcteurs…)
CREATE FUNCTION pg_temp.nom_complet(n int) RETURNS text LANGUAGE sql IMMUTABLE AS $f$
    SELECT (ARRAY['Mamadou', 'Ibrahima', 'Ousmane', 'Alpha', 'Mohamed', 'Sékou', 'Boubacar', 'Abdoulaye', 'Thierno', 'Moussa',
                  'Fatoumata', 'Mariama', 'Aissatou', 'Kadiatou', 'Hawa', 'Fanta', 'Djénabou', 'Salimatou', 'Aminata', 'Mariam'])[1 + abs(n * 7) % 20]
           || ' ' ||
           (ARRAY['Diallo', 'Camara', 'Bah', 'Barry', 'Sylla', 'Soumah', 'Keita', 'Traoré', 'Condé', 'Touré',
                  'Bangoura', 'Sow', 'Kourouma', 'Doumbouya', 'Cissé', 'Fofana', 'Konaté', 'Sidibé', 'Baldé', 'Diakité',
                  'Savané', 'Béavogui', 'Kamano', 'Haba', 'Loua'])[1 + abs(n * 13 + 5) % 25]
$f$;

-- ------------------------------------------------------------------ Centres d'examen (examens à partir du stade 2)
-- gabarit, famille, établissement qui héberge le centre (un centre par commune ayant des candidats)
CREATE TEMP TABLE demo_hote (gab text, fam text, code_etab text);
INSERT INTO demo_hote VALUES
    ('BAC', 'G', 'GN-CKY-KAL-LY-01'), ('BAC', 'G', 'GN-CKY-DIX-LY-01'), ('BAC', 'G', 'GN-KIN-KIN-LY-01'), ('BAC', 'G', 'GN-KAN-KAN-LY-01'),
    ('BAC', 'FA', 'GN-CKY-DIX-FA-01'), ('BAC', 'FA', 'GN-KAN-KAN-FA-01'),
    ('BACT', 'T', 'GN-CKY-DIX-LT-01'),
    ('BEPC', 'G', 'GN-CKY-KAL-CO-01'), ('BEPC', 'G', 'GN-CKY-DIX-LY-01'), ('BEPC', 'G', 'GN-KIN-KIN-LY-01'), ('BEPC', 'G', 'GN-KAN-KAN-LY-01'),
    ('BEPC', 'FA', 'GN-CKY-DIX-FA-01'), ('BEPC', 'FA', 'GN-KIN-KIN-FA-01'), ('BEPC', 'FA', 'GN-KAN-KAN-FA-01'),
    ('BEPC', 'T', 'GN-KIN-KIN-CT-01'),
    ('CEE', 'G', 'GN-CKY-KAL-EP-01'), ('CEE', 'G', 'GN-CKY-DIX-EP-01'), ('CEE', 'G', 'GN-KIN-KIN-EP-01'), ('CEE', 'G', 'GN-KAN-KAN-EP-01'),
    ('CEE', 'FA', 'GN-CKY-DIX-FA-01'), ('CEE', 'FA', 'GN-KIN-KIN-FA-01'), ('CEE', 'FA', 'GN-KAN-KAN-FA-01'),
    ('COM', 'G', 'GN-CKY-KAL-CO-01'), ('COM', 'G', 'GN-CKY-DIX-LY-01'), ('COM', 'G', 'GN-KIN-KIN-LY-01'), ('COM', 'G', 'GN-KAN-KAN-LY-01');

INSERT INTO centre_examen (examen_id, zone_id, nom, adresse, date_debut, created_at, etablissement_id)
SELECT e.id, et.zone_id, 'Centre – ' || et.nom, et.adresse, d.debut, (d.debut - 80)::timestamptz, et.id
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN demo_hote h ON h.gab = d.gab AND h.fam = d.fam
JOIN etablissement et ON et.code_officiel = h.code_etab
WHERE d.stade >= 2;

-- Chaque candidat est affecté au centre de sa commune
UPDATE candidat_examen c
SET centre_id = (SELECT ce.id FROM centre_examen ce WHERE ce.examen_id = c.examen_id AND ce.zone_id = et.zone_id LIMIT 1)
FROM etablissement et
WHERE et.id = c.etablissement_id
  AND EXISTS (SELECT 1 FROM centre_examen x WHERE x.examen_id = c.examen_id);

-- Un chef de centre par centre (compte limité à son centre)
CREATE TEMP TABLE demo_chef AS
SELECT 'chef.' || lower(replace(d.cle, '-', '.')) || '.' || lower(substr(z.nom, 1, 3)) || '@example.org' AS email,
       ce.nom AS centre, ce.id AS centre_id
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN centre_examen ce ON ce.examen_id = e.id
JOIN zone_geographique z ON z.id = ce.zone_id;
INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom)
SELECT email, '$2a$10$6zOYDBuEs3qDJ.4qJQeFjeMJtm2Ape8Wcfx0QCFM1UuvA.99bAeTO', 'Chef de centre', centre FROM demo_chef;
INSERT INTO affectation_role (utilisateur_id, role, perimetre)
SELECT u.id, 'CHEF_CENTRE', 'PERSONNEL' FROM demo_chef c JOIN utilisateur u ON u.email = c.email;
INSERT INTO acces_centre (utilisateur_id, centre_id)
SELECT u.id, c.centre_id FROM demo_chef c JOIN utilisateur u ON u.email = c.email;

-- Effectifs par centre (utilisés ensuite)
CREATE TEMP TABLE demo_effectif AS
SELECT ce.id AS centre_id, d.cle, d.stade, d.av, d.debut, e.id AS examen_id, z.nom AS commune, count(c.id) AS candidats,
       greatest(1, (count(c.id) + 29) / 30) AS salles
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN centre_examen ce ON ce.examen_id = e.id
JOIN zone_geographique z ON z.id = ce.zone_id
LEFT JOIN candidat_examen c ON c.centre_id = ce.id
GROUP BY ce.id, d.cle, d.stade, d.av, d.debut, e.id, z.nom;

-- ------------------------------------------------------------------ Caractéristiques des centres (état des locaux, risques)
INSERT INTO caracteristiques_centre (centre_id, salles_reserve, etat_tables_bancs, etat_toilettes, etat_eclairage, etat_ventilation,
                                     accessible, distance_max_km, temps_max_min, risque_inondation, risque_enclavement, risque_insecurite,
                                     observation, mis_a_jour_le)
SELECT f.centre_id, 1 + f.centre_id % 2,
       (ARRAY['BON', 'BON', 'MOYEN', 'BON', 'MAUVAIS'])[1 + (f.centre_id * 3) % 5],
       (ARRAY['BON', 'MOYEN', 'MAUVAIS', 'BON', 'BON'])[1 + (f.centre_id * 5) % 5],
       (ARRAY['BON', 'BON', 'MOYEN', 'BON', 'BON'])[1 + (f.centre_id * 7) % 5],
       (ARRAY['MOYEN', 'BON', 'BON', 'MAUVAIS', 'BON'])[1 + (f.centre_id * 11) % 5],
       f.commune <> 'Kankan', 8 + (f.centre_id * 3) % 25, 25 + (f.centre_id * 7) % 70,
       f.commune = 'Kindia', f.commune = 'Kankan', f.commune = 'Dixinn' AND f.centre_id % 3 = 0,
       CASE WHEN f.commune = 'Kindia' THEN 'Zone inondable en saison des pluies : prévoir une salle de repli'
            WHEN f.commune = 'Kankan' THEN 'Accès difficile pour certains candidats éloignés : convoi anticipé' END,
       (f.debut - 45)::timestamptz
FROM demo_effectif f;

-- ------------------------------------------------------------------ Travaux préparatoires du centre (7 éléments du règlement)
INSERT INTO preparation_centre (centre_id, element, fait, observation, fait_le, fait_par)
SELECT f.centre_id, el.element,
       (f.stade >= 3 OR f.av >= 3 OR (f.av = 2 AND el.ordre <= 4)),
       CASE WHEN el.element = 'TABLES_BANCS' AND f.av = 2 THEN 'Réparation de 12 tables-bancs en cours' END,
       CASE WHEN f.stade >= 3 OR f.av >= 3 OR (f.av = 2 AND el.ordre <= 4) THEN (f.debut - 12 + el.ordre)::timestamptz END,
       CASE WHEN f.stade >= 3 OR f.av >= 3 OR (f.av = 2 AND el.ordre <= 4)
            THEN (SELECT c.email FROM demo_chef c WHERE c.centre_id = f.centre_id) END
FROM demo_effectif f,
     (VALUES ('SALLES', 1), ('TABLES_BANCS', 2), ('AFFICHAGE', 3), ('SECRETARIAT', 4), ('SECURITE', 5), ('PERSONNEL', 6), ('MATERIEL', 7)) el(element, ordre);

-- ------------------------------------------------------------------ Répartition en salles : salle, table (1 à 30) et numéro de PV
-- (avancement 2 et plus ; avancement 1 : centres désignés, répartition à faire)
UPDATE candidat_examen c
SET salle = 1 + (r.rang - 1) / 30, table_numero = 1 + (r.rang - 1) % 30,
    numero_pv = format('PV%s-%s-%s', c.examen_id, c.centre_id, lpad(r.rang::text, 4, '0'))
FROM (SELECT c2.id, row_number() OVER (PARTITION BY c2.centre_id ORDER BY c2.etablissement_id, el.nom, el.prenom, c2.id) AS rang
      FROM candidat_examen c2 JOIN eleve el ON el.id = c2.eleve_id
      JOIN demo_effectif f ON f.centre_id = c2.centre_id
      WHERE f.stade >= 3 OR f.av >= 2) r
WHERE r.id = c.id;

-- ------------------------------------------------------------------ Personnel du centre
-- Direction du centre pour tous ; le reste (secrétariat, sécurité, santé, chefs de salle, surveillants) dès l'avancement 2
INSERT INTO personnel_centre (centre_id, role, nom, telephone, salle, created_at)
SELECT f.centre_id, p.role, pg_temp.nom_complet((f.centre_id * 17 + p.rang)::int), '+224 66' || lpad(((f.centre_id * 7907 + p.rang * 7919) % 10000000)::text, 7, '0'),
       NULL, (f.debut - 40)::timestamptz
FROM demo_effectif f,
     (VALUES ('PRESIDENT', 1, 1), ('CHEF_CENTRE', 2, 1), ('SECRETAIRE', 3, 2), ('SECURITE', 4, 2), ('SECURITE', 5, 2), ('SANTE', 6, 2)) p(role, rang, mini)
WHERE (f.stade >= 3 OR f.av >= 2 OR p.mini = 1);

INSERT INTO personnel_centre (centre_id, role, nom, telephone, salle, created_at)
SELECT f.centre_id, p.role, pg_temp.nom_complet((f.centre_id * 19 + s.salle * 3 + p.rang)::int),
       '+224 65' || lpad(((f.centre_id * 7027 + s.salle * 131 + p.rang * 7937) % 10000000)::text, 7, '0'), s.salle, (f.debut - 30)::timestamptz
FROM demo_effectif f
CROSS JOIN LATERAL generate_series(1, f.salles) s(salle),
     (VALUES ('CHEF_SALLE', 1), ('SURVEILLANT', 2), ('SURVEILLANT', 3)) p(role, rang)
WHERE (f.stade >= 3 OR f.av >= 2);

-- ------------------------------------------------------------------ Logistique : livraisons par centre (sujets, copies, enveloppes, fournitures)
-- avancement 2 : prévues ; avancement 3 : expédiées ; déroulement et après : reçues (une livraison incomplète par examen)
INSERT INTO livraison_centre (examen_id, centre_id, article, quantite_prevue, quantite_livree, statut, expediee_le, recue_le, observation)
SELECT f.examen_id, f.centre_id, a.article,
       CASE a.article WHEN 'SUJETS' THEN f.candidats + 10 WHEN 'COPIES' THEN f.candidats * 3
                      WHEN 'ENVELOPPES' THEN (SELECT count(*) * 2 FROM epreuve_examen p WHERE p.examen_id = f.examen_id)::int
                      ELSE f.salles * 5 END,
       CASE WHEN f.stade >= 3 THEN
              CASE WHEN a.article = 'SUJETS' AND f.centre_id = (SELECT min(x.centre_id) FROM demo_effectif x WHERE x.examen_id = f.examen_id)
                   THEN f.candidats + 2
                   ELSE CASE a.article WHEN 'SUJETS' THEN f.candidats + 10 WHEN 'COPIES' THEN f.candidats * 3
                        WHEN 'ENVELOPPES' THEN (SELECT count(*) * 2 FROM epreuve_examen p WHERE p.examen_id = f.examen_id)::int
                        ELSE f.salles * 5 END END END,
       CASE WHEN f.stade >= 3 THEN 'RECUE' WHEN f.av >= 3 THEN 'EXPEDIEE' ELSE 'PREVUE' END,
       CASE WHEN f.stade >= 3 OR f.av >= 3 THEN (f.debut - 5)::timestamptz END,
       CASE WHEN f.stade >= 3 THEN (f.debut - 3)::timestamptz END,
       CASE WHEN f.stade >= 3 AND a.article = 'SUJETS' AND f.centre_id = (SELECT min(x.centre_id) FROM demo_effectif x WHERE x.examen_id = f.examen_id)
            THEN 'Livraison incomplète : 8 exemplaires manquants, complément demandé' END
FROM demo_effectif f, (VALUES ('SUJETS'), ('COPIES'), ('ENVELOPPES'), ('FOURNITURES')) a(article)
WHERE f.stade >= 3 OR f.av >= 2;

-- ------------------------------------------------------------------ Convois (transport des sujets et du matériel)
-- Cas particuliers : retard du convoi de Kankan, accident au BEPC général 2026, perte au CEE général 2026 (Kindia)
INSERT INTO convoi_examen (examen_id, centre_id, vehicule, chauffeur, itineraire, escorte, depart_prevu, arrivee_prevue,
                           depart_reel, arrivee_reelle, carburant_litres, statut, incident_type, incident_description, observation)
SELECT f.examen_id, f.centre_id,
       CASE WHEN f.commune = 'Kankan' THEN 'Camion 4x4 – ' ELSE 'Fourgon – ' END || 'GN-' || (1000 + f.centre_id % 9000),
       pg_temp.nom_complet((f.centre_id * 23)::int),
       'DGECS Conakry → ' || f.commune || ' (centre d''examen)',
       true,
       (f.debut - 3 + time '06:00')::timestamptz,
       (f.debut - 3 + time '06:00' + CASE WHEN f.commune = 'Kankan' THEN interval '14 hours' ELSE interval '5 hours' END)::timestamptz,
       CASE WHEN f.stade >= 3 OR f.av >= 3 THEN (f.debut - 3 + time '06:30')::timestamptz END,
       CASE WHEN f.stade >= 3 THEN (f.debut - 3 + time '06:30' + CASE WHEN f.commune = 'Kankan' THEN interval '19 hours' ELSE interval '5 hours' END)::timestamptz END,
       CASE WHEN f.stade >= 3 OR f.av >= 3 THEN 60 + (f.centre_id * 13) % 140 END,
       CASE WHEN f.stade >= 3 THEN 'LIVRE' WHEN f.av >= 3 AND f.commune <> 'Dixinn' THEN 'EN_ROUTE' ELSE 'PREVU' END,
       CASE WHEN f.stade >= 3 AND f.commune = 'Kankan' THEN 'RETARD'
            WHEN f.cle = 'BEPC-G-2026' AND f.commune = 'Kindia' THEN 'ACCIDENT'
            WHEN f.cle = 'CEE-G-2026' AND f.commune = 'Kindia' THEN 'PERTE' END,
       CASE WHEN f.stade >= 3 AND f.commune = 'Kankan' THEN 'Route coupée par un éboulement : arrivée avec 5 heures de retard'
            WHEN f.cle = 'BEPC-G-2026' AND f.commune = 'Kindia' THEN 'Pneu éclaté sans blessé ; remplacement du véhicule, 3 heures perdues'
            WHEN f.cle = 'CEE-G-2026' AND f.commune = 'Kindia' THEN 'Un carton de fournitures perdu en route, remplacé le lendemain' END,
       CASE WHEN f.commune = 'Kankan' THEN 'Départ anticipé de 3 jours en raison de la distance' END
FROM demo_effectif f
WHERE f.stade >= 3 OR f.av >= 2;

-- ------------------------------------------------------------------ Plis des sujets pour les examens en préparation avancée (avancement 3, avant le déroulement)
-- (les plis des examens déjà déroulés sont créés par le script 05)
INSERT INTO epreuve_centre (centre_id, epreuve_id, nb_exemplaires, statut_pli, conditionne_le, expedie_le)
SELECT f.centre_id, p.id,
       (SELECT count(*) FROM candidat_examen c WHERE c.centre_id = f.centre_id AND (p.serie IS NULL OR c.serie = p.serie)) + 5,
       CASE WHEN p.date_epreuve - f.debut <= 1 THEN 'EXPEDIE_IRE' ELSE 'CONDITIONNE' END,
       (f.debut - 6)::timestamptz,
       CASE WHEN p.date_epreuve - f.debut <= 1 THEN (f.debut - 4)::timestamptz END
FROM demo_effectif f JOIN epreuve_examen p ON p.examen_id = f.examen_id
WHERE f.stade = 2 AND f.av >= 3;

COMMIT;
