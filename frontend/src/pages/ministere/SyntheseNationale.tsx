import { Alert, Card, Loader, Select, SimpleGrid, Stack, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import type { SyntheseNationale as Synthese } from '../../api/types'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'

function Chiffre({ libelle, valeur }: { libelle: string; valeur: number }) {
  return (
    <Card withBorder padding="md">
      <Text size="sm" c="dimmed">
        {libelle}
      </Text>
      <Text fw={700} size="xl">
        {new Intl.NumberFormat('fr-FR').format(valeur)}
      </Text>
    </Card>
  )
}

interface Props {
  /** Clé de cache et chargeur : ministère (national) ou direction régionale (sa zone). */
  cle: string
  charger: (anneeScolaireId: number) => Promise<Synthese>
}

export default function SyntheseNationale({ cle, charger }: Props) {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)

  const synthese = useQuery({
    queryKey: [cle, anneeId],
    queryFn: () => charger(anneeId as number),
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
          <Chiffre libelle="Établissements" valeur={synthese.data.etablissements} />
          <Chiffre libelle="Actifs" valeur={synthese.data.actifs} />
          <Chiffre libelle="Suspendus" valeur={synthese.data.suspendus} />
          <Chiffre libelle="Fermés" valeur={synthese.data.fermes} />
          <Chiffre libelle="Classes" valeur={synthese.data.classes} />
          <Chiffre libelle="Élèves inscrits" valeur={synthese.data.elevesInscrits} />
        </SimpleGrid>
      )}
    </Stack>
  )
}
