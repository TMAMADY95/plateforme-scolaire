import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, Stack, Table, Text, TextInput, Textarea } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { demanderJustification, historiquePresences, justificationsEleve } from '../../api/endpoints'
import type { PresenceEleve } from '../../api/types'
import BadgeStatutPresence from '../../components/BadgeStatutPresence'
import { aujourdhui, formaterDate, ilYaJours } from '../../utils/dates'

export default function OngletPresences({ eleveId }: { eleveId: number }) {
  const [du, setDu] = useState(ilYaJours(30))
  const [au, setAu] = useState(aujourdhui())

  const queryClient = useQueryClient()
  const [aJustifier, setAJustifier] = useState<PresenceEleve | null>(null)
  const [motif, setMotif] = useState('')
  const demandes = useQuery({ queryKey: ['justifications-eleve', eleveId], queryFn: () => justificationsEleve(eleveId) })
  const envoi = useMutation({
    mutationFn: () => demanderJustification((aJustifier as PresenceEleve).id, motif.trim()),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['justifications-eleve', eleveId] })
      notifications.show({ color: 'green', title: 'Demande envoyée', message: "La direction de l'école va l'examiner." })
      setAJustifier(null)
      setMotif('')
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Envoi impossible', message: e.message }),
  })
  const demandeDe = (presenceId: number) => demandes.data?.find((d) => d.presenceId === presenceId)

  const periodeValide = du !== '' && au !== '' && du <= au
  const presences = useQuery({
    queryKey: ['presences-eleve', eleveId, du, au],
    queryFn: () => historiquePresences(eleveId, du, au),
    enabled: periodeValide,
  })

  const donnees = presences.data ?? []
  const absences = donnees.filter((p) => p.statut === 'ABSENT')
  const retards = donnees.filter((p) => p.statut === 'RETARD')
  const absencesJustifiees = absences.filter((p) => p.justifie).length
  // Seuls les jours où un appel a été fait sont listés : on n'affiche que les absences et retards
  const anomalies = donnees.filter((p) => p.statut !== 'PRESENT')

  return (
    <Stack>
      <Group>
        <TextInput label="Du" type="date" value={du} max={au || undefined} onChange={(e) => setDu(e.currentTarget.value)} />
        <TextInput label="Au" type="date" value={au} min={du || undefined} max={aujourdhui()} onChange={(e) => setAu(e.currentTarget.value)} />
      </Group>

      {!periodeValide && (
        <Alert color="orange" variant="light">
          La date de début doit précéder la date de fin.
        </Alert>
      )}
      {presences.isPending && periodeValide && <Loader />}
      {presences.isError && (
        <Alert color="red" variant="light">
          {presences.error.message}
        </Alert>
      )}

      {presences.data && (
        <>
          <Group gap="sm">
            <Badge size="lg" color="red" variant="light">
              {absences.length} absence(s) dont {absencesJustifiees} justifiée(s)
            </Badge>
            <Badge size="lg" color="orange" variant="light">
              {retards.length} retard(s)
            </Badge>
            <Badge size="lg" color="green" variant="light">
              {donnees.length - anomalies.length} jour(s) de présence
            </Badge>
          </Group>

          {anomalies.length === 0 ? (
            <Text c="dimmed">Aucune absence ni aucun retard sur cette période.</Text>
          ) : (
            <Table.ScrollContainer minWidth={480}>
              <Table striped withTableBorder maw={720}>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Date</Table.Th>
                    <Table.Th>Statut</Table.Th>
                    <Table.Th>Motif</Table.Th>
                    <Table.Th>Justifiée</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {anomalies.map((p) => (
                    <Table.Tr key={p.id}>
                      <Table.Td>{formaterDate(p.date)}</Table.Td>
                      <Table.Td>
                        <BadgeStatutPresence statut={p.statut} />
                      </Table.Td>
                      <Table.Td>{p.motif ?? '—'}</Table.Td>
                      <Table.Td>
                        {p.justifie ? (
                          'Oui'
                        ) : demandeDe(p.id)?.statut === 'EN_ATTENTE' ? (
                          <Badge color="blue" variant="light">Demande envoyée</Badge>
                        ) : (
                          <Group gap="xs" wrap="nowrap">
                            Non
                            <Button size="compact-xs" variant="light" onClick={() => setAJustifier(p)}>Justifier</Button>
                          </Group>
                        )}
                        {demandeDe(p.id)?.statut === 'REFUSEE' && (
                          <Text size="xs" c="red">Refusée{demandeDe(p.id)?.reponse ? ` : ${demandeDe(p.id)?.reponse}` : ''}</Text>
                        )}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </>
      )}
      <Modal opened={aJustifier !== null} onClose={() => setAJustifier(null)} title="Justifier une absence">
        <Stack>
          {aJustifier && <Text size="sm">Absence du {formaterDate(aJustifier.date)}</Text>}
          <Textarea label="Motif" placeholder="Maladie, décès dans la famille, rendez-vous médical…" autosize minRows={3} maxLength={500} value={motif} onChange={(e) => setMotif(e.currentTarget.value)} />
          <Group justify="flex-end">
            <Button disabled={motif.trim() === ''} loading={envoi.isPending} onClick={() => envoi.mutate()}>Envoyer à l'école</Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
