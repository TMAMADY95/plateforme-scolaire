-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 9/9 : APRÈS LA PROCLAMATION (août à octobre 2026)
-- Douze réclamations sont déposées (délai de traitement de 15 jours) : certaines acceptées, certaines rejetées dont deux traitées hors délai,
-- deux encore ouvertes. Trois portent sur une note et donnent lieu à une rectification :
--   · une note relevée fait passer un candidat de non admis à admis (son diplôme est délivré après coup) ;
--   · une note abaissée fait perdre l'admission à un candidat : son diplôme est révoqué ;
--   · une rectification est annulée après vérification (note initiale confirmée, réclamation rejetée).
-- Le public vérifie des diplômes en ligne, dont des codes inconnus saisis plusieurs fois (tentatives suspectes). Le budget est soldé et
-- une situation statistique provisoire de l'année est établie. À exécuter APRÈS l'étape 8. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF (SELECT statut_resultats FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026') IS DISTINCT FROM 'PROCLAME' THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 8.';
    END IF;
    IF EXISTS (SELECT 1 FROM reclamation_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')) THEN
        RAISE EXCEPTION 'L''étape 9 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id, annee_scolaire_id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';

-- ---------- 1. Choix des candidats concernés ----------
CREATE TEMP TABLE s_cas (role text, candidat_id bigint);
INSERT INTO s_cas  -- relevé de note : non admis à 9,40 ou plus
SELECT 'GAIN', id FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex) AND decision = 'NON_ADMIS' AND statut_epreuves = 'PRESENT' AND moyenne BETWEEN 9.4 AND 9.99
ORDER BY moyenne DESC, id LIMIT 1;
INSERT INTO s_cas  -- note abaissée : admis de justesse, avec une note de mathématiques assez haute pour la baisser
SELECT 'PERTE', c.id FROM candidat_examen c
WHERE c.examen_id = (SELECT id FROM s_ex) AND c.decision = 'ADMIS' AND c.moyenne BETWEEN 10 AND 10.7
  AND (SELECT n.note FROM note_examen n JOIN epreuve_examen e ON e.id = n.epreuve_id JOIN matiere m ON m.id = e.matiere_id
       WHERE n.candidat_id = c.id AND m.code = 'MATHEMATIQUES') >= 6
ORDER BY c.moyenne, c.id LIMIT 1;
INSERT INTO s_cas  -- rectification annulée : non admis plus loin du seuil
SELECT 'ANNULEE', id FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex) AND decision = 'NON_ADMIS' AND statut_epreuves = 'PRESENT' AND moyenne BETWEEN 8.5 AND 9.39
ORDER BY id DESC LIMIT 1;
INSERT INTO s_cas
SELECT 'ADMIS' || row_number() OVER (ORDER BY id), id FROM (SELECT id FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex) AND decision = 'ADMIS'
    AND id NOT IN (SELECT candidat_id FROM s_cas) ORDER BY abs(hashtext(id::text || 'rec')) LIMIT 4) a;
INSERT INTO s_cas
SELECT 'NON' || row_number() OVER (ORDER BY id), id FROM (SELECT id FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex) AND decision = 'NON_ADMIS'
    AND statut_epreuves = 'PRESENT' AND id NOT IN (SELECT candidat_id FROM s_cas) ORDER BY abs(hashtext(id::text || 'rec2')) LIMIT 4) a;
INSERT INTO s_cas
SELECT 'FRAUDE', id FROM candidat_examen WHERE examen_id = (SELECT id FROM s_ex) AND statut_epreuves = 'ELIMINE' ORDER BY id LIMIT 1;

-- ---------- 2. Les douze réclamations ----------
CREATE TEMP TABLE s_rec (rang int, role text, id bigint);
WITH specs(rang, role, objet, descr, statut, depot, traitee, decision) AS (VALUES
    (1, 'GAIN', 'NOTE', 'Demande de recorrection de la composition de mathématiques.', 'ACCEPTEE', timestamptz '2026-08-03 09:00+00', timestamptz '2026-08-10 11:00+00',
        'Erreur de report constatée sur la copie de mathématiques : note rectifiée, le candidat est admis.'),
    (2, 'PERTE', 'NOTE', 'Signalement d''une note de mathématiques supérieure à celle de la copie.', 'ACCEPTEE', timestamptz '2026-08-04 10:30+00', timestamptz '2026-08-12 09:30+00',
        'Vérification de la copie : la note avait été surévaluée lors du report, elle est rectifiée.'),
    (3, 'ANNULEE', 'NOTE', 'Conteste la note de mathématiques.', 'REJETEE', timestamptz '2026-08-05 08:45+00', timestamptz '2026-08-24 10:00+00',
        'Première rectification annulée après vérification par la commission : note initiale confirmée (traitée hors délai).'),
    (4, 'ADMIS1', 'IDENTITE', 'Prénom mal orthographié sur la liste des admis.', 'ACCEPTEE', timestamptz '2026-08-06 10:00+00', timestamptz '2026-08-08 15:00+00',
        'Acte de naissance vérifié, orthographe corrigée.'),
    (5, 'ADMIS2', 'IDENTITE', 'Date de naissance erronée sur le relevé.', 'ACCEPTEE', timestamptz '2026-08-07 14:20+00', timestamptz '2026-08-14 10:00+00',
        'Date corrigée sur présentation de l''extrait d''acte de naissance.'),
    (6, 'NON1', 'NOTE', 'Conteste la note de philosophie.', 'REJETEE', timestamptz '2026-08-05 09:15+00', timestamptz '2026-08-13 12:00+00',
        'Copie revue par la commission : note maintenue.'),
    (7, 'NON2', 'NOTE', 'Demande de vérification de la moyenne générale.', 'REJETEE', timestamptz '2026-08-06 11:00+00', timestamptz '2026-08-17 09:00+00',
        'Calcul de la moyenne contrôlé : aucune erreur.'),
    (8, 'FRAUDE', 'OMISSION', 'Nom absent de la liste des résultats.', 'REJETEE', timestamptz '2026-08-04 13:00+00', timestamptz '2026-08-11 10:00+00',
        'Candidat éliminé pour fraude pendant les épreuves : décision confirmée.'),
    (9, 'ADMIS3', 'AUTRE', 'Demande de duplicata du relevé de notes.', 'ACCEPTEE', timestamptz '2026-08-10 09:40+00', timestamptz '2026-08-12 16:00+00',
        'Duplicata établi et remis au candidat.'),
    (10, 'NON3', 'NOTE', 'Conteste la note d''histoire-géographie.', 'REJETEE', timestamptz '2026-08-10 15:30+00', timestamptz '2026-08-27 10:00+00',
        'Copie revue : note maintenue (traitée hors délai).'),
    (11, 'NON4', 'NOTE', 'Demande de recorrection de la composition de physique-chimie.', 'OUVERTE', timestamptz '2026-08-12 10:00+00', NULL, NULL),
    (12, 'ADMIS4', 'AUTRE', 'Attestation de réussite non reçue par l''établissement.', 'OUVERTE', timestamptz '2026-08-14 11:20+00', NULL, NULL)),
ins AS (
    INSERT INTO reclamation_examen (examen_id, candidat_id, objet, description, statut, deposee_le, echeance_le, traitee_le, decision)
    SELECT (SELECT id FROM s_ex), k.candidat_id, s.objet, s.descr, s.statut, s.depot, s.depot + interval '15 days', s.traitee, s.decision
    FROM specs s JOIN s_cas k ON k.role = s.role
    RETURNING id, candidat_id)
INSERT INTO s_rec SELECT s.rang, s.role, ins.id FROM ins JOIN s_cas k ON k.candidat_id = ins.candidat_id JOIN specs s ON s.role = k.role;

-- ---------- 3. Rectifications de notes (mathématiques, coefficient 4 sur 19) ----------
DO $$
DECLARE
    x bigint := (SELECT id FROM s_ex);
    r record;
    ep bigint; n0 numeric; nn numeric; m0 numeric; d0 text; m1 numeric; d1 text; rec bigint; etab bigint; fille boolean; ser text;
BEGIN
    SELECT e.id INTO ep FROM epreuve_examen e JOIN matiere m ON m.id = e.matiere_id WHERE e.examen_id = x AND m.code = 'MATHEMATIQUES';
    FOR r IN SELECT role, candidat_id FROM s_cas WHERE role IN ('GAIN', 'PERTE', 'ANNULEE') ORDER BY role LOOP
        SELECT note INTO n0 FROM note_examen WHERE candidat_id = r.candidat_id AND epreuve_id = ep;
        SELECT moyenne, decision, etablissement_id, serie, (SELECT sexe = 'F' FROM eleve WHERE id = eleve_id) INTO m0, d0, etab, ser, fille FROM candidat_examen WHERE id = r.candidat_id;
        SELECT sr.id INTO rec FROM s_rec sr WHERE sr.role = r.role;
        IF r.role = 'GAIN' THEN nn := least(20, n0 + ceil((10 - m0) * 19 / 4.0 * 4) / 4.0 + 0.25);
        ELSIF r.role = 'PERTE' THEN nn := greatest(0, n0 - ceil((m0 - 10) * 19 / 4.0 * 4) / 4.0 - 0.25);
        ELSE nn := n0 + 1; END IF;
        IF r.role = 'ANNULEE' THEN
            -- La note est restituée : seule la trace de la rectification annulée est conservée
            m1 := round(m0 + (nn - n0) * 4 / 19.0, 2); d1 := d0;
            INSERT INTO rectification_note (reclamation_id, candidat_id, epreuve_id, ancienne_note, nouvelle_note, ancienne_moyenne, nouvelle_moyenne, ancienne_decision, nouvelle_decision,
                                            rectifiee_le, rectifiee_par, annulee_le, annulee_par, motif_annulation)
            VALUES (rec, r.candidat_id, ep, n0, nn, m0, m1, d0, d1, timestamptz '2026-08-12 10:00+00', 'dgecs.demo@demo.gn',
                    timestamptz '2026-08-24 10:00+00', 'dgecs.demo@demo.gn', 'Erreur de saisie de la nouvelle note : la commission confirme la note initiale.');
        ELSE
            UPDATE note_examen SET note = nn WHERE candidat_id = r.candidat_id AND epreuve_id = ep;
            UPDATE candidat_examen c SET moyenne = m.moy, decision = CASE WHEN m.moy >= 10 THEN 'ADMIS' ELSE 'NON_ADMIS' END
            FROM (SELECT n.candidat_id, round(sum(n.note * e.coefficient) / sum(e.coefficient), 2) AS moy
                  FROM note_examen n JOIN epreuve_examen e ON e.id = n.epreuve_id WHERE n.candidat_id = r.candidat_id GROUP BY n.candidat_id) m
            WHERE c.id = r.candidat_id AND m.candidat_id = c.id;
            SELECT moyenne, decision INTO m1, d1 FROM candidat_examen WHERE id = r.candidat_id;
            IF (r.role = 'GAIN' AND d1 <> 'ADMIS') OR (r.role = 'PERTE' AND d1 <> 'NON_ADMIS') THEN
                RAISE EXCEPTION 'La rectification % n''a pas modifié la décision (moyenne % → %).', r.role, m0, m1;
            END IF;
            INSERT INTO rectification_note (reclamation_id, candidat_id, epreuve_id, ancienne_note, nouvelle_note, ancienne_moyenne, nouvelle_moyenne, ancienne_decision, nouvelle_decision,
                                            rectifiee_le, rectifiee_par)
            VALUES (rec, r.candidat_id, ep, n0, nn, m0, m1, d0, d1, CASE r.role WHEN 'GAIN' THEN timestamptz '2026-08-10 11:00+00' ELSE timestamptz '2026-08-12 09:30+00' END, 'dgecs.demo@demo.gn');
            -- Totaux de l'établissement et de la série ajustés de ±1
            UPDATE resultat_examen SET admis = admis + CASE r.role WHEN 'GAIN' THEN 1 ELSE -1 END,
                   admis_filles = CASE WHEN fille AND admis_filles IS NOT NULL THEN admis_filles + CASE r.role WHEN 'GAIN' THEN 1 ELSE -1 END ELSE admis_filles END
            WHERE examen_id = x AND etablissement_id = etab;
            UPDATE resultat_examen_serie SET admis = admis + CASE r.role WHEN 'GAIN' THEN 1 ELSE -1 END
            WHERE serie = ser AND resultat_id = (SELECT id FROM resultat_examen WHERE examen_id = x AND etablissement_id = etab);
            IF r.role = 'GAIN' THEN  -- diplôme délivré après la rectification
                INSERT INTO diplome_examen (candidat_id, numero, code, mention, delivre_le)
                VALUES (r.candidat_id, 'DIP-BS26-' || lpad(r.candidat_id::text, 6, '0'), upper(substr(md5('bac-simule' || r.candidat_id), 1, 10)),
                        CASE WHEN m1 >= 16 THEN 'TRES_BIEN' WHEN m1 >= 14 THEN 'BIEN' WHEN m1 >= 12 THEN 'ASSEZ_BIEN' ELSE 'PASSABLE' END, date '2026-09-02');
            ELSE  -- diplôme révoqué (même motif que l'application)
                UPDATE diplome_examen SET revoque = TRUE, revoque_le = timestamptz '2026-08-12 09:30+00', motif_revocation = 'Rectification de note (réclamation n° ' || rec || ')'
                WHERE candidat_id = r.candidat_id;
            END IF;
        END IF;
    END LOOP;
END $$;

-- ---------- 4. Vérifications publiques de diplômes ----------
-- 40 contrôles authentiques, 3 sur le diplôme révoqué, 11 codes inconnus dont deux saisis plusieurs fois (tentatives suspectes)
INSERT INTO verification_diplome (code_saisi, resultat, verifie_le)
SELECT d.code, 'AUTHENTIQUE', timestamptz '2026-08-25 08:00+00' + (abs(hashtext(d.code)) % 60) * interval '1 day' + (abs(hashtext(d.code || 'h')) % 600) * interval '1 minute'
FROM (SELECT d.code FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id
      WHERE c.examen_id = (SELECT id FROM s_ex) AND NOT d.revoque ORDER BY abs(hashtext(d.code || 'v')) LIMIT 40) d;
INSERT INTO verification_diplome (code_saisi, resultat, verifie_le)
SELECT d.code, 'REVOQUE', timestamptz '2026-09-10 09:00+00' + g * interval '9 days'
FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id CROSS JOIN generate_series(0, 2) g
WHERE c.examen_id = (SELECT id FROM s_ex) AND d.revoque;
INSERT INTO verification_diplome (code_saisi, resultat, verifie_le)
SELECT v.code, 'INCONNU', timestamptz '2026-10-08 10:00+00' + v.k * interval '2 days 3 hours'
FROM (VALUES ('SIM0000001', 0), ('SIM0000001', 1), ('SIM0000001', 2), ('SIM0000001', 3), ('SIM0000002', 4), ('SIM0000002', 5), ('SIM0000002', 6),
             ('SIM0000003', 7), ('SIM0000004', 8), ('SIM0000004', 9), ('SIM0000005', 10)) v(code, k);

-- ---------- 5. Budget soldé ----------
UPDATE budget_examen SET montant_paye = montant_engage
WHERE examen_id = (SELECT id FROM s_ex) AND rubrique IN ('CORRECTION', 'IMPRESSION', 'SURVEILLANCE', 'TRANSPORT', 'SECURITE', 'SANTE', 'COMMUNICATION');
UPDATE budget_examen SET montant_engage = 18000000, montant_paye = 18000000, observation = 'Remplacement du sujet de mathématiques, réimpression et frais divers'
WHERE examen_id = (SELECT id FROM s_ex) AND rubrique = 'AUTRE';

-- ---------- 6. Situation statistique provisoire de l'année 2025-2026 ----------
INSERT INTO situation_statistique (annee_scolaire_id, statut, etablie_le, etablie_par, commentaire, inscrits, filles, libres, presents, absents, elimines, admis, centres, salles)
SELECT x.annee_scolaire_id, 'PROVISOIRE', timestamptz '2026-10-20 10:00+00', 'dgecs.demo@demo.gn', 'Situation provisoire établie après le baccalauréat simulé',
       count(c.id), count(*) FILTER (WHERE e.sexe = 'F'), count(*) FILTER (WHERE c.libre),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'), count(*) FILTER (WHERE c.statut_epreuves = 'ABSENT'),
       count(*) FILTER (WHERE c.statut_epreuves = 'ELIMINE'), count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND c.decision = 'ADMIS'),
       (SELECT count(*) FROM centre_examen ce WHERE ce.examen_id = x.id),
       (SELECT count(*) FROM (SELECT DISTINCT c2.centre_id, c2.salle FROM candidat_examen c2 WHERE c2.examen_id = x.id AND c2.centre_id IS NOT NULL AND c2.salle IS NOT NULL) s)
FROM s_ex x JOIN candidat_examen c ON c.examen_id = x.id JOIN eleve e ON e.id = c.eleve_id
GROUP BY x.annee_scolaire_id, x.id;
INSERT INTO situation_statistique_niveau (situation_id, niveau, inscrits, filles, presents, admis)
SELECT (SELECT id FROM situation_statistique WHERE annee_scolaire_id = (SELECT annee_scolaire_id FROM s_ex) AND statut = 'PROVISOIRE' ORDER BY id DESC LIMIT 1),
       coalesce(n.libelle, 'Niveau non précisé'), count(c.id), count(*) FILTER (WHERE e.sexe = 'F'),
       count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT'), count(*) FILTER (WHERE c.statut_epreuves = 'PRESENT' AND c.decision = 'ADMIS')
FROM candidat_examen c JOIN examen_national x ON x.id = c.examen_id LEFT JOIN niveau n ON n.id = x.niveau_id JOIN eleve e ON e.id = c.eleve_id
WHERE x.id = (SELECT id FROM s_ex) GROUP BY coalesce(n.libelle, 'Niveau non précisé');

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 9 chargée : % réclamations, % rectifications (dont % annulée), % diplômes révoqués, % vérifications de diplômes, situation provisoire établie',
        (SELECT count(*) FROM reclamation_examen WHERE examen_id = x),
        (SELECT count(*) FROM rectification_note r JOIN reclamation_examen q ON q.id = r.reclamation_id WHERE q.examen_id = x),
        (SELECT count(*) FROM rectification_note r JOIN reclamation_examen q ON q.id = r.reclamation_id WHERE q.examen_id = x AND r.annulee_le IS NOT NULL),
        (SELECT count(*) FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id WHERE c.examen_id = x AND d.revoque),
        (SELECT count(*) FROM verification_diplome);
END $$;
