-- Plans d'action après inspection, et journal des alertes envoyées.

CREATE TABLE plan_action (
    id               BIGSERIAL PRIMARY KEY,
    etablissement_id BIGINT        NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    inspection_id    BIGINT        REFERENCES inspection(id) ON DELETE SET NULL,
    cree_par_id      BIGINT        NOT NULL REFERENCES utilisateur(id),
    description      VARCHAR(2000) NOT NULL,
    echeance         DATE          NOT NULL,
    statut           VARCHAR(10)   NOT NULL DEFAULT 'A_FAIRE' CHECK (statut IN ('A_FAIRE', 'EN_COURS', 'TERMINE', 'ANNULE')),
    note             VARCHAR(1000),
    created_at       TIMESTAMPTZ   NOT NULL,
    termine_le       TIMESTAMPTZ,
    dernier_rappel   TIMESTAMPTZ
);
CREATE INDEX ix_plan_action_etab ON plan_action(etablissement_id, statut);
CREATE INDEX ix_plan_action_echeance ON plan_action(echeance) WHERE statut IN ('A_FAIRE', 'EN_COURS');

CREATE TABLE alerte_envoyee (
    id          BIGSERIAL PRIMARY KEY,
    envoyee_le  TIMESTAMPTZ   NOT NULL,
    type        VARCHAR(20)   NOT NULL,
    canal       VARCHAR(10)   NOT NULL CHECK (canal IN ('EMAIL', 'SMS')),
    destinataire VARCHAR(255) NOT NULL,
    resume      VARCHAR(500)  NOT NULL,
    reussie     BOOLEAN       NOT NULL
);
CREATE INDEX ix_alerte_envoyee_date ON alerte_envoyee(envoyee_le DESC);
