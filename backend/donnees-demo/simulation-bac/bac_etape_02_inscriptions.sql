-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 2/9 : CANDIDATURES ET LISTES DES ÉTABLISSEMENTS (février à avril 2026)
-- Les lycées inscrivent leurs élèves de 12e année (avec leur série), des candidats libres s'ajoutent, les dossiers se constituent
-- (demande, acte de naissance, diplôme précédent, attestation) et chaque établissement transmet sa liste : certaines sont déjà
-- validées par la région, d'autres attendent encore, une est renvoyée à l'établissement pour dossiers incomplets.
-- À exécuter APRÈS l'étape 1. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026') THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 1.';
    END IF;
    IF EXISTS (SELECT 1 FROM candidat_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'L''étape 2 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id, annee_scolaire_id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';

-- ---------- 1. Candidats scolarisés : tous les élèves de 12e année inscrits en 2025-2026 ----------
INSERT INTO candidat_examen (examen_id, eleve_id, etablissement_id, classe_id, created_at, serie, libre)
SELECT (SELECT id FROM s_ex), i.eleve_id, i.etablissement_id, i.classe_id,
       timestamptz '2026-02-02 08:00+00' + (abs(hashtext(i.eleve_id::text || 'ins')) % 40) * interval '1 day', cl.serie, FALSE
FROM inscription i
JOIN classe cl ON cl.id = i.classe_id AND cl.serie IS NOT NULL
JOIN niveau n ON n.id = cl.niveau_id AND n.code = 'GN-12'
JOIN annee_scolaire a ON a.id = cl.annee_scolaire_id AND a.libelle = '2025-2026'
WHERE i.statut = 'ACCEPTEE';

-- ---------- 2. Candidats libres : 14 élèves hors classe d'examen, rattachés à un lycée ----------
INSERT INTO candidat_examen (examen_id, eleve_id, etablissement_id, classe_id, created_at, serie, libre)
SELECT (SELECT id FROM s_ex), r.eleve_id, r.etablissement_id, NULL, timestamptz '2026-03-02 08:00+00' + (r.rang % 20) * interval '1 day', r.serie, TRUE
FROM (SELECT i.eleve_id, i.etablissement_id, cl.serie, row_number() OVER (ORDER BY abs(hashtext(i.eleve_id::text || 'libre'))) AS rang
      FROM inscription i
      JOIN classe cl ON cl.id = i.classe_id AND cl.serie IS NOT NULL
      JOIN niveau n ON n.id = cl.niveau_id AND n.code = 'GN-11'
      JOIN annee_scolaire a ON a.id = cl.annee_scolaire_id AND a.libelle = '2025-2026'
      JOIN etablissement e ON e.id = i.etablissement_id AND e.code_officiel IN ('GN-0003', 'GN-0006', 'GN-0013', 'GN-0017', 'GN-0019', 'GN-0023', 'GN-0027')
      WHERE i.statut = 'ACCEPTEE') r
WHERE r.rang <= 14;

UPDATE candidat_examen SET numero = 'BS-' || lpad(id::text, 6, '0') WHERE examen_id = (SELECT id FROM s_ex);

-- ---------- 3. Candidats en situation de handicap (aménagements à prévoir dans les centres) ----------
UPDATE candidat_examen SET handicap = v.handicap, amenagement = v.amenagement
FROM (SELECT id, row_number() OVER (ORDER BY id) AS rang FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex)) k
JOIN (VALUES (7, 'MOTEUR', 'Salle au rez-de-chaussée, place proche de la sortie'),
             (41, 'VISUEL', 'Sujets en gros caractères et tiers-temps supplémentaire'),
             (88, 'AUDITIF', 'Consignes écrites et place au premier rang'),
             (130, 'MOTEUR', 'Table adaptée et accès sans escalier'),
             (201, 'AUTRE', 'Tiers-temps supplémentaire et salle calme')) v(rang, handicap, amenagement) ON v.rang = k.rang
WHERE candidat_examen.id = k.id;

-- ---------- 4. Dossiers de candidature : pièces fournies (un candidat libre n'a pas d'attestation) ----------
-- Environ 6 % de pièces manquantes ; le lycée de Labé (GN-0019) a beaucoup de dossiers incomplets.
INSERT INTO piece_candidature (candidat_id, type, reference, fournie_le)
SELECT c.id, t.type, CASE t.type WHEN 'ACTE_NAISSANCE' THEN 'ACTE-' || c.id WHEN 'DIPLOME' THEN 'DIP-' || c.id ELSE NULL END,
       (date '2026-02-05' + (abs(hashtext(c.id::text || t.type)) % 45))
FROM candidat_examen c
JOIN etablissement e ON e.id = c.etablissement_id
CROSS JOIN (VALUES ('DEMANDE'), ('ACTE_NAISSANCE'), ('DIPLOME'), ('ATTESTATION')) t(type)
WHERE c.examen_id = (SELECT id FROM s_ex)
  AND NOT (t.type = 'ATTESTATION' AND c.libre)
  AND abs(hashtext(c.id::text || 'manque' || t.type)) % 100 >= CASE WHEN e.code_officiel = 'GN-0019' THEN 12 ELSE 2 END;

-- ---------- 5. Listes transmises par les établissements : état d'avancement du circuit de validation ----------
INSERT INTO liste_candidats (examen_id, etablissement_id, statut, transmise_le, validee_region_le, validee_le, motif_rejet)
SELECT (SELECT id FROM s_ex), e.id, v.statut,
       CASE WHEN v.statut <> 'BROUILLON' THEN v.transmise END,
       CASE WHEN v.statut IN ('VALIDEE_REGION', 'VALIDEE') THEN v.transmise + interval '5 days' END,
       CASE WHEN v.statut = 'VALIDEE' THEN v.transmise + interval '9 days' END,
       v.motif
FROM (VALUES
    ('GN-0003', 'VALIDEE', timestamptz '2026-03-16 10:00+00', NULL),
    ('GN-0006', 'VALIDEE', timestamptz '2026-03-18 10:00+00', NULL),
    ('GN-0007', 'VALIDEE_REGION', timestamptz '2026-03-27 10:00+00', NULL),
    ('GN-0010', 'TRANSMISE', timestamptz '2026-04-06 10:00+00', NULL),
    ('GN-0013', 'VALIDEE', timestamptz '2026-03-20 10:00+00', NULL),
    ('GN-0017', 'TRANSMISE', timestamptz '2026-04-08 10:00+00', NULL),
    ('GN-0019', 'BROUILLON', timestamptz '2026-03-30 10:00+00', 'Dossiers incomplets : actes de naissance, diplômes et attestations manquants. Compléter puis retransmettre la liste.'),
    ('GN-0023', 'VALIDEE_REGION', timestamptz '2026-03-30 10:00+00', NULL),
    ('GN-0027', 'VALIDEE', timestamptz '2026-03-23 10:00+00', NULL)
) v(code, statut, transmise, motif)
JOIN etablissement e ON e.code_officiel = v.code;

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 2 chargée : % candidats (dont % libres), % pièces, % listes, % candidats avec aménagement',
        (SELECT count(*) FROM candidat_examen WHERE examen_id = x), (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND libre),
        (SELECT count(*) FROM piece_candidature p JOIN candidat_examen c ON c.id = p.candidat_id WHERE c.examen_id = x),
        (SELECT count(*) FROM liste_candidats WHERE examen_id = x), (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND handicap IS NOT NULL);
END $$;
