# Lot annulation des paiements hors ligne

Backend : `POST /api/factures/{factureId}/paiements/{paiementId}/annulation` {motif} (direction / personnel admin
de l'établissement). Seuls les paiements VALIDE en espèces, virement ou chèque s'annulent ; un paiement en ligne
se rembourse chez le prestataire (409). Le paiement passe en ANNULE (rien n'est supprimé : date, auteur, motif),
la facture retrouve son solde, l'audit enregistre ANNULATION_PAIEMENT. Migration V9 (colonnes + index de
référence qui ignore les paiements annulés : on peut ressaisir un chèque avec la même référence).
`mvn clean verify` (4 tests ajoutés dans PaiementServiceTest).

Web : Finances > Paiements > bouton « Annuler » (motif obligatoire). `npm run build`.
