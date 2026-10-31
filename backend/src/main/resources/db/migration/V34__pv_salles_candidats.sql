-- Numéro de procès-verbal (PV), salle et table de chaque candidat dans son centre d'examen.
-- Le règlement général impose au plus 30 candidats par salle, un candidat par table-banc, et un numéro de PV unique.
ALTER TABLE candidat_examen
    ADD COLUMN numero_pv    VARCHAR(40),
    ADD COLUMN salle        INTEGER,
    ADD COLUMN table_numero INTEGER,
    ADD CONSTRAINT ck_candidat_salle CHECK (salle IS NULL OR salle >= 1),
    ADD CONSTRAINT ck_candidat_table CHECK (table_numero IS NULL OR table_numero BETWEEN 1 AND 30),
    -- Vérifié en fin de transaction : une répartition refaite peut échanger deux numéros.
    ADD CONSTRAINT uq_candidat_pv UNIQUE (examen_id, numero_pv) DEFERRABLE INITIALLY DEFERRED;
