-- Résultats individuels des candidats : statut à l'examen, notes par épreuve, moyenne pondérée et décision.
ALTER TABLE epreuve_examen
    ADD COLUMN coefficient INTEGER NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_epreuve_coefficient CHECK (coefficient >= 1);

-- INSCRIT : pas encore composé ; PRESENT, ABSENT ; ELIMINE : fraude constatée (disqualification à l'examen).
ALTER TABLE candidat_examen
    ADD COLUMN statut_epreuves VARCHAR(10) NOT NULL DEFAULT 'INSCRIT',
    ADD COLUMN moyenne         NUMERIC(5,2),
    ADD COLUMN decision        VARCHAR(10),
    ADD CONSTRAINT ck_candidat_statut CHECK (statut_epreuves IN ('INSCRIT', 'PRESENT', 'ABSENT', 'ELIMINE')),
    ADD CONSTRAINT ck_candidat_decision CHECK (decision IS NULL OR decision IN ('ADMIS', 'NON_ADMIS')),
    ADD CONSTRAINT ck_candidat_moyenne CHECK (moyenne IS NULL OR (moyenne >= 0 AND moyenne <= 20));

CREATE TABLE note_examen (
    candidat_id BIGINT       NOT NULL REFERENCES candidat_examen(id) ON DELETE CASCADE,
    epreuve_id  BIGINT       NOT NULL REFERENCES epreuve_examen(id) ON DELETE CASCADE,
    note        NUMERIC(4,2) NOT NULL CHECK (note >= 0 AND note <= 20),
    PRIMARY KEY (candidat_id, epreuve_id)
);
