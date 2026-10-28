-- =====================================================================
-- Plateforme de gestion scolaire : frais scolaires et factures (Étape 3a)
-- Migration Flyway : backend/src/main/resources/db/migration/
--
-- Ne stocke AUCUNE donnée bancaire. Les paiements (Étape 3b) ne
-- conserveront qu'une référence de transaction fournie par le prestataire.
-- =====================================================================

-- ---------- Grille des frais autorisés ----------
-- Un frais est défini par établissement, année scolaire, et éventuellement un niveau.
-- niveau_id NULL = le frais s'applique à tous les niveaux de l'établissement.

CREATE TABLE frais (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    etablissement_id  BIGINT NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    annee_scolaire_id BIGINT NOT NULL REFERENCES annee_scolaire(id),
    niveau_id         BIGINT REFERENCES niveau(id),
    libelle           VARCHAR(150) NOT NULL,
    montant           NUMERIC(12,2) NOT NULL,
    devise            VARCHAR(3) NOT NULL DEFAULT 'XOF',
    echeance          DATE,
    obligatoire       BOOLEAN NOT NULL DEFAULT TRUE,
    actif             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_frais_montant CHECK (montant >= 0)
);

-- Un même libellé ne peut pas être défini deux fois pour le même couple
-- établissement / année / niveau. Deux index partiels, car PostgreSQL
-- considère NULL comme distinct dans une contrainte d'unicité classique.
CREATE UNIQUE INDEX ux_frais_avec_niveau
    ON frais (etablissement_id, annee_scolaire_id, niveau_id, lower(libelle))
    WHERE niveau_id IS NOT NULL;
CREATE UNIQUE INDEX ux_frais_sans_niveau
    ON frais (etablissement_id, annee_scolaire_id, lower(libelle))
    WHERE niveau_id IS NULL;

CREATE INDEX ix_frais_etab_annee ON frais(etablissement_id, annee_scolaire_id);

-- ---------- Factures ----------
-- Une facture par élève et par frais. Le montant dû est figé au moment de
-- l'émission : modifier la grille plus tard ne change pas les factures émises.

CREATE TABLE facture (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    frais_id          BIGINT NOT NULL REFERENCES frais(id),
    eleve_id          BIGINT NOT NULL REFERENCES eleve(id),
    etablissement_id  BIGINT NOT NULL REFERENCES etablissement(id),
    annee_scolaire_id BIGINT NOT NULL REFERENCES annee_scolaire(id),
    numero            VARCHAR(40) NOT NULL,
    libelle           VARCHAR(150) NOT NULL,       -- recopié du frais à l'émission
    montant_du        NUMERIC(12,2) NOT NULL,
    montant_exonere   NUMERIC(12,2) NOT NULL DEFAULT 0,
    devise            VARCHAR(3) NOT NULL DEFAULT 'XOF',
    echeance          DATE,
    statut            VARCHAR(20) NOT NULL DEFAULT 'IMPAYEE',
    date_emission     DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_facture_statut CHECK (statut IN
        ('IMPAYEE', 'PARTIELLE', 'PAYEE', 'EXONEREE', 'ANNULEE')),
    CONSTRAINT ck_facture_montant_du     CHECK (montant_du >= 0),
    CONSTRAINT ck_facture_montant_exonere CHECK (montant_exonere >= 0),
    CONSTRAINT ck_facture_exonere_borne   CHECK (montant_exonere <= montant_du)
);

CREATE UNIQUE INDEX ux_facture_numero ON facture(numero);
-- Une seule facture par élève et par frais : la génération est idempotente.
CREATE UNIQUE INDEX ux_facture_frais_eleve ON facture(frais_id, eleve_id);
CREATE INDEX ix_facture_eleve  ON facture(eleve_id, statut);
CREATE INDEX ix_facture_etab   ON facture(etablissement_id, annee_scolaire_id, statut);
CREATE INDEX ix_facture_echeance ON facture(echeance) WHERE statut IN ('IMPAYEE', 'PARTIELLE');

-- ---------- Exonérations (bourses, remises) ----------

CREATE TABLE exoneration (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    facture_id      BIGINT NOT NULL REFERENCES facture(id) ON DELETE CASCADE,
    montant         NUMERIC(12,2) NOT NULL,
    motif           VARCHAR(255) NOT NULL,
    type_exoneration VARCHAR(30) NOT NULL DEFAULT 'REMISE',
    decide_par      BIGINT NOT NULL REFERENCES utilisateur(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_exoneration_montant CHECK (montant > 0),
    CONSTRAINT ck_exoneration_type CHECK (type_exoneration IN
        ('BOURSE', 'REMISE', 'AIDE_SOCIALE', 'CORRECTION'))
);

CREATE INDEX ix_exoneration_facture ON exoneration(facture_id);

-- ---------- Compteur de numéros de facture ----------
-- Une séquence par établissement et par année, pour des numéros lisibles
-- du type FAC-2026-000123.

CREATE TABLE compteur_facture (
    etablissement_id  BIGINT NOT NULL REFERENCES etablissement(id) ON DELETE CASCADE,
    annee_scolaire_id BIGINT NOT NULL REFERENCES annee_scolaire(id),
    dernier_numero    BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (etablissement_id, annee_scolaire_id)
);
