-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 7/9 : DÉLIBÉRATION DU JURY (24 juillet 2026)
-- Le jury national se réunit (8 membres présents sur 9, le quorum est atteint) et délibère : l'examen passe à l'état « délibéré ».
-- Les résultats sont consolidés par établissement (présents, admis, absents, éliminés, filles) et par série ; ils ne sont pas encore
-- proclamés. À exécuter APRÈS l'étape 6. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM commission_correction WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 6.';
    END IF;
    IF EXISTS (SELECT 1 FROM jury_membre WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'L''étape 7 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';

-- ---------- 1. Jury de délibération du baccalauréat ----------
INSERT INTO jury_membre (examen_id, ordre, fonction, nom, present)
SELECT (SELECT id FROM s_ex), v.ordre, v.fonction, v.nom, v.present
FROM (VALUES
    (1, 'Directeur national de l''enseignement secondaire général et technique (président)', 'Mamadou Lamine Sylla', TRUE),
    (2, 'Représentant du ministère de l''Enseignement supérieur (premier vice-président)', 'Fanta Bérété', TRUE),
    (3, 'Représentant du ministère de l''Enseignement technique (deuxième vice-président)', 'Ibrahima Sory Bah', TRUE),
    (4, 'Directeur général des examens et du contrôle scolaire (rapporteur)', 'Thierno Baldé', TRUE),
    (5, 'Proviseur de lycée public', 'Aminata Kourouma', TRUE),
    (6, 'Proviseur de lycée privé', 'Sékou Fofana', TRUE),
    (7, 'Professeur de lycée', 'Mariama Yansané', TRUE),
    (8, 'Membre de la FEGUIPAE', 'Ousmane Sidibé', TRUE),
    (9, 'Fondateur d''établissement privé', 'Hawa Diakité', FALSE)) v(ordre, fonction, nom, present);

-- ---------- 2. La délibération a eu lieu : résultats délibérés, en attente de proclamation ----------
UPDATE examen_national SET statut_resultats = 'DELIBERE', date_deliberation = date '2026-07-24' WHERE id = (SELECT id FROM s_ex);

-- ---------- 3. Résultats consolidés par établissement, avec les filles ----------
INSERT INTO resultat_examen (examen_id, etablissement_id, presentes, admis, absents, elimines, presentes_filles, admis_filles)
SELECT c.examen_id, c.etablissement_id,
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'),
       count(*) FILTER (WHERE c.decision = 'ADMIS'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ABSENT'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ELIMINE'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND e.sexe = 'F'),
       count(*) FILTER (WHERE c.decision = 'ADMIS' AND e.sexe = 'F')
FROM candidat_examen c JOIN eleve e ON e.id = c.eleve_id
WHERE c.examen_id = (SELECT id FROM s_ex) GROUP BY c.examen_id, c.etablissement_id;

-- ... et par série (sciences mathématiques, sciences sociales)
INSERT INTO resultat_examen_serie (resultat_id, serie, presentes, admis)
SELECT r.id, c.serie, count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'), count(*) FILTER (WHERE c.decision = 'ADMIS')
FROM candidat_examen c JOIN resultat_examen r ON r.examen_id = c.examen_id AND r.etablissement_id = c.etablissement_id
WHERE c.examen_id = (SELECT id FROM s_ex) AND c.serie IS NOT NULL
GROUP BY r.id, c.serie HAVING count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT') > 0;

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 7 chargée : jury de % membres, % établissements consolidés, % admis sur % présents (état : délibéré)',
        (SELECT count(*) FROM jury_membre WHERE examen_id = x), (SELECT count(*) FROM resultat_examen WHERE examen_id = x),
        (SELECT sum(admis) FROM resultat_examen WHERE examen_id = x), (SELECT sum(presentes) FROM resultat_examen WHERE examen_id = x);
END $$;
