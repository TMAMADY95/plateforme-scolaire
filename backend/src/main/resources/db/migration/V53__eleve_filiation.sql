-- État civil complet pour les diplômes : lieu de naissance, père et mère (facultatifs).
ALTER TABLE eleve
    ADD COLUMN lieu_naissance VARCHAR(150),
    ADD COLUMN nom_pere       VARCHAR(150),
    ADD COLUMN nom_mere       VARCHAR(150);
