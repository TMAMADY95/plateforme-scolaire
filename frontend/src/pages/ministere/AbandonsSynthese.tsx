import { Alert, Badge, Group, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { syntheseAbandons } from '../../api/endpoints'
import { MOTIFS_ABANDON, libelleDans } from '../../api/types'

const taux = (v: number | null) => (v === null ? '—' : `${String(v).replace('.', ',')} %`)

/** Abandons, orphelins et élèves en situation de handicap sur le périmètre : totaux uniquement. */
export default function AbandonsSynthese({ anneeId }: { anneeId: number }) {
  const synthese = useQuery({ queryKey: ['abandons-synthese', anneeId], queryFn: () => syntheseAbandons(anneeId) })
  const d = synthese.data
  return (
    <Stack>
      <div>
        <Title order={4}>Abandon scolaire et élèves vulnérables</Title>
        <Text c="dimmed" size="sm">Déclarés par les directions d'établissement. Taux d'abandon = abandons pour 100 élèves inscrits. Aucune donnée nominative.</Text>
      </div>
      {synthese.isLoading && <Loader />}
      {synthese.isError && <Alert color="red">Synthèse indisponible.</Alert>}
      {d && (
        <>
          <Group gap="xs">
            <Badge variant="light" size="lg" color={d.resume.abandons ? 'orange' : 'gray'}>{d.resume.abandons} abandons ({taux(d.resume.tauxAbandon)})</Badge>
            <Badge variant="light" size="lg">{d.resume.fillesAbandons} filles</Badge>
            <Badge variant="light" size="lg">{d.resume.orphelins} orphelins</Badge>
            <Badge variant="light" size="lg">{d.resume.handicapes} élèves en situation de handicap</Badge>
            {d.motifs.map((m) => (
              <Badge key={m.motif} variant="outline" size="lg">{libelleDans(MOTIFS_ABANDON, m.motif)} : {m.nombre}</Badge>
            ))}
          </Group>
          <Table.ScrollContainer minWidth={650}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Établissement</Table.Th>
                  <Table.Th>Zone</Table.Th>
                  <Table.Th>Élèves</Table.Th>
                  <Table.Th>Abandons</Table.Th>
                  <Table.Th>Taux</Table.Th>
                  <Table.Th>Orphelins</Table.Th>
                  <Table.Th>Handicap</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.etablissements.map((l) => (
                  <Table.Tr key={l.etablissementId}>
                    <Table.Td>{l.etablissement}</Table.Td>
                    <Table.Td>{l.zone ?? '—'}</Table.Td>
                    <Table.Td>{l.eleves}</Table.Td>
                    <Table.Td>{l.abandons}</Table.Td>
                    <Table.Td>{taux(l.tauxAbandon)}</Table.Td>
                    <Table.Td>{l.orphelins}</Table.Td>
                    <Table.Td>{l.handicapes}</Table.Td>
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
