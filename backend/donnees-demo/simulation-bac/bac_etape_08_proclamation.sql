-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 8/9 : PROCLAMATION DES RÉSULTATS ET DIPLÔMES (31 juillet au 20 août 2026)
-- Le ministère proclame les résultats : l'examen passe à l'état « proclamé », les élèves et leurs parents voient leur résultat (onglet
-- « Examens » de l'application mobile et du portail). Les diplômes sont ensuite délivrés à tous les admis, avec leur mention
-- (passable dès 10, assez bien dès 12, bien dès 14, très bien dès 16). À exécuter APRÈS l'étape 7. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM jury_membre WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 7.';
    END IF;
    IF (SELECT statut_resultats FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026') = 'PROCLAME' THEN
        RAISE EXCEPTION 'L''étape 8 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';

UPDATE examen_national SET statut_resultats = 'PROCLAME', date_proclamation = date '2026-07-31' WHERE id = (SELECT id FROM s_ex);

INSERT INTO diplome_examen (candidat_id, numero, code, mention, delivre_le)
SELECT c.id, 'DIP-BS26-' || lpad(c.id::text, 6, '0'), upper(substr(md5('bac-simule' || c.id), 1, 10)),
       CASE WHEN c.moyenne >= 16 THEN 'TRES_BIEN' WHEN c.moyenne >= 14 THEN 'BIEN' WHEN c.moyenne >= 12 THEN 'ASSEZ_BIEN' ELSE 'PASSABLE' END,
       date '2026-08-20'
FROM candidat_examen c WHERE c.examen_id = (SELECT id FROM s_ex) AND c.decision = 'ADMIS';

UPDATE budget_examen SET montant_paye = v.paye
FROM (VALUES ('TRANSPORT', 78000000), ('SECURITE', 66000000), ('AUTRE', 14000000)) v(rubrique, paye)
WHERE budget_examen.rubrique = v.rubrique AND budget_examen.examen_id = (SELECT id FROM s_ex);

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 8 chargée : résultats proclamés, % diplômes délivrés (dont % très bien, % bien, % assez bien, % passable)',
        (SELECT count(*) FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id WHERE c.examen_id = x),
        (SELECT count(*) FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id WHERE c.examen_id = x AND d.mention = 'TRES_BIEN'),
        (SELECT count(*) FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id WHERE c.examen_id = x AND d.mention = 'BIEN'),
        (SELECT count(*) FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id WHERE c.examen_id = x AND d.mention = 'ASSEZ_BIEN'),
        (SELECT count(*) FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id WHERE c.examen_id = x AND d.mention = 'PASSABLE');
END $$;
