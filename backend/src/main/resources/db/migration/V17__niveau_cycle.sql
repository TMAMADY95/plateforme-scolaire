-- Chaque niveau (CP1, 6e, Terminale…) appartient à un cycle : sert aux effectifs par cycle.
-- Nul tant que le ministère ne l'a pas renseigné dans les référentiels.
ALTER TABLE niveau ADD COLUMN cycle_id BIGINT REFERENCES cycle_scolaire(id);
CREATE INDEX ix_niveau_cycle ON niveau(cycle_id);
