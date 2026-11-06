-- =====================================================================================
-- DÉMONSTRATION 0/3 — Supprime TOUTES les données et conserve la structure.
-- Sont conservés : l'historique Flyway et les référentiels créés par les migrations
-- (cycles scolaires, types d'établissement, types d'examen, critères des centres d'examen).
-- Le TRUNCATE ... CASCADE vide aussi type_examen (il référence niveau) : ses 3 lignes sont donc recréées en fin de script.
-- Attention : les comptes (dont le ministère) sont supprimés ; le script 01 recrée ceux de la démonstration.
-- =====================================================================================
DO $$
DECLARE
  tables text;
BEGIN
  SELECT string_agg(format('%I.%I', schemaname, tablename), ', ')
    INTO tables
    FROM pg_tables
   WHERE schemaname = 'public'
     AND tablename NOT IN ('flyway_schema_history', 'cycle_scolaire', 'type_etablissement', 'type_examen', 'critere_centre_examen');

  EXECUTE 'TRUNCATE TABLE ' || tables || ' RESTART IDENTITY CASCADE';
END $$;

INSERT INTO type_examen (code, libelle, niveau_organisation) VALUES
    ('CEE',  'Certificat de fin d''études élémentaires (CEE)', 'PREFECTURE'),
    ('BEPC', 'Brevet d''études du premier cycle (BEPC)',       'REGION'),
    ('BAC',  'Baccalauréat',                                    'NATIONAL')
ON CONFLICT (code) DO NOTHING;
