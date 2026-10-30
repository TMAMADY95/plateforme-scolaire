-- Secteur (public / privé) et cycles (primaire, collège, lycée) des établissements.
-- Un établissement peut proposer un seul cycle ou plusieurs ; la liste des cycles est un référentiel du ministère.

CREATE TABLE cycle_scolaire (
    id      BIGSERIAL PRIMARY KEY,
    code    VARCHAR(30)  NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    ordre   INT          NOT NULL CHECK (ordre >= 1)
);

INSERT INTO cycle_scolaire (code, libelle, ordre) VALUES
    ('PRIMAIRE', 'Primaire', 1),
    ('COLLEGE',  'Collège',  2),
    ('LYCEE',    'Lycée',    3);

CREATE TABLE etablissement_cycle (
    etablissement_id BIGINT NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    cycle_id         BIGINT NOT NULL REFERENCES cycle_scolaire(id),
    PRIMARY KEY (etablissement_id, cycle_id)
);
CREATE INDEX ix_etab_cycle_cycle ON etablissement_cycle(cycle_id);

ALTER TABLE etablissement
    ADD COLUMN secteur VARCHAR(10) NOT NULL DEFAULT 'PUBLIC' CHECK (secteur IN ('PUBLIC', 'PRIVE'));

-- Reprise de l'ancien champ libre « type » quand il correspond à un cycle connu (sans accent ni casse).
INSERT INTO etablissement_cycle (etablissement_id, cycle_id)
SELECT e.id, c.id
FROM etablissement e
JOIN cycle_scolaire c
  ON c.code = translate(upper(trim(e.type_etablissement)), 'ÉÈÊ', 'EEE');

ALTER TABLE etablissement DROP COLUMN type_etablissement;
