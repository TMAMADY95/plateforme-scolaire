-- Liaison école-famille : rendez-vous parents-enseignants et autorisations de sortie.

CREATE TABLE rendez_vous (
    id             BIGSERIAL PRIMARY KEY,
    eleve_id       BIGINT       NOT NULL REFERENCES eleve(id) ON DELETE CASCADE,
    enseignant_id  BIGINT       NOT NULL REFERENCES enseignant(id) ON DELETE CASCADE,
    demande_par_id BIGINT       NOT NULL REFERENCES utilisateur(id),
    date_heure     TIMESTAMP    NOT NULL,
    motif          VARCHAR(500) NOT NULL,
    statut         VARCHAR(10)  NOT NULL DEFAULT 'DEMANDE' CHECK (statut IN ('DEMANDE', 'CONFIRME', 'REFUSE', 'ANNULE')),
    reponse        VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_rendez_vous_eleve ON rendez_vous(eleve_id, date_heure DESC);
CREATE INDEX ix_rendez_vous_enseignant ON rendez_vous(enseignant_id, date_heure DESC);

CREATE TABLE autorisation_sortie (
    id               BIGSERIAL PRIMARY KEY,
    eleve_id         BIGINT       NOT NULL REFERENCES eleve(id) ON DELETE CASCADE,
    etablissement_id BIGINT       NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    demandee_par_id  BIGINT       NOT NULL REFERENCES utilisateur(id),
    date_sortie      DATE         NOT NULL,
    heure_sortie     TIME         NOT NULL,
    motif            VARCHAR(500) NOT NULL,
    accompagnateur   VARCHAR(150),
    statut           VARCHAR(10)  NOT NULL DEFAULT 'DEMANDEE' CHECK (statut IN ('DEMANDEE', 'ACCEPTEE', 'REFUSEE')),
    reponse          VARCHAR(500),
    traitee_par_id   BIGINT       REFERENCES utilisateur(id),
    created_at       TIMESTAMPTZ  NOT NULL,
    traitee_le       TIMESTAMPTZ
);
CREATE INDEX ix_sortie_eleve ON autorisation_sortie(eleve_id, date_sortie DESC);
CREATE INDEX ix_sortie_etab ON autorisation_sortie(etablissement_id, statut);
