-- =====================================================================================================
-- SIMULATION D'UN BACCALAURÉAT, ÉTAPE 5/9 : DÉROULEMENT DES ÉPREUVES (29 juin au 3 juillet 2026)
-- Les plis sont ouverts en présence de deux témoins, les compositions se déroulent, les présents, absents et éliminés sont relevés,
-- les copies ramassées et les rapports de surveillance remis. Incidents : trois fraudes avec élimination, retards, malaises, une coupure
-- de courant et, le mardi, une fuite présumée du sujet de mathématiques : la variante A est compromise, la variante B est tirée et
-- l'épreuve commence avec deux heures de retard dans tous les centres. À Nzérékoré, le rapport d'histoire-géographie n'est pas encore clos.
-- À exécuter APRÈS l'étape 4. Une seule fois.
-- =====================================================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM epreuve_centre WHERE epreuve_id IN (SELECT id FROM epreuve_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'))) THEN
        RAISE EXCEPTION 'Chargez d''abord l''étape 4.';
    END IF;
    IF EXISTS (SELECT 1 FROM epreuve_centre WHERE rapport_clos AND epreuve_id IN (SELECT id FROM epreuve_examen WHERE examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026'))) THEN
        RAISE EXCEPTION 'L''étape 5 a déjà été chargée.';
    END IF;
END $$;

CREATE TEMP TABLE s_ex AS SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026';
CREATE TEMP TABLE s_centre AS
SELECT ce.id AS centre_id, row_number() OVER (ORDER BY e.code_officiel) AS rang
FROM centre_examen ce JOIN etablissement e ON e.id = ce.etablissement_id WHERE ce.examen_id = (SELECT id FROM s_ex);
CREATE TEMP TABLE s_epr AS
SELECT e.id AS epreuve_id, m.code, e.date_epreuve, e.heure_debut, e.heure_fin FROM epreuve_examen e JOIN matiere m ON m.id = e.matiere_id WHERE e.examen_id = (SELECT id FROM s_ex);

-- ---------- 1. Fraudes : le premier candidat des centres 2, 4 et 7, éliminé pendant l'épreuve indiquée ----------
CREATE TEMP TABLE s_fraude AS
SELECT sc.centre_id, (SELECT min(id) FROM candidat_examen k WHERE k.centre_id = sc.centre_id) AS candidat_id, v.code, v.heure, v.texte
FROM s_centre sc JOIN (VALUES
    (2, 'PHILOSOPHIE', time '09:20', 'Antisèche découverte sur le candidat pendant l''épreuve de philosophie.'),
    (4, 'MATHEMATIQUES', time '11:40', 'Téléphone portable utilisé pendant l''épreuve de mathématiques.'),
    (7, 'PHYSIQUE_CHIMIE', time '09:50', 'Échange de copies avec un voisin, constaté par deux surveillants.')) v(rang, code, heure, texte) ON v.rang = sc.rang;

UPDATE candidat_examen c SET statut_epreuves = CASE
    WHEN c.id IN (SELECT candidat_id FROM s_fraude) THEN 'ELIMINE'
    WHEN abs(hashtext(c.id::text || 'abs')) % 100 < 4 THEN 'ABSENT' ELSE 'PRESENT' END
WHERE c.examen_id = (SELECT id FROM s_ex);

INSERT INTO incident_examen (examen_id, centre_id, candidat_id, epreuve_id, type, description, survenu_le, signale_par, statut, decision, elimine)
SELECT (SELECT id FROM s_ex), f.centre_id, f.candidat_id, e.epreuve_id, 'FRAUDE', f.texte, (e.date_epreuve + f.heure) AT TIME ZONE 'Africa/Conakry',
       'Chef de salle', 'TRAITE', 'Procès-verbal de fraude établi ; le candidat est éliminé de l''examen.', TRUE
FROM s_fraude f JOIN s_epr e ON e.code = f.code;

-- ---------- 2. Autres incidents ----------
INSERT INTO incident_examen (examen_id, centre_id, candidat_id, epreuve_id, type, description, survenu_le, signale_par, statut, decision, elimine)
SELECT (SELECT id FROM s_ex), sc.centre_id, NULL, e.epreuve_id, v.type, v.descr, (e.date_epreuve + v.heure) AT TIME ZONE 'Africa/Conakry', v.par, v.statut, v.decision, FALSE
FROM (VALUES
    (1, 'PHILOSOPHIE', 'RETARD', time '08:25', 'Plusieurs candidats arrivés après l''ouverture des portes (embouteillages).', 'Secrétaire du centre', 'TRAITE', 'Admis en salle, temps de composition inchangé.'),
    (2, 'PHILOSOPHIE', 'RETARD', time '08:30', 'Cinq candidats se sont présentés après le début de l''épreuve.', 'Secrétaire du centre', 'TRAITE', 'Admis avant la fin du premier quart d''heure, conformément au règlement.'),
    (3, 'ANGLAIS', 'RETARD', time '14:10', 'Une candidate arrivée en retard à cause d''un accident de la route.', 'Chef de salle', 'TRAITE', 'Admise en salle sans temps supplémentaire.'),
    (6, 'PHILOSOPHIE', 'RETARD', time '08:20', 'Retard de trois candidats venant de villages éloignés.', 'Secrétaire du centre', 'TRAITE', 'Admis en salle.'),
    (2, 'MATHEMATIQUES', 'MALAISE', time '11:05', 'Un candidat a eu un malaise ; pris en charge par le personnel de santé du centre.', 'Personnel de santé', 'TRAITE', 'Candidat revenu en salle après repos, temps compensé.'),
    (5, 'PHILOSOPHIE', 'MALAISE', time '10:15', 'Une candidate a fait un malaise dû à la chaleur.', 'Personnel de santé', 'TRAITE', 'Soins sur place, composition terminée.'),
    (7, 'PHYSIQUE_CHIMIE', 'MALAISE', time '10:20', 'Deux candidats incommodés par la chaleur dans une salle sans ventilation.', 'Chef de salle', 'TRAITE', 'Pris en charge, deux candidats ont terminé.'),
    (6, 'MATHEMATIQUES', 'PERTURBATION', time '11:10', 'Coupure de courant de 25 minutes dans deux salles.', 'Président du centre', 'TRAITE', 'Temps supplémentaire de 25 minutes accordé aux salles concernées.'),
    (2, 'MATHEMATIQUES', 'FUITE_SUJET', time '07:40', 'Une photographie du sujet de mathématiques circule avant l''épreuve (signalement relayé par le président du centre).', 'Président du centre', 'TRAITE',
        'Décision de la DGECS : sujet A retiré, sujet B tiré dans tous les centres ; début de l''épreuve reporté à 10 h.'),
    (4, 'FRANCAIS', 'AUTRE', time '08:40', 'Erreur de numérotation de tables dans une salle : trois candidats mal placés.', 'Chef de salle', 'TRAITE', 'Candidats replacés, aucun effet sur l''épreuve.')
) v(rang, code, type, heure, descr, par, statut, decision)
JOIN s_centre sc ON sc.rang = v.rang JOIN s_epr e ON e.code = v.code;

-- ---------- 3. Sujets tirés : A partout, sauf mathématiques (A compromis, B tiré) ----------
UPDATE sujet_examen s SET statut = CASE WHEN e.code = 'MATHEMATIQUES' AND s.variante = 'A' THEN 'COMPROMIS'
                                        WHEN (e.code = 'MATHEMATIQUES' AND s.variante = 'B') OR (e.code <> 'MATHEMATIQUES' AND s.variante = 'A') THEN 'TIRE' ELSE s.statut END,
       tire_le = CASE WHEN (e.code = 'MATHEMATIQUES' AND s.variante = 'B') THEN (e.date_epreuve + time '09:00') AT TIME ZONE 'Africa/Conakry'
                      WHEN (e.code <> 'MATHEMATIQUES' AND s.variante = 'A') THEN ((e.date_epreuve + e.heure_debut) - interval '1 hour') AT TIME ZONE 'Africa/Conakry' ELSE s.tire_le END
FROM s_epr e WHERE s.epreuve_id = e.epreuve_id;

-- ---------- 4. Plis ouverts, compositions, effectifs, copies et rapports ----------
-- Retard au démarrage : 0 à 8 minutes (dans la tolérance), sauf Nzérékoré en physique-chimie (35 minutes) ; mathématiques à 10 h partout.
UPDATE epreuve_centre ec SET
    statut_pli = 'OUVERT',
    recu_president_le = coalesce(ec.recu_president_le, (s.date_epreuve + time '06:30') AT TIME ZONE 'Africa/Conakry'),
    ouvert_le = s.debut_reel - interval '25 minutes',
    ouvert_par = 'Président du centre', temoin1 = 'Délégué des candidats', temoin2 = 'Enseignant surveillant',
    composition_debut = s.debut_reel, composition_fin = s.fin_reelle,
    presents = s.attendus - s.abs - s.eli, absents = s.abs, elimines = s.eli,
    copies_ramassees = s.attendus - s.abs - s.eli - CASE WHEN s.rang = 3 AND s.code = 'ANGLAIS' THEN 2 ELSE 0 END,
    observations = CASE WHEN s.rang = 3 AND s.code = 'ANGLAIS' THEN 'Deux copies introuvables lors du ramassage : recherche en cours.'
                        WHEN s.code = 'MATHEMATIQUES' THEN 'Épreuve débutée à 10 h sur décision de la DGECS (remplacement du sujet).'
                        WHEN s.rang = 7 AND s.code = 'PHYSIQUE_CHIMIE' THEN 'Début retardé : pli arrivé tardivement de la DPE.' ELSE 'RAS' END,
    rapport_clos = NOT (s.rang = 7 AND s.code = 'HIST_GEO')
FROM (SELECT ec2.id, sc.rang, e.code, e.date_epreuve,
             (SELECT count(*) FROM candidat_examen k WHERE k.centre_id = ec2.centre_id)::int AS attendus,
             (SELECT count(*) FROM candidat_examen k WHERE k.centre_id = ec2.centre_id AND k.statut_epreuves = 'ABSENT')::int AS abs,
             (SELECT count(*) FROM incident_examen i JOIN epreuve_examen fe ON fe.id = i.epreuve_id
              WHERE i.centre_id = ec2.centre_id AND i.elimine AND (fe.date_epreuve + fe.heure_debut) <= (e.date_epreuve + e.heure_debut))::int AS eli,
             ((e.date_epreuve + CASE WHEN e.code = 'MATHEMATIQUES' THEN time '10:00' ELSE e.heure_debut END)
                 + (CASE WHEN sc.rang = 7 AND e.code = 'PHYSIQUE_CHIMIE' THEN 35 ELSE abs(hashtext(ec2.centre_id::text || e.code)) % 9 END) * interval '1 minute') AT TIME ZONE 'Africa/Conakry' AS debut_reel,
             ((e.date_epreuve + CASE WHEN e.code = 'MATHEMATIQUES' THEN time '14:00' ELSE e.heure_fin END)
                 + (CASE WHEN sc.rang = 7 AND e.code = 'PHYSIQUE_CHIMIE' THEN 35 ELSE abs(hashtext(ec2.centre_id::text || e.code)) % 9 END) * interval '1 minute') AT TIME ZONE 'Africa/Conakry' AS fin_reelle
      FROM epreuve_centre ec2 JOIN s_epr e ON e.epreuve_id = ec2.epreuve_id JOIN s_centre sc ON sc.centre_id = ec2.centre_id) s
WHERE ec.id = s.id;

-- ---------- 5. Fournitures de Nzérékoré enfin reçues, budget engagé ----------
UPDATE livraison_centre SET statut = 'RECUE', quantite_livree = quantite_prevue, recue_le = timestamptz '2026-06-28 09:00+00', observation = 'Reçues juste avant le début des épreuves.'
WHERE examen_id = (SELECT id FROM s_ex) AND article = 'FOURNITURES' AND statut = 'EXPEDIEE';
UPDATE budget_examen SET montant_engage = v.engage, montant_paye = v.paye, observation = coalesce(v.obs, budget_examen.observation)
FROM (VALUES ('TRANSPORT', 78000000, 61000000, NULL), ('SURVEILLANCE', 214000000, 150000000, 'Primes de surveillance versées en deux temps'),
             ('SECURITE', 66000000, 50000000, NULL), ('SANTE', 23000000, 23000000, NULL), ('COMMUNICATION', 12000000, 12000000, NULL),
             ('AUTRE', 18000000, 10000000, 'Remplacement du sujet de mathématiques et réimpression')) v(rubrique, engage, paye, obs)
WHERE budget_examen.rubrique = v.rubrique AND budget_examen.examen_id = (SELECT id FROM s_ex);

DO $$
DECLARE x bigint := (SELECT id FROM s_ex);
BEGIN
    RAISE NOTICE 'Étape 5 chargée : % candidats présents, % absents, % éliminés, % incidents, % rapports clos sur % plis',
        (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND statut_epreuves = 'PRESENT'),
        (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND statut_epreuves = 'ABSENT'),
        (SELECT count(*) FROM candidat_examen WHERE examen_id = x AND statut_epreuves = 'ELIMINE'),
        (SELECT count(*) FROM incident_examen WHERE examen_id = x),
        (SELECT count(*) FROM epreuve_centre ec JOIN epreuve_examen e ON e.id = ec.epreuve_id WHERE e.examen_id = x AND ec.rapport_clos),
        (SELECT count(*) FROM epreuve_centre ec JOIN epreuve_examen e ON e.id = ec.epreuve_id WHERE e.examen_id = x);
END $$;
