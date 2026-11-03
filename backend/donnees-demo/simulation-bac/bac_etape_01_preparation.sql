-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 1/9 : PRÉPARATION NATIONALE (janvier 2026)
-- La DGECS crée l'examen « Baccalauréat simulé 2026 » (12e année, 7 épreuves avec coefficients), fixe le calendrier prévisionnel
-- de la correction, de la délibération et de la proclamation, déclare ses besoins en personnel (global et par épreuve) et
-- prépare le budget prévisionnel (aucun engagement pour l'instant).
-- À exécuter APRÈS demo_donnees.sql et demo_examens_complements.sql. Une seule fois :
--   psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f bac_etape_01_preparation.sql
-- =====================================================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026') THEN
        RAISE EXCEPTION 'L''étape 1 a déjà été chargée.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'dgecs.demo@demo.gn') THEN
        RAISE EXCEPTION 'Chargez d''abord demo_donnees.sql puis demo_examens_complements.sql.';
    END IF;
END $$;

-- ---------- 1. L'examen : national, lycée, noté sur 20 ----------
INSERT INTO examen_national (libelle, annee_scolaire_id, cycle_id, niveau_id, niveau_organisation, date_debut, date_fin,
                             statut_resultats, correction_debut_prevue, correction_fin_prevue, deliberation_prevue, proclamation_prevue, bareme)
SELECT 'Baccalauréat simulé 2026', a.id, c.id, n.id, 'NATIONAL', date '2026-06-29', date '2026-07-03',
       'EN_COURS', date '2026-07-06', date '2026-07-17', date '2026-07-24', date '2026-07-31', 20
FROM annee_scolaire a, cycle_scolaire c, niveau n
WHERE a.libelle = '2025-2026' AND c.code = 'LYCEE' AND n.code = 'GN-12';

-- ---------- 2. Les 7 épreuves : 5 jours, du lundi 29 juin au vendredi 3 juillet 2026 ----------
INSERT INTO epreuve_examen (examen_id, matiere_id, date_epreuve, heure_debut, heure_fin, coefficient, created_at)
SELECT (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'), m.id, v.jour, v.debut::time, v.fin::time, v.coef, timestamptz '2026-01-12 09:00+00'
FROM (VALUES ('PHILOSOPHIE', date '2026-06-29', '08:00', '12:00', 3), ('ANGLAIS', date '2026-06-29', '14:00', '16:00', 2),
             ('MATHEMATIQUES', date '2026-06-30', '08:00', '12:00', 4),
             ('PHYSIQUE_CHIMIE', date '2026-07-01', '08:00', '11:00', 3), ('SVT', date '2026-07-01', '14:00', '16:00', 2),
             ('FRANCAIS', date '2026-07-02', '08:00', '12:00', 3), ('HIST_GEO', date '2026-07-03', '08:00', '11:00', 2)) v(matiere, jour, debut, fin, coef)
JOIN matiere m ON m.code = v.matiere;

-- ---------- 3. Besoins en personnel déclarés par la DGECS (global puis par épreuve) ----------
INSERT INTO besoin_personnel_examen (examen_id, fonction, requis)
SELECT (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'), fonction, requis
FROM (VALUES ('SECURITE', 28), ('SANTE', 14), ('CORRECTEUR', 70), ('JURY', 9), ('SUPERVISEUR', 12), ('INSPECTEUR', 8), ('INFORMATIQUE', 6)) v(fonction, requis);

-- Sécurité et santé sont nécessaires chaque jour ; les correcteurs dépendent du volume de copies de chaque épreuve
INSERT INTO besoin_personnel_epreuve (epreuve_id, fonction, requis)
SELECT e.id, b.fonction,
       CASE b.fonction
           WHEN 'CORRECTEUR' THEN CASE m.code WHEN 'MATHEMATIQUES' THEN 14 WHEN 'FRANCAIS' THEN 12 WHEN 'PHILOSOPHIE' THEN 12 WHEN 'PHYSIQUE_CHIMIE' THEN 10 ELSE 8 END
           WHEN 'INFORMATIQUE' THEN CASE WHEN m.code IN ('MATHEMATIQUES', 'PHYSIQUE_CHIMIE') THEN 6 ELSE 4 END
           WHEN 'SUPERVISEUR' THEN 12 WHEN 'INSPECTEUR' THEN 8 WHEN 'SECURITE' THEN 28 WHEN 'SANTE' THEN 14 ELSE 9 END
FROM epreuve_examen e JOIN matiere m ON m.id = e.matiere_id
CROSS JOIN (VALUES ('SECURITE'), ('SANTE'), ('CORRECTEUR'), ('SUPERVISEUR'), ('INSPECTEUR'), ('INFORMATIQUE')) b(fonction)
WHERE e.examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026');

-- ---------- 4. Budget prévisionnel en francs guinéens (rien d'engagé ni de payé) ----------
INSERT INTO budget_examen (examen_id, rubrique, montant_prevu, montant_engage, montant_paye, observation)
SELECT (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'), rubrique, prevu, 0, 0, NULL
FROM (VALUES ('IMPRESSION', 180000000), ('TRANSPORT', 95000000), ('SURVEILLANCE', 220000000), ('CORRECTION', 260000000),
             ('SECURITE', 70000000), ('SANTE', 25000000), ('COMMUNICATION', 20000000), ('AUTRE', 30000000)) v(rubrique, prevu);

DO $$
DECLARE x bigint := (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026');
BEGIN
    RAISE NOTICE 'Étape 1 chargée : examen n° %, % épreuves, % besoins globaux, % besoins par épreuve, % rubriques de budget', x,
        (SELECT count(*) FROM epreuve_examen WHERE examen_id = x), (SELECT count(*) FROM besoin_personnel_examen WHERE examen_id = x),
        (SELECT count(*) FROM besoin_personnel_epreuve b JOIN epreuve_examen e ON e.id = b.epreuve_id WHERE e.examen_id = x),
        (SELECT count(*) FROM budget_examen WHERE examen_id = x);
END $$;
