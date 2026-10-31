-- Période officielle d'un examen, emploi du temps des épreuves, et centres d'examen désignés parmi les établissements.

ALTER TABLE examen_national ADD COLUMN date_debut DATE;
ALTER TABLE examen_national ADD COLUMN date_fin DATE;

CREATE TABLE epreuve_examen (
    id            BIGSERIAL PRIMARY KEY,
    examen_id     BIGINT       NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    matiere_id    BIGINT       REFERENCES matiere(id),
    libelle       VARCHAR(150),
    date_epreuve  DATE         NOT NULL,
    heure_debut   TIME         NOT NULL,
    heure_fin     TIME         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_epreuve_matiere CHECK (matiere_id IS NOT NULL OR libelle IS NOT NULL),
    CONSTRAINT ck_epreuve_horaire CHECK (heure_fin > heure_debut)
);
CREATE INDEX idx_epreuve_examen ON epreuve_examen (examen_id, date_epreuve, heure_debut);

-- Un centre d'examen est un établissement scolaire désigné pour accueillir les candidats.
ALTER TABLE centre_examen ADD COLUMN etablissement_id BIGINT REFERENCES etablissement(id) ON DELETE SET NULL;
