-- =====================================================================================
-- DÉMONSTRATION 3/6 — Inscriptions : candidats réguliers et libres, listes par établissement (tous les états du circuit),
-- dossiers de candidature (pièces), candidats en situation de handicap. À exécuter APRÈS le script 02.
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

-- ------------------------------------------------------------------ Candidats réguliers
-- Élèves acceptés des classes du niveau de l'examen (année de l'examen), dans les établissements autorisés dont le type
-- est concerné par l'examen et qui ont le cycle concerné. Les établissements non autorisés n'ont aucun candidat.
CREATE TEMP TABLE demo_candidat AS
SELECT e.id AS examen_id, d.cle, d.stade, d.orga, i.eleve_id, i.etablissement_id, i.classe_id, cl.serie, et.code_officiel
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN inscription i ON i.annee_scolaire_id = e.annee_scolaire_id AND i.statut = 'ACCEPTEE'
JOIN classe cl ON cl.id = i.classe_id AND cl.niveau_id = e.niveau_id
JOIN etablissement et ON et.id = i.etablissement_id AND et.autorisation_statut = 'AUTORISE'
JOIN type_etablissement t ON t.id = et.type_etablissement_id AND t.code = ANY (d.types)
WHERE d.stade >= 1
  AND EXISTS (SELECT 1 FROM etablissement_cycle ec WHERE ec.etablissement_id = et.id AND ec.cycle_id = e.cycle_id);

INSERT INTO candidat_examen (examen_id, eleve_id, etablissement_id, classe_id, created_at, statut_epreuves, libre, serie)
SELECT c.examen_id, c.eleve_id, c.etablissement_id, c.classe_id, (d.debut - 120)::timestamptz, 'INSCRIT', false, c.serie
FROM demo_candidat c JOIN demo_examen d ON d.cle = c.cle;

-- ------------------------------------------------------------------ Candidats libres (BEPC et baccalauréat) : 3 dans chacun des 2 premiers établissements concernés
CREATE TEMP TABLE demo_libre AS
SELECT d.cle, e.id AS examen_id, s.etablissement_id, s.rang, g.n,
       row_number() OVER (PARTITION BY d.cle ORDER BY s.rang, g.n) AS numero
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN LATERAL (SELECT x.etablissement_id, row_number() OVER (ORDER BY x.code_officiel) AS rang
              FROM (SELECT DISTINCT c.etablissement_id, c.code_officiel FROM demo_candidat c WHERE c.cle = d.cle) x
              ORDER BY x.code_officiel LIMIT 2) s ON true
CROSS JOIN generate_series(1, 3) g(n)
WHERE d.stade >= 1 AND d.orga IN ('NATIONAL', 'REGION');

INSERT INTO eleve (matricule, nom, prenom, date_naissance, sexe, lieu_naissance, nom_pere, nom_mere)
SELECT 'ELV-L-' || l.cle || '-' || l.numero,
       split_part(pg_temp.nom_complet((l.examen_id * 53 + l.numero::int * 17)::int), ' ', 2),
       split_part(pg_temp.nom_complet((l.examen_id * 53 + l.numero::int * 17)::int), ' ', 1),
       make_date(substr(d.annee, 1, 4)::int - CASE WHEN d.cycle = 'LYCEE' THEN 20 ELSE 17 END, 1 + (l.numero::int * 5) % 12, 1 + (l.numero::int * 7) % 27),
       CASE WHEN l.numero % 2 = 0 THEN 'F' ELSE 'M' END, 'Conakry',
       pg_temp.nom_complet((l.examen_id * 61 + l.numero::int)::int), pg_temp.nom_complet((l.examen_id * 67 + l.numero::int)::int)
FROM demo_libre l JOIN demo_examen d ON d.cle = l.cle;

INSERT INTO candidat_examen (examen_id, eleve_id, etablissement_id, created_at, statut_epreuves, libre, serie)
SELECT l.examen_id, el.id, l.etablissement_id, (d.debut - 100)::timestamptz, 'INSCRIT', true,
       CASE WHEN d.gab = 'BAC' THEN (ARRAY['SCIENCES_MATHEMATIQUES', 'SCIENCES_EXPERIMENTALES', 'SCIENCES_SOCIALES'])[1 + l.numero::int % 3] END
FROM demo_libre l JOIN demo_examen d ON d.cle = l.cle JOIN eleve el ON el.matricule = 'ELV-L-' || l.cle || '-' || l.numero;

UPDATE candidat_examen SET numero = format('EX%s-%s', examen_id, lpad(id::text, 6, '0')) WHERE numero IS NULL;

-- ------------------------------------------------------------------ Listes de candidats par établissement
-- stade 1 : tous les états du circuit (brouillon, transmise, validée par la région, rejetée puis renvoyée en brouillon, validée) ;
-- stade 2 et plus : toutes validées.
INSERT INTO liste_candidats (examen_id, etablissement_id, statut, transmise_le, validee_region_le, validee_le, motif_rejet)
SELECT x.examen_id, x.etablissement_id,
       CASE WHEN x.stade >= 2 THEN 'VALIDEE'
            ELSE (ARRAY['BROUILLON', 'TRANSMISE', 'VALIDEE_REGION', 'VALIDEE', 'BROUILLON'])[1 + x.rn % 5] END,
       CASE WHEN x.stade >= 2 OR x.rn % 5 IN (1, 2, 3) THEN (x.debut - 70)::timestamptz END,
       CASE WHEN x.stade >= 2 OR x.rn % 5 IN (2, 3) THEN (x.debut - 62)::timestamptz END,
       CASE WHEN x.stade >= 2 OR x.rn % 5 = 3 THEN (x.debut - 55)::timestamptz END,
       CASE WHEN x.stade = 1 AND x.rn % 5 = 4 THEN 'Liste renvoyée : photos d''identité manquantes pour plusieurs candidats' END
FROM (SELECT c.examen_id, c.etablissement_id, d.stade, d.debut,
             row_number() OVER (PARTITION BY c.examen_id ORDER BY min(c.code_officiel)) - 1 AS rn
      FROM demo_candidat c JOIN demo_examen d ON d.cle = c.cle
      GROUP BY c.examen_id, c.etablissement_id, d.stade, d.debut) x;

-- ------------------------------------------------------------------ Dossiers de candidature : pièces fournies
-- Pièces requises : demande, acte de naissance, attestation d'inscription (sauf candidat libre), diplôme précédent (BEPC et BAC).
-- Stade 1 : environ 12 % de dossiers incomplets ; ensuite tous complets.
INSERT INTO piece_candidature (candidat_id, type, reference, fournie_le)
SELECT c.id, p.type, 'REF-' || c.id || '-' || p.abr, (d.debut - 100)::date
FROM candidat_examen c
JOIN examen_national e ON e.id = c.examen_id
JOIN demo_examen d ON d.libelle = e.libelle,
     (VALUES ('DEMANDE', 'DEM'), ('ACTE_NAISSANCE', 'ACT'), ('ATTESTATION', 'ATT'), ('DIPLOME', 'DIP')) p(type, abr)
WHERE NOT (p.type = 'ATTESTATION' AND c.libre)
  AND NOT (p.type = 'DIPLOME' AND d.orga NOT IN ('NATIONAL', 'REGION'))
  AND NOT (d.stade = 1 AND abs(hashtext('piece' || c.id::text)) % 100 < 12
           AND p.type = (CASE WHEN c.libre OR d.orga IN ('NATIONAL', 'REGION') THEN 'DIPLOME' ELSE 'ATTESTATION' END));

-- ------------------------------------------------------------------ Candidats en situation de handicap : environ 2,5 % (aménagements accordés)
UPDATE candidat_examen
SET handicap = (ARRAY['MOTEUR', 'VISUEL', 'AUDITIF', 'INTELLECTUEL', 'AUTRE'])[1 + abs(hashtext('hand' || id::text)) % 5],
    amenagement = (ARRAY['Salle au rez-de-chaussée et table adaptée', 'Sujet en gros caractères ou en braille, assistant lecteur',
                         'Interprète en langue des signes, consignes écrites', 'Tiers-temps supplémentaire et salle calme',
                         'Aménagement individuel décidé par la commission'])[1 + abs(hashtext('hand' || id::text)) % 5]
WHERE abs(hashtext('hand' || id::text)) % 1000 < 25;

COMMIT;
