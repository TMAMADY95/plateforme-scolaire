-- Décision du conseil de classe en fin d'année pour un élève : passage, redoublement ou exclusion.
CREATE TABLE decision_fin_annee (
    id                BIGSERIAL PRIMARY KEY,
    inscription_id    BIGINT NOT NULL UNIQUE REFERENCES inscription(id) ON DELETE CASCADE,
    classe_id         BIGINT NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    moyenne_annuelle  NUMERIC(5, 2),
    suggestion        VARCHAR(12),
    decision          VARCHAR(12) NOT NULL,
    observation       VARCHAR(500),
    decide_le         DATE NOT NULL,
    decide_par        VARCHAR(150),
    CONSTRAINT ck_decision_fin_annee CHECK (decision IN ('PASSAGE', 'REDOUBLEMENT', 'EXCLUSION')),
    CONSTRAINT ck_decision_suggestion CHECK (suggestion IS NULL OR suggestion IN ('ADMIS', 'A_DELIBERER', 'REDOUBLE'))
);
CREATE INDEX ix_decision_fin_annee_classe ON decision_fin_annee(classe_id);
