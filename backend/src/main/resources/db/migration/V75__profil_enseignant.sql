-- Fiche professionnelle de l'enseignant (rubriques de l'annuaire statistique : formation, statut, matière, âge, handicap).
-- Données personnelles : visibles de la direction de l'établissement ; le ministère ne voit que des totaux.
CREATE TABLE profil_enseignant (
    enseignant_id            BIGINT PRIMARY KEY REFERENCES enseignant(id) ON DELETE CASCADE,
    date_naissance           DATE,
    sexe                     CHAR(1)      CHECK (sexe IN ('F', 'M')),
    statut                   VARCHAR(20)  CHECK (statut IN ('FONCTIONNAIRE', 'CONTRACTUEL', 'VOLONTAIRE', 'VACATAIRE')),
    formation_academique     VARCHAR(20)  CHECK (formation_academique IN ('BAC', 'BAC_2', 'LICENCE', 'MASTER', 'DOCTORAT', 'AUTRE')),
    formation_professionnelle VARCHAR(20) CHECK (formation_professionnelle IN ('AUCUNE', 'ENI', 'ENS', 'AUTRE')),
    matiere_principale_id    BIGINT       REFERENCES matiere(id),
    date_prise_service       DATE,
    handicap                 VARCHAR(15)  CHECK (handicap IN ('MOTEUR', 'VISUEL', 'AUDITIF', 'INTELLECTUEL', 'AUTRE')),
    mis_a_jour_le            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    mis_a_jour_par           VARCHAR(255)
);
CREATE INDEX idx_profil_enseignant_matiere ON profil_enseignant (matiere_principale_id);
