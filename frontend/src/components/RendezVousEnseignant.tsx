import { useState } from 'react'
import { Badge, Button, Card, Group, Stack, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mesRendezVous, repondreRendezVous } from '../api/endpoints'
import type { RendezVous } from '../api/types'
import { formaterDate } from '../utils/dates'

/** Demandes de rendez-vous des familles et rendez-vous confirmés à venir. */
export default function RendezVousEnseignant() {
  const queryClient = useQueryClient()
  const rendezVous = useQuery({ queryKey: ['mes-rendez-vous'], queryFn: mesRendezVous })
  const [reponses, setReponses] = useState<Record<number, string>>({})
  const reponse = useMutation({
    mutationFn: ({ r, accepter }: { r: RendezVous; accepter: boolean }) => repondreRendezVous(r.id, accepter, reponses[r.id]?.trim() || null),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['mes-rendez-vous'] }),
    onError: (e) => notifications.show({ color: 'red', title: 'Réponse impossible', message: e.message }),
  })

  if (!rendezVous.data || rendezVous.data.length === 0) return null
  return (
    <Card withBorder padding="lg">
      <Text fw={600} mb="sm">Rendez-vous avec les familles</Text>
      <Stack gap="xs">
        {rendezVous.data.map((r) => (
          <Card key={r.id} withBorder padding="sm">
            <Group justify="space-between">
              <Text size="sm" fw={600}>{r.eleve} — {formaterDate(r.dateHeure.slice(0, 10))} à {r.dateHeure.slice(11, 16).replace(':', ' h ')}</Text>
              <Badge color={r.statut === 'CONFIRME' ? 'green' : 'blue'} variant="light">{r.statut === 'CONFIRME' ? 'Confirmé' : 'À confirmer'}</Badge>
            </Group>
            <Text size="sm">« {r.motif} »</Text>
            {r.statut === 'DEMANDE' && (
              <Group mt="xs" align="flex-end" wrap="nowrap">
                <TextInput style={{ flex: 1 }} size="xs" placeholder="Message à la famille (facultatif)" maxLength={500} value={reponses[r.id] ?? ''} onChange={(e) => setReponses({ ...reponses, [r.id]: e.currentTarget.value })} />
                <Button size="xs" color="green" loading={reponse.isPending} onClick={() => reponse.mutate({ r, accepter: true })}>Confirmer</Button>
                <Button size="xs" color="red" variant="light" loading={reponse.isPending} onClick={() => reponse.mutate({ r, accepter: false })}>Refuser</Button>
              </Group>
            )}
          </Card>
        ))}
      </Stack>
    </Card>
  )
}
