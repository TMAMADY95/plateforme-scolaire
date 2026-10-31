-- Types d'établissement définis par le ministère (communautaire, franco-arabe, confessionnel, technique…).
-- Le secteur (public/privé) reste une information distincte.
CREATE TABLE type_etablissement (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    libelle     VARCHAR(150) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

INSERT INTO type_etablissement (code, libelle) VALUES
    ('CLASSIQUE', 'Établissement classique'),
    ('COMMUNAUTAIRE', 'Établissement communautaire'),
    ('FRANCO_ARABE', 'Franco-arabe'),
    ('CONFESSIONNEL', 'Confessionnel'),
    ('TECHNIQUE', 'Enseignement technique et professionnel');

ALTER TABLE etablissement ADD COLUMN type_etablissement_id BIGINT REFERENCES type_etablissement (id);
CREATE INDEX idx_etablissement_type ON etablissement (type_etablissement_id);
