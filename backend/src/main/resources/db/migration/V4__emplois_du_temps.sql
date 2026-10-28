-- =====================================================================
-- Plateforme de gestion scolaire : emplois du temps (Étape 2, Module 9)
-- Migration Flyway : backend/src/main/resources/db/migration/
-- =====================================================================

-- ---------- Salles ----------

CREATE TABLE salle (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    etablissement_id BIGINT NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    nom              VARCHAR(50) NOT NULL,
    capacite         INT,
    type_salle       VARCHAR(50),                     -- ex. SALLE_DE_CLASSE, LABO, GYMNASE
    CONSTRAINT ck_salle_capacite CHECK (capacite IS NULL OR capacite > 0),
    UNIQUE (etablissement_id, nom)
);
CREATE INDEX ix_salle_etab ON salle(etablissement_id);

-- ---------- Créneaux (emploi du temps par classe) ----------

CREATE TABLE creneau (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    classe_id       BIGINT NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    matiere_id      BIGINT NOT NULL REFERENCES matiere(id),
    enseignant_id   BIGINT REFERENCES enseignant(id),   -- peut être vide tant que l'affectation n'est pas faite
    salle_id        BIGINT REFERENCES salle(id),
    jour_semaine    SMALLINT NOT NULL,                  -- 1 = lundi ... 7 = dimanche
    heure_debut     TIME NOT NULL,
    heure_fin       TIME NOT NULL,
    CONSTRAINT ck_creneau_jour  CHECK (jour_semaine BETWEEN 1 AND 7),
    CONSTRAINT ck_creneau_heures CHECK (heure_fin > heure_debut)
);
CREATE INDEX ix_creneau_classe     ON creneau(classe_id, jour_semaine, heure_debut);
CREATE INDEX ix_creneau_enseignant ON creneau(enseignant_id, jour_semaine, heure_debut);
CREATE INDEX ix_creneau_salle      ON creneau(salle_id, jour_semaine, heure_debut);

-- Un enseignant ne peut pas avoir deux cours qui se chevauchent le même jour.
-- (Contrainte d'exclusion : PostgreSQL gère le chevauchement d'intervalles.)
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE creneau ADD CONSTRAINT ex_creneau_enseignant
    EXCLUDE USING gist (
        enseignant_id WITH =,
        jour_semaine WITH =,
        tsrange(
            ('2000-01-01'::date + heure_debut),
            ('2000-01-01'::date + heure_fin)
        ) WITH &&
    ) WHERE (enseignant_id IS NOT NULL);

-- Une salle ne peut pas accueillir deux cours qui se chevauchent le même jour.
ALTER TABLE creneau ADD CONSTRAINT ex_creneau_salle
    EXCLUDE USING gist (
        salle_id WITH =,
        jour_semaine WITH =,
        tsrange(
            ('2000-01-01'::date + heure_debut),
            ('2000-01-01'::date + heure_fin)
        ) WITH &&
    ) WHERE (salle_id IS NOT NULL);
