-- Discipline : incidents commis par un élève dans son établissement et sanction décidée par la direction.
CREATE TABLE incident_disciplinaire (
    id                BIGSERIAL PRIMARY KEY,
    eleve_id          BIGINT NOT NULL REFERENCES eleve(id),
    etablissement_id  BIGINT NOT NULL REFERENCES etablissement(id),
    annee_scolaire_id BIGINT NOT NULL REFERENCES annee_scolaire(id),
    date_faits        DATE NOT NULL,
    type              VARCHAR(20) NOT NULL,
    gravite           VARCHAR(12) NOT NULL,
    description       VARCHAR(1000) NOT NULL,
    statut            VARCHAR(10) NOT NULL DEFAULT 'OUVERT',
    sanction          VARCHAR(25),
    exclusion_jours   INTEGER,
    exclusion_debut   DATE,
    observation       VARCHAR(500),
    parents_informes  BOOLEAN NOT NULL DEFAULT FALSE,
    signale_par       VARCHAR(150),
    signale_le        DATE NOT NULL,
    decide_par        VARCHAR(150),
    decide_le         DATE,
    CONSTRAINT ck_incident_disc_type CHECK (type IN ('INSOLENCE', 'VIOLENCE', 'HARCELEMENT', 'FRAUDE', 'DEGRADATION', 'VOL', 'TELEPHONE', 'TENUE', 'AUTRE')),
    CONSTRAINT ck_incident_disc_gravite CHECK (gravite IN ('LEGERE', 'GRAVE', 'TRES_GRAVE')),
    CONSTRAINT ck_incident_disc_statut CHECK (statut IN ('OUVERT', 'SANCTIONNE', 'CLASSE')),
    CONSTRAINT ck_incident_disc_sanction CHECK (sanction IS NULL OR sanction IN ('AVERTISSEMENT', 'BLAME', 'RETENUE', 'EXCLUSION_TEMPORAIRE', 'CONSEIL_DISCIPLINE', 'EXCLUSION_DEFINITIVE')),
    CONSTRAINT ck_incident_disc_jours CHECK (exclusion_jours IS NULL OR exclusion_jours > 0)
);
CREATE INDEX ix_incident_disc_etab ON incident_disciplinaire(etablissement_id, annee_scolaire_id);
CREATE INDEX ix_incident_disc_eleve ON incident_disciplinaire(eleve_id);
