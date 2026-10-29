-- Coefficient d'une matière dans une classe (pondère la moyenne générale). Absent = 1.
CREATE TABLE coefficient_matiere (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    classe_id    BIGINT NOT NULL REFERENCES classe(id) ON DELETE CASCADE,
    matiere_id   BIGINT NOT NULL REFERENCES matiere(id),
    coefficient  NUMERIC(4,2) NOT NULL,
    CONSTRAINT uq_coef_classe_matiere UNIQUE (classe_id, matiere_id),
    CONSTRAINT ck_coef_matiere CHECK (coefficient > 0)
);
