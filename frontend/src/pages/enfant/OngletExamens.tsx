import { Alert, Badge, Card, Group, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { resultatsExamensEleve } from '../../api/endpoints'

const MENTIONS: Record<string, string> = {
  TRES_BIEN: 'Très bien',
  BIEN: 'Bien',
  ASSEZ_BIEN: 'Assez bien',
  PASSABLE: 'Passable',
}

/** Résultats proclamés des examens nationaux (CEE, BEPC, BAC) ; rien ne s'affiche avant la proclamation. */
export default function OngletExamens({ eleveId }: { eleveId: number }) {
  const resultats = useQuery({ queryKey: ['resultats-examens', eleveId], queryFn: () => resultatsExamensEleve(eleveId) })

  if (resultats.isPending) return <Loader size="sm" />
  if (resultats.isError) return <Alert color="red" variant="light">{resultats.error.message}</Alert>
  if (resultats.data.length === 0) {
    return <Text size="sm" c="dimmed">Aucun résultat d'examen national proclamé pour le moment.</Text>
  }

  return (
    <Stack>
      {resultats.data.map((r) => (
        <Card key={r.examenId} withBorder>
          <Stack gap="xs">
            <Group justify="space-between">
              <Title order={4}>{r.examen} ({r.anneeScolaire})</Title>
              {r.statut === 'ABSENT' && <Badge color="gray">Absent(e)</Badge>}
              {r.statut === 'ELIMINE' && <Badge color="red">Éliminé(e)</Badge>}
              {r.decision === 'ADMIS' && <Badge color="green">Admis(e){r.mention ? ` : ${MENTIONS[r.mention] ?? r.mention}` : ''}</Badge>}
              {r.decision === 'NON_ADMIS' && r.statut === 'PRESENT' && <Badge color="red">Non admis(e)</Badge>}
            </Group>
            <Text size="sm" c="dimmed">
              {r.numero ? `N° ${r.numero} · ` : ''}
              {r.dateProclamation ? `Proclamé le ${new Date(r.dateProclamation).toLocaleDateString('fr-FR')}` : 'Proclamé'}
              {r.moyenne !== null ? ` · Moyenne ${r.moyenne.toFixed(2)}/${r.bareme}` : ''}
            </Text>
            {r.notes.length > 0 && (
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Épreuve</Table.Th>
                    <Table.Th>Coef.</Table.Th>
                    <Table.Th>Note /{r.bareme}</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {r.notes.map((n) => (
                    <Table.Tr key={n.epreuve}>
                      <Table.Td>{n.epreuve}</Table.Td>
                      <Table.Td>{n.coefficient}</Table.Td>
                      <Table.Td>{n.note.toFixed(2)}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            )}
          </Stack>
        </Card>
      ))}
    </Stack>
  )
}
