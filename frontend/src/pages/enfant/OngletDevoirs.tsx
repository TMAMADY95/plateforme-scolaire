import { Alert, Badge, Card, Group, Loader, Stack, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerDevoirs } from '../../api/endpoints'
import { aujourdhui, formaterDate } from '../../utils/dates'

/** Devoirs de la classe, en lecture seule : espace élève et espace parent. */
export default function OngletDevoirs({ classeId }: { classeId: number | null }) {
  const devoirs = useQuery({
    queryKey: ['devoirs', classeId],
    queryFn: () => listerDevoirs(classeId as number),
    enabled: classeId !== null,
  })

  if (classeId === null) {
    return (
      <Alert color="blue" variant="light">
        Aucune inscription active : les devoirs ne sont pas disponibles.
      </Alert>
    )
  }

  const today = aujourdhui()
  return (
    <Stack>
      <Title order={4}>Devoirs</Title>
      {devoirs.isPending && <Loader />}
      {devoirs.isError && (
        <Alert color="red" variant="light">
          {devoirs.error.message}
        </Alert>
      )}
      {devoirs.data && devoirs.data.length === 0 && <Text c="dimmed">Aucun devoir à venir.</Text>}
      {devoirs.data?.map((d) => (
        <Card key={d.id} withBorder padding="md">
          <Group justify="space-between" align="flex-start" wrap="nowrap">
            <div>
              <Group gap="xs" mb={4}>
                <Badge variant="light">{d.matiere}</Badge>
                <Text size="xs" c="dimmed">
                  {d.enseignant}
                </Text>
              </Group>
              <Text fw={600}>{d.titre}</Text>
              {d.consigne !== null && (
                <Text size="sm" mt={4} style={{ whiteSpace: 'pre-wrap' }}>
                  {d.consigne}
                </Text>
              )}
            </div>
            <Badge color={d.dateLimite < today ? 'gray' : d.dateLimite === today ? 'orange' : 'blue'} variant="filled">
              Pour le {formaterDate(d.dateLimite)}
            </Badge>
          </Group>
        </Card>
      ))}
    </Stack>
  )
}
