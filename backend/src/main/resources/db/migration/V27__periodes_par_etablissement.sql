-- Chaque établissement peut découper son année en trimestres ou en semestres.
-- etablissement_id NULL : période nationale (définie par le ministère), utilisée par défaut.
-- Dès qu'un établissement définit ses propres périodes pour une année, ce sont elles qui s'appliquent à ses classes
-- (toutes les classes, y compris celles d'examen).

ALTER TABLE periode
    ADD COLUMN etablissement_id BIGINT REFERENCES etablissement(id) ON DELETE CASCADE,
    ADD COLUMN type_periode     VARCHAR(10) NOT NULL DEFAULT 'AUTRE'
        CHECK (type_periode IN ('TRIMESTRE', 'SEMESTRE', 'AUTRE'));

-- L'ancienne unicité (année, ordre) vaut désormais par établissement.
DO $$
DECLARE
    nom TEXT;
BEGIN
    FOR nom IN SELECT conname FROM pg_constraint WHERE conrelid = 'periode'::regclass AND contype = 'u' LOOP
        EXECUTE format('ALTER TABLE periode DROP CONSTRAINT %I', nom);
    END LOOP;
END $$;

CREATE UNIQUE INDEX uq_periode_annee_etab_ordre ON periode (annee_scolaire_id, COALESCE(etablissement_id, 0), ordre);
CREATE INDEX ix_periode_etablissement ON periode (etablissement_id) WHERE etablissement_id IS NOT NULL;
