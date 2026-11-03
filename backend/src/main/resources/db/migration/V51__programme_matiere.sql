-- Programme officiel : matières d'un niveau (et d'une série au lycée) avec volume horaire hebdomadaire et coefficient indicatif.
-- Une grille par année scolaire : les horaires évoluent, le ministère les met à jour sans toucher au code.
CREATE TABLE programme_matiere (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    annee_scolaire_id BIGINT NOT NULL REFERENCES annee_scolaire(id) ON DELETE CASCADE,
    niveau_id         BIGINT NOT NULL REFERENCES niveau(id),
    serie             VARCHAR(30),
    matiere_id        BIGINT NOT NULL REFERENCES matiere(id),
    heures_hebdo      NUMERIC(4,2),
    coefficient       NUMERIC(4,2),
    note              VARCHAR(300),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_programme_serie CHECK (serie IS NULL OR serie IN ('SCIENCES_MATHEMATIQUES', 'SCIENCES_SOCIALES', 'SCIENCES_EXPERIMENTALES')),
    CONSTRAINT ck_programme_heures CHECK (heures_hebdo IS NULL OR heures_hebdo >= 0),
    CONSTRAINT ck_programme_coef CHECK (coefficient IS NULL OR coefficient > 0)
);
CREATE UNIQUE INDEX uq_programme_matiere ON programme_matiere (annee_scolaire_id, niveau_id, COALESCE(serie, ''), matiere_id);
CREATE INDEX idx_programme_niveau ON programme_matiere (annee_scolaire_id, niveau_id);
