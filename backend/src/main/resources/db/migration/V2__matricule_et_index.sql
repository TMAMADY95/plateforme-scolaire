-- Numérotation des matricules élèves (format appliqué par le service : ELV-00000001)
CREATE SEQUENCE eleve_matricule_seq START WITH 1 INCREMENT BY 1;

-- Recherche des inscriptions d'un élève
CREATE INDEX ix_inscription_eleve ON inscription(eleve_id);
