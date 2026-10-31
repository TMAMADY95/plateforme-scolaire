import { useState } from 'react'
import { Button, Card, Group, Select, Table, Text } from '@mantine/core'
import type { LigneEtablissementStat, LigneZoneStat } from '../api/types'
import { BadgeSecteur, BadgesCycles } from './BadgesEtablissement'
import EnteteSection from './EnteteSection'
import { exporterCsv } from '../utils/exportCsv'

const nombre = (n: number) => new Intl.NumberFormat('fr-FR').format(n)
const ratio = (eleves: number, enseignants: number) =>
  enseignants === 0 ? '—' : new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(eleves / enseignants)

type Tri = 'nom' | 'eleves' | 'enseignants' | 'personnelDirection' | 'classes'

const TRIS = [
  { value: 'nom', label: 'Nom' },
  { value: 'eleves', label: 'Nombre d\'élèves' },
  { value: 'enseignants', label: 'Nombre d\'enseignants' },
  { value: 'personnelDirection', label: 'Personnel de direction' },
  { value: 'classes', label: 'Nombre de classes' },
]

/** Effectifs de chaque établissement : élèves, enseignants, direction, classes, élèves par enseignant. */
export function TableauEtablissements({
  lignes,
  onVoir,
}: {
  lignes: LigneEtablissementStat[]
  onVoir: (ligne: LigneEtablissementStat) => void
}) {
  const [tri, setTri] = useState<Tri>('nom')
  const tries = [...lignes].sort((a, b) =>
    tri === 'nom' ? a.nom.localeCompare(b.nom, 'fr') : (b[tri] as number) - (a[tri] as number),
  )
  const total = (cle: 'eleves' | 'enseignants' | 'personnelDirection' | 'classes') =>
    lignes.reduce((somme, l) => somme + l[cle], 0)

  return (
    <Card withBorder padding="lg">
      <EnteteSection
        titre="Par établissement"
        description="Les effectifs de chaque établissement ; cliquez sur « Détail » pour ses graphiques."
        action={
          <Group gap="xs">
          <Button
            size="xs"
            variant="light"
            disabled={lignes.length === 0}
            onClick={() =>
              exporterCsv(
                'effectifs-par-etablissement',
                [
                  { titre: 'Code', valeur: (l: LigneEtablissementStat) => l.codeOfficiel },
                  { titre: 'Établissement', valeur: (l) => l.nom },
                  { titre: 'Zone', valeur: (l) => l.zone },
                  { titre: 'Secteur', valeur: (l) => (l.secteur === 'PUBLIC' ? 'Public' : 'Privé') },
                  { titre: 'Cycles', valeur: (l) => l.cycles.map((c) => c.libelle).join(', ') },
                  { titre: 'Élèves', valeur: (l) => l.eleves },
                  { titre: 'Enseignants', valeur: (l) => l.enseignants },
                  { titre: 'Personnel de direction', valeur: (l) => l.personnelDirection },
                  { titre: 'Classes', valeur: (l) => l.classes },
                ],
                tries,
              )
            }
          >
            Exporter (Excel)
          </Button>
          <Select
            aria-label="Trier par"
            data={TRIS}
            value={tri}
            onChange={(v) => setTri((v ?? 'nom') as Tri)}
            allowDeselect={false}
            leftSection={<Text size="xs">Tri</Text>}
            leftSectionWidth={40}
            w={230}
          />
          </Group>
        }
      />
      {lignes.length === 0 ? (
        <Text c="dimmed" mt="md">
          Aucun établissement dans ce périmètre.
        </Text>
      ) : (
        <Table.ScrollContainer minWidth={900} mt="md">
          <Table highlightOnHover verticalSpacing="xs">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Établissement</Table.Th>
                <Table.Th>Secteur</Table.Th>
                <Table.Th>Cycles</Table.Th>
                <Table.Th ta="right">Élèves</Table.Th>
                <Table.Th ta="right">Enseignants</Table.Th>
                <Table.Th ta="right">Direction</Table.Th>
                <Table.Th ta="right">Classes</Table.Th>
                <Table.Th ta="right">Élèves / ens.</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {tries.map((l) => (
                <Table.Tr key={l.id}>
                  <Table.Td>
                    <Text fw={600}>{l.nom}</Text>
                    <Text size="xs" c="dimmed">
                      {l.codeOfficiel}
                      {l.zone ? ` · ${l.zone}` : ''}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <BadgeSecteur secteur={l.secteur} />
                  </Table.Td>
                  <Table.Td>
                    <BadgesCycles cycles={l.cycles} />
                  </Table.Td>
                  <Table.Td ta="right">{nombre(l.eleves)}</Table.Td>
                  <Table.Td ta="right">{nombre(l.enseignants)}</Table.Td>
                  <Table.Td ta="right">{nombre(l.personnelDirection)}</Table.Td>
                  <Table.Td ta="right">{nombre(l.classes)}</Table.Td>
                  <Table.Td ta="right">{ratio(l.eleves, l.enseignants)}</Table.Td>
                  <Table.Td>
                    <Button size="compact-xs" variant="light" onClick={() => onVoir(l)}>
                      Détail
                    </Button>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
            <Table.Tfoot>
              <Table.Tr>
                <Table.Th colSpan={3}>Total ({lignes.length} établissement{lignes.length > 1 ? 's' : ''})</Table.Th>
                <Table.Th ta="right">{nombre(total('eleves'))}</Table.Th>
                <Table.Th ta="right">{nombre(total('enseignants'))}</Table.Th>
                <Table.Th ta="right">{nombre(total('personnelDirection'))}</Table.Th>
                <Table.Th ta="right">{nombre(total('classes'))}</Table.Th>
                <Table.Th ta="right">{ratio(total('eleves'), total('enseignants'))}</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Tfoot>
          </Table>
        </Table.ScrollContainer>
      )}
    </Card>
  )
}

/** Effectifs de chaque zone (région, département…), sous-zones comprises. */
export function TableauZones({
  lignes,
  zoneChoisie,
  onChoisir,
}: {
  lignes: LigneZoneStat[]
  zoneChoisie: number | null
  onChoisir: (zoneId: number | null) => void
}) {
  return (
    <Card withBorder padding="lg">
      <EnteteSection
        titre="Par région et zone"
        description="Chaque zone compte ses sous-zones. Choisissez-en une pour filtrer toute la page."
        action={
          <Group gap="xs">
            {zoneChoisie !== null && (
              <Button variant="default" size="xs" onClick={() => onChoisir(null)}>
                Revenir au pays entier
              </Button>
            )}
            <Button
              size="xs"
              variant="light"
              disabled={lignes.length === 0}
              onClick={() =>
                exporterCsv(
                  'effectifs-par-zone',
                  [
                    { titre: 'Zone', valeur: (l: LigneZoneStat) => l.nom },
                    { titre: 'Type', valeur: (l) => l.typeZone },
                    { titre: 'Établissements', valeur: (l) => l.etablissements },
                    { titre: 'Élèves', valeur: (l) => l.eleves },
                    { titre: 'Enseignants', valeur: (l) => l.enseignants },
                    { titre: 'Personnel de direction', valeur: (l) => l.personnelDirection },
                    { titre: 'Classes', valeur: (l) => l.classes },
                  ],
                  lignes,
                )
              }
            >
              Exporter (Excel)
            </Button>
          </Group>
        }
      />
      {lignes.length === 0 ? (
        <Text c="dimmed" mt="md">
          Aucun établissement n'est rattaché à une zone.
        </Text>
      ) : (
        <Table.ScrollContainer minWidth={760} mt="md">
          <Table highlightOnHover verticalSpacing="xs">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Zone</Table.Th>
                <Table.Th ta="right">Établissements</Table.Th>
                <Table.Th ta="right">Élèves</Table.Th>
                <Table.Th ta="right">Enseignants</Table.Th>
                <Table.Th ta="right">Direction</Table.Th>
                <Table.Th ta="right">Classes</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {lignes.map((l) => (
                <Table.Tr key={l.zoneId} bg={l.zoneId === zoneChoisie ? 'var(--mantine-color-blue-light)' : undefined}>
                  <Table.Td>
                    <Text fw={600}>{l.nom}</Text>
                    <Text size="xs" c="dimmed">
                      {l.typeZone}
                    </Text>
                  </Table.Td>
                  <Table.Td ta="right">{nombre(l.etablissements)}</Table.Td>
                  <Table.Td ta="right">{nombre(l.eleves)}</Table.Td>
                  <Table.Td ta="right">{nombre(l.enseignants)}</Table.Td>
                  <Table.Td ta="right">{nombre(l.personnelDirection)}</Table.Td>
                  <Table.Td ta="right">{nombre(l.classes)}</Table.Td>
                  <Table.Td>
                    <Group justify="flex-end">
                      <Button
                        size="compact-xs"
                        variant={l.zoneId === zoneChoisie ? 'filled' : 'light'}
                        onClick={() => onChoisir(l.zoneId === zoneChoisie ? null : l.zoneId)}
                      >
                        {l.zoneId === zoneChoisie ? 'Choisie' : 'Voir'}
                      </Button>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
    </Card>
  )
}
