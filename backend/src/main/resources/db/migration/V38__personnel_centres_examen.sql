-- Personnel d'un centre d'examen : président de la commission de surveillance, chef de centre, secrétariat,
-- surveillants (deux par salle), chefs de salle, sécurité et santé. Désignés par note de service de l'autorité éducative.
CREATE TABLE personnel_centre (
    id         BIGSERIAL PRIMARY KEY,
    centre_id  BIGINT       NOT NULL REFERENCES centre_examen(id) ON DELETE CASCADE,
    role       VARCHAR(20)  NOT NULL,
    nom        VARCHAR(150) NOT NULL,
    telephone  VARCHAR(40),
    salle      INTEGER,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_personnel_role CHECK (role IN ('PRESIDENT', 'CHEF_CENTRE', 'SECRETAIRE', 'SURVEILLANT', 'CHEF_SALLE', 'SECURITE', 'SANTE')),
    CONSTRAINT ck_personnel_salle CHECK ((role IN ('SURVEILLANT', 'CHEF_SALLE')) = (salle IS NOT NULL)),
    CONSTRAINT ck_personnel_salle_pos CHECK (salle IS NULL OR salle >= 1)
);
CREATE INDEX ix_personnel_centre ON personnel_centre(centre_id, role);
