import { useState } from 'react'
import { ActionIcon, Alert, Badge, Button, Group, Loader, Menu, Stack, Table, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { changerStatutEtablissement, creerDirection, listerEtablissements, listerZones } from '../../api/endpoints'
import type { Etablissement, StatutEtablissement } from '../../api/types'
import ConfirmationModal from '../../components/ConfirmationModal'
import ComptesDirectionModal from './ComptesDirectionModal'
import DirectionModal from './DirectionModal'
import EtablissementModal from './EtablissementModal'

const COULEURS: Record<string, string> = { ACTIF: 'green', SUSPENDU: 'orange', FERME: 'gray' }

export default function ListeEtablissements() {
  const queryClient = useQueryClient()
  const [creation, { open: ouvrirCreation, close: fermerCreation }] = useDisclosure()
  const [aModifier, setAModifier] = useState<Etablissement | null>(null)
  const [pourDirection, setPourDirection] = useState<Etablissement | null>(null)
  const [pourComptes, setPourComptes] = useState<Etablissement | null>(null)
  const [changement, setChangement] = useState<{ etablissement: Etablissement; statut: StatutEtablissement } | null>(
    null,
  )

  const etablissements = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })
  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones })
  const nomsZones = new Map((zones.data ?? []).map((z) => [z.id, z.nom]))

  const statut = useMutation({
    mutationFn: (c: { etablissement: Etablissement; statut: StatutEtablissement }) =>
      changerStatutEtablissement(c.etablissement.id, c.statut),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['etablissements'] })
      void queryClient.invalidateQueries({ queryKey: ['synthese-nationale'] })
      setChangement(null)
    },
    onError: (e) => {
      setChangement(null)
      notifications.show({ color: 'red', title: 'Changement refusé', message: e.message })
    },
  })

  return (
    <Stack mt="md">
      <Group justify="flex-end">
        <Button onClick={ouvrirCreation}>Nouvel établissement</Button>
      </Group>

      {etablissements.isPending && <Loader />}
      {etablissements.isError && (
        <Alert color="red" variant="light">
          {etablissements.error.message}
        </Alert>
      )}
      {etablissements.data && etablissements.data.length === 0 && (
        <Text c="dimmed">Aucun établissement. Créez le premier avec « Nouvel établissement ».</Text>
      )}
      {etablissements.data && etablissements.data.length > 0 && (
        <Table.ScrollContainer minWidth={640}>
          <Table striped highlightOnHover withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Code</Table.Th>
                <Table.Th>Nom</Table.Th>
                <Table.Th>Type</Table.Th>
                <Table.Th>Zone</Table.Th>
                <Table.Th>Statut</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {etablissements.data.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td>{e.codeOfficiel}</Table.Td>
                  <Table.Td>{e.nom}</Table.Td>
                  <Table.Td>{e.typeEtablissement ?? '—'}</Table.Td>
                  <Table.Td>{e.zoneId === null ? '—' : (nomsZones.get(e.zoneId) ?? '—')}</Table.Td>
                  <Table.Td>
                    <Badge color={COULEURS[e.statut] ?? 'gray'}>{e.statut}</Badge>
                  </Table.Td>
                  <Table.Td>
                    <Menu position="bottom-end" withinPortal>
                      <Menu.Target>
                        <ActionIcon variant="subtle" aria-label={`Actions pour ${e.nom}`}>
                          …
                        </ActionIcon>
                      </Menu.Target>
                      <Menu.Dropdown>
                        <Menu.Item onClick={() => setAModifier(e)}>Modifier</Menu.Item>
                        <Menu.Item disabled={e.statut !== 'ACTIF'} onClick={() => setPourDirection(e)}>
                          Créer le compte de direction
                        </Menu.Item>
                        <Menu.Item onClick={() => setPourComptes(e)}>Comptes de direction</Menu.Item>
                        <Menu.Divider />
                        {e.statut !== 'ACTIF' && (
                          <Menu.Item onClick={() => setChangement({ etablissement: e, statut: 'ACTIF' })}>
                            Réactiver
                          </Menu.Item>
                        )}
                        {e.statut === 'ACTIF' && (
                          <Menu.Item color="orange" onClick={() => setChangement({ etablissement: e, statut: 'SUSPENDU' })}>
                            Suspendre
                          </Menu.Item>
                        )}
                        {e.statut !== 'FERME' && (
                          <Menu.Item color="red" onClick={() => setChangement({ etablissement: e, statut: 'FERME' })}>
                            Fermer
                          </Menu.Item>
                        )}
                      </Menu.Dropdown>
                    </Menu>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {creation && <EtablissementModal zones={zones.data ?? []} onFermer={fermerCreation} />}
      {aModifier && (
        <EtablissementModal etablissement={aModifier} zones={zones.data ?? []} onFermer={() => setAModifier(null)} />
      )}
      {pourDirection && (
        <DirectionModal
          titre={`Direction de « ${pourDirection.nom} »`}
          creer={(requete) => creerDirection(pourDirection.id, requete)}
          onFermer={() => setPourDirection(null)}
        />
      )}
      {pourComptes && <ComptesDirectionModal etablissement={pourComptes} onFermer={() => setPourComptes(null)} />}
      {changement && (
        <ConfirmationModal
          titre="Changer le statut"
          message={`« ${changement.etablissement.nom} » passera au statut ${changement.statut}.`}
          libelleConfirmer="Confirmer"
          couleur={changement.statut === 'ACTIF' ? 'green' : 'red'}
          enCours={statut.isPending}
          onConfirmer={() => statut.mutate(changement)}
          onFermer={() => setChangement(null)}
        />
      )}
    </Stack>
  )
}
