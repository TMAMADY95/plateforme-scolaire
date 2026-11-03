import { Alert, Stack, Table, Text, Title } from '@mantine/core'
import { useElevesSansActe } from '../../hooks/useIdentiteNationale'
import { libelleActe } from '../../api/typesEcole'
import { formaterDate } from '../../utils/dates'

export default function PageElevesSansActe({ etablissementId }: { etablissementId: number }) {
  const eleves = useElevesSansActe(etablissementId)

  return (
    <Stack>
      <Title order={2}>Élèves sans acte de naissance</Title>
      <Text size="sm" c="dimmed">
        Un acte de naissance (extrait ou jugement supplétif) est exigé pour l'inscription au CEE, au BEPC et au
        baccalauréat. Cette liste sert à régulariser les dossiers avant la campagne d'inscription aux examens.
      </Text>
      {eleves.isError && (
        <Alert color="red" variant="light">
          {eleves.error.message}
        </Alert>
      )}
      {eleves.data && eleves.data.length === 0 && (
        <Alert color="green" variant="light">
          Tous les élèves inscrits de l'établissement ont une pièce d'état civil enregistrée.
        </Alert>
      )}
      {eleves.data && eleves.data.length > 0 && (
        <>
          <Table.ScrollContainer minWidth={720}>
            <Table striped highlightOnHover withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Nom</Table.Th>
                  <Table.Th>Prénom</Table.Th>
                  <Table.Th>Matricule</Table.Th>
                  <Table.Th>INE</Table.Th>
                  <Table.Th>Naissance</Table.Th>
                  <Table.Th>État civil</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {eleves.data.map((e) => (
                  <Table.Tr key={e.id}>
                    <Table.Td>{e.nom}</Table.Td>
                    <Table.Td>{e.prenom}</Table.Td>
                    <Table.Td>{e.matricule}</Table.Td>
                    <Table.Td>{e.ine}</Table.Td>
                    <Table.Td>{formaterDate(e.dateNaissance)}</Table.Td>
                    <Table.Td>{libelleActe(e.typeActeNaissance)}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
          <Text size="sm" c="dimmed">
            {eleves.data.length} élève(s) à régulariser.
          </Text>
        </>
      )}
    </Stack>
  )
}
