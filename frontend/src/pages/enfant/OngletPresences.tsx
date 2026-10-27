import { useState } from 'react'
import { Alert, Badge, Group, Loader, Stack, Table, Text, TextInput } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { historiquePresences } from '../../api/endpoints'
import BadgeStatutPresence from '../../components/BadgeStatutPresence'
import { aujourdhui, formaterDate, ilYaJours } from '../../utils/dates'

export default function OngletPresences({ eleveId }: { eleveId: number }) {
  const [du, setDu] = useState(ilYaJours(30))
  const [au, setAu] = useState(aujourdhui())

  const periodeValide = du !== '' && au !== '' && du <= au
  const presences = useQuery({
    queryKey: ['presences-eleve', eleveId, du, au],
    queryFn: () => historiquePresences(eleveId, du, au),
    enabled: periodeValide,
  })

  const donnees = presences.data ?? []
  const absences = donnees.filter((p) => p.statut === 'ABSENT')
  const retards = donnees.filter((p) => p.statut === 'RETARD')
  const absencesJustifiees = absences.filter((p) => p.justifie).length
  // Seuls les jours où un appel a été fait sont listés : on n'affiche que les absences et retards
  const anomalies = donnees.filter((p) => p.statut !== 'PRESENT')

  return (
    <Stack>
      <Group>
        <TextInput label="Du" type="date" value={du} max={au || undefined} onChange={(e) => setDu(e.currentTarget.value)} />
        <TextInput label="Au" type="date" value={au} min={du || undefined} max={aujourdhui()} onChange={(e) => setAu(e.currentTarget.value)} />
      </Group>

      {!periodeValide && (
        <Alert color="orange" variant="light">
          La date de début doit précéder la date de fin.
        </Alert>
      )}
      {presences.isPending && periodeValide && <Loader />}
      {presences.isError && (
        <Alert color="red" variant="light">
          {presences.error.message}
        </Alert>
      )}

      {presences.data && (
        <>
          <Group gap="sm">
            <Badge size="lg" color="red" variant="light">
              {absences.length} absence(s) dont {absencesJustifiees} justifiée(s)
            </Badge>
            <Badge size="lg" color="orange" variant="light">
              {retards.length} retard(s)
            </Badge>
            <Badge size="lg" color="green" variant="light">
              {donnees.length - anomalies.length} jour(s) de présence
            </Badge>
          </Group>

          {anomalies.length === 0 ? (
            <Text c="dimmed">Aucune absence ni aucun retard sur cette période.</Text>
          ) : (
            <Table.ScrollContainer minWidth={480}>
              <Table striped withTableBorder maw={720}>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Date</Table.Th>
                    <Table.Th>Statut</Table.Th>
                    <Table.Th>Motif</Table.Th>
                    <Table.Th>Justifiée</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {anomalies.map((p) => (
                    <Table.Tr key={p.id}>
                      <Table.Td>{formaterDate(p.date)}</Table.Td>
                      <Table.Td>
                        <BadgeStatutPresence statut={p.statut} />
                      </Table.Td>
                      <Table.Td>{p.motif ?? '—'}</Table.Td>
                      <Table.Td>{p.justifie ? 'Oui' : 'Non'}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </>
      )}
    </Stack>
  )
}
