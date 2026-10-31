-- Examens nationaux (CEE, BEPC, Baccalauréat…) et leurs résultats par établissement.

CREATE TABLE examen_national (
    id                BIGSERIAL PRIMARY KEY,
    libelle           VARCHAR(150) NOT NULL,
    annee_scolaire_id BIGINT       NOT NULL REFERENCES annee_scolaire(id),
    cycle_id          BIGINT REFERENCES cycle_scolaire(id),
    CONSTRAINT uq_examen_annee UNIQUE (libelle, annee_scolaire_id)
);

CREATE TABLE resultat_examen (
    id               BIGSERIAL PRIMARY KEY,
    examen_id        BIGINT NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    etablissement_id BIGINT NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    presentes        INT    NOT NULL CHECK (presentes >= 0),
    admis            INT    NOT NULL CHECK (admis >= 0),
    CONSTRAINT ck_admis_presentes CHECK (admis <= presentes),
    CONSTRAINT uq_resultat_examen UNIQUE (examen_id, etablissement_id)
);
CREATE INDEX ix_resultat_examen_etab ON resultat_examen(etablissement_id);
