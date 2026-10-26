-- Sécurité des comptes : changement de mot de passe obligatoire, révocation des jetons, verrouillage
ALTER TABLE utilisateur
    ADD COLUMN doit_changer_mot_de_passe BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN mot_de_passe_modifie_le   TIMESTAMPTZ,
    ADD COLUMN echecs_connexion          INT         NOT NULL DEFAULT 0,
    ADD COLUMN verrouille_jusqu_a        TIMESTAMPTZ;
