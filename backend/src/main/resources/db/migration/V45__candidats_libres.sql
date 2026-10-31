-- Candidature libre au BEPC et au BAC : un candidat qui n'est inscrit dans aucune classe de l'année dépose son dossier
-- auprès d'un établissement. Il n'est soumis ni à la règle des 25 % d'absences ni à la participation aux évaluations.
ALTER TABLE candidat_examen ADD COLUMN libre BOOLEAN NOT NULL DEFAULT FALSE;
