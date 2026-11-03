-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 6/9 : CORRECTION ET SAISIE DES NOTES (6 au 17 juillet 2026)
-- Sept commissions de correction (une par épreuve) corrigent toutes les copies ramassées. Les notes de chaque candidat présent
-- sont saisies pour les sept épreuves ; la moyenne pondérée par les coefficients et la décision (admis à partir de 10/20) sont calculées.
-- Les candidats éliminés pour fraude sont non admis, les absents n'ont ni moyenne ni décision. À exécuter APRÈS l'étape 5. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM epreuve_centre WHERE rapport_clos AND epreuve_id IN (SELECT id FROM epreuve_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'))) THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 5.';
    END IF;
    IF EXISTS (SELECT 1 FROM commission_correction WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'L''étape 6 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';
CREATE TEMP TABLE s_nom AS
SELECT n, (ARRAY['Mamadou', 'Fanta', 'Ibrahima', 'Aminata', 'Sékou', 'Mariama', 'Ousmane', 'Hawa', 'Alpha', 'Kadiatou'])[1 + (n - 1) % 10] || ' ' ||
       (ARRAY['Sylla', 'Camara', 'Barry', 'Diallo', 'Touré', 'Bah', 'Condé', 'Keita', 'Soumah', 'Bangoura'])[1 + (n - 1) / 10] AS nom
FROM generate_series(1, 100) n;

-- ---------- 1. Commissions de correction : une par épreuve, toutes les copies corrigées ----------
INSERT INTO commission_correction (examen_id, epreuve_id, nom, lieu, nb_copies, nb_copies_corrigees, cloturee, created_at)
SELECT e.examen_id, e.id, 'Commission ' || m.libelle, CASE WHEN m.code IN ('MATHEMATIQUES', 'PHYSIQUE_CHIMIE', 'SVT') THEN 'Lycée Donka, Conakry' ELSE 'Lycée Ratoma 1, Conakry' END,
       (SELECT coalesce(sum(ec.copies_ramassees), 0) FROM epreuve_centre ec WHERE ec.epreuve_id = e.id)::int,
       (SELECT coalesce(sum(ec.copies_ramassees), 0) FROM epreuve_centre ec WHERE ec.epreuve_id = e.id)::int, TRUE, timestamptz '2026-07-04 09:00+00'
FROM epreuve_examen e JOIN matiere m ON m.id = e.matiere_id WHERE e.examen_id = (SELECT id FROM s_ex);

INSERT INTO membre_commission_correction (commission_id, role, nom)
SELECT cc.id, v.role, (SELECT nom FROM s_nom WHERE n = 1 + ((500 + cc.id * 20 + v.k) * 37) % 100)
FROM commission_correction cc
JOIN epreuve_examen e ON e.id = cc.epreuve_id JOIN matiere m ON m.id = e.matiere_id
CROSS JOIN LATERAL (SELECT 'PRESIDENT' AS role, 1 AS k UNION ALL SELECT 'SECRETAIRE', 2
                    UNION ALL SELECT 'CORRECTEUR', 2 + g FROM generate_series(1, CASE m.code WHEN 'MATHEMATIQUES' THEN 14 WHEN 'FRANCAIS' THEN 12 WHEN 'PHILOSOPHIE' THEN 12
                                                                                    WHEN 'PHYSIQUE_CHIMIE' THEN 10 ELSE 8 END) g) v
WHERE cc.examen_id = (SELECT id FROM s_ex);

-- ---------- 2. Notes : une par épreuve pour chaque candidat présent (par quarts de point) ----------
-- Chaque candidat a un niveau propre (de 3 à 16) ; la note de chaque épreuve s'en écarte de ± 3,5 points ; les mathématiques sont un peu plus sévères.
INSERT INTO note_examen (candidat_id, epreuve_id, note)
SELECT c.id, e.id,
       greatest(0, least(20, round((3 + 13 * ((abs(hashtext(c.id::text || 'cap')) % 1000) / 1000.0)
                                    + ((abs(hashtext(c.id::text || '-' || e.id::text)) % 701) - 350) / 100.0
                                    + CASE m.code WHEN 'MATHEMATIQUES' THEN -1.2 WHEN 'ANGLAIS' THEN 0.8 ELSE 0 END) * 4) / 4.0))::numeric(4,2)
FROM candidat_examen c
JOIN epreuve_examen e ON e.examen_id = c.examen_id JOIN matiere m ON m.id = e.matiere_id
WHERE c.examen_id = (SELECT id FROM s_ex) AND c.statut_epreuves = 'PRESENT';

-- ---------- 3. Moyennes pondérées et décisions ----------
UPDATE candidat_examen c SET moyenne = m.moy, decision = CASE WHEN m.moy >= 10 THEN 'ADMIS' ELSE 'NON_ADMIS' END
FROM (SELECT n.candidat_id, round(sum(n.note * e.coefficient) / sum(e.coefficient), 2) AS moy
      FROM note_examen n JOIN epreuve_examen e ON e.id = n.epreuve_id WHERE e.examen_id = (SELECT id FROM s_ex) GROUP BY n.candidat_id) m
WHERE m.candidat_id = c.id;
UPDATE candidat_examen SET decision = 'NON_ADMIS' WHERE examen_id = (SELECT id FROM s_ex) AND statut_epreuves = 'ELIMINE';

-- ---------- 4. Budget : correction engagée, impression soldée ----------
UPDATE budget_examen SET montant_engage = v.engage, montant_paye = v.paye
FROM (VALUES ('CORRECTION', 238000000, 150000000), ('IMPRESSION', 172000000, 172000000), ('SURVEILLANCE', 214000000, 214000000)) v(rubrique, engage, paye)
WHERE budget_examen.rubrique = v.rubrique AND budget_examen.examen_id = (SELECT id FROM s_ex);

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 6 chargée : % commissions, % correcteurs, % notes, % admis sur % présents (moyenne générale %)',
        (SELECT count(*) FROM commission_correction WHERE examen_id = x),
        (SELECT count(*) FROM membre_commission_correction m JOIN commission_correction c ON c.id = m.commission_id WHERE c.examen_id = x AND m.role = 'CORRECTEUR'),
        (SELECT count(*) FROM note_examen n JOIN epreuve_examen e ON e.id = n.epreuve_id WHERE e.examen_id = x),
        (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND decision = 'ADMIS'),
        (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND statut_epreuves = 'PRESENT'),
        (SELECT round(avg(moyenne), 2) FROM candidat_examen WHERE examen_id = x);
END $$;
