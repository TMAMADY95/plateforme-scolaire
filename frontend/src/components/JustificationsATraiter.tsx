import { useState } from 'react'
import { Alert, Button, Card, Group, Stack, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deciderJustification, justificationsEnAttente } from '../api/endpoints'
import type { Justification } from '../api/types'
import { formaterDate } from '../utils/dates'

/** Demandes de justification d'absence envoyées par les familles, à accepter ou refuser. */
export default function JustificationsATraiter({ etablissementId }: { etablissementId: number }) {
  const queryClient = useQueryClient()
  const demandes = useQuery({ queryKey: ['justifications', etablissementId], queryFn: () => justificationsEnAttente(etablissementId) })
  const [reponses, setReponses] = useState<Record<number, string>>({})

  const decision = useMutation({
    mutationFn: ({ j, accepter }: { j: Justification; accepter: boolean }) =>
      deciderJustification(j.id, accepter, reponses[j.id]?.trim() || null),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['justifications'] })
      void queryClient.invalidateQueries({ queryKey: ['presences'] })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Décision impossible', message: e.message }),
  })

  if (!demandes.data || demandes.data.length === 0) return null
  return (
    <Alert color="blue" variant="light" title={`${demandes.data.length} demande(s) de justification`}>
      <Stack gap="xs">
        {demandes.data.map((j) => (
          <Card key={j.id} withBorder padding="sm">
            <Text size="sm" fw={600}>
              {j.eleve}{j.classe ? ` (${j.classe})` : ''} — {j.typePresence === 'RETARD' ? 'retard' : 'absence'} du {formaterDate(j.dateAbsence)}
            </Text>
            <Text size="sm">« {j.motif} »</Text>
            <Group mt="xs" align="flex-end" wrap="nowrap">
              <TextInput
                style={{ flex: 1 }}
                size="xs"
                placeholder="Réponse à la famille (facultatif)"
                maxLength={500}
                value={reponses[j.id] ?? ''}
                onChange={(e) => setReponses({ ...reponses, [j.id]: e.currentTarget.value })}
              />
              <Button size="xs" color="green" loading={decision.isPending} onClick={() => decision.mutate({ j, accepter: true })}>Accepter</Button>
              <Button size="xs" color="red" variant="light" loading={decision.isPending} onClick={() => decision.mutate({ j, accepter: false })}>Refuser</Button>
            </Group>
          </Card>
        ))}
      </Stack>
    </Alert>
  )
}
