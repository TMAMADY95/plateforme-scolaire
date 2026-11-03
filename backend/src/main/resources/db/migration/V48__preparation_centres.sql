-- Préparation matérielle d'un centre d'examen : sept travaux à accomplir avant les épreuves (règlement général).
-- Le centre est « prêt » quand les sept sont faits.
CREATE TABLE preparation_centre (
    id           BIGSERIAL PRIMARY KEY,
    centre_id    BIGINT       NOT NULL REFERENCES centre_examen(id) ON DELETE CASCADE,
    element      VARCHAR(15)  NOT NULL,
    fait         BOOLEAN      NOT NULL DEFAULT FALSE,
    observation  VARCHAR(500),
    fait_le      TIMESTAMPTZ,
    fait_par     VARCHAR(255),
    CONSTRAINT uq_preparation_centre UNIQUE (centre_id, element),
    CONSTRAINT ck_preparation_element CHECK (element IN
        ('SALLES', 'TABLES_BANCS', 'AFFICHAGE', 'SECRETARIAT', 'SECURITE', 'PERSONNEL', 'MATERIEL'))
);
