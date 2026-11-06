import { Alert, Badge, Group, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { syntheseEnseignants } from '../../api/endpoints'

/** Enseignants du périmètre : profils renseignés, formés au métier, non titulaires, postes à pourvoir. Totaux uniquement. */
export default function EnseignantsSynthese() {
  const synthese = useQuery({ queryKey: ['enseignants-synthese'], queryFn: syntheseEnseignants })
  const d = synthese.data
  return (
    <Stack>
      <div>
        <Title order={4}>Enseignants et besoins</Title>
        <Text c="dimmed" size="sm">
          Postes à pourvoir = postes prévus par l'établissement moins enseignants rattachés. Aucune donnée nominative n'est affichée.
        </Text>
      </div>
      {synthese.isLoading && <Loader />}
      {synthese.isError && <Alert color="red">Synthèse indisponible.</Alert>}
      {d && (
        <>
          <Group gap="xs">
            <Badge variant="light" size="lg">{d.resume.enseignants} enseignants</Badge>
            <Badge variant="light" size="lg">{d.resume.profilsRenseignes} profils renseignés</Badge>
            <Badge variant="light" size="lg" color="green">{d.resume.formes} formés au métier</Badge>
            <Badge variant="light" size="lg" color={d.resume.nonTitulaires ? 'yellow' : 'gray'}>{d.resume.nonTitulaires} non titulaires</Badge>
            <Badge variant="light" size="lg">{d.resume.femmes} femmes</Badge>
            <Badge variant="light" size="lg" color={d.resume.deficitTotal ? 'orange' : 'gray'}>{d.resume.deficitTotal} postes à pourvoir</Badge>
          </Group>
          <Table.ScrollContainer minWidth={700}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Établissement</Table.Th>
                  <Table.Th>Zone</Table.Th>
                  <Table.Th>Enseignants</Table.Th>
                  <Table.Th>Profils renseignés</Table.Th>
                  <Table.Th>Formés</Table.Th>
                  <Table.Th>Non titulaires</Table.Th>
                  <Table.Th>Postes prévus</Table.Th>
                  <Table.Th>À pourvoir</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.etablissements.map((l) => (
                  <Table.Tr key={l.etablissementId}>
                    <Table.Td>{l.etablissement}</Table.Td>
                    <Table.Td>{l.zone ?? '—'}</Table.Td>
                    <Table.Td>{l.enseignants}</Table.Td>
                    <Table.Td>{l.profilsRenseignes}</Table.Td>
                    <Table.Td>{l.formes}</Table.Td>
                    <Table.Td>{l.nonTitulaires}</Table.Td>
                    <Table.Td>{l.postes ?? '—'}</Table.Td>
                    <Table.Td>{l.deficit ?? '—'}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </>
      )}
    </Stack>
  )
}
