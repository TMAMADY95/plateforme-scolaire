-- Manuels scolaires : catalogue national (par matière et niveau) et dotation de chaque établissement.
CREATE TABLE manuel_scolaire (
    id            BIGSERIAL PRIMARY KEY,
    matiere_id    BIGINT NOT NULL REFERENCES matiere(id),
    niveau_id     BIGINT NOT NULL REFERENCES niveau(id),
    titre         VARCHAR(200) NOT NULL,
    editeur       VARCHAR(150),
    annee_edition INTEGER,
    CONSTRAINT uq_manuel_scolaire UNIQUE (matiere_id, niveau_id, titre),
    CONSTRAINT ck_manuel_annee CHECK (annee_edition IS NULL OR annee_edition BETWEEN 1900 AND 2100)
);
CREATE INDEX ix_manuel_niveau ON manuel_scolaire(niveau_id);

CREATE TABLE dotation_manuel (
    id                BIGSERIAL PRIMARY KEY,
    etablissement_id  BIGINT NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    annee_scolaire_id BIGINT NOT NULL REFERENCES annee_scolaire(id),
    manuel_id         BIGINT NOT NULL REFERENCES manuel_scolaire(id),
    recus             INTEGER NOT NULL DEFAULT 0,
    distribues        INTEGER NOT NULL DEFAULT 0,
    perdus            INTEGER NOT NULL DEFAULT 0,
    observation       VARCHAR(500),
    mis_a_jour_le     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    mis_a_jour_par    VARCHAR(150),
    CONSTRAINT uq_dotation_manuel UNIQUE (etablissement_id, annee_scolaire_id, manuel_id),
    CONSTRAINT ck_dotation_quantites CHECK (recus >= 0 AND distribues >= 0 AND perdus >= 0 AND distribues + perdus <= recus)
);
CREATE INDEX ix_dotation_manuel_annee ON dotation_manuel(annee_scolaire_id);
