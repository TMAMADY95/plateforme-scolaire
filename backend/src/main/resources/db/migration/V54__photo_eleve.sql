-- Photo d'identité de l'élève (JPEG ou PNG), stockée à part pour ne pas alourdir les requêtes sur les élèves.
-- Elle figure sur le diplôme d'examen.
CREATE TABLE photo_eleve (
    eleve_id       BIGINT PRIMARY KEY REFERENCES eleve(id) ON DELETE CASCADE,
    type_mime      VARCHAR(100) NOT NULL,
    taille         BIGINT NOT NULL,
    contenu        BYTEA NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ajoutee_par_id BIGINT REFERENCES utilisateur(id)
);
