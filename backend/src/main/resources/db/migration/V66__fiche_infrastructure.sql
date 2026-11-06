-- Infrastructures et équipements d'un établissement pour une année scolaire (carte scolaire) : salles, places assises, latrines, eau, électricité, équipements.
CREATE TABLE fiche_infrastructure (
    id                  BIGSERIAL PRIMARY KEY,
    etablissement_id    BIGINT NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    annee_scolaire_id   BIGINT NOT NULL REFERENCES annee_scolaire(id),
    salles_dur          INTEGER NOT NULL DEFAULT 0,
    salles_provisoires  INTEGER NOT NULL DEFAULT 0,
    salles_a_rehabiliter INTEGER NOT NULL DEFAULT 0,
    places_assises      INTEGER NOT NULL DEFAULT 0,
    latrines_filles     INTEGER NOT NULL DEFAULT 0,
    latrines_garcons    INTEGER NOT NULL DEFAULT 0,
    latrines_personnel  INTEGER NOT NULL DEFAULT 0,
    eau                 VARCHAR(10) NOT NULL DEFAULT 'AUCUNE',
    electricite         VARCHAR(10) NOT NULL DEFAULT 'AUCUNE',
    cloture             BOOLEAN NOT NULL DEFAULT FALSE,
    lavage_mains        BOOLEAN NOT NULL DEFAULT FALSE,
    bibliotheque        BOOLEAN NOT NULL DEFAULT FALSE,
    laboratoire         BOOLEAN NOT NULL DEFAULT FALSE,
    salle_informatique  BOOLEAN NOT NULL DEFAULT FALSE,
    cantine             BOOLEAN NOT NULL DEFAULT FALSE,
    terrain_sport       BOOLEAN NOT NULL DEFAULT FALSE,
    observation         VARCHAR(500),
    mis_a_jour_le       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    mis_a_jour_par      VARCHAR(150),
    CONSTRAINT uq_fiche_infrastructure UNIQUE (etablissement_id, annee_scolaire_id),
    CONSTRAINT ck_fiche_infra_nombres CHECK (salles_dur >= 0 AND salles_provisoires >= 0 AND salles_a_rehabiliter >= 0 AND places_assises >= 0
        AND latrines_filles >= 0 AND latrines_garcons >= 0 AND latrines_personnel >= 0),
    CONSTRAINT ck_fiche_infra_eau CHECK (eau IN ('AUCUNE', 'PUITS', 'FORAGE', 'RESEAU')),
    CONSTRAINT ck_fiche_infra_elec CHECK (electricite IN ('AUCUNE', 'RESEAU', 'SOLAIRE', 'GROUPE'))
);
CREATE INDEX ix_fiche_infra_annee ON fiche_infrastructure(annee_scolaire_id);
