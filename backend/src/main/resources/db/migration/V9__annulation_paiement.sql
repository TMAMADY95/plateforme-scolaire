-- Annulation d'un paiement validé saisi hors ligne : on garde la trace (qui, quand, pourquoi), on ne supprime rien.
ALTER TABLE paiement ADD COLUMN annule_le TIMESTAMPTZ;
ALTER TABLE paiement ADD COLUMN annule_par BIGINT REFERENCES utilisateur(id);
ALTER TABLE paiement ADD COLUMN motif_annulation VARCHAR(255);

-- Un chèque ou un virement annulé (erreur de saisie) doit pouvoir être ressaisi correctement avec la même référence.
DROP INDEX ux_paiement_ref_externe;
CREATE UNIQUE INDEX ux_paiement_ref_externe ON paiement(mode, reference_externe)
    WHERE reference_externe IS NOT NULL AND statut <> 'ANNULE';
