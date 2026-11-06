-- Emploi du temps par série : une épreuve peut être propre à une série du lycée (null = commune à toutes les séries de l'examen).
ALTER TABLE epreuve_examen ADD COLUMN serie VARCHAR(30);
ALTER TABLE epreuve_examen ADD CONSTRAINT ck_epreuve_serie CHECK (serie IS NULL OR serie IN ('SCIENCES_MATHEMATIQUES', 'SCIENCES_SOCIALES', 'SCIENCES_EXPERIMENTALES'));
CREATE INDEX idx_epreuve_examen_serie ON epreuve_examen (examen_id, serie);
