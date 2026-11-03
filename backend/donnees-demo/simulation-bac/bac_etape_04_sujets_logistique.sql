-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 4/9 : SUJETS, IMPRESSION ET LOGISTIQUE (mai à juin 2026)
-- Trois variantes de sujet par épreuve sont élaborées puis validées. Les plis sont conditionnés, expédiés à l'IRE, reçus à la DPE
-- puis remis aux présidents de centre. Fournitures et transport (convois) sont suivis centre par centre, avec deux incidents de route.
-- Les centres terminent leur préparation, le personnel manquant est complété et le budget commence à être engagé.
-- À exécuter APRÈS l'étape 3. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM centre_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 3.';
    END IF;
    IF EXISTS (SELECT 1 FROM sujet_examen WHERE epreuve_id IN (SELECT id FROM epreuve_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'))) THEN
        RAISE EXCEPTION 'L''étape 4 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';
CREATE TEMP TABLE s_nom AS
SELECT n, (ARRAY['Mamadou', 'Fanta', 'Ibrahima', 'Aminata', 'Sékou', 'Mariama', 'Ousmane', 'Hawa', 'Alpha', 'Kadiatou'])[1 + (n - 1) % 10] || ' ' ||
       (ARRAY['Sylla', 'Camara', 'Barry', 'Diallo', 'Touré', 'Bah', 'Condé', 'Keita', 'Soumah', 'Bangoura'])[1 + (n - 1) / 10] AS nom
FROM generate_series(1, 100) n;
CREATE TEMP TABLE s_centre AS
SELECT ce.id AS centre_id, e.code_officiel AS code, row_number() OVER (ORDER BY e.code_officiel) AS rang,
       (SELECT count(*) FROM candidat_examen k WHERE k.centre_id = ce.id)::int AS candidats,
       (SELECT count(DISTINCT k.salle) FROM candidat_examen k WHERE k.centre_id = ce.id)::int AS salles
FROM centre_examen ce JOIN etablissement e ON e.id = ce.etablissement_id WHERE ce.examen_id = (SELECT id FROM s_ex);

-- ---------- 1. Banque de sujets : trois variantes par épreuve, élaborées en mars et validées fin mai ----------
INSERT INTO sujet_examen (epreuve_id, variante, intitule, auteurs, statut, valide_le, created_at)
SELECT e.id, v.variante, 'Sujet ' || v.variante || ' : ' || COALESCE(m.libelle, 'épreuve'), 'Commission nationale d''élaboration',
       'VALIDE', timestamptz '2026-05-26 10:00+00', timestamptz '2026-03-10 09:00+00'
FROM epreuve_examen e LEFT JOIN matiere m ON m.id = e.matiere_id, (VALUES ('A'), ('B'), ('C')) v(variante)
WHERE e.examen_id = (SELECT id FROM s_ex);

-- ---------- 2. Plis : un par centre et par épreuve, exemplaires = candidats + 5 % de marge ----------
-- La veille de chaque épreuve, les plis sont chez les présidents de centre ; à Nzérékoré (piste difficile), ceux des trois dernières
-- journées ne sont encore qu'à la DPE.
INSERT INTO epreuve_centre (centre_id, epreuve_id, nb_exemplaires, statut_pli, conditionne_le, expedie_le, recu_dpe_le, recu_president_le, rapport_clos)
SELECT sc.centre_id, e.id, sc.candidats + (sc.candidats * 5 + 99) / 100,
       CASE WHEN sc.rang = 7 AND e.date_epreuve >= date '2026-07-01' THEN 'RECU_DPE' ELSE 'RECU_PRESIDENT' END,
       e.date_epreuve - 12 + time '09:00', e.date_epreuve - 9 + time '09:00', e.date_epreuve - 6 + time '10:00',
       CASE WHEN sc.rang = 7 AND e.date_epreuve >= date '2026-07-01' THEN NULL ELSE e.date_epreuve - 1 + time '16:00' END, FALSE
FROM s_centre sc JOIN epreuve_examen e ON e.examen_id = (SELECT id FROM s_ex);

-- ---------- 3. Fournitures livrées aux centres (sujets, copies, enveloppes, fournitures) ----------
INSERT INTO livraison_centre (examen_id, centre_id, article, quantite_prevue, quantite_livree, statut, expediee_le, recue_le, observation)
SELECT (SELECT id FROM s_ex), sc.centre_id, a.article,
       CASE a.article WHEN 'SUJETS' THEN 7 * (sc.candidats + (sc.candidats * 5 + 99) / 100) WHEN 'COPIES' THEN sc.candidats * 14
                      WHEN 'ENVELOPPES' THEN sc.salles * 7 ELSE sc.candidats END,
       CASE WHEN sc.rang = 7 AND a.article = 'FOURNITURES' THEN NULL
            ELSE CASE a.article WHEN 'SUJETS' THEN 7 * (sc.candidats + (sc.candidats * 5 + 99) / 100) WHEN 'COPIES' THEN sc.candidats * 14
                                WHEN 'ENVELOPPES' THEN sc.salles * 7 ELSE sc.candidats END END,
       CASE WHEN sc.rang = 7 AND a.article = 'FOURNITURES' THEN 'EXPEDIEE' ELSE 'RECUE' END,
       timestamptz '2026-06-18 09:00+00' + sc.rang * interval '4 hours',
       CASE WHEN NOT (sc.rang = 7 AND a.article = 'FOURNITURES') THEN timestamptz '2026-06-24 15:00+00' + sc.rang * interval '5 hours' END,
       CASE WHEN sc.rang = 6 AND a.article = 'ENVELOPPES' THEN 'Rupture signalée à la livraison, complément livré le lendemain.'
            WHEN sc.rang = 7 AND a.article = 'FOURNITURES' THEN 'Kits de fournitures en route avec un second véhicule.' END
FROM s_centre sc CROSS JOIN (VALUES ('SUJETS'), ('COPIES'), ('ENVELOPPES'), ('FOURNITURES')) a(article);

-- ---------- 4. Transport : un convoi par centre, deux incidents de route ----------
INSERT INTO convoi_examen (examen_id, centre_id, vehicule, chauffeur, itineraire, escorte, depart_prevu, arrivee_prevue, depart_reel, arrivee_reelle,
                           carburant_litres, statut, incident_type, incident_description, observation)
SELECT (SELECT id FROM s_ex), sc.centre_id,
       CASE WHEN sc.rang IN (1, 2) THEN 'Camion Isuzu GN-' || (4500 + sc.rang) || '-A' ELSE 'Camion Mitsubishi GN-' || (4600 + sc.rang) || '-B' END,
       (SELECT nom FROM s_nom WHERE n = 1 + ((sc.rang + 500) * 37) % 100),
       CASE sc.rang WHEN 1 THEN 'DGECS (Kaloum) → Dixinn' WHEN 2 THEN 'DGECS (Kaloum) → Ratoma' WHEN 3 THEN 'Conakry → Coyah'
                    WHEN 4 THEN 'Conakry → Coyah → Kindia' WHEN 5 THEN 'Conakry → Mamou → Labé' WHEN 6 THEN 'Conakry → Mamou → Kankan'
                    ELSE 'Conakry → Mamou → Faranah → Nzérékoré' END,
       sc.rang >= 4,
       timestamptz '2026-06-22 06:00+00' + (CASE WHEN sc.rang >= 5 THEN 0 ELSE 1 END) * interval '1 day',
       timestamptz '2026-06-22 06:00+00' + (CASE sc.rang WHEN 1 THEN 1 WHEN 2 THEN 1 WHEN 3 THEN 1 WHEN 4 THEN 1 WHEN 5 THEN 2 WHEN 6 THEN 2 ELSE 3 END) * interval '1 day' + interval '8 hours',
       timestamptz '2026-06-22 06:30+00' + (CASE WHEN sc.rang >= 5 THEN 0 ELSE 1 END) * interval '1 day',
       -- Nzérékoré arrive avec 28 h de retard (piste inondée) ; Kankan avec 5 h (panne de camion)
       timestamptz '2026-06-22 06:00+00' + (CASE sc.rang WHEN 1 THEN 1 WHEN 2 THEN 1 WHEN 3 THEN 1 WHEN 4 THEN 1 WHEN 5 THEN 2 WHEN 6 THEN 2 ELSE 3 END) * interval '1 day'
           + (CASE sc.rang WHEN 7 THEN interval '36 hours' WHEN 6 THEN interval '13 hours' ELSE interval '7 hours' END),
       CASE sc.rang WHEN 1 THEN 35 WHEN 2 THEN 40 WHEN 3 THEN 90 WHEN 4 THEN 180 WHEN 5 THEN 520 WHEN 6 THEN 610 ELSE 840 END,
       'LIVRE',
       CASE sc.rang WHEN 7 THEN 'RETARD' WHEN 6 THEN 'RUPTURE_MATERIEL' END,
       CASE sc.rang WHEN 7 THEN 'Piste inondée après un orage : le convoi a dû attendre la décrue pendant plus d''une journée.'
                    WHEN 6 THEN 'Enveloppes de pli manquantes à la livraison ; un second envoi a complété le stock.' END,
       CASE WHEN sc.rang = 5 THEN 'Départ anticipé pour tenir compte de la route de Labé.' END
FROM s_centre sc;

-- ---------- 5. Préparation des centres terminée, personnel complété ----------
UPDATE preparation_centre SET fait = TRUE, observation = NULL, fait_le = timestamptz '2026-06-23 12:00+00' + (id % 40) * interval '1 hour',
       fait_par = 'chef.bac' || (SELECT rang FROM s_centre WHERE centre_id = preparation_centre.centre_id) || '@demo.gn'
WHERE centre_id IN (SELECT centre_id FROM s_centre) AND NOT fait;

INSERT INTO personnel_centre (centre_id, role, nom, telephone, salle)
SELECT sc.centre_id, 'SURVEILLANT', 'Salifou Konaté', '+224 62 999 00 00', s.salle
FROM s_centre sc
JOIN (SELECT p.centre_id, p.salle FROM personnel_centre p WHERE p.role = 'SURVEILLANT' GROUP BY p.centre_id, p.salle HAVING count(*) < 2) s ON s.centre_id = sc.centre_id;

INSERT INTO personnel_examen (examen_id, fonction, nom, telephone, created_at)
SELECT (SELECT id FROM s_ex), v.fonction, (SELECT nom FROM s_nom WHERE n = 1 + ((v.base + g) * 37) % 100), '+224 64' || (v.base + g) || ' 30 00', timestamptz '2026-06-10 10:00+00'
FROM (VALUES ('SUPERVISEUR', 200, 9, 12), ('INSPECTEUR', 300, 6, 8), ('INFORMATIQUE', 400, 5, 6)) v(fonction, base, debut, fin)
CROSS JOIN LATERAL generate_series(v.debut, v.fin) g;

-- ---------- 6. Budget : impression et transport engagés, communication payée ----------
UPDATE budget_examen SET montant_engage = v.engage, montant_paye = v.paye, observation = v.obs
FROM (VALUES ('IMPRESSION', 172000000, 120000000, 'Tirage des sujets et des copies de composition'),
             ('TRANSPORT', 61000000, 40000000, 'Convois vers les 7 centres, escortes comprises'),
             ('COMMUNICATION', 8000000, 8000000, NULL),
             ('SECURITE', 20000000, 0, 'Acompte engagé pour les forces de sécurité')) v(rubrique, engage, paye, obs)
WHERE budget_examen.rubrique = v.rubrique AND budget_examen.examen_id = (SELECT id FROM s_ex);

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 4 chargée : % sujets, % plis, % fournitures, % convois, % personnels nationaux',
        (SELECT count(*) FROM sujet_examen s JOIN epreuve_examen e ON e.id = s.epreuve_id WHERE e.examen_id = x),
        (SELECT count(*) FROM epreuve_centre ec JOIN epreuve_examen e ON e.id = ec.epreuve_id WHERE e.examen_id = x),
        (SELECT count(*) FROM livraison_centre WHERE examen_id = x), (SELECT count(*) FROM convoi_examen WHERE examen_id = x),
        (SELECT count(*) FROM personnel_examen WHERE examen_id = x);
END $$;
