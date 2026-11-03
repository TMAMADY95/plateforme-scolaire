-- Budget d'un examen par rubrique (montants en francs guinéens) : prévu, engagé, payé.
CREATE TABLE budget_examen (
    id             BIGSERIAL PRIMARY KEY,
    examen_id      BIGINT NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    rubrique       VARCHAR(20) NOT NULL,
    montant_prevu  BIGINT NOT NULL DEFAULT 0,
    montant_engage BIGINT NOT NULL DEFAULT 0,
    montant_paye   BIGINT NOT NULL DEFAULT 0,
    observation    VARCHAR(500),
    CONSTRAINT ux_budget_examen UNIQUE (examen_id, rubrique),
    CONSTRAINT ck_budget_rubrique CHECK (rubrique IN ('IMPRESSION', 'TRANSPORT', 'SURVEILLANCE', 'CORRECTION', 'SECURITE', 'SANTE', 'COMMUNICATION', 'AUTRE')),
    CONSTRAINT ck_budget_montants CHECK (montant_prevu >= 0 AND montant_engage >= 0 AND montant_paye >= 0 AND montant_paye <= montant_engage)
);
