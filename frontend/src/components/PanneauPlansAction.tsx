import { useState } from 'react'
import { Alert, Badge, Group, Loader, SegmentedControl, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import type { PlanAction } from '../api/types'
import { COULEURS_PLAN, LIBELLES_PLAN } from './PlansActionEtablissement'

const jour = (iso: string) => new Date(iso).toLocaleDateString('fr-FR')

/** Vue d'ensemble des plans d'action du périmètre (national ou régional). */
export default function PanneauPlansAction({ cle, charger }: { cle: string; charger: () => Promise<PlanAction[]> }) {
  const plans = useQuery({ queryKey: ['plans-action', 'liste', cle], queryFn: charger })
  const [filtre, setFiltre] = useState('OUVERTS')

  const visibles = (plans.data ?? []).filter((p) => {
    if (filtre === 'RETARD') return p.enRetard
    if (filtre === 'OUVERTS') return p.statut === 'A_FAIRE' || p.statut === 'EN_COURS'
    return true
  })
  const enRetard = (plans.data ?? []).filter((p) => p.enRetard).length

  return (
    <Stack mt="md">
      <Group justify="space-between">
        <SegmentedControl
          value={filtre}
          onChange={setFiltre}
          data={[
            { value: 'OUVERTS', label: 'Ouverts' },
            { value: 'RETARD', label: `En retard (${enRetard})` },
            { value: 'TOUS', label: 'Tous' },
          ]}
        />
      </Group>
      {plans.isPending && <Loader />}
      {plans.isError && <Alert color="red" variant="light">{plans.error.message}</Alert>}
      {plans.data && visibles.length === 0 && <Text c="dimmed">Aucun plan d'action à afficher.</Text>}
      {visibles.length > 0 && (
        <Table verticalSpacing="xs" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Établissement</Table.Th>
              <Table.Th>Mesure</Table.Th>
              <Table.Th>Échéance</Table.Th>
              <Table.Th>Statut</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {visibles.map((p) => (
              <Table.Tr key={p.id}>
                <Table.Td>
                  {p.etablissement}
                  {p.zone && <Text size="xs" c="dimmed">{p.zone}</Text>}
                </Table.Td>
                <Table.Td>{p.description}</Table.Td>
                <Table.Td>
                  <Text size="sm" c={p.enRetard ? 'red' : undefined}>{jour(p.echeance)}</Text>
                </Table.Td>
                <Table.Td><Badge color={COULEURS_PLAN[p.statut]} variant="light">{LIBELLES_PLAN[p.statut]}</Badge></Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}
