-- Calendrier prévisionnel des suites de l'examen : correction, délibération, proclamation.
-- date_deliberation et date_proclamation (existantes) restent les dates réelles.
ALTER TABLE examen_national
    ADD COLUMN correction_debut_prevue DATE,
    ADD COLUMN correction_fin_prevue DATE,
    ADD COLUMN deliberation_prevue DATE,
    ADD COLUMN proclamation_prevue DATE;

ALTER TABLE examen_national
    ADD CONSTRAINT ck_examen_calendrier_previsionnel CHECK (
        (correction_debut_prevue IS NULL OR correction_fin_prevue IS NULL OR correction_fin_prevue >= correction_debut_prevue)
        AND (correction_fin_prevue IS NULL OR deliberation_prevue IS NULL OR deliberation_prevue >= correction_fin_prevue)
        AND (deliberation_prevue IS NULL OR proclamation_prevue IS NULL OR proclamation_prevue >= deliberation_prevue));
