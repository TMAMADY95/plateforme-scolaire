-- Conditions fixées par le ministère pour qu'un établissement puisse être désigné centre d'examen.
-- Une seule ligne (id = 1). Valeurs par défaut permissives : aucune restriction tant que le ministère n'en fixe pas.
CREATE TABLE critere_centre_examen (
    id                   BIGINT PRIMARY KEY CHECK (id = 1),
    secteur_admis        VARCHAR(10) NOT NULL DEFAULT 'TOUS' CHECK (secteur_admis IN ('TOUS', 'PUBLIC')),
    autorisation_requise BOOLEAN     NOT NULL DEFAULT FALSE,
    salles_minimum       INT         NOT NULL DEFAULT 0 CHECK (salles_minimum >= 0),
    capacite_minimum     INT         NOT NULL DEFAULT 0 CHECK (capacite_minimum >= 0),
    updated_at           TIMESTAMP   NOT NULL DEFAULT now()
);

INSERT INTO critere_centre_examen (id) VALUES (1);
