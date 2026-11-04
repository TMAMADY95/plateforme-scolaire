import { Alert, Stack, Table, Text, Title } from '@mantine/core'
import { useElevesSansActe } from '../../hooks/useIdentiteNationale'
import { libelleActe } from '../../api/aideIdentite'
import { formaterDate } from '../../utils/dates'

export default function PageElevesSansActe({ etablissementId }: { etablissementId: number }) {
  const eleves = useElevesSansActe(etablissementId)

  return (
    <Stack>
      <Title order={2}>Eleves sans acte de naissance</Title>
      <Text size="sm" c="dimmed">
        Un acte de naissance (extrait ou jugement suppletif) est exige pour l'inscription au CEE, au BEPC et au
        baccalaureat. Cette liste sert a regulariser les dossiers avant la campagne d'inscription aux examens.
      </Text>
      {eleves.isError && (
        <Alert color="red" variant="light">
          {eleves.error.message}
        </Alert>
      )}
      {eleves.data && eleves.data.length === 0 && (
        <Alert color="green" variant="light">
          Tous les eleves inscrits de l'etablissement ont une piece d'etat civil enregistree.
        </Alert>
      )}
      {eleves.data && eleves.data.length > 0 && (
        <>
          <Table.ScrollContainer minWidth={720}>
            <Table striped highlightOnHover withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Nom</Table.Th>
                  <Table.Th>Prenom</Table.Th>
                  <Table.Th>Matricule</Table.Th>
                  <Table.Th>INE</Table.Th>
                  <Table.Th>Naissance</Table.Th>
                  <Table.Th>Etat civil</Table.Th>
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
            {eleves.data.length} eleve(s) a regulariser.
          </Text>
        </>
      )}
    </Stack>
  )
}
