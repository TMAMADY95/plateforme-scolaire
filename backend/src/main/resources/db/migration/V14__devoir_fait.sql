-- « Fait » : un élève (ou son parent) marque un devoir comme terminé.
CREATE TABLE devoir_fait (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    devoir_id         BIGINT NOT NULL REFERENCES devoir(id) ON DELETE CASCADE,
    eleve_id          BIGINT NOT NULL REFERENCES eleve(id),
    fait_le           TIMESTAMPTZ NOT NULL,
    par_utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
    CONSTRAINT uq_devoir_fait UNIQUE (devoir_id, eleve_id)
);
CREATE INDEX ix_devoir_fait_eleve ON devoir_fait(eleve_id);
