import { Alert, Badge, Group, Loader, Modal, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { controleCoherenceExamen } from '../api/endpoints'

/** Contrôle de cohérence (lecture seule) : anomalies des listes et des notes avant transmission ou validation. */
export default function ControleCoherenceModal({ etablissementId, examenId, titre, onFermer }: {
  etablissementId: number
  examenId: number
  titre: string
  onFermer: () => void
}) {
  const donnees = useQuery({
    queryKey: ['controle-coherence', etablissementId, examenId],
    queryFn: () => controleCoherenceExamen(etablissementId, examenId),
  })
  const d = donnees.data

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Contrôle de cohérence : ${titre}`}>
      <Stack gap="sm">
        {donnees.isLoading && <Loader size="sm" />}
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Group gap="xs">
              <Text size="sm" c="dimmed">{d.candidats} candidat(s) contrôlé(s)</Text>
              <Badge color={d.erreurs > 0 ? 'red' : 'green'} variant="light">{d.erreurs} erreur(s)</Badge>
              <Badge color={d.avertissements > 0 ? 'orange' : 'green'} variant="light">{d.avertissements} avertissement(s)</Badge>
            </Group>
            {d.anomalies.length === 0 ? (
              <Alert color="green" variant="light">Aucune anomalie détectée.</Alert>
            ) : (
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Gravité</Table.Th>
                    <Table.Th>Candidat</Table.Th>
                    <Table.Th>N°</Table.Th>
                    <Table.Th>Anomalie</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.anomalies.map((a, i) => (
                    <Table.Tr key={`${a.candidatId}-${a.code}-${i}`}>
                      <Table.Td>
                        <Badge color={a.gravite === 'ERREUR' ? 'red' : 'orange'} variant="light">
                          {a.gravite === 'ERREUR' ? 'Erreur' : 'À vérifier'}
                        </Badge>
                      </Table.Td>
                      <Table.Td>{a.candidat}</Table.Td>
                      <Table.Td>{a.numero ?? '-'}</Table.Td>
                      <Table.Td>{a.message}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            )}
          </>
        )}
      </Stack>
    </Modal>
  )
}
