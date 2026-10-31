-- Cahier de texte numérique et justifications d'absence demandées en ligne par les familles.

CREATE TABLE cahier_texte (
    id            BIGSERIAL PRIMARY KEY,
    classe_id     BIGINT        NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    matiere_id    BIGINT        NOT NULL REFERENCES matiere(id),
    enseignant_id BIGINT        NOT NULL REFERENCES enseignant(id),
    date_seance   DATE          NOT NULL,
    contenu       VARCHAR(4000) NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_cahier_texte_classe ON cahier_texte(classe_id, date_seance DESC);

CREATE TABLE justification_absence (
    id             BIGSERIAL PRIMARY KEY,
    presence_id    BIGINT       NOT NULL REFERENCES presence(id) ON DELETE CASCADE,
    soumise_par_id BIGINT       NOT NULL REFERENCES utilisateur(id),
    motif          VARCHAR(500) NOT NULL,
    statut         VARCHAR(10)  NOT NULL DEFAULT 'EN_ATTENTE' CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE')),
    reponse        VARCHAR(500),
    traitee_par_id BIGINT       REFERENCES utilisateur(id),
    created_at     TIMESTAMPTZ  NOT NULL,
    traitee_le     TIMESTAMPTZ
);
CREATE INDEX ix_justification_presence ON justification_absence(presence_id);
CREATE INDEX ix_justification_statut ON justification_absence(statut);
