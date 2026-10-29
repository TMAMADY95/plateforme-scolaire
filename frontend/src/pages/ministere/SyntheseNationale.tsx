import { Alert, Loader, Select, SimpleGrid, Stack } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import type { ReussiteReseau, Statistiques, SyntheseNationale as Synthese } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import CarteReussiteReseau from '../../components/CarteReussiteReseau'
import PanneauStatistiques from '../../components/PanneauStatistiques'
import type { NomIcone } from '../../components/Icone'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'

function Chiffre({ libelle, valeur, icone, couleur }: { libelle: string; valeur: number; icone: NomIcone; couleur?: string }) {
  return <CarteChiffre libelle={libelle} valeur={new Intl.NumberFormat('fr-FR').format(valeur)} icone={icone} couleur={couleur} />
}

interface Props {
  /** Clé de cache et chargeur : ministère (national) ou direction régionale (sa zone). */
  cle: string
  charger: (anneeScolaireId: number) => Promise<Synthese>
  /** Statistiques détaillées (graphiques) du même périmètre. */
  chargerStatistiques: (anneeScolaireId: number) => Promise<Statistiques>
  /** Taux de réussite du même périmètre, pour une période. */
  chargerReussite: (anneeScolaireId: number, periodeId: number) => Promise<ReussiteReseau>
}

export default function SyntheseNationale({ cle, charger, chargerStatistiques, chargerReussite }: Props) {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)

  const synthese = useQuery({
    queryKey: [cle, anneeId],
    queryFn: () => charger(anneeId as number),
    enabled: anneeId !== null,
  })

  const statistiques = useQuery({
    queryKey: [`${cle}-statistiques`, anneeId],
    queryFn: () => chargerStatistiques(anneeId as number),
    enabled: anneeId !== null,
  })

  return (
    <Stack mt="md">
      <Select
        label="Année scolaire"
        data={annee.options}
        value={annee.anneeId}
        onChange={annee.setAnneeChoisie}
        allowDeselect={false}
        maw={260}
      />
      {annee.annees.isSuccess && annee.options.length === 0 && (
        <Alert color="blue" variant="light">
          Aucune année scolaire n'existe : créez-en une dans l'onglet « Référentiels ».
        </Alert>
      )}
      {synthese.isPending && anneeId !== null && <Loader />}
      {synthese.isError && (
        <Alert color="red" variant="light">
          {synthese.error.message}
        </Alert>
      )}
      {synthese.data && (
        <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }}>
          <Chiffre libelle="Établissements" valeur={synthese.data.etablissements} icone="ecole" />
          <Chiffre libelle="Actifs" valeur={synthese.data.actifs} icone="comptes" couleur="green" />
          <Chiffre libelle="Suspendus" valeur={synthese.data.suspendus} icone="alerte" couleur="orange" />
          <Chiffre libelle="Fermés" valeur={synthese.data.fermes} icone="alerte" couleur="gray" />
          <Chiffre libelle="Classes" valeur={synthese.data.classes} icone="classes" couleur="cyan" />
          <Chiffre libelle="Élèves inscrits" valeur={synthese.data.elevesInscrits} icone="eleves" />
        </SimpleGrid>
      )}
      {statistiques.isError && (
        <Alert color="red" variant="light">
          {statistiques.error.message}
        </Alert>
      )}
      {statistiques.data && <PanneauStatistiques statistiques={statistiques.data} avecReseau />}
      {anneeId !== null && <CarteReussiteReseau cle={`${cle}-reussite`} anneeScolaireId={anneeId} charger={chargerReussite} />}
    </Stack>
  )
}
