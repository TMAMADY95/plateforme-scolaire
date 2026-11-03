-- Besoins en personnel déclarés par épreuve (jour d'examen) : complètent les besoins globaux de l'examen.
CREATE TABLE besoin_personnel_epreuve (
    id          BIGSERIAL PRIMARY KEY,
    epreuve_id  BIGINT NOT NULL REFERENCES epreuve_examen(id) ON DELETE CASCADE,
    fonction    VARCHAR(15) NOT NULL,
    requis      INTEGER NOT NULL,
    CONSTRAINT ck_besoin_epreuve_fonction CHECK (fonction IN ('SECURITE', 'SANTE', 'CORRECTEUR', 'JURY', 'SUPERVISEUR', 'INSPECTEUR', 'INFORMATIQUE')),
    CONSTRAINT ck_besoin_epreuve_requis CHECK (requis >= 0),
    CONSTRAINT ux_besoin_personnel_epreuve UNIQUE (epreuve_id, fonction)
);
