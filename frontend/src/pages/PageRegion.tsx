import { Alert, Badge, Loader, Stack, Table, Tabs, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import {
  listerEtablissements,
  reussiteRegionale,
  statistiquesRegionales,
  statistiquesRegionalesParEtablissement,
  syntheseRegionale,
} from '../api/endpoints'
import { BadgeSecteur, BadgesCycles } from '../components/BadgesEtablissement'
import SyntheseNationale from './ministere/SyntheseNationale'

const COULEURS: Record<string, string> = { ACTIF: 'green', SUSPENDU: 'orange', FERME: 'gray' }

/** Direction régionale : consultation seule des écoles de sa zone. */
export default function PageRegion() {
  const etablissements = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })

  return (
    <Stack>
      <Title order={2}>Direction régionale</Title>
      <Tabs defaultValue="synthese">
        <Tabs.List>
          <Tabs.Tab value="synthese">Chiffres clés</Tabs.Tab>
          <Tabs.Tab value="etablissements">Établissements de la zone</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="synthese">
          <SyntheseNationale
            cle="synthese-regionale"
            charger={syntheseRegionale}
            chargerStatistiques={statistiquesRegionales}
            chargerParEtablissement={statistiquesRegionalesParEtablissement}
            chargerReussite={reussiteRegionale}
          />
        </Tabs.Panel>
        <Tabs.Panel value="etablissements">
          <Stack mt="md">
            {etablissements.isPending && <Loader />}
            {etablissements.isError && (
              <Alert color="red" variant="light">
                {etablissements.error.message}
              </Alert>
            )}
            {etablissements.data && etablissements.data.length === 0 && (
              <Text c="dimmed">Aucun établissement dans votre zone.</Text>
            )}
            {etablissements.data && etablissements.data.length > 0 && (
              <Table.ScrollContainer minWidth={520}>
                <Table striped withTableBorder>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Code</Table.Th>
                      <Table.Th>Nom</Table.Th>
                      <Table.Th>Secteur</Table.Th>
                      <Table.Th>Cycles</Table.Th>
                      <Table.Th>Statut</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {etablissements.data.map((e) => (
                      <Table.Tr key={e.id}>
                        <Table.Td>{e.codeOfficiel}</Table.Td>
                        <Table.Td>{e.nom}</Table.Td>
                        <Table.Td>
                          <BadgeSecteur secteur={e.secteur} />
                        </Table.Td>
                        <Table.Td>
                          <BadgesCycles cycles={e.cycles} />
                        </Table.Td>
                        <Table.Td>
                          <Badge color={COULEURS[e.statut] ?? 'gray'}>{e.statut}</Badge>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            )}
          </Stack>
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
