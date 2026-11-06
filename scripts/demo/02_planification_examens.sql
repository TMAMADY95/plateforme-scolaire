-- =====================================================================================
-- DÉMONSTRATION 2/6 — Planification des examens : 18 examens (CEE, BEPC, BAC, examen communal ; général, franco-arabe,
-- technique), calendriers par série, sujets, besoins et personnel national, budget.
-- Année 2025-2026 : 9 examens passés ; année 2026-2027 : 9 examens à venir. À exécuter APRÈS le script 01.
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

-- ------------------------------------------------------------------ Examens (calendrier prévisionnel inclus)
INSERT INTO examen_national (libelle, annee_scolaire_id, cycle_id, niveau_id, niveau_organisation, date_debut, date_fin,
                             correction_debut_prevue, correction_fin_prevue, deliberation_prevue, proclamation_prevue, bareme)
SELECT d.libelle, a.id, c.id, n.id, d.orga, d.debut, d.debut + d.nbj,
       d.debut + d.nbj + 6, d.debut + d.nbj + 10, d.debut + d.nbj + 15, d.debut + d.nbj + 17,
       CASE WHEN d.cycle = 'PRIMAIRE' THEN 10 ELSE 20 END
FROM demo_examen d
JOIN annee_scolaire a ON a.libelle = d.annee
JOIN cycle_scolaire c ON c.code = d.cycle
JOIN niveau n ON n.code = d.niveau;

INSERT INTO examen_type_etablissement (examen_id, type_etablissement_id)
SELECT e.id, t.id FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN type_etablissement t ON t.code = ANY (d.types);

-- ------------------------------------------------------------------ Épreuves : calendrier par série (aucun chevauchement pour une même série)
-- gabarit, famille (ALL = tous ; FA = franco-arabe ; T = technique), matière, jour (0 = premier jour), début, fin, coefficient, série
CREATE TEMP TABLE demo_tpl (gab text, fam text, matiere text, jour int, debut time, fin time, coef int, serie text);
INSERT INTO demo_tpl VALUES
    ('CEE', 'ALL', 'FRANCAIS',     0, '08:00', '10:30', 3, NULL), ('CEE', 'ALL', 'MATHS',        0, '11:00', '13:00', 3, NULL),
    ('CEE', 'ALL', 'SCIENCES_OBS', 1, '08:00', '09:30', 2, NULL), ('CEE', 'ALL', 'HIST_GEO',     1, '10:00', '11:30', 2, NULL),
    ('CEE', 'ALL', 'ECM',          1, '12:00', '13:00', 1, NULL),
    ('CEE', 'FA',  'ARABE',        2, '08:00', '10:00', 3, NULL), ('CEE', 'FA',  'EDU_ISLAMIQUE', 2, '10:30', '11:30', 2, NULL),
    ('COM', 'ALL', 'FRANCAIS',     0, '08:00', '10:00', 3, NULL), ('COM', 'ALL', 'MATHS',        0, '10:30', '12:30', 3, NULL),
    ('COM', 'ALL', 'SVT',          1, '08:00', '10:00', 2, NULL), ('COM', 'ALL', 'HIST_GEO',     1, '10:30', '12:30', 2, NULL),
    ('COM', 'ALL', 'ANGLAIS',      1, '13:00', '14:30', 1, NULL),
    ('BEPC', 'ALL', 'FRANCAIS',    0, '08:00', '11:00', 4, NULL), ('BEPC', 'ALL', 'MATHS',       0, '11:30', '14:30', 4, NULL),
    ('BEPC', 'ALL', 'PHYS_CHIMIE', 1, '08:00', '10:00', 3, NULL), ('BEPC', 'ALL', 'SVT',         1, '10:30', '12:30', 2, NULL),
    ('BEPC', 'ALL', 'HIST_GEO',    2, '08:00', '10:00', 2, NULL), ('BEPC', 'ALL', 'ANGLAIS',     2, '10:30', '12:30', 2, NULL),
    ('BEPC', 'FA',  'ARABE',       3, '08:00', '11:00', 3, NULL), ('BEPC', 'FA',  'EDU_ISLAMIQUE', 3, '11:30', '13:30', 2, NULL),
    ('BEPC', 'T',   'TECHNOLOGIE', 3, '08:00', '11:00', 3, NULL),
    ('BACT', 'ALL', 'MATHS',        0, '08:00', '12:00', 4, NULL), ('BACT', 'ALL', 'PHYS_CHIMIE', 1, '08:00', '11:00', 3, NULL),
    ('BACT', 'ALL', 'TECHNOLOGIE',  1, '11:30', '15:30', 5, NULL), ('BACT', 'ALL', 'FRANCAIS',    2, '08:00', '11:00', 3, NULL),
    ('BACT', 'ALL', 'ANGLAIS',      2, '11:30', '13:30', 2, NULL), ('BACT', 'ALL', 'PRATIQUE_PRO', 3, '08:00', '14:00', 5, NULL),
    -- Baccalauréat (séries SM, SE, SS) : épreuves de série puis épreuves communes
    ('BAC', 'ALL', 'MATHS',       0, '08:00', '12:00', 5, 'SCIENCES_MATHEMATIQUES'), ('BAC', 'ALL', 'SVT',         0, '08:00', '12:00', 5, 'SCIENCES_EXPERIMENTALES'),
    ('BAC', 'ALL', 'HIST_GEO',    0, '08:00', '12:00', 5, 'SCIENCES_SOCIALES'),
    ('BAC', 'ALL', 'PHYS_CHIMIE', 1, '08:00', '12:00', 4, 'SCIENCES_MATHEMATIQUES'), ('BAC', 'ALL', 'PHYS_CHIMIE', 1, '08:00', '12:00', 4, 'SCIENCES_EXPERIMENTALES'),
    ('BAC', 'ALL', 'PHILO',       1, '08:00', '12:00', 4, 'SCIENCES_SOCIALES'),
    ('BAC', 'ALL', 'FRANCAIS',    2, '08:00', '11:00', 3, NULL),
    ('BAC', 'ALL', 'PHILO',       3, '08:00', '11:00', 2, 'SCIENCES_MATHEMATIQUES'), ('BAC', 'ALL', 'PHILO',       3, '08:00', '11:00', 2, 'SCIENCES_EXPERIMENTALES'),
    ('BAC', 'ALL', 'MATHS',       3, '08:00', '11:00', 2, 'SCIENCES_SOCIALES'),
    ('BAC', 'ALL', 'ANGLAIS',     3, '11:30', '13:30', 2, NULL),
    ('BAC', 'ALL', 'SVT',         4, '08:00', '11:00', 2, 'SCIENCES_MATHEMATIQUES'), ('BAC', 'ALL', 'MATHS',       4, '08:00', '11:00', 3, 'SCIENCES_EXPERIMENTALES'),
    ('BAC', 'ALL', 'ECM',         4, '08:00', '10:00', 2, 'SCIENCES_SOCIALES'),
    ('BAC', 'FA',  'ARABE',       5, '08:00', '11:00', 3, NULL), ('BAC', 'FA',  'EDU_ISLAMIQUE', 5, '11:30', '13:00', 2, NULL);

INSERT INTO epreuve_examen (examen_id, matiere_id, date_epreuve, heure_debut, heure_fin, coefficient, serie, created_at)
SELECT e.id, m.id, d.debut + t.jour, t.debut, t.fin, t.coef, t.serie, (d.debut - 120)::timestamptz
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN demo_tpl t ON t.gab = d.gab AND (t.fam = 'ALL' OR t.fam = d.fam)
JOIN matiere m ON m.code = t.matiere;

-- ------------------------------------------------------------------ Sujets (banque de sujets : 3 variantes par épreuve)
-- stade 0 : en élaboration · 1 : A et B validées · 2 : toutes validées · 3 et + : une variante tirée au sort
-- Cas particulier : fuite du sujet de mathématiques SM au BAC général 2026 (variante A compromise, variante B tirée).
INSERT INTO sujet_examen (epreuve_id, variante, intitule, auteurs, statut, valide_le, tire_le, created_at)
SELECT p.id, v.variante,
       'Sujet de ' || m.libelle || ' – variante ' || v.variante
         || CASE WHEN p.serie IS NOT NULL THEN ' (' || replace(p.serie, '_', ' ') || ')' ELSE '' END,
       'Commission nationale de rédaction – ' || m.libelle,
       CASE
         WHEN d.cle = 'BAC-G-2026' AND m.code = 'MATHS' AND p.serie = 'SCIENCES_MATHEMATIQUES' AND v.variante = 'A' THEN 'COMPROMIS'
         WHEN d.stade = 0 THEN 'ELABORATION'
         WHEN d.stade = 1 AND v.variante = 'C' THEN 'ELABORATION'
         WHEN d.stade >= 3 AND v.variante = (CASE WHEN d.cle = 'BAC-G-2026' AND m.code = 'MATHS' AND p.serie = 'SCIENCES_MATHEMATIQUES' THEN 'B'
                                                  ELSE (ARRAY['A', 'B', 'C'])[1 + p.id % 3] END) THEN 'TIRE'
         ELSE 'VALIDE' END,
       CASE WHEN d.stade >= 1 AND NOT (d.stade = 1 AND v.variante = 'C') THEN (d.debut - 60)::timestamptz END,
       CASE WHEN d.stade >= 3 AND v.variante = (CASE WHEN d.cle = 'BAC-G-2026' AND m.code = 'MATHS' AND p.serie = 'SCIENCES_MATHEMATIQUES' THEN 'B'
                                                     ELSE (ARRAY['A', 'B', 'C'])[1 + p.id % 3] END)
            THEN (p.date_epreuve + p.heure_debut - interval '30 minutes') END,
       (d.debut - 100)::timestamptz
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN epreuve_examen p ON p.examen_id = e.id
JOIN matiere m ON m.id = p.matiere_id,
     (VALUES ('A'), ('B'), ('C')) v(variante)
WHERE NOT (d.stade = 0 AND v.variante = 'C');

-- ------------------------------------------------------------------ Besoins en personnel (examen et épreuves) et personnel national désigné
INSERT INTO besoin_personnel_examen (examen_id, fonction, requis)
SELECT e.id, f.fonction,
       CASE f.fonction WHEN 'JURY' THEN 9 WHEN 'SUPERVISEUR' THEN 8 WHEN 'INSPECTEUR' THEN 6 WHEN 'INFORMATIQUE' THEN 4
                       WHEN 'CORRECTEUR' THEN CASE d.gab WHEN 'BAC' THEN 120 WHEN 'BACT' THEN 40 WHEN 'BEPC' THEN 90 WHEN 'COM' THEN 60 ELSE 80 END
                       WHEN 'SECURITE' THEN 40 ELSE 12 END
FROM demo_examen d JOIN examen_national e ON e.libelle = d.libelle,
     (VALUES ('JURY'), ('SUPERVISEUR'), ('INSPECTEUR'), ('INFORMATIQUE'), ('CORRECTEUR'), ('SECURITE'), ('SANTE')) f(fonction);

INSERT INTO besoin_personnel_epreuve (epreuve_id, fonction, requis)
SELECT p.id, f.fonction, f.requis
FROM demo_examen d JOIN examen_national e ON e.libelle = d.libelle JOIN epreuve_examen p ON p.examen_id = e.id,
     (VALUES ('CORRECTEUR', 12), ('SECURITE', 6), ('SANTE', 2)) f(fonction, requis);

-- Personnel national : au complet pour les examens passés, partiel (les deux tiers) pour les examens à venir
INSERT INTO personnel_examen (examen_id, fonction, nom, telephone, created_at)
SELECT e.id, b.fonction, pg_temp.nom_complet((e.id * 31 + b.rang)::int), '+224 62' || lpad(((e.id * 7919 + b.rang * 104729) % 10000000)::text, 7, '0'),
       (d.debut - 90)::timestamptz
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN LATERAL (SELECT f.fonction, g.rang
              FROM (VALUES ('JURY', 9), ('SUPERVISEUR', 8), ('INSPECTEUR', 6), ('INFORMATIQUE', 4)) f(fonction, requis),
                   generate_series(1, f.requis) g(rang)
              WHERE d.annee = '2025-2026' OR g.rang <= (f.requis * 2 + 2) / 3) b ON true;

-- ------------------------------------------------------------------ Budget (prévu, engagé, payé selon l'avancement)
INSERT INTO budget_examen (examen_id, rubrique, montant_prevu, montant_engage, montant_paye, observation)
SELECT e.id, r.rubrique,
       (r.base * CASE d.gab WHEN 'BAC' THEN 10 WHEN 'BACT' THEN 3 WHEN 'BEPC' THEN 6 WHEN 'COM' THEN 2 ELSE 4 END)::bigint,
       (r.base * CASE d.gab WHEN 'BAC' THEN 10 WHEN 'BACT' THEN 3 WHEN 'BEPC' THEN 6 WHEN 'COM' THEN 2 ELSE 4 END
          * CASE WHEN d.stade = 0 THEN 0 WHEN d.stade = 1 THEN 0.2 WHEN d.stade = 2 THEN 0.6 ELSE 1 END)::bigint,
       (r.base * CASE d.gab WHEN 'BAC' THEN 10 WHEN 'BACT' THEN 3 WHEN 'BEPC' THEN 6 WHEN 'COM' THEN 2 ELSE 4 END
          * CASE WHEN d.stade <= 1 THEN 0 WHEN d.stade = 2 THEN 0.4 WHEN d.stade < 6 THEN 0.8 ELSE 1 END)::bigint,
       CASE WHEN r.rubrique = 'TRANSPORT' AND d.stade >= 3 THEN 'Dépassement lié au retard des convois' END
FROM demo_examen d JOIN examen_national e ON e.libelle = d.libelle,
     (VALUES ('IMPRESSION', 15000000), ('TRANSPORT', 12000000), ('SURVEILLANCE', 20000000), ('CORRECTION', 30000000),
             ('SECURITE', 8000000), ('SANTE', 4000000), ('COMMUNICATION', 3000000), ('AUTRE', 2000000)) r(rubrique, base);

COMMIT;
