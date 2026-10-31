-- =====================================================================================================
-- Jeu de données de démonstration : réseau scolaire de Guinée (régions, écoles, élèves, notes, factures,
-- inspections, examens…). À exécuter UNE SEULE FOIS, après le démarrage du backend (migrations Flyway) :
--
--   psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f demo_donnees.sql
--
-- Tous les comptes ont le mot de passe  Demo!2026  (voir LISEZMOI_DONNEES_DEMO.md).
-- Le script ne modifie aucune donnée existante : il ajoute seulement (zones « Guinée », établissements GN-xxxx,
-- comptes @demo.gn). Il s'arrête avec un message s'il a déjà été exécuté.
-- =====================================================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM utilisateur WHERE lower(email) = 'admin@demo.gn') THEN
        RAISE EXCEPTION 'Le jeu de données de démonstration est déjà chargé (compte admin@demo.gn existant).';
    END IF;
END $$;

-- ---------- Fonctions utilitaires (temporaires : disparaissent à la fin de la session) ----------
-- Entier pseudo-aléatoire stable dans [0, m) à partir d'une clé texte.
CREATE FUNCTION pg_temp.r(cle text, m int) RETURNS int LANGUAGE sql IMMUTABLE AS
$$ SELECT (abs(hashtextextended(cle, 0)) % m)::int $$;
-- Élément d'un tableau choisi de façon stable.
CREATE FUNCTION pg_temp.pick(tab text[], cle text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT tab[1 + pg_temp.r(cle, array_length(tab, 1))] $$;

CREATE FUNCTION pg_temp.mdp() RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT '$2a$10$RM1A7Nltq1C5MLfFfLMQHOIPH/OkfL0Aoo3liD8j5PPxT8wTNs4FC'::text $$;   -- Demo!2026

-- Listes de noms guinéens
CREATE FUNCTION pg_temp.prenom_m(cle text) RETURNS text LANGUAGE sql IMMUTABLE AS $$ SELECT pg_temp.pick(ARRAY[
 'Mamadou','Ibrahima','Mohamed','Alpha','Amadou','Sékou','Moussa','Abdoulaye','Ousmane','Fodé','Boubacar','Thierno',
 'Lansana','Aboubacar','Saliou','Mamady','Sory','Yaya','Facinet','Elhadj','Karamoko','Morlaye','Bakary','Souleymane',
 'Mory','Alhassane','Kémo','Daouda','Lamine','Jean'], cle) $$;
CREATE FUNCTION pg_temp.prenom_f(cle text) RETURNS text LANGUAGE sql IMMUTABLE AS $$ SELECT pg_temp.pick(ARRAY[
 'Fatoumata','Aminata','Mariama','Aïssatou','Kadiatou','Hawa','Djénabou','Nènè','Salimatou','Binta','Fanta','Safiatou',
 'Hadja','Kadija','Mabinty','Oumou','Rokia','Sira','Mariame','Néné','Aïcha','Fatou','Houlèye','Marie','Ramatoulaye',
 'Zainab','Djéneba','Nafi','Kadidiatou','Sally'], cle) $$;
CREATE FUNCTION pg_temp.nom(cle text) RETURNS text LANGUAGE sql IMMUTABLE AS $$ SELECT pg_temp.pick(ARRAY[
 'Diallo','Camara','Bah','Barry','Sow','Sylla','Soumah','Keita','Condé','Touré','Traoré','Bangoura','Kourouma',
 'Doumbouya','Fofana','Konaté','Sidibé','Cissé','Kamano','Guilavogui','Haba','Loua','Béavogui','Tounkara','Savané',
 'Baldé','Bangaly','Kaba','Magassouba','Yansané','Touré','Diakité','Sangaré','Dioubaté','Oularé','Mansaré'], cle) $$;

-- ---------- 1. Référentiels ----------
INSERT INTO annee_scolaire (libelle, date_debut, date_fin) VALUES
    ('2025-2026', '2025-09-01', '2026-06-30'),
    ('2026-2027', '2026-09-01', '2027-06-30')
ON CONFLICT (libelle) DO NOTHING;

INSERT INTO niveau (code, libelle, ordre, cycle_id)
SELECT v.code, v.libelle, v.ordre, (SELECT id FROM cycle_scolaire WHERE code = v.cyc)
FROM (VALUES
    ('GN-01', '1ère année', 1, 'PRIMAIRE'), ('GN-02', '2e année', 2, 'PRIMAIRE'), ('GN-03', '3e année', 3, 'PRIMAIRE'),
    ('GN-04', '4e année', 4, 'PRIMAIRE'),   ('GN-05', '5e année', 5, 'PRIMAIRE'), ('GN-06', '6e année', 6, 'PRIMAIRE'),
    ('GN-07', '7e année', 7, 'COLLEGE'),    ('GN-08', '8e année', 8, 'COLLEGE'),  ('GN-09', '9e année', 9, 'COLLEGE'),
    ('GN-10', '10e année', 10, 'COLLEGE'),  ('GN-11', '11e année', 11, 'LYCEE'),  ('GN-12', '12e année', 12, 'LYCEE'),
    ('GN-13', 'Terminale', 13, 'LYCEE')
) AS v(code, libelle, ordre, cyc)
ON CONFLICT (code) DO NOTHING;

INSERT INTO matiere (code, libelle) VALUES
    ('FRANCAIS', 'Français'), ('MATHEMATIQUES', 'Mathématiques'), ('EVEIL_SCIENTIFIQUE', 'Éveil scientifique'),
    ('EDUC_CIVIQUE', 'Éducation civique et morale'), ('PHYSIQUE_CHIMIE', 'Physique-Chimie'),
    ('SVT', 'Sciences de la vie et de la terre'), ('HIST_GEO', 'Histoire-Géographie'), ('ANGLAIS', 'Anglais'),
    ('EPS', 'Éducation physique et sportive'), ('PHILOSOPHIE', 'Philosophie'), ('INFORMATIQUE', 'Informatique'),
    ('ARABE', 'Arabe'), ('SCIENCES_ECO', 'Sciences économiques')
ON CONFLICT (code) DO NOTHING;

-- Les modèles d'examen proposent la classe et le cycle correspondants.
UPDATE type_examen t SET niveau_id = n.id, cycle_id = n.cycle_id
FROM niveau n
WHERE (t.code, n.code) IN (('CEE', 'GN-06'), ('BEPC', 'GN-10'), ('BAC', 'GN-13')) AND t.niveau_id IS NULL;

-- Matières enseignées par cycle
CREATE TEMP TABLE t_matiere_cycle (cycle text, matiere text);
INSERT INTO t_matiere_cycle VALUES
    ('PRIMAIRE','FRANCAIS'), ('PRIMAIRE','MATHEMATIQUES'), ('PRIMAIRE','EVEIL_SCIENTIFIQUE'), ('PRIMAIRE','EDUC_CIVIQUE'),
    ('COLLEGE','FRANCAIS'), ('COLLEGE','MATHEMATIQUES'), ('COLLEGE','PHYSIQUE_CHIMIE'), ('COLLEGE','SVT'),
    ('COLLEGE','HIST_GEO'), ('COLLEGE','ANGLAIS'), ('COLLEGE','EDUC_CIVIQUE'), ('COLLEGE','EPS'),
    ('LYCEE','FRANCAIS'), ('LYCEE','MATHEMATIQUES'), ('LYCEE','PHYSIQUE_CHIMIE'), ('LYCEE','SVT'),
    ('LYCEE','HIST_GEO'), ('LYCEE','ANGLAIS'), ('LYCEE','PHILOSOPHIE'), ('LYCEE','INFORMATIQUE');

-- ---------- 2. Zones géographiques ----------
CREATE TEMP TABLE zdef (nom text, typ text, parent_nom text, parent_typ text);
INSERT INTO zdef VALUES
    ('Guinée', 'PAYS', NULL, NULL),
    ('Conakry', 'REGION', 'Guinée', 'PAYS'), ('Kindia', 'REGION', 'Guinée', 'PAYS'), ('Labé', 'REGION', 'Guinée', 'PAYS'),
    ('Kankan', 'REGION', 'Guinée', 'PAYS'), ('Nzérékoré', 'REGION', 'Guinée', 'PAYS'),
    ('Conakry', 'PREFECTURE', 'Conakry', 'REGION'),
    ('Kindia', 'PREFECTURE', 'Kindia', 'REGION'), ('Coyah', 'PREFECTURE', 'Kindia', 'REGION'), ('Dubréka', 'PREFECTURE', 'Kindia', 'REGION'),
    ('Labé', 'PREFECTURE', 'Labé', 'REGION'), ('Koubia', 'PREFECTURE', 'Labé', 'REGION'),
    ('Kankan', 'PREFECTURE', 'Kankan', 'REGION'), ('Siguiri', 'PREFECTURE', 'Kankan', 'REGION'),
    ('Nzérékoré', 'PREFECTURE', 'Nzérékoré', 'REGION'), ('Yomou', 'PREFECTURE', 'Nzérékoré', 'REGION'),
    ('Kaloum', 'COMMUNE', 'Conakry', 'PREFECTURE'), ('Dixinn', 'COMMUNE', 'Conakry', 'PREFECTURE'),
    ('Matam', 'COMMUNE', 'Conakry', 'PREFECTURE'), ('Ratoma', 'COMMUNE', 'Conakry', 'PREFECTURE'),
    ('Matoto', 'COMMUNE', 'Conakry', 'PREFECTURE'),
    ('Kindia-centre', 'COMMUNE', 'Kindia', 'PREFECTURE'), ('Coyah-centre', 'COMMUNE', 'Coyah', 'PREFECTURE'),
    ('Dubréka-centre', 'COMMUNE', 'Dubréka', 'PREFECTURE'), ('Labé-centre', 'COMMUNE', 'Labé', 'PREFECTURE'),
    ('Koubia-centre', 'COMMUNE', 'Koubia', 'PREFECTURE'), ('Kankan-centre', 'COMMUNE', 'Kankan', 'PREFECTURE'),
    ('Siguiri-centre', 'COMMUNE', 'Siguiri', 'PREFECTURE'), ('Nzérékoré-centre', 'COMMUNE', 'Nzérékoré', 'PREFECTURE'),
    ('Yomou-centre', 'COMMUNE', 'Yomou', 'PREFECTURE');
CREATE TEMP TABLE zmap (nom text, typ text, id bigint);
DO $$
DECLARE r record; pid bigint; nid bigint;
BEGIN
    FOR r IN SELECT * FROM zdef ORDER BY CASE typ WHEN 'PAYS' THEN 1 WHEN 'REGION' THEN 2 WHEN 'PREFECTURE' THEN 3 ELSE 4 END, nom LOOP
        SELECT id INTO pid FROM zmap WHERE nom = r.parent_nom AND typ = r.parent_typ;
        INSERT INTO zone_geographique (parent_id, nom, type_zone) VALUES (pid, r.nom, r.typ) RETURNING id INTO nid;
        INSERT INTO zmap VALUES (r.nom, r.typ, nid);
    END LOOP;
END $$;

-- ---------- 3. Établissements ----------
-- groupe : taille relative (1 petit, 2 moyen, 3 grand) ; cycles : liste séparée par des virgules
CREATE TEMP TABLE edef (code text, nom text, commune text, secteur text, type_code text, cycles text, statut text,
                        autorisation text, groupe int, adresse text);
INSERT INTO edef VALUES
 ('GN-0001','École primaire Kaloum 1','Kaloum','PUBLIC','CLASSIQUE','PRIMAIRE','ACTIF','AUTORISE',2,'Avenue du Port, Kaloum'),
 ('GN-0002','Collège Kaloum','Kaloum','PUBLIC','CLASSIQUE','COLLEGE','ACTIF','AUTORISE',2,'Rue KA-012, Kaloum'),
 ('GN-0003','Lycée Donka','Dixinn','PUBLIC','CLASSIQUE','LYCEE','ACTIF','AUTORISE',3,'Route de Donka, Dixinn'),
 ('GN-0004','École Sainte-Marie','Dixinn','PRIVE','CONFESSIONNEL','PRIMAIRE,COLLEGE','ACTIF','AUTORISE',2,'Corniche Nord, Dixinn'),
 ('GN-0005','Groupe scolaire Matam','Matam','PUBLIC','CLASSIQUE','PRIMAIRE,COLLEGE','ACTIF','AUTORISE',3,'Marché de Matam'),
 ('GN-0006','Lycée Ratoma 1','Ratoma','PUBLIC','CLASSIQUE','LYCEE','ACTIF','AUTORISE',3,'Axe Hamdallaye-Ratoma'),
 ('GN-0007','Complexe scolaire Les Étoiles','Ratoma','PRIVE','CLASSIQUE','PRIMAIRE,COLLEGE,LYCEE','ACTIF','AUTORISE',3,'Kipé, Ratoma'),
 ('GN-0008','École franco-arabe Al Nour','Ratoma','PRIVE','FRANCO_ARABE','PRIMAIRE,COLLEGE','ACTIF','AUTORISE',2,'Lambanyi, Ratoma'),
 ('GN-0009','Collège Matoto','Matoto','PUBLIC','CLASSIQUE','COLLEGE','ACTIF','AUTORISE',3,'Route du Niger, Matoto'),
 ('GN-0010','Lycée technique de Matoto','Matoto','PUBLIC','TECHNIQUE','LYCEE','ACTIF','AUTORISE',2,'Cité Enco 5, Matoto'),
 ('GN-0011','École primaire Coyah 1','Coyah-centre','PUBLIC','CLASSIQUE','PRIMAIRE','ACTIF','AUTORISE',2,'Quartier Wonkifong, Coyah'),
 ('GN-0012','Collège Coyah','Coyah-centre','PUBLIC','CLASSIQUE','COLLEGE','ACTIF','AUTORISE',2,'Route nationale 1, Coyah'),
 ('GN-0013','Lycée Coyah','Coyah-centre','PUBLIC','CLASSIQUE','LYCEE','ACTIF','AUTORISE',2,'Quartier Gbérédou, Coyah'),
 ('GN-0014','École communautaire de Manéah','Coyah-centre','PUBLIC','COMMUNAUTAIRE','PRIMAIRE','ACTIF','EN_ATTENTE',1,'Village de Manéah'),
 ('GN-0015','Collège privé Les Cèdres','Coyah-centre','PRIVE','CLASSIQUE','COLLEGE','ACTIF','EN_ATTENTE',1,'Quartier Sonfonia, Coyah'),
 ('GN-0016','Collège Kindia','Kindia-centre','PUBLIC','CLASSIQUE','COLLEGE','ACTIF','AUTORISE',2,'Quartier Manquepas, Kindia'),
 ('GN-0017','Lycée Kindia','Kindia-centre','PUBLIC','CLASSIQUE','LYCEE','ACTIF','AUTORISE',3,'Quartier Télico, Kindia'),
 ('GN-0018','École primaire Dubréka','Dubréka-centre','PUBLIC','CLASSIQUE','PRIMAIRE','ACTIF','AUTORISE',2,'Centre-ville, Dubréka'),
 ('GN-0019','Lycée Labé','Labé-centre','PUBLIC','CLASSIQUE','LYCEE','ACTIF','AUTORISE',2,'Quartier Daka, Labé'),
 ('GN-0020','Collège Labé','Labé-centre','PUBLIC','CLASSIQUE','COLLEGE','ACTIF','AUTORISE',2,'Quartier Tata, Labé'),
 ('GN-0021','École privée Fouta','Labé-centre','PRIVE','CLASSIQUE','PRIMAIRE,COLLEGE','ACTIF','AUTORISE',1,'Route de Pita, Labé'),
 ('GN-0022','École primaire Koubia','Koubia-centre','PUBLIC','COMMUNAUTAIRE','PRIMAIRE','ACTIF','EN_ATTENTE',1,'Centre de Koubia'),
 ('GN-0023','Lycée Kankan','Kankan-centre','PUBLIC','CLASSIQUE','LYCEE','ACTIF','AUTORISE',3,'Quartier Missira, Kankan'),
 ('GN-0024','Collège Kankan','Kankan-centre','PUBLIC','CLASSIQUE','COLLEGE','ACTIF','AUTORISE',2,'Quartier Bordo, Kankan'),
 ('GN-0025','École primaire Kankan','Kankan-centre','PUBLIC','CLASSIQUE','PRIMAIRE','ACTIF','AUTORISE',2,'Quartier Sanfil, Kankan'),
 ('GN-0026','Collège Siguiri','Siguiri-centre','PUBLIC','CLASSIQUE','COLLEGE','SUSPENDU','NON_AUTORISE',1,'Quartier Sokoura, Siguiri'),
 ('GN-0027','Lycée Nzérékoré','Nzérékoré-centre','PUBLIC','CLASSIQUE','LYCEE','ACTIF','AUTORISE',2,'Quartier Dorota, Nzérékoré'),
 ('GN-0028','École primaire Nzérékoré 2','Nzérékoré-centre','PUBLIC','CLASSIQUE','PRIMAIRE','FERME','RETIRE',1,'Quartier Gonia, Nzérékoré'),
 ('GN-0029','Collège Yomou','Yomou-centre','PUBLIC','CLASSIQUE','COLLEGE','ACTIF','AUTORISE',1,'Centre de Yomou');

INSERT INTO etablissement (code_officiel, nom, zone_id, secteur, type_etablissement_id, adresse, capacite, statut,
                           autorisation_statut, autorisation_reference, autorisation_date, motif_statut)
SELECT d.code, d.nom, z.id, d.secteur, (SELECT id FROM type_etablissement WHERE code = d.type_code), d.adresse,
       200 + d.groupe * 250 + pg_temp.r(d.code, 100), d.statut, d.autorisation,
       CASE WHEN d.autorisation IN ('AUTORISE','RETIRE','NON_AUTORISE') THEN 'AUT-' || (2008 + pg_temp.r(d.code || 'a', 15)) || '-' || substr(d.code, 4) END,
       CASE WHEN d.autorisation = 'AUTORISE' THEN date '2008-01-01' + pg_temp.r(d.code || 'd', 5000) END,
       CASE WHEN d.statut = 'SUSPENDU' THEN 'Suspendu après inspection : locaux non conformes'
            WHEN d.statut = 'FERME' THEN 'Fermé : bâtiment inutilisable après les pluies' END
FROM edef d JOIN zmap z ON z.nom = d.commune AND z.typ = 'COMMUNE';

INSERT INTO etablissement_cycle (etablissement_id, cycle_id)
SELECT e.id, c.id
FROM edef d
JOIN etablissement e ON e.code_officiel = d.code
CROSS JOIN LATERAL unnest(string_to_array(d.cycles, ',')) AS cy(code)
JOIN cycle_scolaire c ON c.code = cy.code;

-- Année scolaire de référence
CREATE TEMP TABLE t_annee AS
SELECT (SELECT id FROM annee_scolaire WHERE libelle = '2026-2027') AS courante,
       (SELECT id FROM annee_scolaire WHERE libelle = '2025-2026') AS precedente;

-- ---------- 4. Comptes : supervision, directions, secrétariats ----------
CREATE TEMP TABLE t_u (email text, nom text, prenom text, tel text, role text, perimetre text, zone_nom text, zone_typ text, etab_code text);

-- Procédure : crée les comptes de t_u (avec leurs affectations) puis vide la table.
CREATE FUNCTION pg_temp.creer_comptes() RETURNS int LANGUAGE plpgsql AS $$
DECLARE n int;
BEGIN
    INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom, telephone)
    SELECT DISTINCT ON (lower(email)) email, pg_temp.mdp(), nom, prenom, tel FROM t_u;
    INSERT INTO affectation_role (utilisateur_id, role, perimetre, zone_id, etablissement_id)
    SELECT u.id, t.role, t.perimetre, z.id, e.id
    FROM t_u t
    JOIN utilisateur u ON lower(u.email) = lower(t.email)
    LEFT JOIN zmap z ON z.nom = t.zone_nom AND z.typ = t.zone_typ
    LEFT JOIN etablissement e ON e.code_officiel = t.etab_code;
    GET DIAGNOSTICS n = ROW_COUNT;
    TRUNCATE t_u;
    RETURN n;
END $$;

INSERT INTO t_u VALUES
 ('admin@demo.gn',               'Administrateur', 'Technique', '+224 620 00 00 01', 'ADMIN_TECHNIQUE', 'NATIONAL', NULL, NULL, NULL),
 ('ministere.demo@demo.gn',      'Bangoura', 'Mohamed Lamine', '+224 620 00 00 02', 'MINISTERE', 'NATIONAL', NULL, NULL, NULL),
 ('secretaire.general@demo.gn',  'Camara', 'Aïssatou', '+224 620 00 00 03', 'MINISTERE', 'NATIONAL', NULL, NULL, NULL),
 ('dr.conakry@demo.gn',   'Sylla', 'Mamadou Saliou', '+224 621 10 00 01', 'DIRECTION_REGIONALE', 'ZONE', 'Conakry', 'REGION', NULL),
 ('dr.kindia@demo.gn',    'Soumah', 'Fanta', '+224 621 10 00 02', 'DIRECTION_REGIONALE', 'ZONE', 'Kindia', 'REGION', NULL),
 ('dr.labe@demo.gn',      'Barry', 'Alpha Oumar', '+224 621 10 00 03', 'DIRECTION_REGIONALE', 'ZONE', 'Labé', 'REGION', NULL),
 ('dr.kankan@demo.gn',    'Keita', 'Sékou', '+224 621 10 00 04', 'DIRECTION_REGIONALE', 'ZONE', 'Kankan', 'REGION', NULL),
 ('dr.nzerekore@demo.gn', 'Guilavogui', 'Marie', '+224 621 10 00 05', 'DIRECTION_REGIONALE', 'ZONE', 'Nzérékoré', 'REGION', NULL),
 ('dp.coyah@demo.gn',     'Touré', 'Ibrahima', '+224 622 20 00 01', 'DIRECTION_REGIONALE', 'ZONE', 'Coyah', 'PREFECTURE', NULL),
 ('dp.kindia@demo.gn',    'Condé', 'Mariama', '+224 622 20 00 02', 'DIRECTION_REGIONALE', 'ZONE', 'Kindia', 'PREFECTURE', NULL),
 ('dc.coyah@demo.gn',     'Kourouma', 'Facinet', '+224 622 30 00 01', 'DIRECTION_REGIONALE', 'ZONE', 'Coyah-centre', 'COMMUNE', NULL),
 ('dc.ratoma@demo.gn',    'Diallo', 'Hawa', '+224 622 30 00 02', 'DIRECTION_REGIONALE', 'ZONE', 'Ratoma', 'COMMUNE', NULL);
DO $$ BEGIN PERFORM pg_temp.creer_comptes(); END $$;

-- Une direction et un secrétariat par établissement
INSERT INTO t_u
SELECT 'dir.' || substr(d.code, 4) || '@demo.gn', pg_temp.nom('dir' || d.code),
       CASE WHEN pg_temp.r('dirs' || d.code, 3) = 0 THEN pg_temp.prenom_f('dirf' || d.code) ELSE pg_temp.prenom_m('dirm' || d.code) END,
       '+224 62' || (5 + pg_temp.r('dt' || d.code, 4)) || ' ' || lpad(pg_temp.r('dtt' || d.code, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('dtu' || d.code, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('dtv' || d.code, 100)::text, 2, '0'),
       'DIRECTION_ETABLISSEMENT', 'ETABLISSEMENT', NULL, NULL, d.code
FROM edef d;
INSERT INTO t_u
SELECT 'sec.' || substr(d.code, 4) || '@demo.gn', pg_temp.nom('sec' || d.code), pg_temp.prenom_f('secf' || d.code),
       NULL, 'PERSONNEL_ADMIN', 'ETABLISSEMENT', NULL, NULL, d.code
FROM edef d;
DO $$ BEGIN PERFORM pg_temp.creer_comptes(); END $$;

-- ---------- 5. Périodes de chaque établissement (trimestres ou semestres) ----------
INSERT INTO periode (annee_scolaire_id, libelle, ordre, date_debut, date_fin, etablissement_id, type_periode)
SELECT a.id, p.libelle, p.ordre, p.debut, p.fin, e.id, p.typ
FROM edef d
JOIN etablissement e ON e.code_officiel = d.code
CROSS JOIN annee_scolaire a
CROSS JOIN LATERAL (
    SELECT * FROM (VALUES
        ('1er trimestre', 1, make_date(extract(year FROM a.date_debut)::int, 9, 1),  make_date(extract(year FROM a.date_debut)::int, 12, 20), 'TRIMESTRE'),
        ('2e trimestre',  2, make_date(extract(year FROM a.date_debut)::int + 1, 1, 5), make_date(extract(year FROM a.date_debut)::int + 1, 3, 31), 'TRIMESTRE'),
        ('3e trimestre',  3, make_date(extract(year FROM a.date_debut)::int + 1, 4, 10), make_date(extract(year FROM a.date_debut)::int + 1, 6, 30), 'TRIMESTRE')
    ) t(libelle, ordre, debut, fin, typ)
    WHERE NOT (d.secteur = 'PRIVE' OR d.type_code IN ('FRANCO_ARABE', 'TECHNIQUE'))
    UNION ALL
    SELECT * FROM (VALUES
        ('1er semestre', 1, make_date(extract(year FROM a.date_debut)::int, 9, 1), make_date(extract(year FROM a.date_debut)::int + 1, 1, 31), 'SEMESTRE'),
        ('2e semestre',  2, make_date(extract(year FROM a.date_debut)::int + 1, 2, 8), make_date(extract(year FROM a.date_debut)::int + 1, 6, 30), 'SEMESTRE')
    ) s(libelle, ordre, debut, fin, typ)
    WHERE d.secteur = 'PRIVE' OR d.type_code IN ('FRANCO_ARABE', 'TECHNIQUE')
) p
WHERE a.libelle IN ('2025-2026', '2026-2027');

-- ---------- 6. Classes (année précédente et année courante) et salles ----------
CREATE TEMP TABLE t_cls_def AS
SELECT e.id AS etab_id, d.code, d.groupe, n.id AS niveau_id, n.ordre, regexp_replace(n.libelle, ' année', '') AS base, s.sec
FROM edef d
JOIN etablissement e ON e.code_officiel = d.code
JOIN cycle_scolaire c ON c.code = ANY (string_to_array(d.cycles, ','))
JOIN niveau n ON n.cycle_id = c.id AND n.code LIKE 'GN-%'
CROSS JOIN LATERAL (
    SELECT sec FROM unnest(ARRAY['A', 'B']) WITH ORDINALITY AS u(sec, i)
    WHERE i <= CASE WHEN d.groupe = 3 THEN 2 WHEN d.groupe = 2 AND pg_temp.r(d.code || n.code, 2) = 0 THEN 2 ELSE 1 END
) s;

INSERT INTO classe (etablissement_id, annee_scolaire_id, niveau_id, nom, capacite)
SELECT cd.etab_id, a.id, cd.niveau_id, cd.base || ' ' || cd.sec, 40
FROM t_cls_def cd CROSS JOIN annee_scolaire a
WHERE a.libelle IN ('2025-2026', '2026-2027');

-- Une salle par classe de l'année courante, plus des salles spécialisées selon le cycle et la taille
INSERT INTO salle (etablissement_id, nom, capacite, type_salle)
SELECT x.etab_id, 'Salle ' || x.k, 30 + pg_temp.r('sal' || x.etab_id || '-' || x.k, 25), 'SALLE_DE_CLASSE'
FROM (SELECT cl.etablissement_id AS etab_id,
             row_number() OVER (PARTITION BY cl.etablissement_id ORDER BY n.ordre, cl.nom) AS k
      FROM classe cl JOIN niveau n ON n.id = cl.niveau_id
      WHERE cl.annee_scolaire_id = (SELECT courante FROM t_annee)
        AND cl.etablissement_id IN (SELECT e.id FROM edef d JOIN etablissement e ON e.code_officiel = d.code)) x;
INSERT INTO salle (etablissement_id, nom, capacite, type_salle)
SELECT e.id, 'Laboratoire', 24, 'LABO'
FROM edef d JOIN etablissement e ON e.code_officiel = d.code WHERE d.cycles ~ 'COLLEGE|LYCEE';
INSERT INTO salle (etablissement_id, nom, capacite, type_salle)
SELECT e.id, 'Salle informatique', 20, 'LABO'
FROM edef d JOIN etablissement e ON e.code_officiel = d.code WHERE d.cycles ~ 'LYCEE' AND d.groupe >= 2;
INSERT INTO salle (etablissement_id, nom, capacite, type_salle)
SELECT e.id, 'Amphithéâtre', 120, 'AMPHITHEATRE'
FROM edef d JOIN etablissement e ON e.code_officiel = d.code WHERE d.groupe = 3;

-- ---------- 7. Enseignants : un par matière et par établissement ----------
CREATE TEMP TABLE t_ens AS
SELECT d.code AS etab_code, mc.matiere, row_number() OVER (PARTITION BY d.code ORDER BY mc.matiere) AS n
FROM edef d
CROSS JOIN LATERAL (SELECT DISTINCT m.matiere FROM t_matiere_cycle m WHERE m.cycle = ANY (string_to_array(d.cycles, ','))) mc;

INSERT INTO t_u
SELECT 'ens.' || substr(t.etab_code, 4) || '.' || lpad(t.n::text, 2, '0') || '@demo.gn', pg_temp.nom('ens' || t.etab_code || t.n),
       CASE WHEN pg_temp.r('enss' || t.etab_code || t.n, 3) = 0 THEN pg_temp.prenom_f('ensf' || t.etab_code || t.n) ELSE pg_temp.prenom_m('ensm' || t.etab_code || t.n) END,
       '+224 66' || (1 + pg_temp.r('et' || t.etab_code || t.n, 8)) || ' ' || lpad(pg_temp.r('ett' || t.etab_code || t.n, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('etu' || t.etab_code || t.n, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('etv' || t.etab_code || t.n, 100)::text, 2, '0'),
       'ENSEIGNANT', 'ETABLISSEMENT', NULL, NULL, t.etab_code
FROM t_ens t;
DO $$ BEGIN PERFORM pg_temp.creer_comptes(); END $$;

INSERT INTO enseignant (utilisateur_id, matricule)
SELECT u.id, 'ENS-' || substr(t.etab_code, 4) || '-' || lpad(t.n::text, 2, '0')
FROM t_ens t JOIN utilisateur u ON lower(u.email) = 'ens.' || substr(t.etab_code, 4) || '.' || lpad(t.n::text, 2, '0') || '@demo.gn';

-- Table de correspondance : (établissement, matière) -> enseignant
CREATE TEMP TABLE t_ens_map AS
SELECT e.id AS etab_id, m.id AS matiere_id, ens.id AS ens_id, u.id AS user_id, t.etab_code, t.matiere AS matiere_code
FROM t_ens t
JOIN etablissement e ON e.code_officiel = t.etab_code
JOIN matiere m ON m.code = t.matiere
JOIN utilisateur u ON lower(u.email) = 'ens.' || substr(t.etab_code, 4) || '.' || lpad(t.n::text, 2, '0') || '@demo.gn'
JOIN enseignant ens ON ens.utilisateur_id = u.id;

-- Postes budgétaires : un peu plus ou un peu moins que les enseignants en poste (alimente les mutations)
UPDATE etablissement e SET postes_enseignants =
    (SELECT count(*) FROM t_ens_map m WHERE m.etab_id = e.id) + (pg_temp.r('postes' || e.code_officiel, 5) - 2)
WHERE e.code_officiel LIKE 'GN-%';

-- ---------- 8. Élèves, inscriptions, parents ----------
CREATE TEMP TABLE t_cls AS
SELECT cl.id AS class_id, cl.etablissement_id AS etab_id, d.code AS etab_code, n.ordre, n.id AS niveau_id, cl.nom,
       c.code AS cycle_code, 14 + pg_temp.r('eff' || d.code || cl.nom, 12) AS effectif
FROM classe cl
JOIN niveau n ON n.id = cl.niveau_id
JOIN cycle_scolaire c ON c.id = n.cycle_id
JOIN etablissement e ON e.id = cl.etablissement_id
JOIN edef d ON d.code = e.code_officiel
WHERE cl.annee_scolaire_id = (SELECT courante FROM t_annee);

-- Clé de famille : un frère ou une sœur sur 25 partage le nom et les parents d'un autre élève (parfois dans une autre école)
CREATE FUNCTION pg_temp.famille(n bigint) RETURNS bigint LANGUAGE sql IMMUTABLE AS
$$ SELECT CASE WHEN n > 17 AND (n - 17) % 25 = 0 THEN n - 17 ELSE n END $$;

CREATE TEMP TABLE t_pup AS
SELECT row_number() OVER (ORDER BY c.class_id, g.i) AS n, c.class_id, c.etab_id, c.etab_code, c.ordre, c.niveau_id, c.cycle_code
FROM t_cls c CROSS JOIN LATERAL generate_series(1, c.effectif) g(i);
ALTER TABLE t_pup ADD COLUMN sexe char(1), ADD COLUMN prenom text, ADD COLUMN nom text, ADD COLUMN naissance date, ADD COLUMN matricule text;
UPDATE t_pup SET sexe = CASE WHEN pg_temp.r('sx' || n, 2) = 0 THEN 'F' ELSE 'M' END;
UPDATE t_pup SET prenom = CASE WHEN sexe = 'F' THEN pg_temp.prenom_f('pp' || n) ELSE pg_temp.prenom_m('pp' || n) END,
                 nom = pg_temp.nom('pf' || pg_temp.famille(n)),
                 naissance = make_date(2026 - (5 + ordre) - pg_temp.r('an' || n, 2), 1 + pg_temp.r('mo' || n, 12), 1 + pg_temp.r('jo' || n, 28)),
                 matricule = 'DEMO-' || lpad(n::text, 6, '0');

INSERT INTO eleve (matricule, nom, prenom, date_naissance, sexe)
SELECT matricule, nom, prenom, naissance, sexe FROM t_pup ORDER BY n;

-- Inscriptions acceptées de l'année courante
INSERT INTO inscription (eleve_id, etablissement_id, annee_scolaire_id, classe_id, statut, date_demande, date_decision, decide_par)
SELECT e.id, p.etab_id, (SELECT courante FROM t_annee), p.class_id, 'ACCEPTEE',
       date '2026-08-03' + pg_temp.r('dd' || p.n, 25), date '2026-08-03' + pg_temp.r('dd' || p.n, 25) + 1 + pg_temp.r('dc' || p.n, 8),
       u.id
FROM t_pup p
JOIN eleve e ON e.matricule = p.matricule
JOIN utilisateur u ON lower(u.email) = 'dir.' || substr(p.etab_code, 4) || '@demo.gn';

-- Élèves en attente de décision, refusés ou annulés (sans classe)
CREATE TEMP TABLE t_extra AS
SELECT row_number() OVER (ORDER BY d.code, g.i) AS n, e.id AS etab_id, d.code AS etab_code, d.cycles, g.i,
       CASE WHEN g.i <= 1 + pg_temp.r('att' || d.code, 3) THEN 'EN_ATTENTE'
            WHEN g.i = 4 THEN 'REFUSEE' ELSE 'ANNULEE' END AS statut
FROM edef d
JOIN etablissement e ON e.code_officiel = d.code
CROSS JOIN LATERAL generate_series(1, CASE WHEN d.groupe = 1 THEN 3 ELSE 5 END) g(i)
WHERE d.statut = 'ACTIF' AND (g.i <= 3 OR pg_temp.r('ex' || d.code || g.i, 2) = 0);
ALTER TABLE t_extra ADD COLUMN niveau_ordre int, ADD COLUMN sexe char(1), ADD COLUMN prenom text, ADD COLUMN nom text, ADD COLUMN matricule text;
UPDATE t_extra x SET niveau_ordre = (SELECT n.ordre FROM niveau n JOIN cycle_scolaire c ON c.id = n.cycle_id
                                      WHERE n.code LIKE 'GN-%' AND c.code = (string_to_array(x.cycles, ','))[1]
                                      ORDER BY n.ordre LIMIT 1 OFFSET pg_temp.r('xo' || x.n, 3)),
                     sexe = CASE WHEN pg_temp.r('xs' || x.n, 2) = 0 THEN 'F' ELSE 'M' END,
                     matricule = 'DEMO-X' || lpad(x.n::text, 5, '0');
UPDATE t_extra SET niveau_ordre = COALESCE(niveau_ordre, 1),
                   prenom = CASE WHEN sexe = 'F' THEN pg_temp.prenom_f('xp' || n) ELSE pg_temp.prenom_m('xp' || n) END,
                   nom = pg_temp.nom('xn' || n);
INSERT INTO eleve (matricule, nom, prenom, date_naissance, sexe)
SELECT matricule, nom, prenom, make_date(2026 - (5 + niveau_ordre) - pg_temp.r('xa' || n, 2), 1 + pg_temp.r('xm' || n, 12), 1 + pg_temp.r('xj' || n, 28)), sexe
FROM t_extra ORDER BY n;
INSERT INTO inscription (eleve_id, etablissement_id, annee_scolaire_id, classe_id, statut, date_demande, date_decision, decide_par)
SELECT e.id, x.etab_id, (SELECT courante FROM t_annee), NULL, x.statut,
       date '2026-09-01' + pg_temp.r('xd' || x.n, 50),
       CASE WHEN x.statut = 'EN_ATTENTE' THEN NULL ELSE date '2026-09-01' + pg_temp.r('xd' || x.n, 50) + 3 END,
       CASE WHEN x.statut = 'EN_ATTENTE' THEN NULL ELSE u.id END
FROM t_extra x
JOIN eleve e ON e.matricule = x.matricule
JOIN utilisateur u ON lower(u.email) = 'dir.' || substr(x.etab_code, 4) || '@demo.gn';

-- Transferts : une inscription TRANSFEREE dans une autre école de même niveau, en plus de l'inscription acceptée
INSERT INTO inscription (eleve_id, etablissement_id, annee_scolaire_id, classe_id, statut, date_demande, date_decision, decide_par)
SELECT e.id, c2.etab_id, (SELECT courante FROM t_annee), c2.class_id, 'TRANSFEREE', date '2026-08-10', date '2026-08-12',
       (SELECT u.id FROM utilisateur u WHERE lower(u.email) = 'dir.' || substr(c2.etab_code, 4) || '@demo.gn')
FROM t_pup p
JOIN eleve e ON e.matricule = p.matricule
CROSS JOIN LATERAL (SELECT c.* FROM t_cls c WHERE c.niveau_id = p.niveau_id AND c.etab_id <> p.etab_id
                    ORDER BY pg_temp.r('tr' || p.n || '-' || c.class_id, 1000) LIMIT 1) c2
WHERE p.n % 150 = 5;

-- Année précédente : l'élève était dans la classe du niveau inférieur de la même école, quand elle existe
INSERT INTO inscription (eleve_id, etablissement_id, annee_scolaire_id, classe_id, statut, date_demande, date_decision, decide_par)
SELECT e.id, p.etab_id, (SELECT precedente FROM t_annee), cp.id, 'ACCEPTEE', date '2025-08-04' + pg_temp.r('hd' || p.n, 20), date '2025-08-20' + pg_temp.r('hd' || p.n, 10),
       (SELECT u.id FROM utilisateur u WHERE lower(u.email) = 'dir.' || substr(p.etab_code, 4) || '@demo.gn')
FROM t_pup p
JOIN eleve e ON e.matricule = p.matricule
JOIN LATERAL (SELECT cl.id FROM classe cl JOIN niveau n ON n.id = cl.niveau_id
              WHERE cl.etablissement_id = p.etab_id AND cl.annee_scolaire_id = (SELECT precedente FROM t_annee) AND n.ordre = p.ordre - 1
              ORDER BY pg_temp.r('hc' || p.n || '-' || cl.id, 1000) LIMIT 1) cp ON TRUE
WHERE p.ordre >= 2;

-- Parents : un compte par famille (80 %), un second parent pour une famille sur sept
CREATE TEMP TABLE t_par AS
SELECT DISTINCT pg_temp.famille(n) AS pk FROM t_pup WHERE pg_temp.famille(n) % 5 <> 4;
ALTER TABLE t_par ADD COLUMN pere bool, ADD COLUMN nom text, ADD COLUMN prenom text, ADD COLUMN tel text;
UPDATE t_par SET pere = pg_temp.r('pr' || pk, 2) = 0, nom = pg_temp.nom('pf' || pk);
UPDATE t_par SET prenom = CASE WHEN pere THEN pg_temp.prenom_m('pq' || pk) ELSE pg_temp.prenom_f('pq' || pk) END,
                 tel = CASE WHEN pg_temp.r('pt' || pk, 100) < 85
                            THEN '+224 6' || (20 + pg_temp.r('pu' || pk, 70)) || ' ' || lpad(pg_temp.r('pv' || pk, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('pw' || pk, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('px' || pk, 100)::text, 2, '0') END;
INSERT INTO t_u SELECT 'parent.' || pk || '@demo.gn', nom, prenom, tel, 'PARENT', 'PERSONNEL', NULL, NULL, NULL FROM t_par;
INSERT INTO t_u SELECT 'parent.' || pk || 'b@demo.gn', nom, CASE WHEN pere THEN pg_temp.prenom_f('pb' || pk) ELSE pg_temp.prenom_m('pb' || pk) END,
                       '+224 65' || (1 + pg_temp.r('bu' || pk, 8)) || ' ' || lpad(pg_temp.r('bv' || pk, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('bw' || pk, 100)::text, 2, '0') || ' ' || lpad(pg_temp.r('bx' || pk, 100)::text, 2, '0'),
                       'PARENT', 'PERSONNEL', NULL, NULL, NULL
FROM t_par WHERE pk % 7 = 0;
DO $$ BEGIN PERFORM pg_temp.creer_comptes(); END $$;

INSERT INTO lien_parental (eleve_id, utilisateur_id, relation, responsable_legal)
SELECT e.id, u.id, CASE WHEN pr.pere THEN 'Père' WHEN pg_temp.r('rel' || p.n, 12) = 0 THEN 'Tuteur' ELSE 'Mère' END, TRUE
FROM t_pup p
JOIN t_par pr ON pr.pk = pg_temp.famille(p.n)
JOIN eleve e ON e.matricule = p.matricule
JOIN utilisateur u ON lower(u.email) = 'parent.' || pr.pk || '@demo.gn';
INSERT INTO lien_parental (eleve_id, utilisateur_id, relation, responsable_legal)
SELECT e.id, u.id, CASE WHEN pr.pere THEN 'Mère' ELSE 'Père' END, FALSE
FROM t_pup p
JOIN t_par pr ON pr.pk = pg_temp.famille(p.n) AND pr.pk % 7 = 0
JOIN eleve e ON e.matricule = p.matricule
JOIN utilisateur u ON lower(u.email) = 'parent.' || pr.pk || 'b@demo.gn';

-- Comptes élèves (collège et lycée) : un élève sur douze
INSERT INTO t_u SELECT 'eleve.' || n || '@demo.gn', nom, prenom, NULL, 'ELEVE', 'PERSONNEL', NULL, NULL, NULL
FROM t_pup WHERE ordre >= 9 AND n % 12 = 0;
DO $$ BEGIN PERFORM pg_temp.creer_comptes(); END $$;
UPDATE eleve e SET utilisateur_id = u.id
FROM t_pup p JOIN utilisateur u ON lower(u.email) = 'eleve.' || p.n || '@demo.gn'
WHERE e.matricule = p.matricule;

-- Correspondance élève / inscription courante, utilisée par les sections suivantes
CREATE TEMP TABLE t_insc AS
SELECT p.n, p.class_id, p.etab_id, p.etab_code, p.ordre, p.cycle_code, e.id AS eleve_id, i.id AS inscription_id,
       (SELECT u.id FROM lien_parental l JOIN utilisateur u ON u.id = l.utilisateur_id
         WHERE l.eleve_id = e.id AND l.responsable_legal ORDER BY l.id LIMIT 1) AS parent_user_id
FROM t_pup p
JOIN eleve e ON e.matricule = p.matricule
JOIN inscription i ON i.eleve_id = e.id AND i.annee_scolaire_id = (SELECT courante FROM t_annee) AND i.statut = 'ACCEPTEE';
CREATE INDEX ON t_insc (class_id);

-- ---------- 9. Pédagogie : affectations, coefficients, emplois du temps, évaluations et notes ----------
-- Toutes les classes (année précédente et courante) avec la liste de leurs matières et de leurs enseignants
CREATE TEMP TABLE t_cm AS
SELECT cl.id AS class_id, cl.annee_scolaire_id AS annee_id, cl.etablissement_id AS etab_id, e.code_officiel AS etab_code,
       c.code AS cycle_code, n.ordre, m.id AS matiere_id, m.code AS matiere_code, em.ens_id, em.user_id, te.n AS tn,
       (dense_rank() OVER (PARTITION BY cl.etablissement_id, cl.annee_scolaire_id ORDER BY n.ordre, cl.nom) - 1) AS ci
FROM classe cl
JOIN etablissement e ON e.id = cl.etablissement_id AND e.code_officiel LIKE 'GN-%'
JOIN niveau n ON n.id = cl.niveau_id
JOIN cycle_scolaire c ON c.id = n.cycle_id
JOIN t_matiere_cycle mc ON mc.cycle = c.code
JOIN matiere m ON m.code = mc.matiere
JOIN t_ens_map em ON em.etab_id = cl.etablissement_id AND em.matiere_id = m.id
JOIN t_ens te ON te.etab_code = e.code_officiel AND te.matiere = m.code;
CREATE INDEX ON t_cm (class_id);

INSERT INTO affectation_enseignant (enseignant_id, classe_id, matiere_id)
SELECT ens_id, class_id, matiere_id FROM t_cm;

INSERT INTO coefficient_matiere (classe_id, matiere_id, coefficient)
SELECT class_id, matiere_id,
       CASE matiere_code WHEN 'MATHEMATIQUES' THEN 4 WHEN 'FRANCAIS' THEN 4 WHEN 'PHYSIQUE_CHIMIE' THEN 3 WHEN 'PHILOSOPHIE' THEN 3
            WHEN 'SVT' THEN 2 WHEN 'HIST_GEO' THEN 2 WHEN 'ANGLAIS' THEN 2 WHEN 'EVEIL_SCIENTIFIQUE' THEN 2 ELSE 1 END
FROM t_cm WHERE annee_id = (SELECT courante FROM t_annee);

-- Emploi du temps de l'année courante. Le créneau dépend de la classe (ci) et de la matière (tn) : un enseignant
-- n'a jamais deux classes en même temps et chaque classe a sa propre salle.
INSERT INTO creneau (classe_id, matiere_id, enseignant_id, salle_id, jour_semaine, heure_debut, heure_fin)
SELECT cm.class_id, cm.matiere_id, cm.ens_id, s.id,
       (1 + ((cm.ci + cm.tn * 7) % 30) / 6)::int,
       make_time((8 + ((cm.ci + cm.tn * 7) % 30) % 6)::int, 0, 0),
       make_time((9 + ((cm.ci + cm.tn * 7) % 30) % 6)::int, 0, 0)
FROM t_cm cm
JOIN salle s ON s.etablissement_id = cm.etab_id AND s.nom = 'Salle ' || (cm.ci + 1)
WHERE cm.annee_id = (SELECT courante FROM t_annee);

-- Évaluations et notes. Année courante : première période en cours (3 évaluations par matière).
-- Année précédente : une composition par matière et par période.
CREATE FUNCTION pg_temp.note(eleve bigint, ev bigint, matiere bigint, etab bigint) RETURNS numeric LANGUAGE sql IMMUTABLE AS $$
    SELECT LEAST(20, GREATEST(0, round((6 + pg_temp.r('ab' || eleve, 100) / 10.0
                                          + (pg_temp.r('sf' || etab, 30) / 10.0 - 1.5)
                                          + (pg_temp.r('su' || eleve || '-' || matiere, 40) / 10.0 - 2)
                                          + (pg_temp.r('nz' || eleve || '-' || ev, 60) / 10.0 - 3)) * 4) / 4))::numeric(5,2) $$;

WITH ins AS (
    INSERT INTO evaluation (classe_id, matiere_id, periode_id, enseignant_id, titre, date_evaluation, coefficient, bareme)
    SELECT cm.class_id, cm.matiere_id, p.id, cm.ens_id, ev.titre, p.date_debut + ev.jour, ev.coef, 20
    FROM t_cm cm
    JOIN periode p ON p.etablissement_id = cm.etab_id AND p.annee_scolaire_id = cm.annee_id AND p.ordre = 1
    CROSS JOIN (VALUES ('Interrogation écrite', 25, 1), ('Devoir surveillé', 40, 1), ('Composition', 55, 2)) ev(titre, jour, coef)
    WHERE cm.annee_id = (SELECT courante FROM t_annee)
    RETURNING id, classe_id, matiere_id
)
INSERT INTO note (evaluation_id, inscription_id, valeur, absent, modifie_par)
SELECT ins.id, i.id, CASE WHEN a.absent THEN NULL ELSE pg_temp.note(i.eleve_id, ins.id, ins.matiere_id, i.etablissement_id) END,
       a.absent, cm.user_id
FROM ins
JOIN t_cm cm ON cm.class_id = ins.classe_id AND cm.matiere_id = ins.matiere_id
JOIN inscription i ON i.classe_id = ins.classe_id AND i.statut = 'ACCEPTEE'
CROSS JOIN LATERAL (SELECT pg_temp.r('abs' || i.id || '-' || ins.id, 100) < 3 AS absent) a;

WITH ins AS (
    INSERT INTO evaluation (classe_id, matiere_id, periode_id, enseignant_id, titre, date_evaluation, coefficient, bareme)
    SELECT cm.class_id, cm.matiere_id, p.id, cm.ens_id, 'Composition ' || p.ordre, p.date_fin - 14, 2, 20
    FROM t_cm cm
    JOIN periode p ON p.etablissement_id = cm.etab_id AND p.annee_scolaire_id = cm.annee_id
    WHERE cm.annee_id = (SELECT precedente FROM t_annee)
    RETURNING id, classe_id, matiere_id
)
INSERT INTO note (evaluation_id, inscription_id, valeur, absent, modifie_par)
SELECT ins.id, i.id, CASE WHEN a.absent THEN NULL ELSE pg_temp.note(i.eleve_id, ins.id, ins.matiere_id, i.etablissement_id) END,
       a.absent, cm.user_id
FROM ins
JOIN t_cm cm ON cm.class_id = ins.classe_id AND cm.matiere_id = ins.matiere_id
JOIN inscription i ON i.classe_id = ins.classe_id AND i.statut = 'ACCEPTEE'
CROSS JOIN LATERAL (SELECT pg_temp.r('abs' || i.id || '-' || ins.id, 100) < 3 AS absent) a;

-- ---------- 10. Vie scolaire : devoirs, annonces, cahier de textes, présences, justifications, rendez-vous... ----------
CREATE TEMP TABLE t_dir AS
SELECT e.id AS etab_id, e.code_officiel AS etab_code, u.id AS user_id
FROM etablissement e JOIN utilisateur u ON lower(u.email) = 'dir.' || substr(e.code_officiel, 4) || '@demo.gn'
WHERE e.code_officiel LIKE 'GN-%';

-- Devoirs à la maison (un cours sur cinq) ; une partie est déjà marquée « fait » par les parents
INSERT INTO devoir (classe_id, matiere_id, enseignant_id, titre, consigne, date_limite, created_at)
SELECT cm.class_id, cm.matiere_id, cm.ens_id,
       pg_temp.pick(ARRAY['Exercices de révision', 'Lecture et résumé', 'Problèmes à résoudre', 'Exposé à préparer', 'Leçon à apprendre', 'Rédaction'], 'dv' || cm.class_id || cm.matiere_id),
       'Travail à rendre : faire les exercices demandés en classe et soigner la présentation du cahier.',
       date '2026-10-22' + pg_temp.r('dl' || cm.class_id || cm.matiere_id, 24),
       timestamptz '2026-10-19 09:00+00' + pg_temp.r('dc' || cm.class_id || cm.matiere_id, 9) * interval '1 day'
FROM t_cm cm
WHERE cm.annee_id = (SELECT courante FROM t_annee) AND pg_temp.r('dv?' || cm.class_id || cm.matiere_id, 100) < 20;

INSERT INTO devoir_fait (devoir_id, eleve_id, fait_le, par_utilisateur_id)
SELECT d.id, ti.eleve_id, d.created_at + interval '1 day 4 hours', ti.parent_user_id
FROM devoir d
JOIN t_insc ti ON ti.class_id = d.classe_id AND ti.parent_user_id IS NOT NULL
WHERE pg_temp.r('df' || d.id || '-' || ti.n, 100) < 45;

-- Annonces de classe
INSERT INTO annonce (classe_id, auteur_id, titre, contenu, created_at)
SELECT c.class_id, dr.user_id,
       pg_temp.pick(ARRAY['Réunion de parents', 'Calendrier des compositions', 'Rappel : tenue scolaire', 'Sortie pédagogique', 'Fermeture exceptionnelle', 'Remise des bulletins'], 'an' || c.class_id || g.i),
       'Chers parents, nous vous informons que cette annonce concerne la classe ' || c.nom || '. Merci de votre collaboration.',
       timestamptz '2026-10-14 08:00+00' + pg_temp.r('ad' || c.class_id || g.i, 14) * interval '1 day'
FROM t_cls c
JOIN t_dir dr ON dr.etab_id = c.etab_id
CROSS JOIN generate_series(1, 2) g(i)
WHERE g.i = 1 OR pg_temp.r('a2' || c.class_id, 2) = 0;

-- Cahier de textes
INSERT INTO cahier_texte (classe_id, matiere_id, enseignant_id, date_seance, contenu, created_at)
SELECT cm.class_id, cm.matiere_id, cm.ens_id,
       date '2026-10-05' + pg_temp.r('cj' || cm.class_id || cm.matiere_id || g.i, 5) + 7 * (g.i - 1),
       pg_temp.pick(ARRAY['Rappel de la leçon précédente et correction des exercices.', 'Cours : introduction du nouveau chapitre.',
                          'Travaux dirigés et exercices d''application.', 'Évaluation écrite et corrigé collectif.',
                          'Révision en vue de la composition.'], 'ct' || cm.class_id || cm.matiere_id || g.i),
       timestamptz '2026-10-05 17:00+00' + 7 * (g.i - 1) * interval '1 day'
FROM t_cm cm CROSS JOIN generate_series(1, 3) g(i)
WHERE cm.annee_id = (SELECT courante FROM t_annee) AND pg_temp.r('cx' || cm.class_id || cm.matiere_id, 100) < 30;

-- Présences : trois semaines de classe (12 au 30 octobre 2026) ; quelques élèves très absents
INSERT INTO presence (inscription_id, date_seance, statut, motif, justifie, saisi_par, created_at)
SELECT ti.inscription_id, d.jour, s.statut,
       CASE WHEN s.statut = 'ABSENT' AND s.just THEN pg_temp.pick(ARRAY['Maladie', 'Deuil familial', 'Rendez-vous médical', 'Voyage'], 'pm' || ti.n || d.jour) END,
       s.statut = 'ABSENT' AND s.just, dr.user_id, (d.jour + time '08:30') AT TIME ZONE 'UTC'
FROM t_insc ti
JOIN t_dir dr ON dr.etab_id = ti.etab_id
CROSS JOIN (SELECT dd::date AS jour FROM generate_series(date '2026-10-12', date '2026-10-30', interval '1 day') dd
            WHERE extract(isodow FROM dd) < 6) d
CROSS JOIN LATERAL (
    SELECT CASE WHEN x < CASE WHEN ti.n % 97 = 0 THEN 55 ELSE 5 END THEN 'ABSENT'
                WHEN x < CASE WHEN ti.n % 97 = 0 THEN 60 ELSE 10 END THEN 'RETARD' ELSE 'PRESENT' END AS statut,
           pg_temp.r('pj' || ti.n || d.jour, 100) < 25 AS just
    FROM (SELECT pg_temp.r('ps' || ti.n || d.jour, 100) AS x) q
) s;

-- Justifications d'absence déposées par les parents
INSERT INTO justification_absence (presence_id, soumise_par_id, motif, statut, reponse, traitee_par_id, created_at, traitee_le)
SELECT pr.id, ti.parent_user_id,
       pg_temp.pick(ARRAY['Enfant malade, certificat médical joint', 'Décès dans la famille', 'Problème de transport', 'Consultation à l''hôpital'], 'jm' || pr.id),
       s.statut,
       CASE s.statut WHEN 'ACCEPTEE' THEN 'Justification acceptée.' WHEN 'REFUSEE' THEN 'Motif insuffisant, merci de fournir un justificatif.' END,
       CASE WHEN s.statut <> 'EN_ATTENTE' THEN dr.user_id END,
       (pr.date_seance + time '18:00') AT TIME ZONE 'UTC',
       CASE WHEN s.statut <> 'EN_ATTENTE' THEN (pr.date_seance + 1 + time '10:00') AT TIME ZONE 'UTC' END
FROM presence pr
JOIN t_insc ti ON ti.inscription_id = pr.inscription_id AND ti.parent_user_id IS NOT NULL
JOIN t_dir dr ON dr.etab_id = ti.etab_id
CROSS JOIN LATERAL (SELECT CASE WHEN x < 25 THEN 'EN_ATTENTE' WHEN x < 65 THEN 'ACCEPTEE' ELSE 'REFUSEE' END AS statut
                    FROM (SELECT pg_temp.r('js' || pr.id, 100) AS x) q) s
WHERE pr.statut = 'ABSENT' AND NOT pr.justifie AND pg_temp.r('jx' || pr.id, 100) < 40;
UPDATE presence SET justifie = TRUE, motif = COALESCE(motif, 'Justifiée par la famille')
WHERE id IN (SELECT j.presence_id FROM justification_absence j JOIN presence p2 ON p2.id = j.presence_id
             JOIN t_insc ti ON ti.inscription_id = p2.inscription_id WHERE j.statut = 'ACCEPTEE');

-- Rendez-vous parents / enseignants
INSERT INTO rendez_vous (eleve_id, enseignant_id, demande_par_id, date_heure, motif, statut, reponse, created_at)
SELECT ti.eleve_id, cm.ens_id, ti.parent_user_id,
       timestamp '2026-11-02 14:00' + pg_temp.r('rd' || ti.n, 5) * interval '1 day' + pg_temp.r('rh' || ti.n, 4) * interval '1 hour',
       pg_temp.pick(ARRAY['Suivi des résultats', 'Comportement en classe', 'Orientation après le BEPC', 'Difficultés en mathématiques', 'Absences répétées'], 'rm' || ti.n),
       s.statut,
       CASE s.statut WHEN 'CONFIRME' THEN 'Rendez-vous confirmé, merci de vous présenter à l''accueil.' WHEN 'REFUSE' THEN 'Indisponible à cette date, merci de proposer un autre jour.' END,
       timestamptz '2026-10-24 09:00+00' + pg_temp.r('rc' || ti.n, 6) * interval '1 day'
FROM t_insc ti
JOIN LATERAL (SELECT c.ens_id FROM t_cm c WHERE c.class_id = ti.class_id ORDER BY pg_temp.r('rv' || ti.n || '-' || c.matiere_id, 1000) LIMIT 1) cm ON TRUE
CROSS JOIN LATERAL (SELECT CASE WHEN x < 40 THEN 'DEMANDE' WHEN x < 75 THEN 'CONFIRME' WHEN x < 85 THEN 'REFUSE' ELSE 'ANNULE' END AS statut
                    FROM (SELECT pg_temp.r('rs' || ti.n, 100) AS x) q) s
WHERE ti.parent_user_id IS NOT NULL AND pg_temp.r('rdv' || ti.n, 100) < 2;

-- Autorisations de sortie
INSERT INTO autorisation_sortie (eleve_id, etablissement_id, demandee_par_id, date_sortie, heure_sortie, motif, accompagnateur,
                                  statut, reponse, traitee_par_id, created_at, traitee_le)
SELECT ti.eleve_id, ti.etab_id, ti.parent_user_id, date '2026-10-26' + pg_temp.r('sd' || ti.n, 10),
       time '10:30' + pg_temp.r('sh' || ti.n, 6) * interval '30 minutes',
       pg_temp.pick(ARRAY['Rendez-vous médical', 'Cérémonie familiale', 'Visite de la famille à l''hôpital', 'Démarche administrative'], 'sm' || ti.n),
       pg_temp.pick(ARRAY['Le père', 'La mère', 'Un oncle', 'La tante'], 'sa' || ti.n),
       s.statut,
       CASE s.statut WHEN 'ACCEPTEE' THEN 'Sortie autorisée.' WHEN 'REFUSEE' THEN 'Sortie impossible pendant la composition.' END,
       CASE WHEN s.statut <> 'DEMANDEE' THEN dr.user_id END,
       timestamptz '2026-10-25 08:00+00' + pg_temp.r('sc' || ti.n, 5) * interval '1 day',
       CASE WHEN s.statut <> 'DEMANDEE' THEN timestamptz '2026-10-26 08:30+00' + pg_temp.r('sc' || ti.n, 5) * interval '1 day' END
FROM t_insc ti
JOIN t_dir dr ON dr.etab_id = ti.etab_id
CROSS JOIN LATERAL (SELECT CASE WHEN x < 35 THEN 'DEMANDEE' WHEN x < 80 THEN 'ACCEPTEE' ELSE 'REFUSEE' END AS statut
                    FROM (SELECT pg_temp.r('ss' || ti.n, 100) AS x) q) s
WHERE ti.parent_user_id IS NOT NULL AND pg_temp.r('sor' || ti.n, 100) < 2;

-- Messagerie parents / enseignants
CREATE TEMP TABLE t_conv AS
SELECT row_number() OVER (ORDER BY ti.n) AS k, ti.eleve_id, ti.parent_user_id AS parent_id, cm.user_id AS personnel_id,
       1 + pg_temp.r('cn' || ti.n, 3) AS nb, m.libelle AS matiere
FROM t_insc ti
JOIN LATERAL (SELECT c.user_id, c.matiere_id FROM t_cm c WHERE c.class_id = ti.class_id
              ORDER BY pg_temp.r('cv' || ti.n || '-' || c.matiere_id, 1000) LIMIT 1) cm ON TRUE
JOIN matiere m ON m.id = cm.matiere_id
WHERE ti.parent_user_id IS NOT NULL AND pg_temp.r('conv' || ti.n, 100) < 1;
DO $$
DECLARE c record; cid bigint; t0 timestamptz; textes text[]; dernier_auteur bigint;
BEGIN
    FOR c IN SELECT * FROM t_conv LOOP
        t0 := timestamptz '2026-10-20 08:00+00' + (c.k % 9) * interval '1 day';
        textes := ARRAY['Bonjour, je voudrais savoir comment mon enfant se comporte en ' || c.matiere || '.',
                        'Bonjour, il progresse mais doit s''appliquer davantage dans les exercices à la maison.',
                        'Merci beaucoup pour votre retour, nous allons suivre cela de près.'];
        dernier_auteur := CASE WHEN c.nb % 2 = 1 THEN c.parent_id ELSE c.personnel_id END;
        INSERT INTO conversation (eleve_id, parent_id, personnel_id, sujet, created_at, dernier_message_le, dernier_auteur_id,
                                  dernier_extrait, parent_lu_le, personnel_lu_le)
        VALUES (c.eleve_id, c.parent_id, c.personnel_id, 'Suivi en ' || c.matiere, t0, t0 + (c.nb - 1) * interval '3 hours',
                dernier_auteur, left(textes[c.nb], 140),
                CASE WHEN dernier_auteur = c.parent_id OR c.k % 2 = 0 THEN t0 + c.nb * interval '3 hours' END,
                CASE WHEN dernier_auteur = c.personnel_id OR c.k % 2 = 0 THEN t0 + c.nb * interval '3 hours' END)
        RETURNING id INTO cid;
        INSERT INTO message (conversation_id, auteur_id, contenu, created_at)
        SELECT cid, CASE WHEN i % 2 = 1 THEN c.parent_id ELSE c.personnel_id END, textes[i], t0 + (i - 1) * interval '3 hours'
        FROM generate_series(1, c.nb) i;
    END LOOP;
END $$;

-- ---------- 11. Finances : frais, factures, exonérations, paiements ----------
CREATE TEMP TABLE t_etab_k AS
SELECT e.id AS etab_id, d.code, d.secteur, d.cycles, d.groupe, row_number() OVER (ORDER BY d.code) AS k,
       pg_temp.r('cantine' || d.code, 2) = 0 AS cantine
FROM edef d JOIN etablissement e ON e.code_officiel = d.code;

INSERT INTO frais (etablissement_id, annee_scolaire_id, niveau_id, libelle, montant, devise, echeance, obligatoire, actif)
SELECT k.etab_id, (SELECT courante FROM t_annee), NULL, 'Frais d''inscription',
       CASE k.secteur WHEN 'PUBLIC' THEN 20000 ELSE 100000 END, 'GNF', date '2026-09-30', TRUE, TRUE
FROM t_etab_k k;
INSERT INTO frais (etablissement_id, annee_scolaire_id, niveau_id, libelle, montant, devise, echeance, obligatoire, actif)
SELECT DISTINCT k.etab_id, (SELECT courante FROM t_annee), cd.niveau_id, 'Scolarité',
       CASE WHEN k.secteur = 'PUBLIC' THEN (CASE WHEN cd.ordre <= 6 THEN 40000 WHEN cd.ordre <= 10 THEN 70000 ELSE 100000 END)
            ELSE (CASE WHEN cd.ordre <= 6 THEN 250000 WHEN cd.ordre <= 10 THEN 400000 ELSE 550000 END) END,
       'GNF', date '2026-10-15', TRUE, TRUE
FROM t_etab_k k JOIN t_cls_def cd ON cd.etab_id = k.etab_id;
INSERT INTO frais (etablissement_id, annee_scolaire_id, niveau_id, libelle, montant, devise, echeance, obligatoire, actif)
SELECT k.etab_id, (SELECT courante FROM t_annee), NULL, 'Cantine scolaire', 15000, 'GNF', date '2026-10-31', FALSE, TRUE
FROM t_etab_k k WHERE k.cantine;
INSERT INTO frais (etablissement_id, annee_scolaire_id, niveau_id, libelle, montant, devise, echeance, obligatoire, actif)
SELECT k.etab_id, (SELECT courante FROM t_annee), NULL, 'Tenue scolaire', 60000, 'GNF', date '2026-10-31', FALSE, TRUE
FROM t_etab_k k WHERE k.secteur = 'PRIVE';

CREATE FUNCTION pg_temp.num6(n bigint) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT repeat('0', greatest(0, 6 - length(n::text))) || n::text $$;

-- Factures des élèves inscrits (numéros uniques sur tout le réseau : une plage de 100 000 par école)
CREATE TEMP TABLE t_fac AS
SELECT ti.etab_id, ti.eleve_id, f.id AS frais_id, f.libelle, f.montant, f.echeance, k.k,
       row_number() OVER (PARTITION BY ti.etab_id ORDER BY ti.n, f.id) AS seq
FROM t_insc ti
JOIN t_etab_k k ON k.etab_id = ti.etab_id
JOIN classe cl ON cl.id = ti.class_id
JOIN frais f ON f.etablissement_id = ti.etab_id AND f.annee_scolaire_id = (SELECT courante FROM t_annee)
WHERE (f.niveau_id IS NULL AND f.libelle = 'Frais d''inscription')
   OR f.niveau_id = cl.niveau_id
   OR (f.libelle = 'Cantine scolaire' AND pg_temp.r('ca' || ti.n, 100) < 30)
   OR (f.libelle = 'Tenue scolaire' AND pg_temp.r('tu' || ti.n, 100) < 60);

INSERT INTO facture (frais_id, eleve_id, etablissement_id, annee_scolaire_id, numero, libelle, montant_du, montant_exonere,
                     devise, echeance, statut, date_emission)
SELECT t.frais_id, t.eleve_id, t.etab_id, (SELECT courante FROM t_annee),
       'FAC-2026-' || pg_temp.num6(t.k * 100000 + t.seq), t.libelle, t.montant, 0, 'GNF', t.echeance,
       CASE WHEN pg_temp.r('an' || t.frais_id || t.eleve_id, 100) < 1 THEN 'ANNULEE' ELSE 'IMPAYEE' END,
       date '2026-09-05' + pg_temp.r('de' || t.frais_id || t.eleve_id, 10)
FROM t_fac t;
INSERT INTO compteur_facture (etablissement_id, annee_scolaire_id, dernier_numero)
SELECT t.etab_id, (SELECT courante FROM t_annee), max(t.k * 100000 + t.seq) FROM t_fac t GROUP BY t.etab_id;

-- Exonérations (bourses, remises, aides sociales) sur 6 % des scolarités
INSERT INTO exoneration (facture_id, montant, motif, type_exoneration, decide_par, created_at)
SELECT f.id, round(f.montant_du * x.part / 100.0, 2),
       CASE x.typ WHEN 'BOURSE' THEN 'Bourse d''excellence' WHEN 'AIDE_SOCIALE' THEN 'Famille en difficulté' ELSE 'Remise fratrie' END,
       x.typ, dr.user_id, timestamptz '2026-09-20 10:00+00'
FROM facture f
JOIN t_dir dr ON dr.etab_id = f.etablissement_id
JOIN frais fr ON fr.id = f.frais_id AND fr.libelle = 'Scolarité'
CROSS JOIN LATERAL (SELECT pg_temp.pick(ARRAY['BOURSE', 'REMISE', 'AIDE_SOCIALE'], 'ex' || f.id) AS typ,
                           (ARRAY[25, 50, 100])[1 + pg_temp.r('ep' || f.id, 3)] AS part) x
WHERE f.statut <> 'ANNULEE' AND pg_temp.r('exo' || f.id, 100) < 6;
UPDATE facture f SET montant_exonere = e.total FROM (SELECT facture_id, sum(montant) AS total FROM exoneration GROUP BY facture_id) e
WHERE e.facture_id = f.id AND f.etablissement_id IN (SELECT etab_id FROM t_dir);

-- Paiements : intégral, partiel, en attente, en échec ou annulé
CREATE TEMP TABLE t_pay AS
SELECT f.id AS facture_id, f.etablissement_id, f.montant_du - f.montant_exonere AS du, f.date_emission,
       pg_temp.r('fp' || f.id, 100) AS x,
       pg_temp.pick(ARRAY['ESPECES', 'ESPECES', 'ESPECES', 'ESPECES', 'MOBILE_MONEY', 'MOBILE_MONEY', 'MOBILE_MONEY', 'VIREMENT', 'CHEQUE'], 'pmode' || f.id) AS mode
FROM facture f WHERE f.statut <> 'ANNULEE' AND f.montant_du - f.montant_exonere > 0 AND f.etablissement_id IN (SELECT etab_id FROM t_dir);

CREATE FUNCTION pg_temp.ref_ext(mode text, cle text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT CASE mode WHEN 'MOBILE_MONEY' THEN 'MM' || upper(substr(md5(cle), 1, 10)) WHEN 'VIREMENT' THEN 'VIR' || upper(substr(md5(cle), 1, 10))
                    WHEN 'CHEQUE' THEN 'CHQ' || lpad((abs(hashtextextended(cle, 1)) % 9000000 + 1000000)::text, 7, '0') END $$;

-- 1) paiements intégraux (45 %)
INSERT INTO paiement (facture_id, montant, devise, mode, statut, reference_interne, reference_externe, enregistre_par, created_at, valide_le)
SELECT p.facture_id, p.du, 'GNF', p.mode, 'VALIDE', 'PAY-' || upper(substr(md5('i' || p.facture_id), 1, 12)),
       pg_temp.ref_ext(p.mode, 'e' || p.facture_id), CASE WHEN p.mode = 'MOBILE_MONEY' THEN NULL ELSE sec.id END,
       (p.date_emission + 3 + pg_temp.r('pd' || p.facture_id, 20) + time '10:00') AT TIME ZONE 'UTC',
       (p.date_emission + 3 + pg_temp.r('pd' || p.facture_id, 20) + time '10:05') AT TIME ZONE 'UTC'
FROM t_pay p
JOIN etablissement e ON e.id = p.etablissement_id
JOIN utilisateur sec ON lower(sec.email) = 'sec.' || substr(e.code_officiel, 4) || '@demo.gn'
WHERE p.x < 45;
-- 2) paiements partiels (20 %) : une ou deux tranches
INSERT INTO paiement (facture_id, montant, devise, mode, statut, reference_interne, reference_externe, enregistre_par, created_at, valide_le)
SELECT p.facture_id, round(p.du * t.part / 100.0 / 1000) * 1000, 'GNF', p.mode, 'VALIDE',
       'PAY-' || upper(substr(md5('t' || t.tr || p.facture_id), 1, 12)), pg_temp.ref_ext(p.mode, 'p' || t.tr || p.facture_id),
       CASE WHEN p.mode = 'MOBILE_MONEY' THEN NULL ELSE sec.id END,
       (p.date_emission + 5 + t.tr * 9 + time '11:00') AT TIME ZONE 'UTC', (p.date_emission + 5 + t.tr * 9 + time '11:03') AT TIME ZONE 'UTC'
FROM t_pay p
JOIN etablissement e ON e.id = p.etablissement_id
JOIN utilisateur sec ON lower(sec.email) = 'sec.' || substr(e.code_officiel, 4) || '@demo.gn'
CROSS JOIN LATERAL (VALUES (1, 30 + pg_temp.r('t1' || p.facture_id, 30)), (2, 10 + pg_temp.r('t2' || p.facture_id, 10))) t(tr, part)
WHERE p.x >= 45 AND p.x < 65 AND (t.tr = 1 OR pg_temp.r('tr2' || p.facture_id, 2) = 0)
  AND round(p.du * t.part / 100.0 / 1000) * 1000 > 0;
-- 3) paiements en attente (mobile money), en échec, ou annulés
INSERT INTO paiement (facture_id, montant, devise, mode, statut, reference_interne, reference_externe, enregistre_par, created_at, valide_le,
                      annule_le, annule_par, motif_annulation)
SELECT p.facture_id, p.du, 'GNF', CASE WHEN p.x < 76 THEN 'MOBILE_MONEY' ELSE p.mode END,
       CASE WHEN p.x < 72 THEN 'EN_ATTENTE' WHEN p.x < 76 THEN 'ECHEC' ELSE 'ANNULE' END,
       'PAY-' || upper(substr(md5('z' || p.facture_id), 1, 12)),
       CASE WHEN p.x < 72 THEN NULL ELSE pg_temp.ref_ext(CASE WHEN p.x < 76 THEN 'MOBILE_MONEY' ELSE p.mode END, 'z' || p.facture_id) END,
       CASE WHEN p.x >= 76 AND p.mode <> 'MOBILE_MONEY' THEN sec.id END,
       (p.date_emission + 8 + time '09:00') AT TIME ZONE 'UTC',
       CASE WHEN p.x >= 76 THEN (p.date_emission + 8 + time '09:10') AT TIME ZONE 'UTC' END,
       CASE WHEN p.x >= 76 THEN (p.date_emission + 12 + time '15:00') AT TIME ZONE 'UTC' END,
       CASE WHEN p.x >= 76 THEN sec.id END,
       CASE WHEN p.x >= 76 THEN 'Erreur de saisie du montant' END
FROM t_pay p
JOIN etablissement e ON e.id = p.etablissement_id
JOIN utilisateur sec ON lower(sec.email) = 'sec.' || substr(e.code_officiel, 4) || '@demo.gn'
WHERE p.x >= 65 AND p.x < 80;
-- Un paiement annulé a été refait correctement
INSERT INTO paiement (facture_id, montant, devise, mode, statut, reference_interne, reference_externe, enregistre_par, created_at, valide_le)
SELECT p.facture_id, p.du, 'GNF', p.mode, 'VALIDE', 'PAY-' || upper(substr(md5('y' || p.facture_id), 1, 12)),
       pg_temp.ref_ext(p.mode, 'y' || p.facture_id), CASE WHEN p.mode = 'MOBILE_MONEY' THEN NULL ELSE sec.id END,
       (p.date_emission + 13 + time '10:00') AT TIME ZONE 'UTC', (p.date_emission + 13 + time '10:02') AT TIME ZONE 'UTC'
FROM t_pay p
JOIN etablissement e ON e.id = p.etablissement_id
JOIN utilisateur sec ON lower(sec.email) = 'sec.' || substr(e.code_officiel, 4) || '@demo.gn'
WHERE p.x >= 76 AND p.x < 80 AND pg_temp.r('refait' || p.facture_id, 2) = 0;

-- Statut des factures d'après les paiements validés (même règle que l'application)
UPDATE facture f SET statut = CASE
        WHEN f.statut = 'ANNULEE' THEN 'ANNULEE'
        WHEN q.paye = 0 AND f.montant_exonere = f.montant_du THEN 'EXONEREE'
        WHEN q.paye >= f.montant_du - f.montant_exonere THEN 'PAYEE'
        WHEN q.paye > 0 THEN 'PARTIELLE'
        ELSE 'IMPAYEE' END
FROM (SELECT f2.id, COALESCE(sum(p.montant) FILTER (WHERE p.statut = 'VALIDE'), 0) AS paye
      FROM facture f2 LEFT JOIN paiement p ON p.facture_id = f2.id GROUP BY f2.id) q
WHERE q.id = f.id AND f.etablissement_id IN (SELECT etab_id FROM t_dir);

-- ---------- 12. Conformité : inspections, signalements de sécurité, plans d'action, alertes ----------
CREATE TEMP TABLE t_reg AS
SELECT e.id AS etab_id, e.code_officiel AS code, u.id AS user_id, u.email
FROM etablissement e
JOIN zone_geographique c ON c.id = e.zone_id
JOIN zone_geographique p ON p.id = c.parent_id
JOIN zone_geographique r ON r.id = p.parent_id
JOIN utilisateur u ON lower(u.email) = 'dr.' || lower(translate(r.nom, 'éÉ', 'eE')) || '@demo.gn'
WHERE e.code_officiel LIKE 'GN-%';

-- Deux inspections par école : une ancienne, une récente (ajustées ensuite pour quelques cas particuliers)
INSERT INTO inspection (etablissement_id, inspecteur_id, date_inspection, resultat, observations, created_at)
SELECT r.etab_id, r.user_id, x.dt, x.resultat,
       CASE x.resultat WHEN 'CONFORME' THEN pg_temp.pick(ARRAY['Locaux propres, effectifs conformes, registres à jour.', 'Bon fonctionnement général, quelques réparations mineures à prévoir.', 'Enseignants présents, classes bien tenues.'], 'ob' || r.code || x.i)
                       ELSE pg_temp.pick(ARRAY['Toiture et clôture à réparer, latrines insuffisantes.', 'Salles surchargées et absence d''extincteurs.', 'Registres incomplets, point d''eau hors service.'], 'ob' || r.code || x.i) END,
       x.dt::timestamptz + interval '16 hours'
FROM t_reg r
CROSS JOIN LATERAL (
    SELECT i, CASE WHEN i = 1 THEN date '2025-10-06' + pg_temp.r('i1' || r.code, 40) ELSE date '2026-03-02' + pg_temp.r('i2' || r.code, 150) END AS dt,
           CASE WHEN pg_temp.r('ir' || r.code || i, 100) < CASE WHEN i = 1 THEN 35 ELSE 30 END THEN 'NON_CONFORME' ELSE 'CONFORME' END AS resultat
    FROM generate_series(1, 2) i
) x;
DELETE FROM inspection WHERE etablissement_id IN (SELECT id FROM etablissement WHERE code_officiel IN ('GN-0014', 'GN-0015'));           -- jamais inspectées
DELETE FROM inspection WHERE etablissement_id = (SELECT id FROM etablissement WHERE code_officiel = 'GN-0019') AND date_inspection > date '2026-01-01';
UPDATE inspection SET date_inspection = date '2025-05-14', created_at = timestamptz '2025-05-14 16:00+00'
WHERE etablissement_id = (SELECT id FROM etablissement WHERE code_officiel = 'GN-0019');                                             -- inspection de plus d'un an
UPDATE inspection SET resultat = 'NON_CONFORME', observations = 'Bâtiment dégradé : suspension recommandée.'
WHERE etablissement_id IN (SELECT id FROM etablissement WHERE code_officiel IN ('GN-0026', 'GN-0028', 'GN-0009'))
  AND date_inspection = (SELECT max(i2.date_inspection) FROM inspection i2 WHERE i2.etablissement_id = inspection.etablissement_id);

-- Pièces jointes de quelques inspections (petits fichiers texte)
INSERT INTO piece_jointe (inspection_id, ajoutee_par_id, nom, type_mime, taille, contenu, created_at)
SELECT i.id, i.inspecteur_id, 'rapport-inspection-' || i.id || '.txt', 'text/plain', octet_length(t.txt), t.txt, i.created_at + interval '1 hour'
FROM inspection i
CROSS JOIN LATERAL (SELECT convert_to('Rapport d''inspection du ' || i.date_inspection || ' - résultat : ' || i.resultat || E'\n' || coalesce(i.observations, ''), 'UTF8') AS txt) t
WHERE i.resultat = 'NON_CONFORME' AND pg_temp.r('pj' || i.id, 100) < 60;

-- Signalements de sécurité
INSERT INTO signalement_securite (etablissement_id, signale_par_id, categorie, gravite, description, statut, created_at, resolu_at)
SELECT dr.etab_id, dr.user_id, x.categorie, x.gravite, x.description, x.statut,
       x.cree, CASE WHEN x.statut = 'RESOLU' THEN x.cree + (3 + pg_temp.r('sr' || dr.etab_code, 30)) * interval '1 day' END
FROM t_dir dr
JOIN etablissement e ON e.id = dr.etab_id AND e.statut = 'ACTIF'
CROSS JOIN LATERAL (
    SELECT pg_temp.pick(ARRAY['INCENDIE', 'BATIMENT', 'EAU_HYGIENE', 'CLOTURE', 'AUTRE'], 'sc' || dr.etab_code) AS categorie,
           (ARRAY['FAIBLE', 'MOYENNE', 'ELEVEE'])[1 + pg_temp.r('sg' || dr.etab_code, 3)] AS gravite,
           pg_temp.pick(ARRAY['Fissures importantes sur le mur d''une salle de classe.', 'Pas d''extincteur dans le bloc administratif.',
                              'Latrines en mauvais état, risque sanitaire.', 'Portail et clôture endommagés, intrusions possibles.',
                              'Câbles électriques apparents dans une salle.'], 'sd' || dr.etab_code) AS description,
           CASE WHEN pg_temp.r('ss' || dr.etab_code, 100) < 55 THEN 'OUVERT' ELSE 'RESOLU' END AS statut,
           timestamptz '2026-07-01 09:00+00' + pg_temp.r('st' || dr.etab_code, 100) * interval '1 day' AS cree
) x
WHERE pg_temp.r('sig' || dr.etab_code, 100) < 45;
INSERT INTO signalement_securite (etablissement_id, signale_par_id, categorie, gravite, description, statut, created_at)
SELECT dr.etab_id, dr.user_id, 'BATIMENT', 'ELEVEE', 'Toiture menaçant de s''effondrer sur deux salles de classe.', 'OUVERT', timestamptz '2026-10-20 10:00+00'
FROM t_dir dr WHERE dr.etab_code = 'GN-0009';
INSERT INTO signalement_securite (etablissement_id, signale_par_id, categorie, gravite, description, statut, created_at, resolu_at)
SELECT dr.etab_id, dr.user_id, 'INCENDIE', 'ELEVEE', 'Départ de feu dans le magasin, éteint par le voisinage.', 'RESOLU', timestamptz '2026-09-12 14:00+00', timestamptz '2026-09-30 12:00+00'
FROM t_dir dr WHERE dr.etab_code = 'GN-0017';

-- Plans d'action issus des inspections non conformes
INSERT INTO plan_action (etablissement_id, inspection_id, cree_par_id, description, echeance, statut, note, created_at, termine_le, dernier_rappel)
SELECT i.etablissement_id, i.id, i.inspecteur_id, x.description, x.echeance, x.statut,
       CASE x.statut WHEN 'EN_COURS' THEN 'Travaux commencés, matériaux commandés.' WHEN 'TERMINE' THEN 'Travaux réceptionnés.' WHEN 'ANNULE' THEN 'Remplacé par un autre plan.' END,
       i.created_at + interval '1 day',
       CASE WHEN x.statut = 'TERMINE' THEN i.created_at + interval '40 days' END,
       CASE WHEN x.statut IN ('A_FAIRE', 'EN_COURS') AND x.echeance < date '2026-10-31' THEN timestamptz '2026-10-26 07:00+00' END
FROM inspection i
CROSS JOIN LATERAL (
    SELECT j, pg_temp.pick(ARRAY['Réparer la toiture des salles 3 et 4', 'Installer des extincteurs et un plan d''évacuation', 'Construire des latrines supplémentaires',
                                 'Réhabiliter la clôture de l''établissement', 'Compléter les registres d''appel et de notes'], 'pl' || i.id || j) AS description,
           date '2026-08-15' + pg_temp.r('pe' || i.id || j, 140) AS echeance,
           (ARRAY['A_FAIRE', 'EN_COURS', 'EN_COURS', 'TERMINE', 'ANNULE'])[1 + pg_temp.r('pst' || i.id || j, 5)] AS statut
    FROM generate_series(1, 2) j
) x
WHERE i.resultat = 'NON_CONFORME' AND i.date_inspection >= date '2026-01-01';

-- État des alertes d'urgence (écoles dont un signalement grave est ouvert) et journal des alertes envoyées
INSERT INTO alerte_urgence_etat (etablissement_id, urgent, derniere_alerte)
SELECT DISTINCT s.etablissement_id, TRUE, timestamptz '2026-10-27 07:00+00'
FROM signalement_securite s WHERE s.statut = 'OUVERT' AND s.gravite = 'ELEVEE'
ON CONFLICT (etablissement_id) DO UPDATE SET urgent = TRUE, derniere_alerte = EXCLUDED.derniere_alerte;

INSERT INTO alerte_envoyee (envoyee_le, type, canal, destinataire, resume, reussie)
SELECT timestamptz '2026-10-27 07:00+00', 'URGENCE', 'EMAIL', r.email,
       'Établissement en situation d''urgence : ' || e.nom || ' (problème de sécurité grave non résolu)', TRUE
FROM alerte_urgence_etat a JOIN etablissement e ON e.id = a.etablissement_id JOIN t_reg r ON r.etab_id = e.id;
INSERT INTO alerte_envoyee (envoyee_le, type, canal, destinataire, resume, reussie)
SELECT p.dernier_rappel, 'PLAN_ACTION', 'EMAIL', d.email,
       'Plan d''action en retard : ' || left(p.description, 80) || ' (échéance ' || to_char(p.echeance, 'DD/MM/YYYY') || ')', pg_temp.r('pa' || p.id, 10) <> 0
FROM plan_action p
JOIN t_dir dr ON dr.etab_id = p.etablissement_id
JOIN utilisateur d ON d.id = dr.user_id
WHERE p.dernier_rappel IS NOT NULL;
-- SMS d'absence envoyés aux parents (une absence récente sur les élèves très absents)
INSERT INTO alerte_envoyee (envoyee_le, type, canal, destinataire, resume, reussie)
SELECT (pr.date_seance + time '10:00') AT TIME ZONE 'UTC', 'ABSENCE', 'SMS', u.email,
       'ABS#' || pr.id || ' ' || el.prenom || ' ' || el.nom || ' était absent(e) le ' || to_char(pr.date_seance, 'DD/MM/YYYY') || '. Contactez l''établissement en cas de besoin.',
       pg_temp.r('ae' || pr.id, 10) <> 0
FROM presence pr
JOIN t_insc ti ON ti.inscription_id = pr.inscription_id AND ti.n % 97 = 0 AND ti.parent_user_id IS NOT NULL
JOIN eleve el ON el.id = ti.eleve_id
JOIN utilisateur u ON u.id = ti.parent_user_id
WHERE pr.statut = 'ABSENT' AND u.telephone IS NOT NULL AND pr.date_seance >= date '2026-10-26';

-- ---------- 13. Examens : modèles, calendrier, centres, candidats et résultats ----------
-- Conditions pour être centre d'examen : établissement actif, autorisé, au moins 3 salles et 60 places
UPDATE critere_centre_examen SET secteur_admis = 'TOUS', autorisation_requise = TRUE, salles_minimum = 3, capacite_minimum = 60, updated_at = now() WHERE id = 1;

CREATE TEMP TABLE t_ex (cle text, libelle text, annee text, cycle text, niveau text, org text, debut date, fin date, modele text);
INSERT INTO t_ex VALUES
 ('CEE26',  'CEE 2026',                           '2025-2026', 'PRIMAIRE', 'GN-06', 'PREFECTURE', date '2026-06-08', date '2026-06-12', 'CEE'),
 ('BEPC26', 'BEPC 2026',                          '2025-2026', 'COLLEGE',  'GN-10', 'REGION',     date '2026-06-15', date '2026-06-19', 'BEPC'),
 ('BAC26',  'Baccalauréat 2026',                  '2025-2026', 'LYCEE',    'GN-13', 'NATIONAL',   date '2026-06-22', date '2026-06-26', 'BAC'),
 ('RENT',   'Évaluation de rentrée 6e année',     '2026-2027', 'PRIMAIRE', 'GN-06', 'PREFECTURE', date '2026-10-05', date '2026-10-09', 'RENT'),
 ('CEE27',  'CEE 2027',                           '2026-2027', 'PRIMAIRE', 'GN-06', 'PREFECTURE', date '2027-06-07', date '2027-06-11', 'CEE'),
 ('BEPC27', 'BEPC 2027',                          '2026-2027', 'COLLEGE',  'GN-10', 'REGION',     date '2027-06-14', date '2027-06-18', 'BEPC'),
 ('BAC27',  'Baccalauréat 2027',                  '2026-2027', 'LYCEE',    'GN-13', 'NATIONAL',   date '2027-06-21', date '2027-06-25', 'BAC'),
 ('COMM27', 'Évaluation communale 6e année',      '2026-2027', 'PRIMAIRE', 'GN-06', 'COMMUNE',    date '2027-05-03', date '2027-05-07', 'COMM'),
 ('FA27',   'CEE franco-arabe 2027',              '2026-2027', 'PRIMAIRE', 'GN-06', 'REGION',     date '2027-06-07', date '2027-06-10', 'FA');

INSERT INTO examen_national (libelle, annee_scolaire_id, cycle_id, niveau_id, niveau_organisation, date_debut, date_fin)
SELECT x.libelle, a.id, c.id, n.id, x.org, x.debut, x.fin
FROM t_ex x
JOIN annee_scolaire a ON a.libelle = x.annee
JOIN cycle_scolaire c ON c.code = x.cycle
JOIN niveau n ON n.code = x.niveau;
CREATE TEMP TABLE t_exid AS
SELECT x.cle, x.modele, x.annee, x.debut, x.fin, x.cycle, x.niveau, x.org, en.id AS examen_id
FROM t_ex x JOIN examen_national en ON en.libelle = x.libelle AND en.annee_scolaire_id = (SELECT id FROM annee_scolaire WHERE libelle = x.annee);

-- Cet examen ne concerne que les établissements franco-arabes
INSERT INTO examen_type_etablissement (examen_id, type_etablissement_id)
SELECT t.examen_id, (SELECT id FROM type_etablissement WHERE code = 'FRANCO_ARABE') FROM t_exid t WHERE t.cle = 'FA27';

-- Emploi du temps des épreuves (jour = décalage par rapport au premier jour)
CREATE TEMP TABLE t_ep (modele text, jour int, matiere text, libelle text, debut time, fin time);
INSERT INTO t_ep VALUES
 ('CEE', 0, 'FRANCAIS', NULL, '08:00', '10:00'), ('CEE', 0, 'MATHEMATIQUES', NULL, '10:30', '12:30'),
 ('CEE', 1, 'EVEIL_SCIENTIFIQUE', NULL, '08:00', '10:00'), ('CEE', 2, NULL, 'Dictée', '08:00', '09:00'), ('CEE', 2, 'EDUC_CIVIQUE', NULL, '09:30', '10:30'),
 ('BEPC', 0, 'FRANCAIS', NULL, '08:00', '11:00'), ('BEPC', 0, 'ANGLAIS', NULL, '14:00', '16:00'), ('BEPC', 1, 'MATHEMATIQUES', NULL, '08:00', '11:00'),
 ('BEPC', 2, 'PHYSIQUE_CHIMIE', NULL, '08:00', '10:00'), ('BEPC', 2, 'SVT', NULL, '10:30', '12:30'), ('BEPC', 3, 'HIST_GEO', NULL, '08:00', '10:00'),
 ('BAC', 0, 'PHILOSOPHIE', NULL, '08:00', '12:00'), ('BAC', 1, 'MATHEMATIQUES', NULL, '08:00', '12:00'), ('BAC', 2, 'PHYSIQUE_CHIMIE', NULL, '08:00', '11:00'),
 ('BAC', 2, 'SVT', NULL, '14:00', '17:00'), ('BAC', 3, 'FRANCAIS', NULL, '08:00', '11:00'), ('BAC', 4, 'ANGLAIS', NULL, '08:00', '10:00'), ('BAC', 4, 'HIST_GEO', NULL, '14:00', '16:00'),
 ('RENT', 0, 'FRANCAIS', NULL, '08:00', '10:00'), ('RENT', 1, 'MATHEMATIQUES', NULL, '08:00', '10:00'),
 ('COMM', 0, 'FRANCAIS', NULL, '08:00', '10:00'), ('COMM', 1, 'MATHEMATIQUES', NULL, '08:00', '10:00'),
 ('FA', 0, 'ARABE', NULL, '08:00', '10:00'), ('FA', 1, 'FRANCAIS', NULL, '08:00', '10:00'), ('FA', 2, 'MATHEMATIQUES', NULL, '08:00', '10:00');
INSERT INTO epreuve_examen (examen_id, matiere_id, libelle, date_epreuve, heure_debut, heure_fin, created_at)
SELECT t.examen_id, m.id, p.libelle, t.debut + p.jour, p.debut, p.fin, now()
FROM t_exid t JOIN t_ep p ON p.modele = t.modele LEFT JOIN matiere m ON m.code = p.matiere;

-- Établissements concernés par chaque examen (cycle proposé, types participants, établissement actif)
CREATE TEMP TABLE t_conc AS
SELECT t.cle, t.examen_id, e.id AS etab_id, e.code_officiel AS code, e.secteur, e.zone_id
FROM t_exid t
JOIN etablissement e ON e.code_officiel LIKE 'GN-%' AND e.statut = 'ACTIF'
WHERE EXISTS (SELECT 1 FROM etablissement_cycle ec JOIN cycle_scolaire c ON c.id = ec.cycle_id WHERE ec.etablissement_id = e.id AND c.code = t.cycle)
  AND (NOT EXISTS (SELECT 1 FROM examen_type_etablissement x WHERE x.examen_id = t.examen_id)
       OR EXISTS (SELECT 1 FROM examen_type_etablissement x WHERE x.examen_id = t.examen_id AND x.type_etablissement_id = e.type_etablissement_id));

-- Centres d'examen : dans chaque commune, l'établissement éligible le mieux équipé (examens de l'année courante)
CREATE TEMP TABLE t_sal AS SELECT etablissement_id AS etab_id, count(*) AS nb, sum(capacite) AS cap FROM salle GROUP BY 1;
CREATE TEMP TABLE t_eligible AS
SELECT e.id AS etab_id, e.zone_id, e.nom, e.adresse, s.cap,
       row_number() OVER (PARTITION BY e.zone_id ORDER BY s.cap DESC, e.code_officiel) AS rang
FROM etablissement e JOIN t_sal s ON s.etab_id = e.id
WHERE e.code_officiel LIKE 'GN-%' AND e.statut = 'ACTIF' AND e.autorisation_statut = 'AUTORISE' AND s.nb >= 3 AND s.cap >= 60;
INSERT INTO centre_examen (examen_id, zone_id, nom, adresse, date_debut, created_at, etablissement_id)
SELECT t.examen_id, el.zone_id, el.nom, el.adresse, t.debut, now(), el.etab_id
FROM t_exid t JOIN t_eligible el ON el.rang = 1
WHERE t.annee = '2026-2027';

-- Candidats inscrits par les directions (≈ 88 % des élèves de la classe d'examen) et affectation aux centres de la commune
INSERT INTO candidat_examen (examen_id, eleve_id, etablissement_id, classe_id, centre_id, created_at)
SELECT c.examen_id, ti.eleve_id, ti.etab_id, ti.class_id,
       CASE WHEN t.cle = 'RENT' OR pg_temp.r('af' || c.examen_id || ti.n, 100) < 70
            THEN (SELECT ce.id FROM centre_examen ce JOIN etablissement ee ON ee.id = c.etab_id
                  WHERE ce.examen_id = c.examen_id AND ce.zone_id = ee.zone_id) END,
       timestamptz '2026-10-01 09:00+00' + pg_temp.r('ci' || ti.n, 20) * interval '1 day'
FROM t_conc c
JOIN t_exid t ON t.cle = c.cle AND t.annee = '2026-2027'
JOIN t_insc ti ON ti.etab_id = c.etab_id
JOIN classe cl ON cl.id = ti.class_id
JOIN niveau n ON n.id = cl.niveau_id AND n.code = t.niveau
WHERE pg_temp.r('cand' || c.examen_id || ti.n, 100) < 88;
UPDATE candidat_examen SET numero = 'EX' || examen_id || '-' || pg_temp.num6(id) WHERE numero IS NULL;

-- Résultats des examens de l'année précédente : 85 % des établissements les ont transmis
CREATE FUNCTION pg_temp.taux(modele text) RETURNS numeric LANGUAGE sql IMMUTABLE AS
$$ SELECT CASE modele WHEN 'CEE' THEN 0.78 WHEN 'BEPC' THEN 0.62 WHEN 'BAC' THEN 0.48 ELSE 0.70 END $$;
INSERT INTO resultat_examen (examen_id, etablissement_id, presentes, admis)
SELECT x.examen_id, x.etab_id, x.presentes, LEAST(x.presentes, round(x.presentes * x.taux)::int)
FROM (
    SELECT c.examen_id, c.etab_id,
           (CASE WHEN t.modele = 'BAC' THEN 8 ELSE 14 END + pg_temp.r('rp' || c.examen_id || c.etab_id, CASE WHEN t.modele = 'BAC' THEN 25 ELSE 35 END)) AS presentes,
           GREATEST(0.05, LEAST(0.99, pg_temp.taux(t.modele) + (pg_temp.r('rt' || c.examen_id || c.etab_id, 30) - 15) / 100.0
                                      + CASE WHEN c.secteur = 'PRIVE' THEN 0.08 ELSE 0 END)) AS taux
    FROM t_conc c JOIN t_exid t ON t.cle = c.cle AND t.annee = '2025-2026'
    WHERE pg_temp.r('rm' || c.examen_id || c.etab_id, 100) < 85
) x;

-- Évaluation de rentrée (octobre 2026) : seule une partie des écoles a déjà transmis ses résultats, les autres seront relancées
INSERT INTO resultat_examen (examen_id, etablissement_id, presentes, admis)
SELECT t.examen_id, k.etab_id, k.nb, LEAST(k.nb, round(k.nb * (0.55 + pg_temp.r('rr' || k.etab_id, 35) / 100.0))::int)
FROM t_exid t
JOIN (SELECT examen_id, etablissement_id AS etab_id, count(*) AS nb FROM candidat_examen GROUP BY 1, 2) k ON k.examen_id = t.examen_id
WHERE t.cle = 'RENT' AND pg_temp.r('rg' || k.etab_id, 100) < 55;

-- Statistiques détaillées : absents, éliminés pour fraude, filles (renseignées par 80 % des écoles) et séries du baccalauréat
UPDATE resultat_examen r SET
    absents = round(r.presentes * (0.03 + pg_temp.r('ab' || r.id, 8) / 100.0))::int,
    elimines = CASE WHEN pg_temp.r('el' || r.id, 100) < 15 THEN 1 + pg_temp.r('en' || r.id, 2) ELSE 0 END
WHERE r.examen_id IN (SELECT examen_id FROM t_exid);

UPDATE resultat_examen r SET presentes_filles = f.pf, admis_filles = f.af
FROM (
    SELECT g.id, g.pf,
           LEAST(g.pf, g.admis, GREATEST(g.admis - (g.presentes - g.pf),
                 round(g.admis * g.pf::numeric / g.presentes * (0.85 + pg_temp.r('fa' || g.id, 25) / 100.0))::int)) AS af
    FROM (
        SELECT r2.id, r2.presentes, r2.admis,
               round(r2.presentes * (0.40 + pg_temp.r('fp' || r2.id, 15) / 100.0))::int AS pf
        FROM resultat_examen r2
        WHERE r2.examen_id IN (SELECT examen_id FROM t_exid) AND r2.presentes > 0 AND pg_temp.r('fs' || r2.id, 100) < 80
    ) g
) f
WHERE r.id = f.id;

-- Baccalauréat 2026 : répartition par série (sciences mathématiques, sociales, expérimentales)
CREATE TEMP TABLE t_serie AS
SELECT r.id AS resultat_id, r.presentes, r.admis,
       round(r.presentes * 0.30)::int AS p1, round(r.presentes * 0.40)::int AS p2,
       r.presentes - round(r.presentes * 0.30)::int - round(r.presentes * 0.40)::int AS p3,
       (r.admis * round(r.presentes * 0.30)::int) / r.presentes AS a1,
       (r.admis * round(r.presentes * 0.40)::int) / r.presentes AS a2,
       0 AS a3, 0 AS reste
FROM resultat_examen r
JOIN t_exid t ON t.examen_id = r.examen_id
JOIN t_ex x ON x.cle = t.cle AND x.modele = 'BAC'
WHERE r.presentes > 0;
UPDATE t_serie SET a3 = LEAST(p3, admis - a1 - a2);
UPDATE t_serie SET reste = admis - a1 - a2 - a3;
-- Le reste d'arrondi est placé dans les séries qui ont encore de la place
UPDATE t_serie SET a1 = a1 + LEAST(reste, p1 - a1), reste = reste - LEAST(reste, p1 - a1);
UPDATE t_serie SET a2 = a2 + LEAST(reste, p2 - a2), reste = reste - LEAST(reste, p2 - a2);
UPDATE t_serie SET a3 = a3 + LEAST(reste, p3 - a3), reste = reste - LEAST(reste, p3 - a3);
INSERT INTO resultat_examen_serie (resultat_id, serie, presentes, admis)
SELECT resultat_id, v.serie, v.p, v.a
FROM t_serie, LATERAL (VALUES ('SCIENCES_MATHEMATIQUES', p1, a1), ('SCIENCES_SOCIALES', p2, a2), ('SCIENCES_EXPERIMENTALES', p3, a3)) AS v(serie, p, a)
WHERE reste = 0 AND v.p > 0;

-- Jurys de délibération des examens 2026 : CEE proclamé, BEPC délibéré (en attente de proclamation), baccalauréat en cours (jury constitué)
UPDATE examen_national SET statut_resultats = 'PROCLAME', date_deliberation = date '2026-07-15', date_proclamation = date '2026-07-20'
WHERE libelle = 'CEE 2026' AND id IN (SELECT examen_id FROM t_exid);
UPDATE examen_national SET statut_resultats = 'DELIBERE', date_deliberation = date '2026-07-22'
WHERE libelle = 'BEPC 2026' AND id IN (SELECT examen_id FROM t_exid);

INSERT INTO jury_membre (examen_id, ordre, fonction, nom, present)
SELECT e.id, m.ordre, m.fonction, m.nom, m.present
FROM examen_national e
JOIN (VALUES
  ('CEE 2026', 1, 'Directeur préfectoral ou communal de l''éducation (président)', 'Mamadou Camara', TRUE),
  ('CEE 2026', 2, 'Chef de la section pédagogique', 'Fanta Soumah', TRUE),
  ('CEE 2026', 3, 'Chargé des examens', 'Ibrahima Bah', TRUE),
  ('CEE 2026', 4, 'Délégué scolaire de l''enseignement élémentaire', 'Aïssatou Barry', TRUE),
  ('CEE 2026', 5, 'Directeur d''école publique', 'Sékou Condé', TRUE),
  ('CEE 2026', 6, 'Directeur d''école privée', 'Mariama Diallo', TRUE),
  ('CEE 2026', 7, 'Enseignant', 'Alpha Keita', TRUE),
  ('CEE 2026', 8, 'Membre de l''APEAE préfectorale', 'Kadiatou Sylla', FALSE),
  ('BEPC 2026', 1, 'Inspecteur régional de l''éducation (président)', 'Ousmane Touré', TRUE),
  ('BEPC 2026', 2, 'Chef de la section pédagogique', 'Hawa Bangoura', TRUE),
  ('BEPC 2026', 3, 'Chargé des examens de l''IRE', 'Moussa Kourouma', TRUE),
  ('BEPC 2026', 4, 'Principal de collège public', 'Aminata Traoré', TRUE),
  ('BEPC 2026', 5, 'Principal de collège privé', 'Lansana Fofana', TRUE),
  ('BEPC 2026', 6, 'Enseignant', 'Namory Sidibé', FALSE),
  ('BEPC 2026', 7, 'Membre de l''APEAE régionale', 'Salématou Bah', FALSE),
  ('Baccalauréat 2026', 1, 'Directeur national de l''enseignement secondaire général et technique (président)', 'Abdoulaye Diakité', TRUE),
  ('Baccalauréat 2026', 2, 'Représentant du ministère de l''Enseignement supérieur (premier vice-président)', 'Mory Kaba', TRUE),
  ('Baccalauréat 2026', 3, 'Représentant du ministère de l''Enseignement technique (deuxième vice-président)', 'Fatoumata Doumbouya', TRUE),
  ('Baccalauréat 2026', 4, 'Directeur général des examens et du contrôle scolaire (rapporteur)', 'Thierno Baldé', TRUE),
  ('Baccalauréat 2026', 5, 'Proviseur de lycée public', 'Jean Loua', TRUE),
  ('Baccalauréat 2026', 6, 'Proviseur de lycée privé', 'Aïcha Camara', TRUE),
  ('Baccalauréat 2026', 7, 'Professeur de lycée', 'Boubacar Sow', TRUE),
  ('Baccalauréat 2026', 8, 'Membre de la FEGUIPAE', NULL, FALSE),
  ('Baccalauréat 2026', 9, 'Fondateur d''établissement privé', NULL, FALSE)
) AS m(libelle, ordre, fonction, nom, present) ON m.libelle = e.libelle
WHERE e.id IN (SELECT examen_id FROM t_exid);

INSERT INTO alerte_envoyee (envoyee_le, type, canal, destinataire, resume, reussie)
SELECT timestamptz '2026-10-26 07:00+00', 'RESULTATS_EXAMEN', 'EMAIL', d.email,
       'EXA#' || t.examen_id || '-' || k.etab_id || ' Les résultats de ' || t.cle_lib || ' n''ont pas été transmis par ' || e.nom, TRUE
FROM (SELECT t2.examen_id, t2.cle, x.libelle AS cle_lib FROM t_exid t2 JOIN t_ex x ON x.cle = t2.cle) t
JOIN (SELECT DISTINCT examen_id, etablissement_id AS etab_id FROM candidat_examen) k ON k.examen_id = t.examen_id
JOIN etablissement e ON e.id = k.etab_id
JOIN t_dir dr ON dr.etab_id = e.id
JOIN utilisateur d ON d.id = dr.user_id
WHERE t.cle = 'RENT'
  AND NOT EXISTS (SELECT 1 FROM resultat_examen r WHERE r.examen_id = t.examen_id AND r.etablissement_id = k.etab_id);

-- ---------- 14. Journal d'audit (échantillon) ----------
INSERT INTO journal_audit (utilisateur_id, action, entite, entite_id, details, adresse_ip, created_at)
SELECT u.id, a.action, a.entite, a.eid, a.details::jsonb, '10.0.' || (1 + pg_temp.r('ip' || a.k, 5)) || '.' || (10 + pg_temp.r('ipp' || a.k, 200)),
       timestamptz '2026-10-01 08:00+00' + a.k * interval '7 hours'
FROM (
    SELECT k, (ARRAY['EXAMEN_CREE', 'EXAMEN_PERIODE_DEFINIE', 'CENTRE_EXAMEN_CREE', 'RESULTAT_EXAMEN_SAISI', 'CRITERES_CENTRES_MODIFIES'])[1 + k % 5] AS action,
           (ARRAY['ExamenNational', 'ExamenNational', 'CentreExamen', 'ResultatExamen', 'CritereCentreExamen'])[1 + k % 5] AS entite,
           1 + k % 9 AS eid,
           '{"origine":"jeu de démonstration"}' AS details
    FROM generate_series(1, 30) k
) a
JOIN utilisateur u ON lower(u.email) = CASE WHEN a.k % 3 = 0 THEN 'ministere.demo@demo.gn' WHEN a.k % 3 = 1 THEN 'dr.conakry@demo.gn' ELSE 'dr.kindia@demo.gn' END;

-- ---------- Récapitulatif ----------
DO $$
DECLARE r text;
BEGIN
    SELECT format('%s établissements, %s comptes, %s élèves, %s inscriptions, %s notes, %s présences, %s factures, %s paiements, %s examens, %s candidats',
        (SELECT count(*) FROM etablissement WHERE code_officiel LIKE 'GN-%'),
        (SELECT count(*) FROM utilisateur WHERE email LIKE '%@demo.gn'),
        (SELECT count(*) FROM eleve WHERE matricule LIKE 'DEMO-%'),
        (SELECT count(*) FROM inscription i JOIN eleve e ON e.id = i.eleve_id WHERE e.matricule LIKE 'DEMO-%'),
        (SELECT count(*) FROM note n JOIN inscription i ON i.id = n.inscription_id JOIN eleve e ON e.id = i.eleve_id WHERE e.matricule LIKE 'DEMO-%'),
        (SELECT count(*) FROM presence p JOIN inscription i ON i.id = p.inscription_id JOIN eleve e ON e.id = i.eleve_id WHERE e.matricule LIKE 'DEMO-%'),
        (SELECT count(*) FROM facture WHERE numero LIKE 'FAC-2026-%'),
        (SELECT count(*) FROM paiement WHERE reference_interne LIKE 'PAY-%'),
        (SELECT count(*) FROM t_exid), (SELECT count(*) FROM candidat_examen)) INTO r;
    RAISE NOTICE 'Jeu de données chargé : %', r;
END $$;
