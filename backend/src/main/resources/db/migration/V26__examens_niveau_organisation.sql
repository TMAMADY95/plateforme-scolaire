-- Le niveau qui organise un examen est une donnée, pas une règle : le CEE (fin de 6e année / CM2) est organisé par
-- commune ou par préfecture, le BEPC (10e année / 3e) par région, le baccalauréat (terminale) au niveau national.
-- niveau_organisation vaut NATIONAL ou l'un des types de zone du référentiel (COMMUNE, PREFECTURE, REGION…).

ALTER TABLE examen_national
    ADD COLUMN niveau_id           BIGINT REFERENCES niveau(id),
    ADD COLUMN niveau_organisation VARCHAR(50) NOT NULL DEFAULT 'NATIONAL';

-- Modèles d'examens proposés à la création ; le ministère peut les modifier ou en ajouter.
CREATE TABLE type_examen (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(30)  NOT NULL UNIQUE,
    libelle             VARCHAR(150) NOT NULL,
    niveau_organisation VARCHAR(50)  NOT NULL,
    niveau_id           BIGINT REFERENCES niveau(id),
    cycle_id            BIGINT REFERENCES cycle_scolaire(id)
);

INSERT INTO type_examen (code, libelle, niveau_organisation) VALUES
    ('CEE',  'Certificat de fin d''études élémentaires (CEE)', 'PREFECTURE'),
    ('BEPC', 'Brevet d''études du premier cycle (BEPC)',       'REGION'),
    ('BAC',  'Baccalauréat',                                    'NATIONAL');
