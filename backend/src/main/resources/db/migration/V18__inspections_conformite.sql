-- Autorisation des établissements, inspections et signalements de sécurité.

ALTER TABLE etablissement
    ADD COLUMN autorisation_statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (autorisation_statut IN ('EN_ATTENTE', 'AUTORISE', 'NON_AUTORISE', 'RETIRE')),
    ADD COLUMN autorisation_reference VARCHAR(100),
    ADD COLUMN autorisation_date DATE,
    ADD COLUMN motif_statut VARCHAR(500);

CREATE TABLE inspection (
    id               BIGSERIAL PRIMARY KEY,
    etablissement_id BIGINT       NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    inspecteur_id    BIGINT       NOT NULL REFERENCES utilisateur(id),
    date_inspection  DATE         NOT NULL,
    resultat         VARCHAR(20)  NOT NULL CHECK (resultat IN ('CONFORME', 'NON_CONFORME')),
    observations     VARCHAR(2000),
    created_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_inspection_etab ON inspection(etablissement_id, date_inspection DESC);

CREATE TABLE signalement_securite (
    id               BIGSERIAL PRIMARY KEY,
    etablissement_id BIGINT       NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    signale_par_id   BIGINT       NOT NULL REFERENCES utilisateur(id),
    categorie        VARCHAR(30)  NOT NULL CHECK (categorie IN ('INCENDIE', 'BATIMENT', 'EAU_HYGIENE', 'CLOTURE', 'AUTRE')),
    gravite          VARCHAR(10)  NOT NULL CHECK (gravite IN ('FAIBLE', 'MOYENNE', 'ELEVEE')),
    description      VARCHAR(2000) NOT NULL,
    statut           VARCHAR(10)  NOT NULL DEFAULT 'OUVERT' CHECK (statut IN ('OUVERT', 'RESOLU')),
    created_at       TIMESTAMPTZ  NOT NULL,
    resolu_at        TIMESTAMPTZ
);
CREATE INDEX ix_signalement_etab ON signalement_securite(etablissement_id, statut);
