import { useState } from 'react'
import { Alert, Badge, Button, Group, Stack, Table, Text, TextInput, Textarea } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { changerStatutPlanAction, creerPlanAction, plansActionEtablissement } from '../api/endpoints'
import type { PlanAction, StatutPlanAction } from '../api/types'

export const LIBELLES_PLAN: Record<StatutPlanAction, string> = {
  A_FAIRE: 'À faire',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  ANNULE: 'Annulé',
}
export const COULEURS_PLAN: Record<StatutPlanAction, string> = {
  A_FAIRE: 'gray',
  EN_COURS: 'blue',
  TERMINE: 'green',
  ANNULE: 'dark',
}
const jour = (iso: string) => new Date(iso).toLocaleDateString('fr-FR')
const aujourdhui = () => new Date().toISOString().slice(0, 10)

interface Props {
  etablissementId: number
  /** Ministère et direction régionale : créent et annulent ; la direction de l'école indique l'avancement. */
  supervision: boolean
}

/** Mesures à prendre après inspection pour un établissement. */
export default function PlansActionEtablissement({ etablissementId, supervision }: Props) {
  const queryClient = useQueryClient()
  const liste = useQuery({ queryKey: ['plans-action', etablissementId], queryFn: () => plansActionEtablissement(etablissementId) })
  const [description, setDescription] = useState('')
  const [echeance, setEcheance] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)

  const rafraichir = () => {
    void queryClient.invalidateQueries({ queryKey: ['plans-action'] })
  }

  const creation = useMutation({
    mutationFn: () => creerPlanAction(etablissementId, { description: description.trim(), echeance }),
    onSuccess: () => {
      rafraichir()
      setDescription('')
      setEcheance('')
      setErreur(null)
      notifications.show({ color: 'green', title: 'Plan d\'action créé', message: 'La direction de l\'établissement sera rappelée avant l\'échéance.' })
    },
    onError: (e) => setErreur(e.message),
  })

  const statut = useMutation({
    mutationFn: ({ plan, nouveau }: { plan: PlanAction; nouveau: StatutPlanAction }) =>
      changerStatutPlanAction(plan.id, nouveau, null),
    onSuccess: rafraichir,
    onError: (e) => notifications.show({ color: 'red', title: 'Modification impossible', message: e.message }),
  })

  return (
    <Stack>
      {liste.data && liste.data.length === 0 && <Text c="dimmed">Aucun plan d'action.</Text>}
      {liste.data && liste.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {liste.data.map((p) => {
              const ouvert = p.statut === 'A_FAIRE' || p.statut === 'EN_COURS'
              return (
                <Table.Tr key={p.id}>
                  <Table.Td>
                    <Text size="sm">{p.description}</Text>
                    <Text size="xs" c="dimmed">Par {p.creePar}</Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm" c={p.enRetard ? 'red' : undefined}>{jour(p.echeance)}</Text>
                    {p.enRetard && <Badge color="red" size="xs">En retard</Badge>}
                  </Table.Td>
                  <Table.Td><Badge color={COULEURS_PLAN[p.statut]} variant="light">{LIBELLES_PLAN[p.statut]}</Badge></Table.Td>
                  <Table.Td>
                    {ouvert && (
                      <Group gap="xs" wrap="nowrap">
                        {p.statut === 'A_FAIRE' && (
                          <Button size="compact-xs" variant="light" onClick={() => statut.mutate({ plan: p, nouveau: 'EN_COURS' })}>Démarré</Button>
                        )}
                        <Button size="compact-xs" variant="light" color="green" onClick={() => statut.mutate({ plan: p, nouveau: 'TERMINE' })}>Terminé</Button>
                        {supervision && (
                          <Button size="compact-xs" variant="subtle" color="gray" onClick={() => statut.mutate({ plan: p, nouveau: 'ANNULE' })}>Annuler</Button>
                        )}
                      </Group>
                    )}
                  </Table.Td>
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      )}
      {supervision && (
        <Stack gap="xs">
          <Textarea label="Nouvelle mesure à prendre" value={description} onChange={(e) => setDescription(e.currentTarget.value)} autosize minRows={2} maxLength={2000} />
          <TextInput label="Échéance" type="date" min={aujourdhui()} value={echeance} onChange={(e) => setEcheance(e.currentTarget.value)} />
          {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
          <Group justify="flex-end">
            <Button disabled={description.trim() === '' || echeance === ''} loading={creation.isPending} onClick={() => creation.mutate()}>
              Ajouter
            </Button>
          </Group>
        </Stack>
      )}
    </Stack>
  )
}
