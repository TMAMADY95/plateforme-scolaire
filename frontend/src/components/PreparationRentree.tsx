import { Alert, Badge, Button, Group, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { preparerRentree, rentree } from '../api/endpoints'

/** Rentrée : crée les inscriptions de l'année suivante des élèves qui passent ou redoublent d'après les décisions du conseil de classe. */
export default function PreparationRentree({ etablissementId, anneeId }: { etablissementId: number; anneeId: number }) {
  const queryClient = useQueryClient()
  const apercu = useQuery({ queryKey: ['rentree', etablissementId, anneeId], queryFn: () => rentree(etablissementId, anneeId) })
  const preparer = useMutation({
    mutationFn: () => preparerRentree(etablissementId, anneeId),
    onSuccess: (r) => {
      notifications.show({ color: 'green', message: `${r.crees} inscription(s) créée(s) : acceptez-les avec une classe dans l'année ${r.rentree.anneeSuivante}.` })
      queryClient.setQueryData(['rentree', etablissementId, anneeId], r.rentree)
      void queryClient.invalidateQueries({ queryKey: ['inscriptions'] })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Action impossible', message: e.message }),
  })
  const d = apercu.data
  if (apercu.isLoading) return <Loader size="sm" />
  if (!d || d.classes.length === 0) return null
  const r = d.resume
  return (
    <Stack mt="xl">
      <div>
        <Title order={3}>Rentrée {d.anneeSuivante ?? ''}</Title>
        <Text c="dimmed" size="sm">
          Les élèves dont le conseil de classe a décidé le passage ou le redoublement sont réinscrits l'année suivante (en attente) ; vous les acceptez ensuite avec une classe. Les exclus ne sont pas réinscrits, les élèves du dernier niveau qui passent sont en fin de parcours.
        </Text>
      </div>
      {d.anneeSuivanteId === null && <Alert color="orange" variant="light">Aucune année scolaire suivante n'est définie : elle doit être créée avant de préparer la rentrée.</Alert>}
      <Group gap="xs">
        <Badge variant="light" size="lg" color="blue">{r.aReinscrire} à réinscrire</Badge>
        <Badge variant="light" size="lg" color="green">{r.dejaInscrits} déjà inscrits</Badge>
        <Badge variant="light" size="lg">{r.finDeParcours} fin de parcours</Badge>
        <Badge variant="light" size="lg" color="red">{r.exclus} exclus</Badge>
        <Badge variant="light" size="lg" color={r.sansDecision ? 'orange' : 'gray'}>{r.sansDecision} sans décision</Badge>
      </Group>
      <Table striped withTableBorder>
        <Table.Thead><Table.Tr><Table.Th>Classe</Table.Th><Table.Th>Passages</Table.Th><Table.Th>Redoublements</Table.Th><Table.Th>Exclusions</Table.Th><Table.Th>Fin de parcours</Table.Th><Table.Th>Sans décision</Table.Th></Table.Tr></Table.Thead>
        <Table.Tbody>
          {d.classes.map((c) => (
            <Table.Tr key={c.classeId}>
              <Table.Td>{c.classe}<Text size="xs" c="dimmed">{c.niveau}</Text></Table.Td>
              <Table.Td>{c.passages}</Table.Td>
              <Table.Td>{c.redoublements}</Table.Td>
              <Table.Td>{c.exclusions}</Table.Td>
              <Table.Td>{c.finDeParcours}</Table.Td>
              <Table.Td>{c.sansDecision}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Group>
        <Button disabled={d.anneeSuivanteId === null || r.aReinscrire === 0} loading={preparer.isPending} onClick={() => preparer.mutate()}>
          Créer les inscriptions de la rentrée ({r.aReinscrire})
        </Button>
      </Group>
    </Stack>
  )
}
