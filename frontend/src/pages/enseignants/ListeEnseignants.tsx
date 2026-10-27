import { Alert, Button, Group, Loader, Stack, Table, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { useQuery } from '@tanstack/react-query'
import { listerEnseignants } from '../../api/endpoints'
import NouvelEnseignantModal from './NouvelEnseignantModal'

export default function ListeEnseignants({ etablissementId }: { etablissementId: number }) {
  const [ouverte, { open, close }] = useDisclosure()
  const enseignants = useQuery({
    queryKey: ['enseignants', etablissementId],
    queryFn: () => listerEnseignants(etablissementId),
  })

  return (
    <Stack mt="md">
      <Group justify="flex-end">
        <Button onClick={open}>Nouvel enseignant</Button>
      </Group>

      {enseignants.isPending && <Loader />}
      {enseignants.isError && (
        <Alert color="red" variant="light">
          {enseignants.error.message}
        </Alert>
      )}
      {enseignants.data && enseignants.data.length === 0 && (
        <Text c="dimmed">Aucun enseignant. Créez le premier avec « Nouvel enseignant ».</Text>
      )}
      {enseignants.data && enseignants.data.length > 0 && (
        <Table.ScrollContainer minWidth={520}>
          <Table striped highlightOnHover withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Nom</Table.Th>
                <Table.Th>Adresse e-mail</Table.Th>
                <Table.Th>Matricule</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {enseignants.data.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td>
                    {e.prenom} {e.nom}
                  </Table.Td>
                  <Table.Td>{e.email}</Table.Td>
                  <Table.Td>{e.matricule ?? '—'}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {ouverte && <NouvelEnseignantModal etablissementId={etablissementId} onFermer={close} />}
    </Stack>
  )
}
