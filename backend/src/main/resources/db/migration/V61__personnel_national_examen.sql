-- Personnel national mobilisé pour un examen (jurys, superviseurs, inspecteurs, agents informatiques) et besoins déclarés par fonction.
CREATE TABLE personnel_examen (
    id          BIGSERIAL PRIMARY KEY,
    examen_id   BIGINT NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    fonction    VARCHAR(15) NOT NULL,
    nom         VARCHAR(150) NOT NULL,
    telephone   VARCHAR(30),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_personnel_examen_fonction CHECK (fonction IN ('JURY', 'SUPERVISEUR', 'INSPECTEUR', 'INFORMATIQUE')),
    CONSTRAINT ux_personnel_examen UNIQUE (examen_id, fonction, nom)
);

CREATE TABLE besoin_personnel_examen (
    id         BIGSERIAL PRIMARY KEY,
    examen_id  BIGINT NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    fonction   VARCHAR(15) NOT NULL,
    requis     INTEGER NOT NULL,
    CONSTRAINT ck_besoin_fonction CHECK (fonction IN ('SECURITE', 'SANTE', 'CORRECTEUR', 'JURY', 'SUPERVISEUR', 'INSPECTEUR', 'INFORMATIQUE')),
    CONSTRAINT ck_besoin_requis CHECK (requis >= 0),
    CONSTRAINT ux_besoin_personnel UNIQUE (examen_id, fonction)
);
