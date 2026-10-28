import { Card, Group, Stack, Table, Text } from '@mantine/core'
import { JOURS, formaterPlage } from '../utils/horaires'

/** Un créneau affichable, quelle que soit la source (direction ou élève). */
export interface LigneCreneau {
  key: string
  matiereLibelle: string
  enseignantNom: string | null
  salleNom: string | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
}

/** Affiche un emploi du temps en cartes, une carte par jour. */
export default function GrilleEmploiDuTemps({ creneaux }: { creneaux: LigneCreneau[] }) {
  const joursUtilises = JOURS.filter((j) => creneaux.some((c) => c.jourSemaine === j.value))

  if (creneaux.length === 0) {
    return (
      <Text c="dimmed">Aucun créneau enregistré pour le moment.</Text>
    )
  }

  return (
    <Stack>
      {joursUtilises.map((jour) => {
        const duJour = creneaux
          .filter((c) => c.jourSemaine === jour.value)
          .sort((a, b) => a.heureDebut.localeCompare(b.heureDebut))
        return (
          <Card key={jour.value} withBorder padding="md">
            <Group justify="space-between" mb="xs">
              <Text fw={600}>{jour.label}</Text>
              <Text size="sm" c="dimmed">
                {duJour.length} cours
              </Text>
            </Group>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Horaire</Table.Th>
                  <Table.Th>Matière</Table.Th>
                  <Table.Th>Enseignant</Table.Th>
                  <Table.Th>Salle</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {duJour.map((c) => (
                  <Table.Tr key={c.key}>
                    <Table.Td>{formaterPlage(c.heureDebut, c.heureFin)}</Table.Td>
                    <Table.Td>{c.matiereLibelle}</Table.Td>
                    <Table.Td>{c.enseignantNom ?? '—'}</Table.Td>
                    <Table.Td>{c.salleNom ?? '—'}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Card>
        )
      })}
    </Stack>
  )
}
