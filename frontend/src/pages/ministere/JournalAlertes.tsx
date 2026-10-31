import { Alert, Badge, Loader, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { journalAlertes } from '../../api/endpoints'

const TYPES: Record<string, string> = {
  URGENCE: 'Inspection urgente',
  PLAN_ACTION: 'Plan d\'action',
  RESULTATS_EXAMEN: 'Résultats d\'examen',
}

/** Qui a été prévenu, quand, par quel canal, et si l'envoi a réussi. */
export default function JournalAlertes() {
  const alertes = useQuery({ queryKey: ['journal-alertes'], queryFn: journalAlertes })
  return (
    <Stack mt="md">
      <Text c="dimmed" size="sm">Les 200 dernières alertes envoyées (inspections urgentes, rappels de plans d'action et de résultats d'examens ; les messages aux parents d'élèves restent visibles uniquement par leur établissement).</Text>
      {alertes.isPending && <Loader />}
      {alertes.isError && <Alert color="red" variant="light">{alertes.error.message}</Alert>}
      {alertes.data && alertes.data.length === 0 && <Text c="dimmed">Aucune alerte envoyée pour le moment.</Text>}
      {alertes.data && alertes.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Date</Table.Th>
              <Table.Th>Type</Table.Th>
              <Table.Th>Canal</Table.Th>
              <Table.Th>Destinataire</Table.Th>
              <Table.Th>Message</Table.Th>
              <Table.Th>Envoi</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {alertes.data.map((a) => (
              <Table.Tr key={a.id}>
                <Table.Td>{new Date(a.envoyeeLe).toLocaleString('fr-FR')}</Table.Td>
                <Table.Td>{TYPES[a.type] ?? a.type}</Table.Td>
                <Table.Td>{a.canal === 'SMS' ? 'SMS' : 'E-mail'}</Table.Td>
                <Table.Td>{a.destinataire}</Table.Td>
                <Table.Td>{a.resume}</Table.Td>
                <Table.Td><Badge color={a.reussie ? 'green' : 'red'} variant="light">{a.reussie ? 'Envoyé' : 'Échec'}</Badge></Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}
