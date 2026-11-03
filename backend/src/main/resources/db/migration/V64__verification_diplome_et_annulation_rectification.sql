-- Traçabilité des vérifications publiques de diplômes (détection de faux) et annulation d'une rectification de note.
CREATE TABLE verification_diplome (
    id          BIGSERIAL PRIMARY KEY,
    code_saisi  VARCHAR(20) NOT NULL,
    resultat    VARCHAR(10) NOT NULL,
    verifie_le  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_verification_resultat CHECK (resultat IN ('AUTHENTIQUE', 'REVOQUE', 'INCONNU'))
);
CREATE INDEX ix_verification_diplome_date ON verification_diplome(verifie_le);
CREATE INDEX ix_verification_diplome_code ON verification_diplome(code_saisi);

ALTER TABLE rectification_note ADD COLUMN annulee_le TIMESTAMP WITH TIME ZONE;
ALTER TABLE rectification_note ADD COLUMN annulee_par VARCHAR(150);
ALTER TABLE rectification_note ADD COLUMN motif_annulation VARCHAR(500);
