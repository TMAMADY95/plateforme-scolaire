-- Structures administratives des examens : DGECS (national), direction locale DPE/DCE (zone préfecture ou commune)
-- et chef de centre (accès limité à ses centres d'examen).
ALTER TABLE affectation_role DROP CONSTRAINT ck_role;
ALTER TABLE affectation_role ADD CONSTRAINT ck_role CHECK (role IN (
    'ADMIN_TECHNIQUE', 'MINISTERE', 'DGECS', 'DIRECTION_REGIONALE', 'DIRECTION_LOCALE', 'CHEF_CENTRE',
    'DIRECTION_ETABLISSEMENT', 'PERSONNEL_ADMIN', 'ENSEIGNANT', 'PARENT', 'ELEVE'));

-- Un chef de centre n'a accès qu'aux centres qui lui sont confiés (un centre appartient à un examen).
CREATE TABLE acces_centre (
    id              BIGSERIAL PRIMARY KEY,
    utilisateur_id  BIGINT      NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    centre_id       BIGINT      NOT NULL REFERENCES centre_examen(id) ON DELETE CASCADE,
    cree_le         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_acces_centre UNIQUE (utilisateur_id, centre_id)
);
CREATE INDEX ix_acces_centre_centre ON acces_centre(centre_id);
