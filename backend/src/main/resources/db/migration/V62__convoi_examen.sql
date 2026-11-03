-- Acheminement des sujets et du matériel vers les centres d'examen : véhicule, itinéraire, horaires, carburant, incidents.
CREATE TABLE convoi_examen (
    id                    BIGSERIAL PRIMARY KEY,
    examen_id             BIGINT NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    centre_id             BIGINT NOT NULL REFERENCES centre_examen(id) ON DELETE CASCADE,
    vehicule              VARCHAR(60) NOT NULL,
    chauffeur             VARCHAR(100),
    itineraire            VARCHAR(300),
    escorte               BOOLEAN NOT NULL DEFAULT FALSE,
    depart_prevu          TIMESTAMP WITH TIME ZONE,
    arrivee_prevue        TIMESTAMP WITH TIME ZONE,
    depart_reel           TIMESTAMP WITH TIME ZONE,
    arrivee_reelle        TIMESTAMP WITH TIME ZONE,
    carburant_litres      INTEGER,
    statut                VARCHAR(10) NOT NULL DEFAULT 'PREVU',
    incident_type         VARCHAR(25),
    incident_description  VARCHAR(500),
    observation           VARCHAR(500),
    CONSTRAINT ck_convoi_statut CHECK (statut IN ('PREVU', 'EN_ROUTE', 'LIVRE')),
    CONSTRAINT ck_convoi_incident CHECK (incident_type IS NULL OR incident_type IN ('PERTE', 'RETARD', 'ERREUR_DISTRIBUTION', 'RUPTURE_MATERIEL', 'ACCIDENT', 'AUTRE')),
    CONSTRAINT ck_convoi_carburant CHECK (carburant_litres IS NULL OR carburant_litres >= 0)
);
CREATE INDEX ix_convoi_examen ON convoi_examen(examen_id);
