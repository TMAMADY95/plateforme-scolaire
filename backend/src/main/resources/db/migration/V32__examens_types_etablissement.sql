-- Types d'établissement qui participent à un examen. Aucune ligne pour un examen : tous les types participent.
CREATE TABLE examen_type_etablissement (
    examen_id               BIGINT NOT NULL REFERENCES examen_national (id) ON DELETE CASCADE,
    type_etablissement_id   BIGINT NOT NULL REFERENCES type_etablissement (id) ON DELETE CASCADE,
    PRIMARY KEY (examen_id, type_etablissement_id)
);
