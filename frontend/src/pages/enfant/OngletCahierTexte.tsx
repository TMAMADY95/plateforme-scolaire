import { Alert, Card, Loader, Stack, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { cahierTexteEleve } from '../../api/endpoints'
import { formaterDate } from '../../utils/dates'

/** Ce qui a été enseigné en classe ces 30 derniers jours (lecture seule). */
export default function OngletCahierTexte({ eleveId }: { eleveId: number }) {
  const entrees = useQuery({ queryKey: ['cahier-texte', 'eleve', eleveId], queryFn: () => cahierTexteEleve(eleveId) })
  return (
    <Stack>
      {entrees.isPending && <Loader />}
      {entrees.isError && <Alert color="red" variant="light">{entrees.error.message}</Alert>}
      {entrees.data && entrees.data.length === 0 && <Text c="dimmed">Aucune séance notée sur les 30 derniers jours.</Text>}
      {entrees.data?.map((e) => (
        <Card key={e.id} withBorder padding="sm">
          <Text fw={600} size="sm">{e.matiere} — {formaterDate(e.dateSeance)}</Text>
          <Text size="xs" c="dimmed">{e.enseignant}</Text>
          <Text size="sm" mt={4} style={{ whiteSpace: 'pre-wrap' }}>{e.contenu}</Text>
        </Card>
      ))}
    </Stack>
  )
}
