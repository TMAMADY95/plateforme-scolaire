-- Banque de sujets : trois variantes (A, B, C) par épreuve, élaborées puis validées par la commission des sujets.
-- Le jour de l'épreuve, une variante validée est tirée au sort ; en cas de fuite elle est déclarée compromise
-- et remplacée par une variante de réserve. Le contenu des sujets reste confidentiel : on n'enregistre que leur suivi.
CREATE TABLE sujet_examen (
    id         BIGSERIAL PRIMARY KEY,
    epreuve_id BIGINT       NOT NULL REFERENCES epreuve_examen(id) ON DELETE CASCADE,
    variante   VARCHAR(1)   NOT NULL,
    intitule   VARCHAR(200) NOT NULL,
    auteurs    VARCHAR(300),
    statut     VARCHAR(12)  NOT NULL DEFAULT 'ELABORATION',
    valide_le  TIMESTAMPTZ,
    tire_le    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_sujet_variante UNIQUE (epreuve_id, variante),
    CONSTRAINT ck_sujet_variante CHECK (variante IN ('A', 'B', 'C')),
    CONSTRAINT ck_sujet_statut CHECK (statut IN ('ELABORATION', 'VALIDE', 'TIRE', 'COMPROMIS'))
);
-- Un seul sujet en cours d'utilisation par épreuve.
CREATE UNIQUE INDEX ux_sujet_tire ON sujet_examen(epreuve_id) WHERE statut = 'TIRE';
