-- Correction de V64 : le résultat « AUTHENTIQUE » (11 caractères) ne tenait pas dans VARCHAR(10).
ALTER TABLE verification_diplome ALTER COLUMN resultat TYPE VARCHAR(12);
