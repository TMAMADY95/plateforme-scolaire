-- Transfert d'un élève en cours d'année d'un établissement à un autre : demandé par l'école d'accueil, décidé par l'école d'origine.
CREATE TABLE transfert_eleve (
    id                       BIGSERIAL PRIMARY KEY,
    eleve_id                 BIGINT NOT NULL REFERENCES eleve(id),
    annee_scolaire_id        BIGINT NOT NULL REFERENCES annee_scolaire(id),
    etablissement_origine_id BIGINT NOT NULL REFERENCES etablissement(id),
    etablissement_dest_id    BIGINT NOT NULL REFERENCES etablissement(id),
    motif                    VARCHAR(25) NOT NULL,
    commentaire              VARCHAR(500),
    statut                   VARCHAR(10) NOT NULL DEFAULT 'DEMANDE',
    motif_refus              VARCHAR(500),
    demande_le               DATE NOT NULL,
    demande_par              VARCHAR(150),
    traite_le                DATE,
    traite_par               VARCHAR(150),
    nouvelle_inscription_id  BIGINT REFERENCES inscription(id),
    CONSTRAINT ck_transfert_statut CHECK (statut IN ('DEMANDE', 'APPROUVE', 'REFUSE', 'ANNULE')),
    CONSTRAINT ck_transfert_motif CHECK (motif IN ('DEMENAGEMENT', 'RAPPROCHEMENT_FAMILIAL', 'ORIENTATION', 'DISCIPLINE', 'AUTRE')),
    CONSTRAINT ck_transfert_etabs CHECK (etablissement_origine_id <> etablissement_dest_id)
);
-- Une seule demande ouverte par élève et par année
CREATE UNIQUE INDEX ux_transfert_ouvert ON transfert_eleve(eleve_id, annee_scolaire_id) WHERE statut = 'DEMANDE';
CREATE INDEX ix_transfert_origine ON transfert_eleve(etablissement_origine_id, annee_scolaire_id);
CREATE INDEX ix_transfert_dest ON transfert_eleve(etablissement_dest_id, annee_scolaire_id);
