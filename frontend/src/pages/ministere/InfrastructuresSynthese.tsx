import { Alert, Badge, Group, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { synthesesInfrastructures } from '../../api/endpoints'

const ratio = (v: number | null) => (v === null ? '—' : String(v).replace('.', ','))

/** Carte scolaire sur le périmètre de l'utilisateur : établissements renseignés, ratios et constats. */
export default function InfrastructuresSynthese({ anneeId }: { anneeId: number }) {
  const synthese = useQuery({ queryKey: ['infrastructures-synthese', anneeId], queryFn: () => synthesesInfrastructures(anneeId) })
  const d = synthese.data
  return (
    <Stack>
      <div>
        <Title order={4}>Infrastructures et équipements des établissements</Title>
        <Text c="dimmed" size="sm">Saisies par les directions d'établissement. Seuils d'alerte : plus de 60 élèves par salle, plus de 50 élèves par latrine et par sexe (à confirmer).</Text>
      </div>
      {synthese.isLoading && <Loader />}
      {synthese.isError && <Alert color="red">Synthèse indisponible.</Alert>}
      {d && (
        <>
          <Group gap="xs">
            <Badge variant="light" size="lg">{d.resume.renseignes} / {d.resume.etablissements} renseignés</Badge>
            <Badge variant="light" size="lg" color={d.resume.avecAlertes ? 'orange' : 'green'}>{d.resume.avecAlertes} avec points d'attention</Badge>
            <Badge variant="light" size="lg">{d.resume.salles} salles dont {d.resume.sallesProvisoires} provisoires</Badge>
            <Badge variant="light" size="lg" color={d.resume.sallesARehabiliter ? 'orange' : 'gray'}>{d.resume.sallesARehabiliter} à réhabiliter</Badge>
            <Badge variant="light" size="lg">{ratio(d.resume.elevesParSalle)} élèves par salle</Badge>
            <Badge variant="light" size="lg" color={d.resume.sansEau ? 'red' : 'gray'}>{d.resume.sansEau} sans eau</Badge>
            <Badge variant="light" size="lg" color={d.resume.sansElectricite ? 'yellow' : 'gray'}>{d.resume.sansElectricite} sans électricité</Badge>
            <Badge variant="light" size="lg" color={d.resume.sansLatrinesFilles ? 'red' : 'gray'}>{d.resume.sansLatrinesFilles} sans latrine filles</Badge>
          </Group>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Établissement</Table.Th>
                <Table.Th>Zone</Table.Th>
                <Table.Th>Élèves</Table.Th>
                <Table.Th>Salles</Table.Th>
                <Table.Th>Élèves / salle</Table.Th>
                <Table.Th>Élèves / place</Table.Th>
                <Table.Th>Points d'attention</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {d.lignes.map((l) => (
                <Table.Tr key={l.etablissementId}>
                  <Table.Td>{l.etablissement}</Table.Td>
                  <Table.Td>{l.zone ?? '—'}</Table.Td>
                  <Table.Td>{l.eleves}</Table.Td>
                  {l.renseignee ? (
                    <>
                      <Table.Td>{l.salles}</Table.Td>
                      <Table.Td>{ratio(l.elevesParSalle)}</Table.Td>
                      <Table.Td>{ratio(l.elevesParPlace)}</Table.Td>
                      <Table.Td>{l.alertes.length === 0 ? <Badge color="green" variant="light">Aucun</Badge> : l.alertes.map((a) => <Text key={a} size="xs">• {a}</Text>)}</Table.Td>
                    </>
                  ) : (
                    <Table.Td colSpan={4}><Text size="sm" c="dimmed">Non renseigné</Text></Table.Td>
                  )}
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </>
      )}
    </Stack>
  )
}
