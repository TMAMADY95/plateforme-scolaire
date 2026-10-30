-- Annonces publiées pour une classe (enseignant ou direction) : lues par l'équipe, les élèves et leurs parents.
CREATE TABLE annonce (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    classe_id  BIGINT NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    auteur_id  BIGINT NOT NULL REFERENCES utilisateur(id),
    titre      VARCHAR(200) NOT NULL,
    contenu    VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_annonce_classe ON annonce(classe_id, created_at DESC);
