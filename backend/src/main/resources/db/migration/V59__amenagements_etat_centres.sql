-- Candidats en situation de handicap (type et aménagement accordé) et caractéristiques des centres d'examen
-- (salles de réserve, état des installations, accessibilité, distances, risques).
ALTER TABLE candidat_examen
    ADD COLUMN handicap     VARCHAR(15),
    ADD COLUMN amenagement  VARCHAR(200),
    ADD CONSTRAINT ck_candidat_handicap CHECK (handicap IS NULL OR handicap IN ('MOTEUR', 'VISUEL', 'AUDITIF', 'INTELLECTUEL', 'AUTRE'));

CREATE TABLE caracteristiques_centre (
    centre_id            BIGINT PRIMARY KEY REFERENCES centre_examen(id) ON DELETE CASCADE,
    salles_reserve       INTEGER NOT NULL DEFAULT 0,
    etat_tables_bancs    VARCHAR(10),
    etat_toilettes       VARCHAR(10),
    etat_eclairage       VARCHAR(10),
    etat_ventilation     VARCHAR(10),
    accessible           BOOLEAN,
    distance_max_km      INTEGER,
    temps_max_min        INTEGER,
    risque_inondation    BOOLEAN NOT NULL DEFAULT FALSE,
    risque_enclavement   BOOLEAN NOT NULL DEFAULT FALSE,
    risque_insecurite    BOOLEAN NOT NULL DEFAULT FALSE,
    observation          VARCHAR(500),
    mis_a_jour_le        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_carac_nombres CHECK (salles_reserve >= 0 AND COALESCE(distance_max_km, 0) >= 0 AND COALESCE(temps_max_min, 0) >= 0),
    CONSTRAINT ck_carac_etats CHECK (COALESCE(etat_tables_bancs, 'BON') IN ('BON', 'MOYEN', 'MAUVAIS') AND COALESCE(etat_toilettes, 'BON') IN ('BON', 'MOYEN', 'MAUVAIS')
        AND COALESCE(etat_eclairage, 'BON') IN ('BON', 'MOYEN', 'MAUVAIS') AND COALESCE(etat_ventilation, 'BON') IN ('BON', 'MOYEN', 'MAUVAIS'))
);
