-- =====================================================================
-- Paiements des factures (Étape 3b)
--
-- Ne stocke AUCUNE donnée bancaire : ni numéro de carte, ni code secret.
-- Pour un paiement en ligne, seule la référence de transaction du prestataire est conservée.
-- =====================================================================

CREATE TABLE paiement (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    facture_id         BIGINT NOT NULL REFERENCES facture(id),
    montant            NUMERIC(12,2) NOT NULL,
    devise             VARCHAR(3) NOT NULL,
    mode               VARCHAR(20) NOT NULL,
    statut             VARCHAR(20) NOT NULL,
    reference_interne  VARCHAR(40) NOT NULL,        -- notre numéro : PAY-xxxxxxxx
    reference_externe  VARCHAR(100),                -- prestataire, numéro de chèque ou de virement
    enregistre_par     BIGINT REFERENCES utilisateur(id),   -- agent qui a saisi (paiement hors ligne)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    valide_le          TIMESTAMPTZ,
    CONSTRAINT ck_paiement_montant CHECK (montant > 0),
    CONSTRAINT ck_paiement_mode    CHECK (mode IN ('ESPECES', 'VIREMENT', 'CHEQUE', 'MOBILE_MONEY')),
    CONSTRAINT ck_paiement_statut  CHECK (statut IN ('EN_ATTENTE', 'VALIDE', 'ECHEC', 'ANNULE'))
);

CREATE UNIQUE INDEX ux_paiement_ref_interne ON paiement(reference_interne);
-- Une même référence (chèque, virement, transaction) ne peut pas être saisie deux fois pour un mode.
CREATE UNIQUE INDEX ux_paiement_ref_externe ON paiement(mode, reference_externe) WHERE reference_externe IS NOT NULL;
CREATE INDEX ix_paiement_facture ON paiement(facture_id, statut);
