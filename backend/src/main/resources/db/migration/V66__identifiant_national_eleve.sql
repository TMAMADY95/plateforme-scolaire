-- Identifiant national de l'élève (INE) : attribué une seule fois, conservé de la première inscription au baccalauréat.
-- Format : GN + AA (année d'attribution) + numéro sur 8 chiffres + clé de contrôle sur 2 chiffres (modulo 97).
-- Exemple : GN26000001234 + clé. La clé permet de détecter une faute de frappe sans interroger la base.
CREATE SEQUENCE eleve_ine_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE FUNCTION generer_ine() RETURNS VARCHAR AS $$
DECLARE
    corps BIGINT;
BEGIN
    corps := (to_char(now(), 'YY') || lpad(nextval('eleve_ine_seq')::text, 8, '0'))::BIGINT;
    RETURN 'GN' || lpad(corps::text, 10, '0') || lpad((97 - ((corps * 100) % 97))::text, 2, '0');
END;
$$ LANGUAGE plpgsql VOLATILE;

ALTER TABLE eleve ADD COLUMN ine VARCHAR(14);
UPDATE eleve SET ine = generer_ine() WHERE ine IS NULL;
ALTER TABLE eleve ALTER COLUMN ine SET DEFAULT generer_ine();
ALTER TABLE eleve ALTER COLUMN ine SET NOT NULL;
ALTER TABLE eleve ADD CONSTRAINT uq_eleve_ine UNIQUE (ine);

-- Pièce d'état civil (nécessaire pour l'inscription aux examens nationaux).
ALTER TABLE eleve ADD COLUMN type_acte_naissance VARCHAR(20);
ALTER TABLE eleve ADD COLUMN numero_acte_naissance VARCHAR(60);
ALTER TABLE eleve ADD CONSTRAINT ck_eleve_type_acte
    CHECK (type_acte_naissance IS NULL OR type_acte_naissance IN ('EXTRAIT', 'JUGEMENT_SUPPLETIF', 'AUCUN'));
CREATE INDEX ix_eleve_sans_acte ON eleve(id) WHERE type_acte_naissance IS NULL OR type_acte_naissance = 'AUCUN';
