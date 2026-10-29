-- Messagerie entre un parent et un membre du personnel (enseignant ou direction), à propos d'un élève.
CREATE TABLE conversation (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    eleve_id           BIGINT NOT NULL REFERENCES eleve(id),
    parent_id          BIGINT NOT NULL REFERENCES utilisateur(id),
    personnel_id       BIGINT NOT NULL REFERENCES utilisateur(id),
    sujet              VARCHAR(200) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    dernier_message_le TIMESTAMPTZ NOT NULL,
    dernier_auteur_id  BIGINT NOT NULL REFERENCES utilisateur(id),
    dernier_extrait    VARCHAR(140) NOT NULL,
    parent_lu_le       TIMESTAMPTZ,
    personnel_lu_le    TIMESTAMPTZ,
    CONSTRAINT ck_conversation_participants CHECK (parent_id <> personnel_id)
);
CREATE INDEX ix_conversation_parent    ON conversation(parent_id, dernier_message_le DESC);
CREATE INDEX ix_conversation_personnel ON conversation(personnel_id, dernier_message_le DESC);

CREATE TABLE message (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id  BIGINT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    auteur_id        BIGINT NOT NULL REFERENCES utilisateur(id),
    contenu          VARCHAR(4000) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_message_conversation ON message(conversation_id, created_at, id);
