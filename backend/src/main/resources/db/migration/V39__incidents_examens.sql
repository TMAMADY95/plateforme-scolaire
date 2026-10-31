-- Incidents survenus dans un centre d'examen : fraude, retard, malaise, perturbation, fuite de sujet…
-- Une fraude constatée en flagrant délit élimine le candidat (disqualification à l'examen).
CREATE TABLE incident_examen (
    id          BIGSERIAL PRIMARY KEY,
    examen_id   BIGINT       NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    centre_id   BIGINT       NOT NULL REFERENCES centre_examen(id) ON DELETE CASCADE,
    candidat_id BIGINT       REFERENCES candidat_examen(id) ON DELETE SET NULL,
    epreuve_id  BIGINT       REFERENCES epreuve_examen(id) ON DELETE SET NULL,
    type        VARCHAR(20)  NOT NULL,
    description VARCHAR(2000) NOT NULL,
    survenu_le  TIMESTAMPTZ  NOT NULL,
    signale_par VARCHAR(150),
    statut      VARCHAR(10)  NOT NULL DEFAULT 'OUVERT',
    decision    VARCHAR(1000),
    elimine     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_incident_type CHECK (type IN ('FRAUDE', 'RETARD', 'MALAISE', 'PERTURBATION', 'FUITE_SUJET', 'AUTRE')),
    CONSTRAINT ck_incident_statut CHECK (statut IN ('OUVERT', 'TRAITE')),
    CONSTRAINT ck_incident_elimine CHECK (NOT elimine OR (type = 'FRAUDE' AND candidat_id IS NOT NULL))
);
CREATE INDEX ix_incident_centre ON incident_examen(centre_id, statut);
CREATE INDEX ix_incident_examen ON incident_examen(examen_id, type);
