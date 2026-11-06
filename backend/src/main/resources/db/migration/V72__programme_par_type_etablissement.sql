-- Le programme dépend aussi du type d'établissement (classique, franco-arabe, technique…).
-- type_etablissement_id NULL : programme commun à tous les types. Une ligne propre à un type remplace, pour la même matière,
-- la ligne commune ; elle peut aussi ajouter une matière (arabe en franco-arabe, par exemple).
ALTER TABLE programme_matiere ADD COLUMN type_etablissement_id BIGINT REFERENCES type_etablissement(id);

DROP INDEX uq_programme_matiere;
CREATE UNIQUE INDEX uq_programme_matiere ON programme_matiere (annee_scolaire_id, niveau_id, COALESCE(serie, ''), COALESCE(type_etablissement_id, 0), matiere_id);
CREATE INDEX idx_programme_type ON programme_matiere (type_etablissement_id) WHERE type_etablissement_id IS NOT NULL;
