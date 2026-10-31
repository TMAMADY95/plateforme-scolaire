-- État des alertes d'inspection urgente : évite de renvoyer la même alerte chaque jour.
CREATE TABLE alerte_urgence_etat (
    etablissement_id BIGINT PRIMARY KEY REFERENCES etablissement(id) ON DELETE CASCADE,
    urgent           BOOLEAN     NOT NULL,
    derniere_alerte  TIMESTAMPTZ
);
