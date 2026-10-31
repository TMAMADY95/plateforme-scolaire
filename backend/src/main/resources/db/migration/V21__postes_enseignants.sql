-- Nombre de postes d'enseignants prévus par établissement (null = pas encore renseigné).
ALTER TABLE etablissement ADD COLUMN postes_enseignants INT CHECK (postes_enseignants >= 0);
