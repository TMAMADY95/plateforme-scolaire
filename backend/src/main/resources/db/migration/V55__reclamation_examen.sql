-- Réclamations des candidats après les résultats d'un examen (note, identité, omission…), déposées et traitées par la DGECS.
CREATE TABLE reclamation_examen (
    id            BIGSERIAL PRIMARY KEY,
    examen_id     BIGINT NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    candidat_id   BIGINT NOT NULL REFERENCES candidat_examen(id) ON DELETE CASCADE,
    objet         VARCHAR(20) NOT NULL,
    description   VARCHAR(2000) NOT NULL,
    statut        VARCHAR(10) NOT NULL DEFAULT 'OUVERTE',
    deposee_le    TIMESTAMP WITH TIME ZONE NOT NULL,
    echeance_le   TIMESTAMP WITH TIME ZONE NOT NULL,
    traitee_le    TIMESTAMP WITH TIME ZONE,
    decision      VARCHAR(1000),
    CONSTRAINT ck_reclamation_objet CHECK (objet IN ('NOTE', 'IDENTITE', 'OMISSION', 'AUTRE')),
    CONSTRAINT ck_reclamation_statut CHECK (statut IN ('OUVERTE', 'ACCEPTEE', 'REJETEE'))
);
CREATE INDEX ix_reclamation_examen ON reclamation_examen(examen_id, statut);
