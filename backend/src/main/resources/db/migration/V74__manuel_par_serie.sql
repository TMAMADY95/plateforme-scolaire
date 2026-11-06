-- Un manuel peut être propre à une série du lycée (null : toutes les séries du niveau).
ALTER TABLE manuel_scolaire ADD COLUMN serie VARCHAR(30);
ALTER TABLE manuel_scolaire ADD CONSTRAINT ck_manuel_serie CHECK (serie IS NULL OR serie IN ('SCIENCES_MATHEMATIQUES', 'SCIENCES_SOCIALES', 'SCIENCES_EXPERIMENTALES'));
ALTER TABLE manuel_scolaire DROP CONSTRAINT uq_manuel_scolaire;
CREATE UNIQUE INDEX uq_manuel_scolaire ON manuel_scolaire (matiere_id, niveau_id, COALESCE(serie, ''), titre);
