-- =====================================================================================
-- DÉMONSTRATION 5/6 — Déroulement et correction : statuts des candidats (présents, absents, éliminés), plis ouverts avec témoins,
-- incidents de tous types (fraude, retard, malaise, perturbation, fuite de sujet, autre), commissions de correction, notes,
-- moyennes et décisions. À exécuter APRÈS le script 04.
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

-- Effectifs par centre (recalculés)
CREATE TEMP TABLE demo_effectif AS
SELECT ce.id AS centre_id, d.cle, d.stade, d.av, d.debut, d.orga, d.nbj, e.id AS examen_id, z.nom AS commune, count(c.id) AS candidats,
       greatest(1, (count(c.id) + 29) / 30) AS salles
FROM demo_examen d
JOIN examen_national e ON e.libelle = d.libelle
JOIN centre_examen ce ON ce.examen_id = e.id
JOIN zone_geographique z ON z.id = ce.zone_id
LEFT JOIN candidat_examen c ON c.centre_id = ce.id
GROUP BY ce.id, d.cle, d.stade, d.av, d.debut, d.orga, d.nbj, e.id, z.nom;

-- ------------------------------------------------------------------ Statuts des candidats après les épreuves (stade 3 et plus)
-- Environ 4 % d'absents et 1 % d'éliminés pour fraude (au moins un de chaque par examen)
UPDATE candidat_examen c
SET statut_epreuves = CASE WHEN r.rang <= greatest(1, r.total / 100) THEN 'ELIMINE'
                           WHEN r.rang <= greatest(1, r.total / 100) + greatest(1, r.total * 4 / 100) THEN 'ABSENT'
                           ELSE 'PRESENT' END
FROM (SELECT c2.id, count(*) OVER (PARTITION BY c2.examen_id) AS total,
             row_number() OVER (PARTITION BY c2.examen_id ORDER BY abs(hashtext('statut' || c2.id::text)), c2.id) AS rang
      FROM candidat_examen c2
      JOIN examen_national e ON e.id = c2.examen_id
      JOIN demo_examen d ON d.libelle = e.libelle
      WHERE d.stade >= 3) r
WHERE r.id = c.id;

-- ------------------------------------------------------------------ Plis des épreuves dans les centres : ouverture avec témoins, déroulement de la composition
-- Stade 3 : les épreuves du dernier jour sont encore au rapport non clos ; sinon tous les rapports sont clos.
CREATE TEMP TABLE demo_pli AS
SELECT f.centre_id, p.id AS epreuve_id, f.stade, f.debut, p.date_epreuve, p.heure_debut, p.heure_fin, p.serie, d.nbj,
       count(c.id) FILTER (WHERE p.serie IS NULL OR c.serie = p.serie) AS concernes,
       count(c.id) FILTER (WHERE (p.serie IS NULL OR c.serie = p.serie) AND c.statut_epreuves = 'PRESENT') AS presents,
       count(c.id) FILTER (WHERE (p.serie IS NULL OR c.serie = p.serie) AND c.statut_epreuves = 'ABSENT') AS absents,
       count(c.id) FILTER (WHERE (p.serie IS NULL OR c.serie = p.serie) AND c.statut_epreuves = 'ELIMINE') AS elimines
FROM demo_effectif f
JOIN demo_examen d ON d.cle = f.cle
JOIN epreuve_examen p ON p.examen_id = f.examen_id
LEFT JOIN candidat_examen c ON c.centre_id = f.centre_id
WHERE f.stade >= 3
GROUP BY f.centre_id, p.id, f.stade, f.debut, p.date_epreuve, p.heure_debut, p.heure_fin, p.serie, d.nbj;

INSERT INTO epreuve_centre (centre_id, epreuve_id, nb_exemplaires, statut_pli, conditionne_le, expedie_le, recu_dpe_le, recu_president_le,
                            ouvert_le, ouvert_par, temoin1, temoin2, composition_debut, composition_fin,
                            presents, absents, elimines, copies_ramassees, observations, rapport_clos)
SELECT q.centre_id, q.epreuve_id, q.concernes + 5,
       CASE WHEN q.dernier_jour THEN 'RECU_PRESIDENT' ELSE 'OUVERT' END,
       (q.date_epreuve - 3 + time '09:00')::timestamptz, (q.date_epreuve - 2 + time '09:00')::timestamptz,
       (q.date_epreuve - 1 + time '09:00')::timestamptz, (q.date_epreuve + q.heure_debut - interval '40 minutes')::timestamptz,
       CASE WHEN NOT q.dernier_jour THEN (q.date_epreuve + q.heure_debut - interval '20 minutes')::timestamptz END,
       CASE WHEN NOT q.dernier_jour THEN pg_temp.nom_complet((q.centre_id * 100 + q.epreuve_id)::int) END,
       CASE WHEN NOT q.dernier_jour THEN pg_temp.nom_complet((q.centre_id * 100 + q.epreuve_id + 1)::int) END,
       CASE WHEN NOT q.dernier_jour THEN pg_temp.nom_complet((q.centre_id * 100 + q.epreuve_id + 2)::int) END,
       CASE WHEN NOT q.dernier_jour THEN (q.date_epreuve + q.heure_debut)::timestamptz END,
       CASE WHEN NOT q.dernier_jour THEN (q.date_epreuve + q.heure_fin)::timestamptz END,
       CASE WHEN NOT q.dernier_jour THEN q.presents END,
       CASE WHEN NOT q.dernier_jour THEN q.absents END,
       CASE WHEN NOT q.dernier_jour THEN q.elimines END,
       CASE WHEN NOT q.dernier_jour THEN q.presents + q.elimines END,
       CASE WHEN NOT q.dernier_jour AND q.epreuve_id % 11 = 0 THEN 'Composition interrompue 10 minutes par une coupure d''électricité ; temps compensé' END,
       NOT q.dernier_jour
FROM (SELECT p.*, (p.stade = 3 AND p.date_epreuve - p.debut = p.nbj) AS dernier_jour FROM demo_pli p) q;

-- ------------------------------------------------------------------ Incidents d'examen
-- Une fraude (élimination) par candidat éliminé, puis des incidents de tous types dans le premier centre de chaque examen.
INSERT INTO incident_examen (examen_id, centre_id, candidat_id, epreuve_id, type, description, survenu_le, signale_par, statut, decision, elimine, created_at)
SELECT c.examen_id, c.centre_id, c.id,
       (SELECT p.id FROM epreuve_examen p WHERE p.examen_id = c.examen_id AND (p.serie IS NULL OR p.serie = c.serie) ORDER BY p.date_epreuve, p.heure_debut LIMIT 1),
       'FRAUDE', 'Téléphone portable et antisèche trouvés sur le candidat pendant la composition',
       (d.debut + time '09:15')::timestamptz, pg_temp.nom_complet((c.centre_id * 3)::int), 'TRAITE',
       'Procès-verbal de fraude établi ; candidat exclu de la session conformément au règlement des examens', true, (d.debut + time '09:30')::timestamptz
FROM candidat_examen c
JOIN examen_national e ON e.id = c.examen_id
JOIN demo_examen d ON d.libelle = e.libelle
WHERE c.statut_epreuves = 'ELIMINE' AND c.centre_id IS NOT NULL;

INSERT INTO incident_examen (examen_id, centre_id, candidat_id, epreuve_id, type, description, survenu_le, signale_par, statut, decision, elimine, created_at)
SELECT f.examen_id, f.centre_id,
       CASE i.type WHEN 'RETARD' THEN (SELECT c.id FROM candidat_examen c WHERE c.centre_id = f.centre_id AND c.statut_epreuves = 'PRESENT' ORDER BY c.id LIMIT 1)
                   WHEN 'MALAISE' THEN (SELECT c.id FROM candidat_examen c WHERE c.centre_id = f.centre_id AND c.statut_epreuves = 'PRESENT' ORDER BY c.id OFFSET 5 LIMIT 1) END,
       (SELECT p.id FROM epreuve_examen p WHERE p.examen_id = f.examen_id ORDER BY p.date_epreuve, p.heure_debut, p.id LIMIT 1),
       i.type, i.description, (f.debut + time '08:20')::timestamptz, pg_temp.nom_complet((f.centre_id * 5)::int),
       CASE WHEN f.stade = 3 AND i.ouvert THEN 'OUVERT' ELSE 'TRAITE' END,
       CASE WHEN f.stade = 3 AND i.ouvert THEN NULL ELSE i.decision END,
       false, (f.debut + time '10:00')::timestamptz
FROM (SELECT x.*, row_number() OVER (PARTITION BY x.examen_id ORDER BY x.centre_id) AS rn FROM demo_effectif x WHERE x.stade >= 3) f,
     (VALUES ('RETARD', 'Candidat arrivé 25 minutes après le début de l''épreuve (transport en panne)', 'Admis à composer sans temps additionnel, mention portée au PV', true),
             ('MALAISE', 'Malaise d''un candidat en salle, pris en charge par l''infirmerie du centre', 'Candidat revenu en salle après repos, temps compensé', true),
             ('PERTURBATION', 'Attroupement devant le centre perturbant le début de l''épreuve', 'Forces de l''ordre intervenues, composition reprise après 15 minutes', false),
             ('AUTRE', 'Coupure d''électricité de 40 minutes en fin de matinée', 'Salles dotées de lampes de secours, durée compensée', true)) i(type, description, decision, ouvert)
WHERE f.rn = 1;

-- Fuite du sujet de mathématiques SM au BAC général 2026 : variante A compromise, variante B tirée
INSERT INTO incident_examen (examen_id, centre_id, epreuve_id, type, description, survenu_le, signale_par, statut, decision, elimine, created_at)
SELECT f.examen_id, f.centre_id, p.id, 'FUITE_SUJET',
       'Photo du sujet (variante A) diffusée sur les réseaux sociaux une heure avant l''épreuve',
       (p.date_epreuve - 1 + time '19:00')::timestamptz, 'Inspection générale', 'TRAITE',
       'Variante A déclarée compromise ; variante B tirée au sort pour tous les centres ; enquête ouverte', false, (p.date_epreuve - 1 + time '21:00')::timestamptz
FROM demo_effectif f
JOIN epreuve_examen p ON p.examen_id = f.examen_id AND p.serie = 'SCIENCES_MATHEMATIQUES'
JOIN matiere m ON m.id = p.matiere_id AND m.code = 'MATHS'
WHERE f.cle = 'BAC-G-2026' AND f.commune = 'Kaloum';

-- ------------------------------------------------------------------ Commissions de correction (stade 4 et plus)
INSERT INTO commission_correction (examen_id, epreuve_id, nom, lieu, nb_copies, nb_copies_corrigees, cloturee, created_at)
SELECT d.examen_id, p.id,
       'Commission de correction – ' || m.libelle || CASE WHEN p.serie IS NOT NULL THEN ' (' || replace(p.serie, '_', ' ') || ')' ELSE '' END,
       CASE d.orga WHEN 'NATIONAL' THEN 'Centre national de correction, Conakry' WHEN 'REGION' THEN 'Inspection régionale de l''éducation'
                   ELSE 'Direction préfectorale ou communale de l''éducation' END,
       n.copies,
       CASE WHEN d.stade >= 5 THEN n.copies ELSE (n.copies * (40 + abs(hashtext('corr' || p.id::text)) % 45) / 100)::int END,
       d.stade >= 5, (d.debut + d.nbj + 5)::timestamptz
FROM (SELECT x.*, e.id AS examen_id FROM demo_examen x JOIN examen_national e ON e.libelle = x.libelle WHERE x.stade >= 4) d
JOIN epreuve_examen p ON p.examen_id = d.examen_id
JOIN matiere m ON m.id = p.matiere_id
JOIN LATERAL (SELECT count(*)::int AS copies FROM candidat_examen c
              WHERE c.examen_id = d.examen_id AND c.statut_epreuves IN ('PRESENT', 'ELIMINE') AND (p.serie IS NULL OR c.serie = p.serie)) n ON true;

INSERT INTO membre_commission_correction (commission_id, role, nom, telephone, created_at)
SELECT cc.id, r.role, pg_temp.nom_complet((cc.id * 11 + r.rang)::int), '+224 62' || lpad(((cc.id * 6007 + r.rang * 7013) % 10000000)::text, 7, '0'), cc.created_at
FROM commission_correction cc,
     (VALUES ('PRESIDENT', 1), ('SECRETAIRE', 2), ('CORRECTEUR', 3), ('CORRECTEUR', 4), ('CORRECTEUR', 5)) r(role, rang);

-- ------------------------------------------------------------------ Notes (stade 4 et plus)
-- Niveau de base propre à chaque candidat (5 à 16 sur 20), ajusté par série, + variation par épreuve (± 3,5), ramené au barème.
-- Stade 4 : environ 45 % des candidats ont des notes manquantes (correction en cours) ; ensuite toutes les notes sont saisies.
INSERT INTO note_examen (candidat_id, epreuve_id, note)
SELECT c.id, p.id,
       round(least(20.0, greatest(0.0,
             5 + (abs(hashtext('niveau' || c.id::text)) % 1100) / 100.0
               + CASE c.serie WHEN 'SCIENCES_MATHEMATIQUES' THEN 0.4 WHEN 'SCIENCES_SOCIALES' THEN -0.6 ELSE 0 END
               + ((abs(hashtext('note' || c.id::text || '-' || p.id::text)) % 701) / 100.0 - 3.5)))
             * e.bareme / 20.0, 2)
FROM candidat_examen c
JOIN examen_national e ON e.id = c.examen_id
JOIN demo_examen d ON d.libelle = e.libelle
JOIN epreuve_examen p ON p.examen_id = c.examen_id AND (p.serie IS NULL OR p.serie = c.serie)
WHERE d.stade >= 4 AND c.statut_epreuves = 'PRESENT'
  AND NOT (d.stade = 4 AND abs(hashtext('incomplet' || c.id::text)) % 100 < 45
           AND (abs(hashtext('manque' || c.id::text || '-' || p.id::text)) % 100 < 30
                OR p.id = (SELECT max(p2.id) FROM epreuve_examen p2 WHERE p2.examen_id = c.examen_id AND (p2.serie IS NULL OR p2.serie = c.serie))));

-- Moyennes pondérées (candidats dont toutes les épreuves sont notées) puis décisions proposées (admis à partir de la moitié du barème)
UPDATE candidat_examen c
SET moyenne = m.moyenne
FROM (SELECT n.candidat_id, round(sum(n.note * p.coefficient) / sum(p.coefficient), 2) AS moyenne
      FROM note_examen n JOIN epreuve_examen p ON p.id = n.epreuve_id
      JOIN candidat_examen c2 ON c2.id = n.candidat_id
      GROUP BY n.candidat_id, c2.examen_id, c2.serie
      HAVING count(*) = (SELECT count(*) FROM epreuve_examen p2 WHERE p2.examen_id = c2.examen_id AND (p2.serie IS NULL OR p2.serie = c2.serie))) m
WHERE m.candidat_id = c.id;

UPDATE candidat_examen c
SET decision = CASE WHEN c.statut_epreuves = 'ELIMINE' THEN 'NON_ADMIS'
                    WHEN c.statut_epreuves = 'PRESENT' AND c.moyenne IS NOT NULL AND c.moyenne >= e.bareme * 0.5 THEN 'ADMIS'
                    WHEN c.statut_epreuves = 'PRESENT' AND c.moyenne IS NOT NULL THEN 'NON_ADMIS' END
FROM examen_national e JOIN demo_examen d ON d.libelle = e.libelle
WHERE e.id = c.examen_id AND d.stade >= 4;

COMMIT;
