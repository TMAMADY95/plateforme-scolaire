-- Barème de l'examen : 10 pour l'enseignement primaire (CEE), 20 pour le collège et le lycée (BEPC, BAC).
ALTER TABLE examen_national
    ADD COLUMN bareme SMALLINT NOT NULL DEFAULT 20,
    ADD CONSTRAINT ck_examen_bareme CHECK (bareme IN (10, 20));

UPDATE examen_national x SET bareme = 10
WHERE x.cycle_id IN (SELECT id FROM cycle_scolaire WHERE code = 'PRIMAIRE')
   OR x.niveau_id IN (SELECT n.id FROM niveau n JOIN cycle_scolaire c ON c.id = n.cycle_id WHERE c.code = 'PRIMAIRE');
