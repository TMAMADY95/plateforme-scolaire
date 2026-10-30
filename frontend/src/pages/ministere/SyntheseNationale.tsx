import { useState } from 'react'
import { Alert, Loader, Select, SimpleGrid, Stack, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import type {
  LigneEtablissementStat,
  LigneZoneStat,
  ReussiteReseau,
  Statistiques,
  SyntheseNationale as Synthese,
} from '../../api/types'
import DetailEtablissementModal from '../../components/DetailEtablissementModal'
import { TableauEtablissements, TableauZones } from '../../components/TableauxStatistiques'
import CarteChiffre from '../../components/CarteChiffre'
import CarteReussiteReseau from '../../components/CarteReussiteReseau'
import PanneauStatistiques from '../../components/PanneauStatistiques'
import type { NomIcone } from '../../components/Icone'
import EnteteSection from '../../components/EnteteSection'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'

function Chiffre({ libelle, valeur, icone, couleur }: { libelle: string; valeur: number; icone: NomIcone; couleur?: string }) {
  return <CarteChiffre libelle={libelle} valeur={new Intl.NumberFormat('fr-FR').format(valeur)} icone={icone} couleur={couleur} />
}

interface Props {
  /** Clé de cache et chargeur : ministère (national) ou direction régionale (sa zone). */
  cle: string
  charger: (anneeScolaireId: number) => Promise<Synthese>
  /** Statistiques détaillées (graphiques) du même périmètre. */
  chargerStatistiques: (anneeScolaireId: number, zoneId?: number) => Promise<Statistiques>
  /** Effectifs de chaque établissement du périmètre. */
  chargerParEtablissement: (anneeScolaireId: number, zoneId?: number) => Promise<LigneEtablissementStat[]>
  /** Effectifs par zone : réservé au ministère, qui peut aussi filtrer par zone. */
  chargerParZone?: (anneeScolaireId: number) => Promise<LigneZoneStat[]>
  /** Taux de réussite du même périmètre, pour une période. */
  chargerReussite: (anneeScolaireId: number, periodeId: number) => Promise<ReussiteReseau>
}

export default function SyntheseNationale({
  cle,
  charger,
  chargerStatistiques,
  chargerParEtablissement,
  chargerParZone,
  chargerReussite,
}: Props) {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)

  const [zoneId, setZoneId] = useState<number | null>(null)
  const [detail, setDetail] = useState<LigneEtablissementStat | null>(null)
  const filtreZone = chargerParZone !== undefined && zoneId !== null ? zoneId : undefined

  const synthese = useQuery({
    queryKey: [cle, anneeId],
    queryFn: () => charger(anneeId as number),
    enabled: anneeId !== null,
  })

  const statistiques = useQuery({
    queryKey: [`${cle}-statistiques`, anneeId, filtreZone ?? null],
    queryFn: () => chargerStatistiques(anneeId as number, filtreZone),
    enabled: anneeId !== null,
  })

  const parEtablissement = useQuery({
    queryKey: [`${cle}-par-etablissement`, anneeId, filtreZone ?? null],
    queryFn: () => chargerParEtablissement(anneeId as number, filtreZone),
    enabled: anneeId !== null,
  })

  const parZone = useQuery({
    queryKey: [`${cle}-par-zone`, anneeId],
    queryFn: () => (chargerParZone as (a: number) => Promise<LigneZoneStat[]>)(anneeId as number),
    enabled: anneeId !== null && chargerParZone !== undefined,
  })

  const zoneChoisie = (parZone.data ?? []).find((z) => z.zoneId === filtreZone)

  return (
    <Stack mt="md" gap="lg">
      <EnteteSection
        titre="Chiffres clés"
        description="L'état du réseau pour l'année choisie : établissements, élèves, inscriptions et réussite."
        action={
          <Select
            aria-label="Année scolaire"
            data={annee.options}
            value={annee.anneeId}
            onChange={annee.setAnneeChoisie}
            allowDeselect={false}
            w={200}
          />
        }
      />
      {annee.annees.isSuccess && annee.options.length === 0 && (
        <Alert color="blue" variant="light">
          Aucune année scolaire n'existe : créez-en une dans l'onglet « Référentiels ».
        </Alert>
      )}
      {zoneChoisie && (
        <Alert color="blue" variant="light">
          Chiffres limités à la zone « {zoneChoisie.nom} » (sous-zones comprises).
        </Alert>
      )}
      {synthese.isPending && anneeId !== null && <Loader />}
      {synthese.isError && (
        <Alert color="red" variant="light">
          {synthese.error.message}
        </Alert>
      )}
      {synthese.data && filtreZone === undefined && (
        <>
          <Text size="xs" c="dimmed" tt="uppercase" fw={600} mb={-8}>
            Réseau
          </Text>
        <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }}>
          <Chiffre libelle="Établissements" valeur={synthese.data.etablissements} icone="ecole" />
          <Chiffre libelle="Actifs" valeur={synthese.data.actifs} icone="comptes" couleur="green" />
          <Chiffre libelle="Suspendus" valeur={synthese.data.suspendus} icone="alerte" couleur="orange" />
          <Chiffre libelle="Fermés" valeur={synthese.data.fermes} icone="alerte" couleur="gray" />
          <Chiffre libelle="Classes" valeur={synthese.data.classes} icone="classes" couleur="cyan" />
          <Chiffre libelle="Élèves inscrits" valeur={synthese.data.elevesInscrits} icone="eleves" />
        </SimpleGrid>
        </>
      )}
      {statistiques.isError && (
        <Alert color="red" variant="light">
          {statistiques.error.message}
        </Alert>
      )}
      {statistiques.data && (
        <Text size="xs" c="dimmed" tt="uppercase" fw={600} mb={-8}>
          Activité et répartition
        </Text>
      )}
      {statistiques.data && <PanneauStatistiques statistiques={statistiques.data} avecReseau />}
      {chargerParZone !== undefined && parZone.data && (
        <TableauZones lignes={parZone.data} zoneChoisie={filtreZone ?? null} onChoisir={setZoneId} />
      )}
      {parEtablissement.isError && (
        <Alert color="red" variant="light">
          {parEtablissement.error.message}
        </Alert>
      )}
      {parEtablissement.data && <TableauEtablissements lignes={parEtablissement.data} onVoir={setDetail} />}
      {anneeId !== null && filtreZone === undefined && (
        <CarteReussiteReseau cle={`${cle}-reussite`} anneeScolaireId={anneeId} charger={chargerReussite} />
      )}
      {detail && anneeId !== null && (
        <DetailEtablissementModal etablissement={detail} anneeScolaireId={anneeId} onFermer={() => setDetail(null)} />
      )}
    </Stack>
  )
}
