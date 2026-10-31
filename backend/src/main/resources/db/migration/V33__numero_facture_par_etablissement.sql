-- Le compteur de factures est tenu par établissement et par année (FAC-2026-000001 pour chaque école) :
-- l'unicité du numéro doit donc l'être aussi par établissement, et non sur toute la base.
DROP INDEX IF EXISTS ux_facture_numero;
CREATE UNIQUE INDEX ux_facture_numero ON facture(etablissement_id, numero);
