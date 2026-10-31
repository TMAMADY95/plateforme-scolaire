-- Dossier de candidature : pièces fournies par chaque candidat (demande manuscrite, acte de naissance ou jugement supplétif,
-- copie légalisée du diplôme précédent, attestation d'inscription). Une ligne = une pièce reçue par l'établissement.
CREATE TABLE piece_candidature (
    id          BIGSERIAL PRIMARY KEY,
    candidat_id BIGINT      NOT NULL REFERENCES candidat_examen(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,
    reference   VARCHAR(150),
    fournie_le  DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_piece_candidature UNIQUE (candidat_id, type),
    CONSTRAINT ck_piece_type CHECK (type IN ('DEMANDE', 'ACTE_NAISSANCE', 'DIPLOME', 'ATTESTATION'))
);
