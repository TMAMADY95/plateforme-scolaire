-- =====================================================================================================
-- Complément du jeu de démonstration : migrations V55 à V57 (réclamations, fournitures livrées aux centres, budget).
-- À exécuter UNE SEULE FOIS, APRÈS demo_examens_complements.sql :
--   psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f demo_dgecs_complements.sql
-- Ajoute, pour l'examen « CEE blanc 6e année 2026 » : l'approvisionnement des 4 centres (le dernier est en retard), des réclamations
-- de candidats (acceptées, rejetées, ouvertes dans le délai et ouvertes hors délai) et un budget par rubrique en francs guinéens.
-- Les données sont fictives. supprimer_donnees_demo.sql les supprime avec l'examen (suppression en cascade).
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM examen_national WHERE libelle = 'CEE blanc 6e année 2026') THEN
        RAISE EXCEPTION 'Chargez d''abord demo_examens_complements.sql.';
    END IF;
    IF EXISTS (SELECT 1 FROM budget_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'CEE blanc 6e année 2026')) THEN
        RAISE EXCEPTION 'Le complément DGECS a déjà été chargé.';
    END IF;
END $$;

CREATE TEMP TABLE c_ex AS SELECT id FROM examen_national WHERE libelle = 'CEE blanc 6e année 2026';

-- ---------- 1. Fournitures livrées aux centres ----------
-- Les trois premiers centres ont tout reçu ; le dernier n'a reçu que les sujets, les copies sont expédiées et le reste est prévu.
CREATE TEMP TABLE c_centre AS
SELECT id, row_number() OVER (ORDER BY id) AS rang FROM centre_examen WHERE examen_id = (SELECT id FROM c_ex);

INSERT INTO livraison_centre (examen_id, centre_id, article, quantite_prevue, quantite_livree, statut, expediee_le, recue_le, observation)
SELECT (SELECT id FROM c_ex), c.id, a.article,
       (SELECT greatest(count(*), 1) FROM candidat_examen k WHERE k.centre_id = c.id) * a.par_candidat,
       CASE WHEN c.rang < 4 OR a.article = 'SUJETS' THEN (SELECT greatest(count(*), 1) FROM candidat_examen k WHERE k.centre_id = c.id) * a.par_candidat END,
       CASE WHEN c.rang < 4 OR a.article = 'SUJETS' THEN 'RECUE' WHEN a.article = 'COPIES' THEN 'EXPEDIEE' ELSE 'PREVUE' END,
       CASE WHEN c.rang < 4 OR a.article IN ('SUJETS', 'COPIES') THEN timestamptz '2026-09-08 09:00+00' END,
       CASE WHEN c.rang < 4 OR a.article = 'SUJETS' THEN timestamptz '2026-09-10 15:00+00' END,
       CASE WHEN c.rang = 4 AND a.article = 'COPIES' THEN 'Véhicule en panne, nouvelle expédition prévue' END
FROM c_centre c
CROSS JOIN (VALUES ('SUJETS', 1), ('COPIES', 2), ('ENVELOPPES', 1), ('FOURNITURES', 1)) AS a(article, par_candidat);

-- ---------- 2. Réclamations (résultats proclamés le 13 octobre 2026, délai de traitement de 15 jours) ----------
INSERT INTO reclamation_examen (examen_id, candidat_id, objet, description, statut, deposee_le, echeance_le, traitee_le, decision)
SELECT (SELECT id FROM c_ex), k.id, r.objet, r.description, r.statut, r.depot, r.depot + interval '15 days',
       CASE WHEN r.statut <> 'OUVERTE' THEN r.depot + (r.jours || ' days')::interval END, r.decision
FROM (SELECT id, row_number() OVER (ORDER BY id) AS rang FROM candidat_examen WHERE examen_id = (SELECT id FROM c_ex) AND numero IS NOT NULL) k
JOIN (VALUES
    (1, 'NOTE', 'Demande de recorrection de la composition de mathématiques.', 'ACCEPTEE', timestamptz '2026-10-14 08:00+00', 6, 'Erreur de report de note constatée, moyenne rectifiée.'),
    (2, 'IDENTITE', 'Prénom mal orthographié sur la liste des admis.', 'ACCEPTEE', timestamptz '2026-10-14 10:30+00', 3, 'Acte de naissance vérifié, orthographe corrigée.'),
    (3, 'NOTE', 'Conteste la note de français.', 'REJETEE', timestamptz '2026-10-15 09:15+00', 9, 'Copie revue par la commission : note maintenue.'),
    (4, 'OMISSION', 'Nom absent de la liste des résultats.', 'REJETEE', timestamptz '2026-10-15 14:00+00', 20, 'Candidat éliminé pour fraude : décision confirmée (traitée hors délai).'),
    (5, 'NOTE', 'Demande de vérification de la moyenne.', 'OUVERTE', timestamptz '2026-10-20 11:00+00', 0, NULL),
    (6, 'AUTRE', 'Demande de duplicata du relevé de notes.', 'OUVERTE', timestamptz '2026-10-22 16:45+00', 0, NULL)
) AS r(rang, objet, description, statut, depot, jours, decision) ON r.rang = k.rang;

-- ---------- 3. Budget (francs guinéens) ----------
INSERT INTO budget_examen (examen_id, rubrique, montant_prevu, montant_engage, montant_paye, observation)
SELECT (SELECT id FROM c_ex), rubrique, prevu, engage, paye, observation
FROM (VALUES
    ('IMPRESSION', 12000000, 12800000, 12800000, 'Dépassement : tirage supplémentaire de copies'),
    ('TRANSPORT', 8000000, 7500000, 6000000, NULL),
    ('SURVEILLANCE', 15000000, 15000000, 15000000, NULL),
    ('CORRECTION', 10000000, 9000000, 7000000, NULL),
    ('SECURITE', 5000000, 5000000, 5000000, NULL),
    ('SANTE', 2000000, 1200000, 1200000, NULL),
    ('COMMUNICATION', 1500000, 1500000, 1000000, NULL),
    ('AUTRE', 1000000, 600000, 600000, NULL)
) AS b(rubrique, prevu, engage, paye, observation);

DO $$
DECLARE r text;
BEGIN
    SELECT format('%s fournitures, %s réclamations, %s rubriques de budget',
        (SELECT count(*) FROM livraison_centre WHERE examen_id = (SELECT id FROM c_ex)),
        (SELECT count(*) FROM reclamation_examen WHERE examen_id = (SELECT id FROM c_ex)),
        (SELECT count(*) FROM budget_examen WHERE examen_id = (SELECT id FROM c_ex))) INTO r;
    RAISE NOTICE 'Complément DGECS chargé : %', r;
END $$;
