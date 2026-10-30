import { Alert, Badge, Card, Checkbox, Group, Loader, Stack, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { devoirsEleve, marquerDevoirFait } from '../../api/endpoints'
import { aujourdhui, formaterDate } from '../../utils/dates'

/** Devoirs de l'élève, avec une case « fait » : espace élève et espace parent. */
export default function OngletDevoirs({ classeId, eleveId }: { classeId: number | null; eleveId: number }) {
  const queryClient = useQueryClient()
  const cle = ['devoirs', 'eleve', eleveId]
  const devoirs = useQuery({
    queryKey: cle,
    queryFn: () => devoirsEleve(eleveId),
    enabled: classeId !== null,
  })

  const bascule = useMutation({
    mutationFn: ({ id, fait }: { id: number; fait: boolean }) => marquerDevoirFait(id, eleveId, fait),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: cle }),
    onError: (e) => notifications.show({ color: 'red', title: 'Action impossible', message: e.message }),
  })

  if (classeId === null) {
    return (
      <Alert color="blue" variant="light">
        Aucune inscription active : les devoirs ne sont pas disponibles.
      </Alert>
    )
  }

  const today = aujourdhui()
  const restants = (devoirs.data ?? []).filter((d) => d.fait !== true && d.dateLimite >= today).length

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={4}>Devoirs</Title>
        {devoirs.data && restants > 0 && (
          <Badge variant="light" color="orange">
            {restants} à faire
          </Badge>
        )}
      </Group>
      {devoirs.isPending && <Loader />}
      {devoirs.isError && (
        <Alert color="red" variant="light">
          {devoirs.error.message}
        </Alert>
      )}
      {devoirs.data && devoirs.data.length === 0 && <Text c="dimmed">Aucun devoir à venir.</Text>}
      {devoirs.data?.map((d) => {
        const fait = d.fait === true
        return (
          <Card key={d.id} withBorder padding="md" style={fait ? { opacity: 0.7 } : undefined}>
            <Group justify="space-between" align="flex-start" wrap="nowrap">
              <Group align="flex-start" wrap="nowrap" gap="sm">
                <Checkbox
                  mt={2}
                  checked={fait}
                  disabled={bascule.isPending}
                  onChange={(e) => bascule.mutate({ id: d.id, fait: e.currentTarget.checked })}
                  aria-label={`Marquer « ${d.titre} » comme fait`}
                />
                <div>
                  <Group gap="xs" mb={4}>
                    <Badge variant="light">{d.matiere}</Badge>
                    <Text size="xs" c="dimmed">
                      {d.enseignant}
                    </Text>
                  </Group>
                  <Text fw={600} td={fait ? 'line-through' : undefined}>
                    {d.titre}
                  </Text>
                  {d.consigne !== null && (
                    <Text size="sm" mt={4} style={{ whiteSpace: 'pre-wrap' }}>
                      {d.consigne}
                    </Text>
                  )}
                </div>
              </Group>
              <Badge
                color={fait ? 'green' : d.dateLimite < today ? 'gray' : d.dateLimite === today ? 'orange' : 'blue'}
                variant="filled"
              >
                {fait ? 'Fait' : `Pour le ${formaterDate(d.dateLimite)}`}
              </Badge>
            </Group>
          </Card>
        )
      })}
    </Stack>
  )
}
