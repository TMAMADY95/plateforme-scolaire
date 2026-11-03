-- Approvisionnement des centres d'examen (sujets, copies, enveloppes, fournitures) : quantités prévues, expédition, réception.
CREATE TABLE livraison_centre (
    id               BIGSERIAL PRIMARY KEY,
    examen_id        BIGINT NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    centre_id        BIGINT NOT NULL REFERENCES centre_examen(id) ON DELETE CASCADE,
    article          VARCHAR(20) NOT NULL,
    quantite_prevue  INTEGER NOT NULL DEFAULT 0,
    quantite_livree  INTEGER,
    statut           VARCHAR(10) NOT NULL DEFAULT 'PREVUE',
    expediee_le      TIMESTAMP WITH TIME ZONE,
    recue_le         TIMESTAMP WITH TIME ZONE,
    observation      VARCHAR(500),
    CONSTRAINT ux_livraison_centre UNIQUE (centre_id, article),
    CONSTRAINT ck_livraison_article CHECK (article IN ('SUJETS', 'COPIES', 'ENVELOPPES', 'FOURNITURES')),
    CONSTRAINT ck_livraison_statut CHECK (statut IN ('PREVUE', 'EXPEDIEE', 'RECUE')),
    CONSTRAINT ck_livraison_quantites CHECK (quantite_prevue >= 0 AND (quantite_livree IS NULL OR quantite_livree >= 0))
);
CREATE INDEX ix_livraison_examen ON livraison_centre(examen_id);
