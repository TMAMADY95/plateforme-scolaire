-- Déroulement d'une épreuve dans un centre : circuit du pli de sujets (DGECS -> IRE -> DPE/DCE -> président de la commission de
-- surveillance), ouverture du pli en présence de deux témoins (aucune autorité n'ouvre seule), puis rapport de surveillance
-- (présents, absents, éliminés, copies ramassées).
CREATE TABLE epreuve_centre (
    id                  BIGSERIAL PRIMARY KEY,
    centre_id           BIGINT       NOT NULL REFERENCES centre_examen(id) ON DELETE CASCADE,
    epreuve_id          BIGINT       NOT NULL REFERENCES epreuve_examen(id) ON DELETE CASCADE,
    nb_exemplaires      INTEGER      NOT NULL DEFAULT 0,
    statut_pli          VARCHAR(15)  NOT NULL DEFAULT 'A_CONDITIONNER',
    conditionne_le      TIMESTAMPTZ,
    expedie_le          TIMESTAMPTZ,
    recu_dpe_le         TIMESTAMPTZ,
    recu_president_le   TIMESTAMPTZ,
    ouvert_le           TIMESTAMPTZ,
    ouvert_par          VARCHAR(150),
    temoin1             VARCHAR(150),
    temoin2             VARCHAR(150),
    composition_debut   TIMESTAMPTZ,
    composition_fin     TIMESTAMPTZ,
    presents            INTEGER,
    absents             INTEGER,
    elimines            INTEGER,
    copies_ramassees    INTEGER,
    observations        VARCHAR(2000),
    rapport_clos        BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_epreuve_centre UNIQUE (centre_id, epreuve_id),
    CONSTRAINT ck_pli_statut CHECK (statut_pli IN ('A_CONDITIONNER', 'CONDITIONNE', 'EXPEDIE_IRE', 'RECU_DPE', 'RECU_PRESIDENT', 'OUVERT')),
    CONSTRAINT ck_pli_ouverture CHECK (statut_pli <> 'OUVERT' OR (ouvert_par IS NOT NULL AND temoin1 IS NOT NULL AND temoin2 IS NOT NULL
        AND ouvert_par <> temoin1 AND ouvert_par <> temoin2 AND temoin1 <> temoin2)),
    CONSTRAINT ck_rapport_nombres CHECK (COALESCE(presents, 0) >= 0 AND COALESCE(absents, 0) >= 0 AND COALESCE(elimines, 0) >= 0
        AND COALESCE(copies_ramassees, 0) >= 0 AND nb_exemplaires >= 0)
);
