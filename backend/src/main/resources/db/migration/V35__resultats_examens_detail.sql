-- Statistiques détaillées des résultats d'examens : absents, éliminés (fraude), filles, et séries du baccalauréat.
ALTER TABLE resultat_examen
    ADD COLUMN absents         INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN elimines        INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN presentes_filles INTEGER,
    ADD COLUMN admis_filles     INTEGER,
    ADD CONSTRAINT ck_resultat_absents CHECK (absents >= 0 AND elimines >= 0),
    ADD CONSTRAINT ck_resultat_filles CHECK (
        (presentes_filles IS NULL AND admis_filles IS NULL)
        OR (presentes_filles IS NOT NULL AND admis_filles IS NOT NULL
            AND presentes_filles >= 0 AND admis_filles >= 0 AND admis_filles <= presentes_filles AND presentes_filles <= presentes));

CREATE TABLE resultat_examen_serie (
    resultat_id BIGINT      NOT NULL REFERENCES resultat_examen(id) ON DELETE CASCADE,
    serie       VARCHAR(30) NOT NULL,
    presentes   INTEGER     NOT NULL CHECK (presentes >= 0),
    admis       INTEGER     NOT NULL CHECK (admis >= 0 AND admis <= presentes),
    PRIMARY KEY (resultat_id, serie),
    CONSTRAINT ck_resultat_serie CHECK (serie IN ('SCIENCES_MATHEMATIQUES', 'SCIENCES_SOCIALES', 'SCIENCES_EXPERIMENTALES'))
);
