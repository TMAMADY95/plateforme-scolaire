import { useState } from 'react'
import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  Menu,
  Select,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
} from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  changerStatutEtablissement,
  creerDirection,
  listerCycles,
  listerEtablissements,
  listerZones,
} from '../../api/endpoints'
import type { Etablissement, StatutEtablissement } from '../../api/types'
import { BadgeSecteur, BadgesCycles } from '../../components/BadgesEtablissement'
import ConfirmationModal from '../../components/ConfirmationModal'
import EnteteSection from '../../components/EnteteSection'
import ComptesDirectionModal from './ComptesDirectionModal'
import DirectionModal from './DirectionModal'
import EtablissementModal from './EtablissementModal'
import ImportEtablissementsModal from './ImportEtablissementsModal'

const COULEURS: Record<string, string> = { ACTIF: 'green', SUSPENDU: 'orange', FERME: 'gray' }
const LIBELLES_STATUT: Record<string, string> = { ACTIF: 'Actif', SUSPENDU: 'Suspendu', FERME: 'Fermé' }

function Compteur({ libelle, valeur, couleur }: { libelle: string; valeur: number; couleur: string }) {
  return (
    <Card withBorder padding="sm">
      <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
        {libelle}
      </Text>
      <Text fz={28} fw={700} lh={1.2} c={couleur}>
        {valeur}
      </Text>
    </Card>
  )
}

export default function ListeEtablissements() {
  const queryClient = useQueryClient()
  const [creation, { open: ouvrirCreation, close: fermerCreation }] = useDisclosure()
  const [import_, { open: ouvrirImport, close: fermerImport }] = useDisclosure()
  const [aModifier, setAModifier] = useState<Etablissement | null>(null)
  const [pourDirection, setPourDirection] = useState<Etablissement | null>(null)
  const [pourComptes, setPourComptes] = useState<Etablissement | null>(null)
  const [changement, setChangement] = useState<{ etablissement: Etablissement; statut: StatutEtablissement } | null>(
    null,
  )
  const [recherche, setRecherche] = useState('')
  const [secteur, setSecteur] = useState('TOUS')
  const [cycle, setCycle] = useState<string | null>(null)
  const [statutFiltre, setStatutFiltre] = useState<string | null>(null)

  const etablissements = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })
  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones })
  const cycles = useQuery({ queryKey: ['cycles'], queryFn: listerCycles })
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

  const tous = etablissements.data ?? []
  const requete = recherche.trim().toLowerCase()
  const visibles = tous.filter(
    (e) =>
      (requete === '' || e.nom.toLowerCase().includes(requete) || e.codeOfficiel.toLowerCase().includes(requete)) &&
      (secteur === 'TOUS' || e.secteur === secteur) &&
      (cycle === null || e.cycles.some((c) => String(c.id) === cycle)) &&
      (statutFiltre === null || e.statut === statutFiltre),
  )

  return (
    <Stack mt="md" gap="lg">
      {import_ && <ImportEtablissementsModal onFermer={fermerImport} />}
      <EnteteSection
        titre="Établissements"
        description="Écoles, collèges et lycées du réseau, publics ou privés."
        action={
          <Group gap="xs">
            <Button variant="default" onClick={ouvrirImport}>
              Importer (CSV)
            </Button>
            <Button onClick={ouvrirCreation}>Nouvel établissement</Button>
          </Group>
        }
      />

      {etablissements.isPending && <Loader />}
      {etablissements.isError && (
        <Alert color="red" variant="light">
          {etablissements.error.message}
        </Alert>
      )}

      {etablissements.data && (
        <SimpleGrid cols={{ base: 2, sm: 4 }}>
          <Compteur libelle="Établissements" valeur={tous.length} couleur="marine" />
          <Compteur libelle="Publics" valeur={tous.filter((e) => e.secteur === 'PUBLIC').length} couleur="blue" />
          <Compteur libelle="Privés" valeur={tous.filter((e) => e.secteur === 'PRIVE').length} couleur="grape" />
          <Compteur libelle="Actifs" valeur={tous.filter((e) => e.statut === 'ACTIF').length} couleur="green" />
        </SimpleGrid>
      )}

      {etablissements.data && etablissements.data.length === 0 && (
        <Text c="dimmed">Aucun établissement. Créez le premier avec « Nouvel établissement ».</Text>
      )}

      {etablissements.data && etablissements.data.length > 0 && (
        <Card withBorder padding="md">
          <Stack gap="md">
            <Group align="flex-end" gap="md">
              <TextInput
                label="Rechercher"
                placeholder="Nom ou code officiel"
                value={recherche}
                onChange={(e) => setRecherche(e.currentTarget.value)}
                style={{ flex: 1, minWidth: 200 }}
              />
              <div>
                <Text size="sm" fw={500} mb={4}>
                  Secteur
                </Text>
                <SegmentedControl
                  value={secteur}
                  onChange={setSecteur}
                  data={[
                    { value: 'TOUS', label: 'Tous' },
                    { value: 'PUBLIC', label: 'Public' },
                    { value: 'PRIVE', label: 'Privé' },
                  ]}
                />
              </div>
              <Select
                label="Cycle"
                placeholder="Tous"
                data={(cycles.data ?? []).map((c) => ({ value: String(c.id), label: c.libelle }))}
                value={cycle}
                onChange={setCycle}
                clearable
                w={160}
              />
              <Select
                label="Statut"
                placeholder="Tous"
                data={Object.entries(LIBELLES_STATUT).map(([value, label]) => ({ value, label }))}
                value={statutFiltre}
                onChange={setStatutFiltre}
                clearable
                w={150}
              />
            </Group>

            <Text size="sm" c="dimmed">
              {visibles.length} établissement{visibles.length > 1 ? 's' : ''} sur {tous.length}
            </Text>

            {visibles.length === 0 ? (
              <Text c="dimmed" ta="center" py="lg">
                Aucun établissement ne correspond à ces critères.
              </Text>
            ) : (
              <Table.ScrollContainer minWidth={820}>
                <Table highlightOnHover verticalSpacing="sm">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Établissement</Table.Th>
                      <Table.Th>Secteur</Table.Th>
                      <Table.Th>Cycles</Table.Th>
                      <Table.Th>Zone</Table.Th>
                      <Table.Th>Statut</Table.Th>
                      <Table.Th />
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {visibles.map((e) => (
                      <Table.Tr key={e.id}>
                        <Table.Td>
                          <Text fw={600}>{e.nom}</Text>
                          <Text size="xs" c="dimmed">
                            {e.codeOfficiel}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <BadgeSecteur secteur={e.secteur} />
                          {e.typeEtablissement && (
                            <Text size="xs" c="dimmed">
                              {e.typeEtablissement}
                            </Text>
                          )}
                        </Table.Td>
                        <Table.Td>
                          <BadgesCycles cycles={e.cycles} />
                        </Table.Td>
                        <Table.Td>{e.zoneId === null ? '—' : (nomsZones.get(e.zoneId) ?? '—')}</Table.Td>
                        <Table.Td>
                          <Badge color={COULEURS[e.statut] ?? 'gray'}>{LIBELLES_STATUT[e.statut] ?? e.statut}</Badge>
                        </Table.Td>
                        <Table.Td>
                          <Menu position="bottom-end" withinPortal>
                            <Menu.Target>
                              <ActionIcon variant="subtle" color="gray" aria-label={`Actions pour ${e.nom}`}>
                                ⋯
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
                                <Menu.Item
                                  color="orange"
                                  onClick={() => setChangement({ etablissement: e, statut: 'SUSPENDU' })}
                                >
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
          </Stack>
        </Card>
      )}

      {creation && <EtablissementModal zones={zones.data ?? []} cycles={cycles.data ?? []} onFermer={fermerCreation} />}
      {aModifier && (
        <EtablissementModal
          etablissement={aModifier}
          zones={zones.data ?? []}
          cycles={cycles.data ?? []}
          onFermer={() => setAModifier(null)}
        />
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
          message={`« ${changement.etablissement.nom} » passera au statut ${LIBELLES_STATUT[changement.statut] ?? changement.statut}.`}
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
