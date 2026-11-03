-- Rattrapage : recopie la série de la classe sur les candidats déjà inscrits à un examen (V50).
-- Renseigner d'abord la série des classes du lycée ; sans danger si relancé.
UPDATE candidat_examen c SET serie = cl.serie
FROM classe cl
WHERE c.classe_id = cl.id AND cl.serie IS NOT NULL AND c.serie IS NULL;
