-- Identifiant national unique de l'élève : GN-AA-NNNNNNN-CC (année d'enregistrement, numéro, clé de contrôle modulo 97).
-- La clé détecte les fautes de frappe ; elle ne rend pas le numéro secret. Le matricule ELV-… reste valide.
CREATE SEQUENCE identifiant_national_seq START WITH 1 MAXVALUE 9999999 NO CYCLE;

ALTER TABLE eleve ADD COLUMN identifiant_national VARCHAR(20);

CREATE OR REPLACE FUNCTION generer_identifiant_national(p_instant TIMESTAMP WITH TIME ZONE DEFAULT now()) RETURNS VARCHAR AS $$
DECLARE
    aa   INTEGER := (to_char(p_instant AT TIME ZONE 'Africa/Conakry', 'YY'))::INTEGER;
    n    BIGINT := nextval('identifiant_national_seq');
    base BIGINT;
    cle  INTEGER;
BEGIN
    base := aa::BIGINT * 10000000 + n;
    cle := 97 - ((base * 100) % 97)::INTEGER;
    RETURN 'GN-' || lpad(aa::TEXT, 2, '0') || '-' || lpad(n::TEXT, 7, '0') || '-' || lpad(cle::TEXT, 2, '0');
END;
$$ LANGUAGE plpgsql;

-- Élèves déjà connus : numéros attribués dans l'ordre de création
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT id, created_at FROM eleve ORDER BY id LOOP
        UPDATE eleve SET identifiant_national = generer_identifiant_national(r.created_at) WHERE id = r.id;
    END LOOP;
END $$;

CREATE FUNCTION attribuer_identifiant_national() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.identifiant_national IS NULL THEN
        NEW.identifiant_national := generer_identifiant_national(COALESCE(NEW.created_at, now()));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_eleve_identifiant_national BEFORE INSERT ON eleve
    FOR EACH ROW EXECUTE FUNCTION attribuer_identifiant_national();

ALTER TABLE eleve ALTER COLUMN identifiant_national SET NOT NULL;
ALTER TABLE eleve ADD CONSTRAINT uq_eleve_identifiant_national UNIQUE (identifiant_national);
ALTER TABLE eleve ADD CONSTRAINT ck_eleve_identifiant_national CHECK (identifiant_national ~ '^GN-[0-9]{2}-[0-9]{7}-[0-9]{2}$');

-- Fusion de dossiers en double : trace de l'opération et anciens identifiants qui continuent de désigner l'élève conservé.
CREATE TABLE fusion_eleve (
    id                          BIGSERIAL PRIMARY KEY,
    eleve_conserve_id           BIGINT NOT NULL REFERENCES eleve(id) ON DELETE CASCADE,
    matricule_absorbe           VARCHAR(50) NOT NULL,
    identifiant_national_absorbe VARCHAR(20) NOT NULL,
    motif                       VARCHAR(500) NOT NULL,
    fait_par                    VARCHAR(150),
    fait_le                     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE ancien_identifiant_eleve (
    identifiant  VARCHAR(50) PRIMARY KEY,
    eleve_id     BIGINT NOT NULL REFERENCES eleve(id) ON DELETE CASCADE,
    fusion_id    BIGINT NOT NULL REFERENCES fusion_eleve(id) ON DELETE CASCADE
);
CREATE INDEX ix_ancien_identifiant_eleve ON ancien_identifiant_eleve(eleve_id);
