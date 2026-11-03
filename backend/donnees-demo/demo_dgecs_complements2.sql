-- =====================================================================================================
-- Complément du jeu de démonstration : migrations V58 et V59 (situation statistique, état des centres, candidats en situation de handicap).
-- À exécuter UNE SEULE FOIS, APRÈS demo_examens_complements.sql (et, de préférence, demo_dgecs_complements.sql) :
--   psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f demo_dgecs_complements2.sql
-- Ajoute pour l'examen « CEE blanc 6e année 2026 » : les caractéristiques des 4 centres (un centre à risque, un autre aux installations en
-- mauvais état), trois candidats avec aménagement, et une situation statistique PROVISOIRE de l'année calculée sur les chiffres actuels.
-- Aucune situation définitive n'est créée (elle exige que tous les examens de l'année soient proclamés).
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM examen_national WHERE libelle = 'CEE blanc 6e année 2026') THEN
        RAISE EXCEPTION 'Chargez d''abord demo_examens_complements.sql.';
    END IF;
    IF EXISTS (SELECT 1 FROM caracteristiques_centre) THEN
        RAISE EXCEPTION 'Le complément DGECS (partie 2) a déjà été chargé.';
    END IF;
END $$;

CREATE TEMP TABLE c_ex AS SELECT id, annee_scolaire_id FROM examen_national WHERE libelle = 'CEE blanc 6e année 2026';
CREATE TEMP TABLE c_centre AS
SELECT id, row_number() OVER (ORDER BY id) AS rang FROM centre_examen WHERE examen_id = (SELECT id FROM c_ex);

-- ---------- 1. Caractéristiques des centres ----------
INSERT INTO caracteristiques_centre (centre_id, salles_reserve, etat_tables_bancs, etat_toilettes, etat_eclairage, etat_ventilation, accessible,
                                     distance_max_km, temps_max_min, risque_inondation, risque_enclavement, risque_insecurite, observation, mis_a_jour_le)
SELECT id, CASE rang WHEN 1 THEN 2 WHEN 2 THEN 1 ELSE 0 END,
       CASE rang WHEN 3 THEN 'MOYEN' ELSE 'BON' END,
       CASE rang WHEN 4 THEN 'MAUVAIS' ELSE 'BON' END,
       'BON', CASE rang WHEN 3 THEN 'MOYEN' END,
       rang <> 4, CASE rang WHEN 1 THEN 4 WHEN 2 THEN 9 WHEN 3 THEN 15 ELSE 32 END, CASE rang WHEN 1 THEN 15 WHEN 2 THEN 30 WHEN 3 THEN 45 ELSE 120 END,
       rang = 4, rang = 4, false,
       CASE rang WHEN 4 THEN 'Piste coupée en saison des pluies, toilettes à réparer.' END,
       now() - interval '20 days'
FROM c_centre;

-- ---------- 2. Candidats en situation de handicap ----------
UPDATE candidat_examen SET handicap = v.handicap, amenagement = v.amenagement
FROM (SELECT id, row_number() OVER (ORDER BY id) AS rang FROM candidat_examen WHERE examen_id = (SELECT id FROM c_ex)) k
JOIN (VALUES (1, 'MOTEUR', 'Place au rez-de-chaussée, proche de la sortie'),
             (2, 'VISUEL', 'Sujet en gros caractères et tiers-temps'),
             (3, 'AUDITIF', 'Consignes écrites, placé au premier rang')) AS v(rang, handicap, amenagement) ON v.rang = k.rang
WHERE candidat_examen.id = k.id;

-- ---------- 3. Situation statistique provisoire de l'année ----------
INSERT INTO situation_statistique (annee_scolaire_id, statut, etablie_le, etablie_par, commentaire, inscrits, filles, libres, presents, absents, elimines, admis, centres, salles)
SELECT x.annee_scolaire_id, 'PROVISOIRE', now() - interval '3 days', 'dgecs.demo@demo.gn', 'Situation provisoire de démonstration',
       count(c.id), count(*) FILTER (WHERE e.sexe = 'F'), count(*) FILTER (WHERE c.libre),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'), count(*) FILTER (WHERE c.statut_epreuves = 'ABSENT'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ELIMINE'), count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND c.decision = 'ADMIS'),
       (SELECT count(*) FROM centre_examen ce JOIN examen_national x2 ON x2.id = ce.examen_id WHERE x2.annee_scolaire_id = x.annee_scolaire_id),
       (SELECT count(*) FROM (SELECT DISTINCT c2.centre_id, c2.salle FROM candidat_examen c2 JOIN examen_national x3 ON x3.id = c2.examen_id
                              WHERE x3.annee_scolaire_id = x.annee_scolaire_id AND c2.centre_id IS NOT NULL AND c2.salle IS NOT NULL) s)
FROM candidat_examen c JOIN examen_national x ON x.id = c.examen_id JOIN eleve e ON e.id = c.eleve_id
WHERE x.annee_scolaire_id = (SELECT annee_scolaire_id FROM c_ex)
GROUP BY x.annee_scolaire_id;

INSERT INTO situation_statistique_niveau (situation_id, niveau, inscrits, filles, presents, admis)
SELECT (SELECT id FROM situation_statistique WHERE annee_scolaire_id = (SELECT annee_scolaire_id FROM c_ex) AND statut = 'PROVISOIRE' ORDER BY id DESC LIMIT 1),
       coalesce(n.libelle, 'Niveau non précisé'), count(c.id), count(*) FILTER (WHERE e.sexe = 'F'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'), count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND c.decision = 'ADMIS')
FROM candidat_examen c JOIN examen_national x ON x.id = c.examen_id LEFT JOIN niveau n ON n.id = x.niveau_id JOIN eleve e ON e.id = c.eleve_id
WHERE x.annee_scolaire_id = (SELECT annee_scolaire_id FROM c_ex)
GROUP BY coalesce(n.libelle, 'Niveau non précisé');

DO $$
BEGIN
    RAISE NOTICE 'Complément DGECS (partie 2) chargé : % centres caractérisés, % candidats avec aménagement, % situation(s) statistique(s)',
        (SELECT count(*) FROM caracteristiques_centre), (SELECT count(*) FROM candidat_examen WHERE handicap IS NOT NULL),
        (SELECT count(*) FROM situation_statistique);
END $$;
