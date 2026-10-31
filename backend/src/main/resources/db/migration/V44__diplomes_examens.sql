-- Diplômes (attestations officielles) délivrés aux candidats admis après la proclamation des résultats.
-- Chaque diplôme a un numéro et un code de vérification : toute personne peut en contrôler l'authenticité en ligne.
CREATE TABLE diplome_examen (
    id                BIGSERIAL PRIMARY KEY,
    candidat_id       BIGINT       NOT NULL UNIQUE REFERENCES candidat_examen(id) ON DELETE CASCADE,
    numero            VARCHAR(40)  NOT NULL UNIQUE,
    code              VARCHAR(12)  NOT NULL UNIQUE,
    mention           VARCHAR(12)  NOT NULL,
    delivre_le        DATE         NOT NULL,
    revoque           BOOLEAN      NOT NULL DEFAULT FALSE,
    motif_revocation  VARCHAR(500),
    revoque_le        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_diplome_mention CHECK (mention IN ('PASSABLE', 'ASSEZ_BIEN', 'BIEN', 'TRES_BIEN')),
    CONSTRAINT ck_diplome_revocation CHECK (NOT revoque OR motif_revocation IS NOT NULL)
);
