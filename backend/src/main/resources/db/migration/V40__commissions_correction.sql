-- Commissions de correction des copies : une commission générale par examen et, éventuellement, une sous-commission
-- par discipline (épreuve). Chaque commission a un président, un secrétaire et des correcteurs, et suit l'avancement
-- de la correction (copies reçues / corrigées).
CREATE TABLE commission_correction (
    id                  BIGSERIAL PRIMARY KEY,
    examen_id           BIGINT       NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    epreuve_id          BIGINT       REFERENCES epreuve_examen(id) ON DELETE CASCADE,
    nom                 VARCHAR(150) NOT NULL,
    lieu                VARCHAR(150),
    nb_copies           INTEGER      NOT NULL DEFAULT 0,
    nb_copies_corrigees INTEGER      NOT NULL DEFAULT 0,
    cloturee            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_commission_copies CHECK (nb_copies >= 0 AND nb_copies_corrigees >= 0 AND nb_copies_corrigees <= nb_copies)
);
CREATE UNIQUE INDEX ux_commission_examen_epreuve ON commission_correction(examen_id, COALESCE(epreuve_id, 0));

CREATE TABLE membre_commission_correction (
    id            BIGSERIAL PRIMARY KEY,
    commission_id BIGINT       NOT NULL REFERENCES commission_correction(id) ON DELETE CASCADE,
    role          VARCHAR(12)  NOT NULL,
    nom           VARCHAR(150) NOT NULL,
    telephone     VARCHAR(40),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_membre_commission_role CHECK (role IN ('PRESIDENT', 'SECRETAIRE', 'CORRECTEUR'))
);
CREATE INDEX ix_membre_commission ON membre_commission_correction(commission_id, role);
CREATE UNIQUE INDEX ux_commission_president ON membre_commission_correction(commission_id) WHERE role = 'PRESIDENT';
CREATE UNIQUE INDEX ux_commission_secretaire ON membre_commission_correction(commission_id) WHERE role = 'SECRETAIRE';
