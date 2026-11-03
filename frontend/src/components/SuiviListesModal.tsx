import { Alert, Badge, Group, Loader, Modal, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { suiviListesExamen } from '../api/endpoints'

const STATUTS: Record<string, { libelle: string; couleur: string }> = {
  BROUILLON: { libelle: 'Brouillon', couleur: 'gray' },
  TRANSMISE: { libelle: 'Transmise', couleur: 'blue' },
  VALIDEE_REGION: { libelle: 'Vérifiée', couleur: 'cyan' },
  VALIDEE: { libelle: 'Validée', couleur: 'green' },
}
const ATTENTE: Record<string, string> = { VERIFICATION: 'DPE/DCE ou IRE', VALIDATION: 'DGECS' }

/** Suivi des listes de candidats par établissement : ce qui attend une vérification ou une validation, et depuis combien de jours. */
export default function SuiviListesModal({ examenId, titre, onFermer }: { examenId: number; titre: string; onFermer: () => void }) {
  const donnees = useQuery({ queryKey: ['suivi-listes', examenId], queryFn: () => suiviListesExamen(examenId) })
  const d = donnees.data
  return (
    <Modal opened onClose={onFermer} size="xl" title={`Suivi des listes de candidats : ${titre}`}>
      <Stack gap="sm">
        {donnees.isLoading && <Loader size="sm" />}
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Group gap="xs">
              <Badge color="gray" variant="light">{d.brouillons} brouillon(s)</Badge>
              <Badge color="blue" variant="light">{d.transmises} à vérifier</Badge>
              <Badge color="cyan" variant="light">{d.verifiees} à valider (DGECS)</Badge>
              <Badge color="green" variant="light">{d.validees} validée(s)</Badge>
            </Group>
            {d.lignes.length === 0 ? (
              <Text size="sm" c="dimmed">Aucun candidat inscrit dans votre périmètre.</Text>
            ) : (
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Établissement</Table.Th>
                    <Table.Th>Zone</Table.Th>
                    <Table.Th>Candidats</Table.Th>
                    <Table.Th>Statut</Table.Th>
                    <Table.Th>En attente de</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.lignes.map((l) => (
                    <Table.Tr key={l.etablissementId}>
                      <Table.Td>{l.etablissement}</Table.Td>
                      <Table.Td>{l.zone ?? '-'}</Table.Td>
                      <Table.Td>{l.candidats}</Table.Td>
                      <Table.Td><Badge color={STATUTS[l.statut]?.couleur ?? 'gray'} variant="light">{STATUTS[l.statut]?.libelle ?? l.statut}</Badge></Table.Td>
                      <Table.Td>
                        {l.enAttenteDe ? `${ATTENTE[l.enAttenteDe]}${l.joursAttente !== null ? ` · depuis ${l.joursAttente} j` : ''}` : '-'}
                      </Table.Td>
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
