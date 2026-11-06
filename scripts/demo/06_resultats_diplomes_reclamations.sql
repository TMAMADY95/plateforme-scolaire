-- =====================================================================================
-- DÉMONSTRATION 6/6 — Résultats : réclamations et rectifications de notes, résultats consolidés par établissement et par série,
-- jury de délibération, états des examens (délibéré, proclamé), diplômes (dont révoqués), vérifications publiques.
-- À exécuter APRÈS le script 05. Les examens passés sont à différents stades : voir le LISEZMOI.
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

-- ------------------------------------------------------------------ Réclamations et rectifications de notes (examens proclamés)
-- Pour chaque examen proclamé : les 4 candidats non admis les plus proches du seuil déposent une réclamation sur une note :
--   rangs 1 et 2 : acceptée, note rectifiée, le candidat devient admis ;
--   rang 3 : acceptée puis rectification annulée (la note d'origine est rétablie) ;
--   rang 4 : rejetée après recorrection.
-- Puis des réclamations d'autres objets (identité, omission, autre) ouvertes (donc en retard) ou traitées.
CREATE TEMP TABLE demo_rect AS
SELECT b.*, x.epreuve_id, x.note AS ancienne_note, x.coefficient, x.somme_coef,
       least(b.bareme, x.note + ceil(((b.seuil - b.moyenne) * x.somme_coef / x.coefficient + 0.10) * 100) / 100.0) AS nouvelle_note,
       ceil(((b.seuil - b.moyenne) * x.somme_coef / x.coefficient + 0.10) * 100) / 100.0 AS ajout
FROM (SELECT c.id AS candidat_id, c.examen_id, c.moyenne, e.bareme, e.bareme * 0.5 AS seuil, d.debut + d.nbj + 17 AS proclamation,
             row_number() OVER (PARTITION BY c.examen_id ORDER BY e.bareme * 0.5 - c.moyenne, c.id)::int AS rang
      FROM candidat_examen c
      JOIN examen_national e ON e.id = c.examen_id
      JOIN demo_examen d ON d.libelle = e.libelle
      WHERE d.stade = 6 AND c.statut_epreuves = 'PRESENT' AND c.decision = 'NON_ADMIS'
        AND c.moyenne >= e.bareme * 0.45 AND c.moyenne < e.bareme * 0.5) b
JOIN LATERAL (SELECT p.id AS epreuve_id, n.note, p.coefficient,
                     (SELECT sum(p3.coefficient) FROM note_examen n3 JOIN epreuve_examen p3 ON p3.id = n3.epreuve_id WHERE n3.candidat_id = b.candidat_id) AS somme_coef
              FROM note_examen n JOIN epreuve_examen p ON p.id = n.epreuve_id
              WHERE n.candidat_id = b.candidat_id ORDER BY p.coefficient DESC, p.id LIMIT 1) x ON true
WHERE b.rang <= 4;

INSERT INTO reclamation_examen (examen_id, candidat_id, objet, description, statut, deposee_le, echeance_le, traitee_le, decision)
SELECT r.examen_id, r.candidat_id, 'NOTE',
       'Le candidat conteste sa note de ' || (SELECT m.libelle FROM epreuve_examen p JOIN matiere m ON m.id = p.matiere_id WHERE p.id = r.epreuve_id)
         || ' et demande une vérification de la copie',
       CASE WHEN r.rang <= 3 THEN 'ACCEPTEE' ELSE 'REJETEE' END,
       (r.proclamation + r.rang)::timestamptz, (r.proclamation + r.rang + 15)::timestamptz, (r.proclamation + r.rang + 6)::timestamptz,
       CASE WHEN r.rang <= 3 THEN 'Erreur de report de note constatée à la recorrection ; note rectifiée'
            ELSE 'Après recorrection par un second correcteur, la note initiale est confirmée' END
FROM demo_rect r;

-- Rectifications (rang 3 : annulée, la note d'origine est conservée)
UPDATE note_examen n SET note = r.nouvelle_note
FROM demo_rect r WHERE r.rang <= 2 AND n.candidat_id = r.candidat_id AND n.epreuve_id = r.epreuve_id;
UPDATE candidat_examen c
SET moyenne = round((r.moyenne * r.somme_coef + (r.nouvelle_note - r.ancienne_note) * r.coefficient) / r.somme_coef, 2), decision = 'ADMIS'
FROM demo_rect r WHERE r.rang <= 2 AND c.id = r.candidat_id;

INSERT INTO rectification_note (reclamation_id, candidat_id, epreuve_id, ancienne_note, nouvelle_note, ancienne_moyenne, nouvelle_moyenne,
                                ancienne_decision, nouvelle_decision, rectifiee_le, rectifiee_par, annulee_le, annulee_par, motif_annulation)
SELECT rc.id, r.candidat_id, r.epreuve_id, r.ancienne_note, r.nouvelle_note, r.moyenne,
       round((r.moyenne * r.somme_coef + (r.nouvelle_note - r.ancienne_note) * r.coefficient) / r.somme_coef, 2),
       'NON_ADMIS', 'ADMIS', (r.proclamation + r.rang + 7)::timestamptz, 'dgecs@example.org',
       CASE WHEN r.rang = 3 THEN (r.proclamation + r.rang + 9)::timestamptz END,
       CASE WHEN r.rang = 3 THEN 'dgecs@example.org' END,
       CASE WHEN r.rang = 3 THEN 'Rectification saisie sur la mauvaise épreuve : annulée, note d''origine rétablie' END
FROM demo_rect r
JOIN reclamation_examen rc ON rc.examen_id = r.examen_id AND rc.candidat_id = r.candidat_id AND rc.objet = 'NOTE'
WHERE r.rang <= 3 AND r.nouvelle_note > r.ancienne_note;

-- Autres réclamations : identité, omission, autre, note (ouvertes = en retard puisque le délai de 15 jours est dépassé)
INSERT INTO reclamation_examen (examen_id, candidat_id, objet, description, statut, deposee_le, echeance_le, traitee_le, decision)
SELECT x.examen_id, x.candidat_id, o.objet, o.description, o.statut,
       (x.proclamation + o.jour)::timestamptz, (x.proclamation + o.jour + 15)::timestamptz,
       CASE WHEN o.statut <> 'OUVERTE' THEN (x.proclamation + o.jour + 5)::timestamptz END,
       CASE WHEN o.statut = 'ACCEPTEE' THEN o.decision WHEN o.statut = 'REJETEE' THEN o.decision END
FROM (SELECT c.id AS candidat_id, c.examen_id, d.debut + d.nbj + 17 AS proclamation,
             row_number() OVER (PARTITION BY c.examen_id ORDER BY abs(hashtext('recl' || c.id::text)), c.id) AS rn
      FROM candidat_examen c
      JOIN examen_national e ON e.id = c.examen_id
      JOIN demo_examen d ON d.libelle = e.libelle
      WHERE d.stade = 6 AND c.statut_epreuves = 'PRESENT'
        AND NOT EXISTS (SELECT 1 FROM demo_rect r WHERE r.candidat_id = c.id)) x
JOIN (VALUES (1, 'IDENTITE', 'Le prénom du candidat est mal orthographié sur la liste des résultats', 'OUVERTE', 2, NULL),
             (2, 'IDENTITE', 'Date de naissance erronée sur la liste des résultats', 'ACCEPTEE', 3, 'Identité corrigée dans le registre sur présentation de l''acte de naissance'),
             (3, 'OMISSION', 'Le candidat ne figure pas dans la liste des résultats de son établissement', 'OUVERTE', 4, NULL),
             (4, 'AUTRE', 'Demande de dérogation pour une seconde session', 'REJETEE', 5, 'Hors délai : la seconde session n''est pas prévue pour cet examen'),
             (5, 'NOTE', 'Demande de vérification de la note de français', 'OUVERTE', 6, NULL)) o(rn, objet, description, statut, jour, decision) ON o.rn = x.rn;

-- ------------------------------------------------------------------ Résultats consolidés par établissement (stade 5 et plus)
INSERT INTO resultat_examen (examen_id, etablissement_id, presentes, admis, absents, elimines, presentes_filles, admis_filles)
SELECT c.examen_id, c.etablissement_id,
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND c.decision = 'ADMIS'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ABSENT'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ELIMINE'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND el.sexe = 'F'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND c.decision = 'ADMIS' AND el.sexe = 'F')
FROM candidat_examen c
JOIN eleve el ON el.id = c.eleve_id
JOIN examen_national e ON e.id = c.examen_id
JOIN demo_examen d ON d.libelle = e.libelle
WHERE d.stade >= 5
GROUP BY c.examen_id, c.etablissement_id;

-- Répartition par série (baccalauréat général et franco-arabe)
INSERT INTO resultat_examen_serie (resultat_id, serie, presentes, admis)
SELECT r.id, c.serie,
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND c.decision = 'ADMIS')
FROM candidat_examen c
JOIN resultat_examen r ON r.examen_id = c.examen_id AND r.etablissement_id = c.etablissement_id
WHERE c.serie IS NOT NULL
GROUP BY r.id, c.serie
HAVING count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT') > 0;

-- ------------------------------------------------------------------ Jury de délibération (stade 4 et plus) : composition prévue selon le niveau d'organisation
INSERT INTO jury_membre (examen_id, ordre, fonction, nom, present)
SELECT e.id, f.ordre, f.fonction,
       CASE WHEN d.stade >= 5 THEN pg_temp.nom_complet((e.id * 41 + f.ordre)::int) END,
       d.stade >= 5 AND f.ordre <> 9
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN LATERAL (VALUES
    (1, CASE d.orga WHEN 'NATIONAL' THEN 'Directeur national de l''enseignement secondaire général et technique (président)'
                    WHEN 'REGION' THEN 'Inspecteur régional de l''éducation (président)'
                    ELSE 'Directeur préfectoral ou communal de l''éducation (président)' END),
    (2, CASE d.orga WHEN 'NATIONAL' THEN 'Représentant du ministère de l''Enseignement supérieur (premier vice-président)' ELSE 'Chef de la section pédagogique' END),
    (3, CASE d.orga WHEN 'NATIONAL' THEN 'Représentant du ministère de l''Enseignement technique (deuxième vice-président)' ELSE 'Chargé des examens' END),
    (4, CASE d.orga WHEN 'NATIONAL' THEN 'Directeur général des examens et du contrôle scolaire (rapporteur)' WHEN 'REGION' THEN 'Principal de collège public' ELSE 'Délégué scolaire de l''enseignement élémentaire' END),
    (5, CASE d.orga WHEN 'NATIONAL' THEN 'Proviseur de lycée public' WHEN 'REGION' THEN 'Principal de collège privé' ELSE 'Directeur d''école publique' END),
    (6, CASE d.orga WHEN 'NATIONAL' THEN 'Proviseur de lycée privé' WHEN 'REGION' THEN 'Enseignant' ELSE 'Directeur d''école privée' END),
    (7, CASE d.orga WHEN 'NATIONAL' THEN 'Professeur de lycée' WHEN 'REGION' THEN 'Membre de l''APEAE régionale' ELSE 'Enseignant' END),
    (8, CASE d.orga WHEN 'NATIONAL' THEN 'Membre de la FEGUIPAE' ELSE NULL END),
    (9, CASE d.orga WHEN 'NATIONAL' THEN 'Fondateur d''établissement privé' ELSE NULL END)) f(ordre, fonction) ON f.fonction IS NOT NULL
WHERE d.stade >= 4;
-- (le dernier membre du jury national, absent, montre que le quorum des deux tiers suffit)

-- ------------------------------------------------------------------ États des examens : délibéré (stade 5), proclamé (stade 6)
UPDATE examen_national e
SET statut_resultats = CASE WHEN d.stade = 6 THEN 'PROCLAME' ELSE 'DELIBERE' END,
    date_deliberation = d.debut + d.nbj + 15,
    date_proclamation = CASE WHEN d.stade = 6 THEN d.debut + d.nbj + 17 END
FROM demo_examen d WHERE d.libelle = e.libelle AND d.stade >= 5;

-- ------------------------------------------------------------------ Diplômes (examens proclamés) : un par admis, dont 2 révoqués par examen
INSERT INTO diplome_examen (candidat_id, numero, code, mention, delivre_le, revoque, motif_revocation, revoque_le, created_at)
SELECT x.id, format('DIP-%s-%s', x.examen_id, lpad(x.id::text, 6, '0')),
       translate(upper(substr(md5('diplome-' || x.id::text), 1, 10)), '01', '23'),
       CASE WHEN x.moyenne >= x.bareme * 0.8 THEN 'TRES_BIEN' WHEN x.moyenne >= x.bareme * 0.7 THEN 'BIEN'
            WHEN x.moyenne >= x.bareme * 0.6 THEN 'ASSEZ_BIEN' ELSE 'PASSABLE' END,
       x.proclamation + 7, x.rn <= 2,
       CASE WHEN x.rn <= 2 THEN 'Fraude sur l''identité du titulaire découverte après la délivrance : diplôme révoqué' END,
       CASE WHEN x.rn <= 2 THEN (x.proclamation + 40)::timestamptz END,
       (x.proclamation + 7)::timestamptz
FROM (SELECT c.id, c.examen_id, c.moyenne, e.bareme, d.debut + d.nbj + 17 AS proclamation,
             row_number() OVER (PARTITION BY c.examen_id ORDER BY abs(hashtext('revoque' || c.id::text)), c.id) AS rn
      FROM candidat_examen c
      JOIN examen_national e ON e.id = c.examen_id
      JOIN demo_examen d ON d.libelle = e.libelle
      WHERE d.stade = 6 AND c.decision = 'ADMIS') x;

-- Vérifications publiques de diplômes (authentique, révoqué, inconnu)
INSERT INTO verification_diplome (code_saisi, resultat, verifie_le)
SELECT x.code, x.resultat, (x.date_ref + x.jour)::timestamptz
FROM (SELECT d.code, CASE WHEN d.revoque THEN 'REVOQUE' ELSE 'AUTHENTIQUE' END AS resultat, d.delivre_le AS date_ref,
             row_number() OVER (ORDER BY d.revoque DESC, d.id)::int AS jour
      FROM diplome_examen d WHERE d.id IN (SELECT id FROM diplome_examen ORDER BY revoque DESC, id LIMIT 3)
         OR d.id IN (SELECT id FROM diplome_examen WHERE NOT revoque ORDER BY id LIMIT 6)) x
UNION ALL
SELECT code, 'INCONNU', timestamptz '2026-08-15 10:00+00' FROM (VALUES ('ZZ9999AAAA'), ('K3M5N7P9QR'), ('AB12CD34EF')) v(code);

COMMIT;
