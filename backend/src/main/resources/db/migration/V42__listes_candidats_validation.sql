-- Circuit de validation de la liste des candidats d'un établissement pour un examen :
-- BROUILLON -> TRANSMISE (établissement) -> VALIDEE_REGION (direction régionale) -> VALIDEE (ministère / DGECS).
-- Un rejet renvoie la liste en BROUILLON avec son motif. Hors BROUILLON, la liste n'est plus modifiable.
CREATE TABLE liste_candidats (
    id                BIGSERIAL PRIMARY KEY,
    examen_id         BIGINT      NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    etablissement_id  BIGINT      NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    statut            VARCHAR(15) NOT NULL DEFAULT 'BROUILLON',
    transmise_le      TIMESTAMPTZ,
    validee_region_le TIMESTAMPTZ,
    validee_le        TIMESTAMPTZ,
    motif_rejet       VARCHAR(500),
    CONSTRAINT uq_liste_candidats UNIQUE (examen_id, etablissement_id),
    CONSTRAINT ck_liste_statut CHECK (statut IN ('BROUILLON', 'TRANSMISE', 'VALIDEE_REGION', 'VALIDEE'))
);
