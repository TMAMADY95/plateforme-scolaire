import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, Stack, Table, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listerDevoirs, supprimerDevoir } from '../../api/endpoints'
import type { Devoir } from '../../api/types'
import { formaterDate } from '../../utils/dates'
import DevoirModal from './DevoirModal'

interface Props {
  classeId: number
  /** Matières que l'enseignant assure dans cette classe : les seules pour lesquelles il publie. */
  mesMatieres: { id: number; libelle: string }[]
}

export default function OngletDevoirs({ classeId, mesMatieres }: Props) {
  const queryClient = useQueryClient()
  const [nouveauOuvert, { open, close }] = useDisclosure()
  const [aModifier, setAModifier] = useState<Devoir | null>(null)
  const [aSupprimer, setASupprimer] = useState<Devoir | null>(null)

  const devoirs = useQuery({ queryKey: ['devoirs', classeId], queryFn: () => listerDevoirs(classeId) })

  const suppression = useMutation({
    mutationFn: (d: Devoir) => supprimerDevoir(d.id),
    onSuccess: (_, d) => {
      void queryClient.invalidateQueries({ queryKey: ['devoirs'] })
      notifications.show({ color: 'green', title: 'Devoir supprimé', message: d.titre })
      setASupprimer(null)
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Suppression impossible', message: e.message }),
  })

  return (
    <Stack>
      <Group justify="space-between" align="flex-end">
        <Text c="dimmed" size="sm">
          Devoirs à venir et échus depuis moins de deux semaines.
        </Text>
        <Button onClick={open}>Nouveau devoir</Button>
      </Group>

      {devoirs.isPending && <Loader />}
      {devoirs.isError && (
        <Alert color="red" variant="light">
          {devoirs.error.message}
        </Alert>
      )}
      {devoirs.data && devoirs.data.length === 0 && <Text c="dimmed">Aucun devoir pour le moment.</Text>}

      {devoirs.data && devoirs.data.length > 0 && (
        <Table.ScrollContainer minWidth={720}>
          <Table striped highlightOnHover withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Pour le</Table.Th>
                <Table.Th>Matière</Table.Th>
                <Table.Th>Devoir</Table.Th>
                <Table.Th>Publié par</Table.Th>
                <Table.Th>Terminé</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {devoirs.data.map((d) => {
                // Seules les matières de l'enseignant sont modifiables ; la direction passe par la même API.
                const mien = mesMatieres.some((m) => m.id === d.matiereId)
                return (
                  <Table.Tr key={d.id}>
                    <Table.Td>{formaterDate(d.dateLimite)}</Table.Td>
                    <Table.Td>
                      <Badge variant="light">{d.matiere}</Badge>
                    </Table.Td>
                    <Table.Td>
                      <Text fw={500}>{d.titre}</Text>
                      {d.consigne !== null && (
                        <Text size="sm" c="dimmed" lineClamp={2}>
                          {d.consigne}
                        </Text>
                      )}
                    </Table.Td>
                    <Table.Td>{d.enseignant}</Table.Td>
                    <Table.Td>{d.faits !== null && d.effectif !== null ? `${d.faits} / ${d.effectif}` : '—'}</Table.Td>
                    <Table.Td>
                      {mien && (
                        <Group gap="xs" wrap="nowrap">
                          <Button size="compact-sm" variant="subtle" onClick={() => setAModifier(d)}>
                            Modifier
                          </Button>
                          <Button size="compact-sm" variant="subtle" color="red" onClick={() => setASupprimer(d)}>
                            Supprimer
                          </Button>
                        </Group>
                      )}
                    </Table.Td>
                  </Table.Tr>
                )
              })}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {nouveauOuvert && <DevoirModal classeId={classeId} mesMatieres={mesMatieres} onFermer={close} />}
      {aModifier !== null && (
        <DevoirModal
          classeId={classeId}
          mesMatieres={mesMatieres}
          devoir={aModifier}
          onFermer={() => setAModifier(null)}
        />
      )}
      <Modal opened={aSupprimer !== null} onClose={() => setASupprimer(null)} title="Supprimer le devoir ?">
        <Stack>
          <Text>{aSupprimer?.titre}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setASupprimer(null)}>
              Annuler
            </Button>
            <Button
              color="red"
              loading={suppression.isPending}
              onClick={() => aSupprimer !== null && suppression.mutate(aSupprimer)}
            >
              Supprimer
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
