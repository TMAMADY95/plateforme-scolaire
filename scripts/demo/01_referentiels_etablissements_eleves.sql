-- =====================================================================================
-- DÉMONSTRATION 1/6 — Référentiels, zones, établissements de TOUS les types, comptes, classes et élèves
-- Deux années scolaires : 2025-2026 (terminée : examens passés) et 2026-2027 (en cours : examens à venir).
--
-- À exécuter sur une base où Flyway a déjà créé le schéma, de préférence vidée (00_vider_donnees.sql).
-- Mot de passe de TOUS les comptes : Demo!2026. Données fictives (noms, élèves, établissements inventés).
-- Ce script ne crée AUCUN examen (voir 02 à 06).
-- =====================================================================================
BEGIN;

-- ------------------------------------------------------------------ Référentiels normalement créés par les migrations
-- (recréés ici s'ils manquent, par exemple après un TRUNCATE complet ; sans effet s'ils existent déjà)
INSERT INTO cycle_scolaire (code, libelle, ordre)
SELECT v.code, v.libelle, v.ordre FROM (VALUES ('PRIMAIRE', 'Primaire', 1), ('COLLEGE', 'Collège', 2), ('LYCEE', 'Lycée', 3)) v(code, libelle, ordre)
WHERE NOT EXISTS (SELECT 1 FROM cycle_scolaire c WHERE c.code = v.code);
INSERT INTO type_etablissement (code, libelle)
SELECT v.code, v.libelle FROM (VALUES ('CLASSIQUE', 'Établissement classique'), ('COMMUNAUTAIRE', 'Établissement communautaire'),
    ('FRANCO_ARABE', 'Franco-arabe'), ('CONFESSIONNEL', 'Confessionnel'), ('TECHNIQUE', 'Enseignement technique et professionnel')) v(code, libelle)
WHERE NOT EXISTS (SELECT 1 FROM type_etablissement t WHERE t.code = v.code);
INSERT INTO type_examen (code, libelle, niveau_organisation)
SELECT v.code, v.libelle, v.organisation FROM (VALUES ('CEE', 'Certificat de fin d''études élémentaires (CEE)', 'PREFECTURE'),
    ('BEPC', 'Brevet d''études du premier cycle (BEPC)', 'REGION'), ('BAC', 'Baccalauréat', 'NATIONAL')) v(code, libelle, organisation)
WHERE NOT EXISTS (SELECT 1 FROM type_examen t WHERE t.code = v.code);
INSERT INTO critere_centre_examen (id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM critere_centre_examen);

-- ------------------------------------------------------------------ Années scolaires et périodes
INSERT INTO annee_scolaire (libelle, date_debut, date_fin) VALUES
    ('2025-2026', '2025-10-01', '2026-07-10'),
    ('2026-2027', '2026-10-01', '2027-07-10');
INSERT INTO periode (annee_scolaire_id, libelle, ordre, date_debut, date_fin, type_periode)
SELECT a.id, p.libelle, p.ordre, p.debut, p.fin, 'TRIMESTRE'
FROM annee_scolaire a
JOIN (VALUES ('2025-2026', 'Trimestre 1', 1, DATE '2025-10-06', DATE '2025-12-20'),
             ('2025-2026', 'Trimestre 2', 2, DATE '2026-01-05', DATE '2026-03-28'),
             ('2025-2026', 'Trimestre 3', 3, DATE '2026-04-06', DATE '2026-06-30'),
             ('2026-2027', 'Trimestre 1', 1, DATE '2026-10-05', DATE '2026-12-19'),
             ('2026-2027', 'Trimestre 2', 2, DATE '2027-01-04', DATE '2027-03-27'),
             ('2026-2027', 'Trimestre 3', 3, DATE '2027-04-05', DATE '2027-06-30')) p(annee, libelle, ordre, debut, fin) ON p.annee = a.libelle;

-- ------------------------------------------------------------------ Niveaux
INSERT INTO niveau (code, libelle, ordre, cycle_id)
SELECT n.code, n.libelle, n.ordre, (SELECT id FROM cycle_scolaire WHERE code = n.cycle)
FROM (VALUES ('1A', '1re année', 1, 'PRIMAIRE'), ('2A', '2e année', 2, 'PRIMAIRE'), ('3A', '3e année', 3, 'PRIMAIRE'),
             ('4A', '4e année', 4, 'PRIMAIRE'), ('5A', '5e année', 5, 'PRIMAIRE'), ('6A', '6e année', 6, 'PRIMAIRE'),
             ('7A', '7e année', 7, 'COLLEGE'), ('8A', '8e année', 8, 'COLLEGE'), ('9A', '9e année', 9, 'COLLEGE'),
             ('10A', '10e année', 10, 'COLLEGE'),
             ('11A', '11e année', 11, 'LYCEE'), ('12A', '12e année', 12, 'LYCEE'), ('TERM', 'Terminale', 13, 'LYCEE'))
     AS n(code, libelle, ordre, cycle);

-- ------------------------------------------------------------------ Matières
INSERT INTO matiere (code, libelle) VALUES
    ('FRANCAIS', 'Français'), ('MATHS', 'Mathématiques'), ('SCIENCES_OBS', 'Sciences d''observation'),
    ('HIST_GEO', 'Histoire-Géographie'), ('ECM', 'Éducation civique et morale'), ('PHYS_CHIMIE', 'Physique-Chimie'),
    ('SVT', 'Sciences de la vie et de la Terre'), ('ANGLAIS', 'Anglais'), ('PHILO', 'Philosophie'),
    ('ARABE', 'Langue arabe'), ('EDU_ISLAMIQUE', 'Éducation islamique'),
    ('TECHNOLOGIE', 'Technologie et dessin technique'), ('PRATIQUE_PRO', 'Pratique professionnelle (atelier)');

-- ------------------------------------------------------------------ Zones : pays > région > préfecture > commune
INSERT INTO zone_geographique (parent_id, nom, type_zone) VALUES (NULL, 'Guinée', 'PAYS');
INSERT INTO zone_geographique (parent_id, nom, type_zone)
SELECT (SELECT id FROM zone_geographique WHERE nom = 'Guinée'), r, 'REGION' FROM unnest(ARRAY['Conakry', 'Kindia', 'Kankan']) r;
INSERT INTO zone_geographique (parent_id, nom, type_zone)
SELECT (SELECT id FROM zone_geographique WHERE nom = x.region AND type_zone = 'REGION'), x.nom, 'PREFECTURE'
FROM (VALUES ('Conakry', 'Préfecture de Conakry'), ('Kindia', 'Préfecture de Kindia'), ('Kankan', 'Préfecture de Kankan')) x(region, nom);
INSERT INTO zone_geographique (parent_id, nom, type_zone)
SELECT (SELECT id FROM zone_geographique WHERE nom = x.prefecture AND type_zone = 'PREFECTURE'), x.nom, 'COMMUNE'
FROM (VALUES ('Préfecture de Conakry', 'Kaloum'), ('Préfecture de Conakry', 'Dixinn'),
             ('Préfecture de Kindia', 'Kindia'), ('Préfecture de Kankan', 'Kankan')) x(prefecture, nom);

-- ------------------------------------------------------------------ Établissements : les 5 types, public et privé,
-- dont un en attente d'autorisation et un dont l'autorisation est retirée (aucun candidat : cas d'école).
CREATE TEMP TABLE demo_etab (code text, nom text, commune text, secteur text, type text, cycles text[], salles int, autorisation text);
INSERT INTO demo_etab VALUES
    ('GN-CKY-KAL-EP-01', 'École primaire Kaloum 1',                   'Kaloum', 'PUBLIC', 'CLASSIQUE',     ARRAY['PRIMAIRE'], 8,  'AUTORISE'),
    ('GN-CKY-KAL-CO-01', 'Collège Kaloum',                            'Kaloum', 'PUBLIC', 'CLASSIQUE',     ARRAY['COLLEGE'], 10, 'AUTORISE'),
    ('GN-CKY-KAL-LY-01', 'Lycée Kaloum',                              'Kaloum', 'PUBLIC', 'CLASSIQUE',     ARRAY['LYCEE'], 14,  'AUTORISE'),
    ('GN-CKY-DIX-EP-01', 'École primaire Dixinn',                     'Dixinn', 'PUBLIC', 'CLASSIQUE',     ARRAY['PRIMAIRE'], 8, 'AUTORISE'),
    ('GN-CKY-DIX-FA-01', 'Complexe franco-arabe Al-Nour',             'Dixinn', 'PRIVE',  'FRANCO_ARABE',  ARRAY['PRIMAIRE', 'COLLEGE', 'LYCEE'], 16, 'AUTORISE'),
    ('GN-CKY-DIX-LY-01', 'Collège-Lycée privé Les Palmiers',          'Dixinn', 'PRIVE',  'CLASSIQUE',     ARRAY['COLLEGE', 'LYCEE'], 14, 'AUTORISE'),
    ('GN-KIN-KIN-EP-01', 'École primaire Kindia Centre',              'Kindia', 'PUBLIC', 'CLASSIQUE',     ARRAY['PRIMAIRE'], 8, 'AUTORISE'),
    ('GN-KIN-KIN-LY-01', 'Collège-Lycée Kindia',                      'Kindia', 'PUBLIC', 'CLASSIQUE',     ARRAY['COLLEGE', 'LYCEE'], 14, 'AUTORISE'),
    ('GN-KIN-KIN-FA-01', 'Médersa franco-arabe de Kindia',            'Kindia', 'PRIVE',  'FRANCO_ARABE',  ARRAY['PRIMAIRE', 'COLLEGE'], 10, 'AUTORISE'),
    ('GN-KAN-KAN-EP-01', 'École primaire Kankan Centre',              'Kankan', 'PUBLIC', 'CLASSIQUE',     ARRAY['PRIMAIRE'], 8, 'AUTORISE'),
    ('GN-KAN-KAN-LY-01', 'Collège-Lycée Kankan',                      'Kankan', 'PUBLIC', 'CLASSIQUE',     ARRAY['COLLEGE', 'LYCEE'], 14, 'AUTORISE'),
    ('GN-KAN-KAN-FA-01', 'Institut franco-arabe de Kankan',           'Kankan', 'PUBLIC', 'FRANCO_ARABE',  ARRAY['PRIMAIRE', 'COLLEGE', 'LYCEE'], 16, 'AUTORISE'),
    ('GN-KIN-KIN-EC-01', 'École communautaire de Friguiagbé',         'Kindia', 'PUBLIC', 'COMMUNAUTAIRE', ARRAY['PRIMAIRE'], 6, 'AUTORISE'),
    ('GN-KAN-KAN-CC-01', 'Collège communautaire de Kankan',           'Kankan', 'PUBLIC', 'COMMUNAUTAIRE', ARRAY['COLLEGE'], 8, 'AUTORISE'),
    ('GN-CKY-KAL-SJ-01', 'Collège-Lycée catholique Saint-Joseph',     'Kaloum', 'PRIVE',  'CONFESSIONNEL', ARRAY['COLLEGE', 'LYCEE'], 14, 'AUTORISE'),
    ('GN-KIN-KIN-EV-01', 'École évangélique de Kindia',               'Kindia', 'PRIVE',  'CONFESSIONNEL', ARRAY['PRIMAIRE'], 6, 'AUTORISE'),
    ('GN-CKY-DIX-LT-01', 'Lycée technique de Dixinn',                 'Dixinn', 'PUBLIC', 'TECHNIQUE',     ARRAY['LYCEE'], 16, 'AUTORISE'),
    ('GN-KIN-KIN-CT-01', 'Collège technique de Kindia',               'Kindia', 'PUBLIC', 'TECHNIQUE',     ARRAY['COLLEGE'], 10, 'AUTORISE'),
    ('GN-CKY-KAL-ET-01', 'Collège privé Les Étoiles (autorisation en attente)', 'Kaloum', 'PRIVE', 'CLASSIQUE', ARRAY['COLLEGE'], 6, 'EN_ATTENTE'),
    ('GN-CKY-DIX-EX-01', 'Lycée privé Excellence (autorisation retirée)',        'Dixinn', 'PRIVE', 'CLASSIQUE', ARRAY['LYCEE'], 6, 'RETIRE');

INSERT INTO etablissement (code_officiel, nom, zone_id, adresse, capacite, statut, secteur, autorisation_statut,
                           autorisation_reference, autorisation_date, motif_statut, type_etablissement_id)
SELECT d.code, d.nom, z.id, 'Commune de ' || d.commune || ', Guinée', d.salles * 40, 'ACTIF', d.secteur, d.autorisation,
       CASE WHEN d.autorisation IN ('AUTORISE', 'RETIRE') THEN 'ARR/MEPUA/2020/' || substr(d.code, 8, 10) END,
       CASE WHEN d.autorisation IN ('AUTORISE', 'RETIRE') THEN DATE '2020-09-15' END,
       CASE WHEN d.autorisation = 'RETIRE' THEN 'Locaux non conformes : autorisation retirée après inspection' END,
       t.id
FROM demo_etab d
JOIN zone_geographique z ON z.nom = d.commune AND z.type_zone = 'COMMUNE'
JOIN type_etablissement t ON t.code = d.type;

INSERT INTO etablissement_cycle (etablissement_id, cycle_id)
SELECT e.id, c.id FROM demo_etab d
JOIN etablissement e ON e.code_officiel = d.code
JOIN cycle_scolaire c ON c.code = ANY (d.cycles);

INSERT INTO salle (etablissement_id, nom, capacite, type_salle)
SELECT e.id, 'Salle ' || s, 30, 'CLASSE'
FROM demo_etab d JOIN etablissement e ON e.code_officiel = d.code, generate_series(1, d.salles) s;

-- ------------------------------------------------------------------ Comptes (mot de passe : Demo!2026)
CREATE TEMP TABLE demo_compte (email text, nom text, prenom text, role text, perimetre text, zone text, zone_type text, etab text);
INSERT INTO demo_compte VALUES
    ('ministere@example.org', 'Ministère', 'Démo', 'MINISTERE', 'NATIONAL', NULL, NULL, NULL),
    ('dgecs@example.org',     'DGECS', 'Démo', 'DGECS', 'NATIONAL', NULL, NULL, NULL),
    ('dr.conakry@example.org', 'Direction régionale', 'Conakry', 'DIRECTION_REGIONALE', 'ZONE', 'Conakry', 'REGION', NULL),
    ('dr.kindia@example.org',  'Direction régionale', 'Kindia',  'DIRECTION_REGIONALE', 'ZONE', 'Kindia',  'REGION', NULL),
    ('dr.kankan@example.org',  'Direction régionale', 'Kankan',  'DIRECTION_REGIONALE', 'ZONE', 'Kankan',  'REGION', NULL),
    ('dl.kaloum@example.org',  'Direction communale', 'Kaloum',  'DIRECTION_LOCALE', 'ZONE', 'Kaloum', 'COMMUNE', NULL),
    ('dl.dixinn@example.org',  'Direction communale', 'Dixinn',  'DIRECTION_LOCALE', 'ZONE', 'Dixinn', 'COMMUNE', NULL),
    ('dl.kindia@example.org',  'Direction préfectorale', 'Kindia', 'DIRECTION_LOCALE', 'ZONE', 'Kindia', 'COMMUNE', NULL),
    ('dl.kankan@example.org',  'Direction préfectorale', 'Kankan', 'DIRECTION_LOCALE', 'ZONE', 'Kankan', 'COMMUNE', NULL);
INSERT INTO demo_compte
SELECT 'direction.' || lower(substr(d.code, 4, 3)) || '.' || lower(substr(d.code, 8, 3)) || '.' || lower(substr(d.code, 12, 2)) || '@example.org',
       'Direction', d.nom, 'DIRECTION_ETABLISSEMENT', 'ETABLISSEMENT', NULL, NULL, d.code
FROM demo_etab d;

INSERT INTO utilisateur (email, mot_de_passe_hash, nom, prenom)
SELECT email, '$2a$10$6zOYDBuEs3qDJ.4qJQeFjeMJtm2Ape8Wcfx0QCFM1UuvA.99bAeTO', nom, prenom FROM demo_compte;

INSERT INTO affectation_role (utilisateur_id, role, perimetre, zone_id, etablissement_id)
SELECT u.id, c.role, c.perimetre,
       (SELECT z.id FROM zone_geographique z WHERE z.nom = c.zone AND z.type_zone = c.zone_type),
       (SELECT e.id FROM etablissement e WHERE e.code_officiel = c.etab)
FROM demo_compte c JOIN utilisateur u ON u.email = c.email;

-- ------------------------------------------------------------------ Classes et élèves (chaque année)
-- 6e année (CEE) : 22 ; 9e année (examen communal) : 20 ; 10e année (BEPC) : 22 ;
-- Terminale SM / SE / SS : 10 / 12 / 14 ; Terminale technique (sans série) : 30.
CREATE TEMP TABLE demo_classe (annee text, code_etab text, niveau text, nom text, serie text, effectif int, age int);
INSERT INTO demo_classe
SELECT a.libelle, d.code, '6A', '6e année A', NULL, 22, 12 FROM demo_etab d, annee_scolaire a WHERE 'PRIMAIRE' = ANY (d.cycles);
INSERT INTO demo_classe
SELECT a.libelle, d.code, '9A', '9e année A', NULL, 20, 14 FROM demo_etab d, annee_scolaire a WHERE 'COLLEGE' = ANY (d.cycles);
INSERT INTO demo_classe
SELECT a.libelle, d.code, '10A', '10e année A', NULL, 22, 15 FROM demo_etab d, annee_scolaire a WHERE 'COLLEGE' = ANY (d.cycles);
INSERT INTO demo_classe
SELECT a.libelle, d.code, 'TERM', 'Terminale ' || s.court, s.serie, s.effectif, 18
FROM demo_etab d, annee_scolaire a,
     (VALUES ('SM', 'SCIENCES_MATHEMATIQUES', 10), ('SE', 'SCIENCES_EXPERIMENTALES', 12), ('SS', 'SCIENCES_SOCIALES', 14)) s(court, serie, effectif)
WHERE 'LYCEE' = ANY (d.cycles) AND d.type <> 'TECHNIQUE';
INSERT INTO demo_classe
SELECT a.libelle, d.code, 'TERM', 'Terminale technique', NULL, 30, 18
FROM demo_etab d, annee_scolaire a WHERE 'LYCEE' = ANY (d.cycles) AND d.type = 'TECHNIQUE';

INSERT INTO classe (etablissement_id, annee_scolaire_id, niveau_id, nom, capacite, serie)
SELECT e.id, a.id, n.id, c.nom, 60, c.serie
FROM demo_classe c
JOIN etablissement e ON e.code_officiel = c.code_etab
JOIN niveau n ON n.code = c.niveau
JOIN annee_scolaire a ON a.libelle = c.annee;

DO $$
DECLARE
    prenoms_m text[] := ARRAY['Mamadou', 'Ibrahima', 'Ousmane', 'Alpha', 'Mohamed', 'Sékou', 'Boubacar', 'Abdoulaye', 'Thierno', 'Moussa',
                              'Mory', 'Naby', 'Elhadj', 'Amadou', 'Facinet', 'Lansana', 'Saïkou', 'Karamoko', 'Fodé', 'Aboubacar'];
    prenoms_f text[] := ARRAY['Fatoumata', 'Mariama', 'Aissatou', 'Kadiatou', 'Hawa', 'Fanta', 'Djénabou', 'Salimatou', 'Aminata', 'Mariam',
                              'Nènè', 'Binta', 'Kankou', 'Safiatou', 'Houleymatou', 'Fatim', 'Rouguiatou', 'Zeinab', 'Sarata', 'N''Deye'];
    noms text[] := ARRAY['Diallo', 'Camara', 'Bah', 'Barry', 'Sylla', 'Soumah', 'Keita', 'Traoré', 'Condé', 'Touré',
                         'Bangoura', 'Sow', 'Kourouma', 'Doumbouya', 'Cissé', 'Fofana', 'Konaté', 'Sidibé', 'Baldé', 'Diakité',
                         'Savané', 'Béavogui', 'Kamano', 'Haba', 'Loua'];
    lieux text[] := ARRAY['Conakry', 'Kindia', 'Kankan', 'Labé', 'Mamou', 'Faranah', 'Boké', 'Siguiri'];
    c record;
    k int;
    sexe char(1);
    prenom text;
    nom text;
    eleve_id bigint;
    classe_id bigint;
    etab_id bigint;
    annee_id bigint;
    graine int;
BEGIN
    FOR c IN SELECT dc.*, row_number() OVER (ORDER BY dc.annee, dc.code_etab, dc.nom) AS rang FROM demo_classe dc ORDER BY dc.annee, dc.code_etab, dc.nom LOOP
        SELECT id INTO annee_id FROM annee_scolaire WHERE libelle = c.annee;
        SELECT id INTO etab_id FROM etablissement WHERE code_officiel = c.code_etab;
        SELECT cl.id INTO classe_id FROM classe cl WHERE cl.etablissement_id = etab_id AND cl.annee_scolaire_id = annee_id AND cl.nom = c.nom;
        FOR k IN 1..c.effectif LOOP
            graine := c.rang * 37 + k * 11;
            sexe := CASE WHEN k % 2 = 0 THEN 'F' ELSE 'M' END;
            prenom := CASE WHEN sexe = 'F' THEN prenoms_f[1 + graine % 20] ELSE prenoms_m[1 + graine % 20] END;
            nom := noms[1 + (graine * 3 + k) % 25];
            INSERT INTO eleve (matricule, nom, prenom, date_naissance, sexe, lieu_naissance, nom_pere, nom_mere)
            VALUES ('MAT-' || substr(c.code_etab, 4, 10) || '-' || substr(c.annee, 8, 2) || '-' || c.niveau || coalesce(substr(c.serie, 10, 2), 'TE') || '-' || lpad(k::text, 3, '0'),
                    nom, prenom,
                    make_date(substr(c.annee, 1, 4)::int - c.age + (k % 2), 1 + (graine % 12), 1 + (graine % 27)),
                    sexe, lieux[1 + graine % 8],
                    prenoms_m[1 + (graine * 7) % 20] || ' ' || nom,
                    prenoms_f[1 + (graine * 5) % 20] || ' ' || noms[1 + (graine * 13) % 25])
            RETURNING id INTO eleve_id;
            INSERT INTO inscription (eleve_id, etablissement_id, annee_scolaire_id, classe_id, statut, date_demande, date_decision)
            VALUES (eleve_id, etab_id, annee_id, classe_id, 'ACCEPTEE',
                    make_date(substr(c.annee, 1, 4)::int, 9, 28), make_date(substr(c.annee, 1, 4)::int, 10, 1));
        END LOOP;
    END LOOP;
END $$;

COMMIT;
