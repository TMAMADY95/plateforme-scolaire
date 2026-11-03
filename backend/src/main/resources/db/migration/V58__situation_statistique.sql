-- Situation statistique des examens d'une année : photographie datée, provisoire ou définitive, établie par la DGECS.
CREATE TABLE situation_statistique (
    id                 BIGSERIAL PRIMARY KEY,
    annee_scolaire_id  BIGINT NOT NULL REFERENCES annee_scolaire(id),
    statut             VARCHAR(10) NOT NULL,
    etablie_le         TIMESTAMP WITH TIME ZONE NOT NULL,
    etablie_par        VARCHAR(150),
    commentaire        VARCHAR(500),
    inscrits           INTEGER NOT NULL,
    filles             INTEGER NOT NULL,
    libres             INTEGER NOT NULL,
    presents           INTEGER NOT NULL,
    absents            INTEGER NOT NULL,
    elimines           INTEGER NOT NULL,
    admis              INTEGER NOT NULL,
    centres            INTEGER NOT NULL,
    salles             INTEGER NOT NULL,
    CONSTRAINT ck_situation_statut CHECK (statut IN ('PROVISOIRE', 'DEFINITIVE'))
);
-- Une seule situation définitive par année.
CREATE UNIQUE INDEX ux_situation_definitive ON situation_statistique(annee_scolaire_id) WHERE statut = 'DEFINITIVE';
CREATE INDEX ix_situation_annee ON situation_statistique(annee_scolaire_id, etablie_le DESC);

CREATE TABLE situation_statistique_niveau (
    situation_id  BIGINT NOT NULL REFERENCES situation_statistique(id) ON DELETE CASCADE,
    niveau        VARCHAR(100) NOT NULL,
    inscrits      INTEGER NOT NULL,
    filles        INTEGER NOT NULL,
    presents      INTEGER NOT NULL,
    admis         INTEGER NOT NULL,
    PRIMARY KEY (situation_id, niveau)
);
