-- Gestion des examens par les établissements : centres d'examen, candidats et convocations.

CREATE TABLE centre_examen (
    id           BIGSERIAL PRIMARY KEY,
    examen_id    BIGINT       NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    zone_id      BIGINT       REFERENCES zone_geographique(id),
    nom          VARCHAR(150) NOT NULL,
    adresse      VARCHAR(255),
    date_debut   DATE,
    created_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_centre_examen UNIQUE (examen_id, nom)
);

CREATE TABLE candidat_examen (
    id               BIGSERIAL PRIMARY KEY,
    examen_id        BIGINT      NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    eleve_id         BIGINT      NOT NULL REFERENCES eleve(id) ON DELETE CASCADE,
    etablissement_id BIGINT      NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    classe_id        BIGINT      REFERENCES classe(id) ON DELETE SET NULL,
    centre_id        BIGINT      REFERENCES centre_examen(id) ON DELETE SET NULL,
    numero           VARCHAR(30),
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_candidat_examen UNIQUE (examen_id, eleve_id)
);
CREATE INDEX ix_candidat_examen_etab ON candidat_examen(examen_id, etablissement_id);
