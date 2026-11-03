import { Alert, Badge, Card, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { conformiteProgramme } from '../api/endpoints'
import type { LigneConformiteProgramme } from '../api/types'

const STATUTS: Record<LigneConformiteProgramme['statut'], { libelle: string; couleur: string }> = {
  CONFORME: { libelle: 'Conforme', couleur: 'green' },
  MANQUE: { libelle: "Heures manquantes", couleur: 'orange' },
  EXCES: { libelle: 'Trop d\'heures', couleur: 'orange' },
  NON_PLANIFIEE: { libelle: 'Non planifiée', couleur: 'red' },
  HORS_PROGRAMME: { libelle: 'Hors programme', couleur: 'gray' },
  A_PRECISER: { libelle: 'Horaire non défini', couleur: 'gray' },
}

const h = (n: number | null) => (n === null ? '—' : `${String(Math.round(n * 100) / 100).replace('.', ',')} h`)

/** Heures de l'emploi du temps comparées au programme officiel du niveau : indicatif, ne bloque rien. */
export default function ConformiteProgrammeCarte({ classeId }: { classeId: number }) {
  const requete = useQuery({ queryKey: ['conformite-programme', classeId], queryFn: () => conformiteProgramme(classeId) })
  const d = requete.data
  return (
    <Card withBorder padding="md">
      <Stack gap="xs">
        <Title order={5}>Conformité au programme officiel</Title>
        {requete.isPending && <Loader size="sm" />}
        {requete.isError && <Alert color="red" variant="light">{requete.error.message}</Alert>}
        {d && !d.programmeDisponible && (
          <Text size="sm" c="dimmed">Aucun programme officiel n'est renseigné pour le niveau de cette classe cette année.</Text>
        )}
        {d && d.programmeDisponible && (
          <>
            <Text size="sm">Prévu : {h(d.heuresPrevues)} par semaine · planifié : {h(d.heuresPlanifiees)}</Text>
            <Table.ScrollContainer minWidth={420}>
              <Table withTableBorder verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Matière</Table.Th>
                    <Table.Th ta="right">Prévu</Table.Th>
                    <Table.Th ta="right">Planifié</Table.Th>
                    <Table.Th>État</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.lignes.map((l) => (
                    <Table.Tr key={l.matiereId}>
                      <Table.Td>{l.matiere}</Table.Td>
                      <Table.Td ta="right">{h(l.prevues)}</Table.Td>
                      <Table.Td ta="right">{h(l.planifiees)}</Table.Td>
                      <Table.Td><Badge variant="light" color={STATUTS[l.statut].couleur}>{STATUTS[l.statut].libelle}</Badge></Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </>
        )}
      </Stack>
    </Card>
  )
}
