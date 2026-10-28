-- =====================================================================
-- La plateforme n'a pas de pays cible : aucune devise par défaut en base.
-- La devise est choisie à la création de chaque frais.
-- =====================================================================
ALTER TABLE frais    ALTER COLUMN devise DROP DEFAULT;
ALTER TABLE facture  ALTER COLUMN devise DROP DEFAULT;
