-- Devoirs à faire, publiés par un enseignant pour une classe et une matière.
CREATE TABLE devoir (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    classe_id      BIGINT NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    matiere_id     BIGINT NOT NULL REFERENCES matiere(id),
    enseignant_id  BIGINT NOT NULL REFERENCES enseignant(id),
    titre          VARCHAR(200) NOT NULL,
    consigne       VARCHAR(4000),
    date_limite    DATE NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_devoir_classe ON devoir(classe_id, date_limite);
