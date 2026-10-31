-- Jury de délibération et proclamation des résultats d'un examen.
-- Statut des résultats : EN_COURS (saisie) -> DELIBERE (le jury a siégé) -> PROCLAME (résultats officiels, saisie close).
ALTER TABLE examen_national
    ADD COLUMN statut_resultats    VARCHAR(10) NOT NULL DEFAULT 'EN_COURS',
    ADD COLUMN date_deliberation   DATE,
    ADD COLUMN date_proclamation   DATE,
    ADD CONSTRAINT ck_examen_statut_resultats CHECK (statut_resultats IN ('EN_COURS', 'DELIBERE', 'PROCLAME'));

CREATE TABLE jury_membre (
    id         BIGSERIAL PRIMARY KEY,
    examen_id  BIGINT       NOT NULL REFERENCES examen_national(id) ON DELETE CASCADE,
    ordre      INTEGER      NOT NULL,
    fonction   VARCHAR(150) NOT NULL,
    nom        VARCHAR(150),
    present    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_jury_ordre UNIQUE (examen_id, ordre)
);
