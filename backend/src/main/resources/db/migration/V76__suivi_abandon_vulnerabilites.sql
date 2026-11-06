-- Abandon scolaire en cours d'année et profil de vulnérabilité de l'élève (orphelin, handicap).
-- Données sensibles : visibles de la direction de l'établissement ; le ministère ne voit que des totaux.
CREATE TABLE abandon_scolaire (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inscription_id    BIGINT       NOT NULL UNIQUE REFERENCES inscription(id),
    eleve_id          BIGINT       NOT NULL REFERENCES eleve(id),
    etablissement_id  BIGINT       NOT NULL REFERENCES etablissement(id),
    annee_scolaire_id BIGINT       NOT NULL REFERENCES annee_scolaire(id),
    date_abandon      DATE         NOT NULL,
    motif             VARCHAR(30)  NOT NULL,
    observation       VARCHAR(500),
    signale_par       VARCHAR(255),
    signale_le        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_abandon_motif CHECK (motif IN ('MALADIE', 'MARIAGE_PRECOCE', 'GROSSESSE', 'TRAVAIL', 'DISTANCE', 'DIFFICULTES_FINANCIERES',
                                                 'DEMENAGEMENT', 'DECES', 'ECHEC_SCOLAIRE', 'AUTRE'))
);
CREATE INDEX idx_abandon_etab_annee ON abandon_scolaire (etablissement_id, annee_scolaire_id);

CREATE TABLE profil_vulnerabilite_eleve (
    eleve_id       BIGINT PRIMARY KEY REFERENCES eleve(id) ON DELETE CASCADE,
    orphelin       VARCHAR(10) NOT NULL DEFAULT 'AUCUN' CHECK (orphelin IN ('AUCUN', 'PERE', 'MERE', 'DEUX')),
    handicap       VARCHAR(15) CHECK (handicap IN ('MOTEUR', 'VISUEL', 'AUDITIF', 'INTELLECTUEL', 'AUTRE')),
    mis_a_jour_le  TIMESTAMPTZ NOT NULL DEFAULT now(),
    mis_a_jour_par VARCHAR(255)
);
