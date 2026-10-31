-- Pièces jointes aux inspections (stockées en base : sauvegardées avec le reste) et codes de double authentification.

CREATE TABLE piece_jointe (
    id             BIGSERIAL PRIMARY KEY,
    inspection_id  BIGINT       NOT NULL REFERENCES inspection(id) ON DELETE CASCADE,
    ajoutee_par_id BIGINT       NOT NULL REFERENCES utilisateur(id),
    nom            VARCHAR(255) NOT NULL,
    type_mime      VARCHAR(100) NOT NULL,
    taille         BIGINT       NOT NULL,
    contenu        BYTEA        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_piece_jointe_inspection ON piece_jointe(inspection_id);

CREATE TABLE code_verification (
    utilisateur_id BIGINT      PRIMARY KEY REFERENCES utilisateur(id) ON DELETE CASCADE,
    code_hash      VARCHAR(100) NOT NULL,
    expire_le      TIMESTAMPTZ NOT NULL,
    essais         INT         NOT NULL DEFAULT 0
);
