-- =====================================================================
-- Plateforme de gestion scolaire : schéma initial du MVP (PostgreSQL)
-- Migration Flyway : backend/src/main/resources/db/migration/
-- =====================================================================

-- ---------- Référentiels configurables (rien de codé en dur par pays) ----------

-- Hiérarchie territoriale libre : pays > région > département > commune...
CREATE TABLE zone_geographique (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id  BIGINT REFERENCES zone_geographique(id),
    nom        VARCHAR(150) NOT NULL,
    type_zone  VARCHAR(50)  NOT NULL
);
CREATE INDEX ix_zone_parent ON zone_geographique(parent_id);

CREATE TABLE annee_scolaire (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    libelle     VARCHAR(20) NOT NULL UNIQUE,          -- ex. 2026-2027
    date_debut  DATE NOT NULL,
    date_fin    DATE NOT NULL,
    CONSTRAINT ck_annee_dates CHECK (date_fin > date_debut)
);

-- Trimestres, semestres, etc. : définis librement par année
CREATE TABLE periode (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    annee_scolaire_id BIGINT NOT NULL REFERENCES annee_scolaire(id),
    libelle           VARCHAR(50) NOT NULL,
    ordre             INT NOT NULL,
    date_debut        DATE NOT NULL,
    date_fin          DATE NOT NULL,
    CONSTRAINT ck_periode_dates CHECK (date_fin > date_debut),
    UNIQUE (annee_scolaire_id, ordre)
);

CREATE TABLE niveau (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30)  NOT NULL UNIQUE,
    libelle  VARCHAR(100) NOT NULL,
    ordre    INT NOT NULL
);

CREATE TABLE matiere (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30)  NOT NULL UNIQUE,
    libelle  VARCHAR(100) NOT NULL
);

-- ---------- Établissements ----------

CREATE TABLE etablissement (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code_officiel        VARCHAR(50)  NOT NULL UNIQUE,
    nom                  VARCHAR(200) NOT NULL,
    zone_id              BIGINT REFERENCES zone_geographique(id),
    type_etablissement   VARCHAR(50),
    adresse              VARCHAR(300),
    capacite             INT,
    statut               VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_etab_statut CHECK (statut IN ('ACTIF', 'SUSPENDU', 'FERME'))
);
CREATE INDEX ix_etab_zone ON etablissement(zone_id);

-- ---------- Utilisateurs, rôles et périmètres ----------

CREATE TABLE utilisateur (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email               VARCHAR(255) NOT NULL,
    mot_de_passe_hash   VARCHAR(255) NOT NULL,
    nom                 VARCHAR(100) NOT NULL,
    prenom              VARCHAR(100) NOT NULL,
    telephone           VARCHAR(30),
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_utilisateur_email ON utilisateur (lower(email));

-- Un utilisateur peut avoir plusieurs rôles, chacun limité à un périmètre.
-- PERSONNEL = ses propres données (parent : via lien_parental ; élève : son dossier)
CREATE TABLE affectation_role (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utilisateur_id    BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    role              VARCHAR(40) NOT NULL,
    perimetre         VARCHAR(20) NOT NULL,
    zone_id           BIGINT REFERENCES zone_geographique(id),
    etablissement_id  BIGINT REFERENCES etablissement(id),
    CONSTRAINT ck_role CHECK (role IN (
        'ADMIN_TECHNIQUE', 'MINISTERE', 'DIRECTION_REGIONALE',
        'DIRECTION_ETABLISSEMENT', 'PERSONNEL_ADMIN',
        'ENSEIGNANT', 'PARENT', 'ELEVE')),
    CONSTRAINT ck_perimetre CHECK (
           (perimetre = 'NATIONAL'      AND zone_id IS NULL     AND etablissement_id IS NULL)
        OR (perimetre = 'ZONE'          AND zone_id IS NOT NULL AND etablissement_id IS NULL)
        OR (perimetre = 'ETABLISSEMENT' AND zone_id IS NULL     AND etablissement_id IS NOT NULL)
        OR (perimetre = 'PERSONNEL'     AND zone_id IS NULL     AND etablissement_id IS NULL))
);
CREATE INDEX ix_affect_user ON affectation_role(utilisateur_id);
CREATE INDEX ix_affect_etab ON affectation_role(etablissement_id);

-- ---------- Enseignants et élèves ----------

CREATE TABLE enseignant (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utilisateur_id  BIGINT NOT NULL UNIQUE REFERENCES utilisateur(id),
    matricule       VARCHAR(50) UNIQUE
);

CREATE TABLE eleve (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    matricule       VARCHAR(50) NOT NULL UNIQUE,      -- identifiant scolaire unique
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    date_naissance  DATE NOT NULL,
    sexe            CHAR(1),
    utilisateur_id  BIGINT UNIQUE REFERENCES utilisateur(id),   -- compte élève, optionnel
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_eleve_sexe CHECK (sexe IN ('F', 'M'))
);
-- Aide à la détection de doublons
CREATE INDEX ix_eleve_identite ON eleve (lower(nom), lower(prenom), date_naissance);

CREATE TABLE lien_parental (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    eleve_id           BIGINT NOT NULL REFERENCES eleve(id) ON DELETE CASCADE,
    utilisateur_id     BIGINT NOT NULL REFERENCES utilisateur(id),
    relation           VARCHAR(50) NOT NULL,           -- père, mère, tuteur...
    responsable_legal  BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (eleve_id, utilisateur_id)
);
CREATE INDEX ix_lien_user ON lien_parental(utilisateur_id);

-- ---------- Classes et inscriptions ----------

CREATE TABLE classe (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    etablissement_id   BIGINT NOT NULL REFERENCES etablissement(id),
    annee_scolaire_id  BIGINT NOT NULL REFERENCES annee_scolaire(id),
    niveau_id          BIGINT NOT NULL REFERENCES niveau(id),
    nom                VARCHAR(50) NOT NULL,
    capacite           INT,
    UNIQUE (etablissement_id, annee_scolaire_id, nom)
);
CREATE INDEX ix_classe_etab_annee ON classe(etablissement_id, annee_scolaire_id);

CREATE TABLE affectation_enseignant (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enseignant_id  BIGINT NOT NULL REFERENCES enseignant(id),
    classe_id      BIGINT NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    matiere_id     BIGINT NOT NULL REFERENCES matiere(id),
    UNIQUE (enseignant_id, classe_id, matiere_id)
);

CREATE TABLE inscription (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    eleve_id           BIGINT NOT NULL REFERENCES eleve(id),
    etablissement_id   BIGINT NOT NULL REFERENCES etablissement(id),
    annee_scolaire_id  BIGINT NOT NULL REFERENCES annee_scolaire(id),
    classe_id          BIGINT REFERENCES classe(id),   -- renseignée à la décision
    statut             VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    date_demande       DATE NOT NULL DEFAULT CURRENT_DATE,
    date_decision      DATE,
    decide_par         BIGINT REFERENCES utilisateur(id),
    CONSTRAINT ck_inscription_statut CHECK (statut IN
        ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE', 'TRANSFEREE'))
);
-- Un élève ne peut avoir qu'une inscription active par année scolaire
CREATE UNIQUE INDEX ux_inscription_active
    ON inscription (eleve_id, annee_scolaire_id)
    WHERE statut IN ('EN_ATTENTE', 'ACCEPTEE');
CREATE INDEX ix_inscription_etab   ON inscription(etablissement_id, annee_scolaire_id);
CREATE INDEX ix_inscription_classe ON inscription(classe_id);

-- ---------- Présences ----------

CREATE TABLE presence (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inscription_id  BIGINT NOT NULL REFERENCES inscription(id) ON DELETE CASCADE,
    date_seance     DATE NOT NULL,
    statut          VARCHAR(10) NOT NULL,
    motif           VARCHAR(255),
    justifie        BOOLEAN NOT NULL DEFAULT FALSE,
    saisi_par       BIGINT NOT NULL REFERENCES utilisateur(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_presence_statut CHECK (statut IN ('PRESENT', 'ABSENT', 'RETARD')),
    UNIQUE (inscription_id, date_seance)
);
CREATE INDEX ix_presence_date ON presence(date_seance);

-- ---------- Évaluations et notes ----------

CREATE TABLE evaluation (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    classe_id        BIGINT NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    matiere_id       BIGINT NOT NULL REFERENCES matiere(id),
    periode_id       BIGINT NOT NULL REFERENCES periode(id),
    enseignant_id    BIGINT NOT NULL REFERENCES enseignant(id),
    titre            VARCHAR(200) NOT NULL,
    date_evaluation  DATE NOT NULL,
    coefficient      NUMERIC(4,2) NOT NULL DEFAULT 1,
    bareme           NUMERIC(5,2) NOT NULL DEFAULT 20,
    CONSTRAINT ck_eval_coef   CHECK (coefficient > 0),
    CONSTRAINT ck_eval_bareme CHECK (bareme > 0)
);
CREATE INDEX ix_eval_classe ON evaluation(classe_id, periode_id);

CREATE TABLE note (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evaluation_id   BIGINT NOT NULL REFERENCES evaluation(id) ON DELETE CASCADE,
    inscription_id  BIGINT NOT NULL REFERENCES inscription(id) ON DELETE CASCADE,
    valeur          NUMERIC(5,2),
    absent          BOOLEAN NOT NULL DEFAULT FALSE,
    modifie_par     BIGINT NOT NULL REFERENCES utilisateur(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_note_valeur CHECK (
        (absent = TRUE AND valeur IS NULL) OR (absent = FALSE AND valeur >= 0)),
    UNIQUE (evaluation_id, inscription_id)
);
CREATE INDEX ix_note_inscription ON note(inscription_id);
-- Règle à vérifier côté service : valeur <= evaluation.bareme

-- ---------- Journal d'audit (modifications sensibles) ----------

CREATE TABLE journal_audit (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utilisateur_id  BIGINT REFERENCES utilisateur(id),
    action          VARCHAR(50)  NOT NULL,             -- ex. MODIFICATION_NOTE
    entite          VARCHAR(50)  NOT NULL,
    entite_id       BIGINT,
    details         JSONB,                             -- avant / après
    adresse_ip      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entite ON journal_audit(entite, entite_id);
CREATE INDEX ix_audit_date   ON journal_audit(created_at);
