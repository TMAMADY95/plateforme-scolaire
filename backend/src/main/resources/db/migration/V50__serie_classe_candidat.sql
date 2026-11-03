-- Spécialisation du lycée (11e, 12e, terminale) : sciences mathématiques, sociales ou expérimentales.
-- Facultative : renseignée sur les classes du lycée, recopiée sur les candidats à l'inscription à un examen.
ALTER TABLE classe
    ADD COLUMN serie VARCHAR(30),
    ADD CONSTRAINT ck_classe_serie CHECK (serie IS NULL OR serie IN ('SCIENCES_MATHEMATIQUES', 'SCIENCES_SOCIALES', 'SCIENCES_EXPERIMENTALES'));
ALTER TABLE candidat_examen
    ADD COLUMN serie VARCHAR(30),
    ADD CONSTRAINT ck_candidat_serie CHECK (serie IS NULL OR serie IN ('SCIENCES_MATHEMATIQUES', 'SCIENCES_SOCIALES', 'SCIENCES_EXPERIMENTALES'));
