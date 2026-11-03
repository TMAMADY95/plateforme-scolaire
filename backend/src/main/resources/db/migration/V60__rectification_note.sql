-- Rectification d'une note après proclamation, à la suite d'une réclamation acceptée : on conserve l'ancienne et la nouvelle valeur.
CREATE TABLE rectification_note (
    id                 BIGSERIAL PRIMARY KEY,
    reclamation_id     BIGINT NOT NULL REFERENCES reclamation_examen(id) ON DELETE CASCADE,
    candidat_id        BIGINT NOT NULL REFERENCES candidat_examen(id) ON DELETE CASCADE,
    epreuve_id         BIGINT NOT NULL REFERENCES epreuve_examen(id) ON DELETE CASCADE,
    ancienne_note      NUMERIC(4,2),
    nouvelle_note      NUMERIC(4,2) NOT NULL,
    ancienne_moyenne   NUMERIC(5,2),
    nouvelle_moyenne   NUMERIC(5,2),
    ancienne_decision  VARCHAR(10),
    nouvelle_decision  VARCHAR(10),
    rectifiee_le       TIMESTAMP WITH TIME ZONE NOT NULL,
    rectifiee_par      VARCHAR(150)
);
CREATE INDEX ix_rectification_candidat ON rectification_note(candidat_id);
