import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Stack, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deciderSortie, sortiesEtablissement } from '../api/endpoints'
import type { Sortie } from '../api/types'
import { formaterDate } from '../utils/dates'

/** Demandes de sortie anticipée des familles, et sorties accordées à partir d'aujourd'hui. */
export default function SortiesATraiter({ etablissementId }: { etablissementId: number }) {
  const queryClient = useQueryClient()
  const sorties = useQuery({ queryKey: ['sorties-etablissement', etablissementId], queryFn: () => sortiesEtablissement(etablissementId) })
  const [reponses, setReponses] = useState<Record<number, string>>({})
  const decision = useMutation({
    mutationFn: ({ s, accepter }: { s: Sortie; accepter: boolean }) => deciderSortie(s.id, accepter, reponses[s.id]?.trim() || null),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['sorties-etablissement'] }),
    onError: (e) => notifications.show({ color: 'red', title: 'Décision impossible', message: e.message }),
  })

  if (!sorties.data || sorties.data.length === 0) return null
  const enAttente = sorties.data.filter((s) => s.statut === 'DEMANDEE')
  const accordees = sorties.data.filter((s) => s.statut === 'ACCEPTEE')
  return (
    <Alert color="grape" variant="light" title={`Sorties anticipées : ${enAttente.length} demande(s) en attente`}>
      <Stack gap="xs">
        {enAttente.map((s) => (
          <Card key={s.id} withBorder padding="sm">
            <Text size="sm" fw={600}>{s.eleve} — le {formaterDate(s.dateSortie)} à {s.heureSortie.slice(0, 5).replace(':', ' h ')}</Text>
            <Text size="sm">« {s.motif} »{s.accompagnateur ? ` — viendra le chercher : ${s.accompagnateur}` : ''}</Text>
            <Group mt="xs" align="flex-end" wrap="nowrap">
              <TextInput style={{ flex: 1 }} size="xs" placeholder="Réponse (facultatif)" maxLength={500} value={reponses[s.id] ?? ''} onChange={(e) => setReponses({ ...reponses, [s.id]: e.currentTarget.value })} />
              <Button size="xs" color="green" loading={decision.isPending} onClick={() => decision.mutate({ s, accepter: true })}>Accorder</Button>
              <Button size="xs" color="red" variant="light" loading={decision.isPending} onClick={() => decision.mutate({ s, accepter: false })}>Refuser</Button>
            </Group>
          </Card>
        ))}
        {accordees.length > 0 && (
          <Text size="sm">
            Sorties accordées :{' '}
            {accordees.map((s) => (
              <Badge key={s.id} variant="light" color="green" mr={4}>
                {s.eleve} · {formaterDate(s.dateSortie)} {s.heureSortie.slice(0, 5)}
              </Badge>
            ))}
          </Text>
        )}
      </Stack>
    </Alert>
  )
}
