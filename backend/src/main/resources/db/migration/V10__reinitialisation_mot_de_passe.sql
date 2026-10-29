-- Réinitialisation du mot de passe par e-mail.
-- Le jeton envoyé par e-mail n'est JAMAIS stocké : seulement son empreinte SHA-256.
CREATE TABLE reinitialisation_mdp (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utilisateur_id  BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    jeton_hash      VARCHAR(64) NOT NULL,
    expire_le       TIMESTAMPTZ NOT NULL,
    utilise_le      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_reinit_mdp_jeton ON reinitialisation_mdp(jeton_hash);
CREATE INDEX ix_reinit_mdp_utilisateur ON reinitialisation_mdp(utilisateur_id, created_at);
