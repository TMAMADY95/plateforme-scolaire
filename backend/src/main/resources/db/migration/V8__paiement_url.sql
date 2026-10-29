-- Adresse de paiement chez le prestataire, pour pouvoir reprendre un paiement en cours.
ALTER TABLE paiement ADD COLUMN url_paiement VARCHAR(500);
