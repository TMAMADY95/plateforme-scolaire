import { Alert, Badge, Loader, Stack, Table, Tabs, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import {
  conformiteRegionale,
  evolutionRegionale,
  plansActionRegionaux,
  postesRegionaux,
  rapportRegionalPdf,
  rapportExamenRegional,
  listerEtablissements,
  reussiteRegionale,
  statistiquesRegionales,
  statistiquesRegionalesParEtablissement,
  syntheseRegionale,
} from '../api/endpoints'
import { BadgeSecteur, BadgesCycles } from '../components/BadgesEtablissement'
import Icone from '../components/Icone'
import PanneauPlansAction from '../components/PanneauPlansAction'
import PanneauPostes from '../components/PanneauPostes'
import PanneauExamens from '../components/PanneauExamens'
import TableauDeBordConformite from '../components/TableauDeBordConformite'
import SyntheseNationale from './ministere/SyntheseNationale'
import { SECTIONS_REGION } from './sections'

const COULEURS: Record<string, string> = { ACTIF: 'green', SUSPENDU: 'orange', FERME: 'gray' }

const ONGLETS = [
  { valeur: 'synthese', libelle: 'Chiffres clés', icone: 'resultats' },
  { valeur: 'inspections', libelle: 'Inspections', icone: 'comptes' },
  { valeur: 'examens', libelle: 'Examens', icone: 'notes' },
  { valeur: 'plans', libelle: "Plans d'action", icone: 'presences' },
] as const

/**
 * Direction régionale : consultation seule des écoles de sa zone. Établissements et enseignants sont dans le menu de
 * gauche ; le suivi reste en onglets horizontaux.
 */
export default function PageRegion() {
  const etablissements = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })
  const [parametres, setParametres] = useSearchParams()
  const demande = parametres.get('onglet')
  const section = SECTIONS_REGION.find((s) => s.valeur === demande)
  const onglet = ONGLETS.find((o) => o.valeur === demande)?.valeur ?? 'synthese'

  return (
    <Stack>
      <Title order={2}>{section ? section.libelle : 'Direction régionale'}</Title>
      {section?.valeur === 'etablissements' && (
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
      )}
      {section?.valeur === 'postes' && <PanneauPostes cle="regionale" charger={postesRegionaux} />}
      {!section && (
        <Tabs
          value={onglet}
          onChange={(v) => v && setParametres({ onglet: v }, { replace: true })}
          variant="pills"
          keepMounted={false}
        >
          <Tabs.List mb="xs">
            {ONGLETS.map((o) => (
              <Tabs.Tab key={o.valeur} value={o.valeur} leftSection={<Icone nom={o.icone} taille={16} />}>
                {o.libelle}
              </Tabs.Tab>
            ))}
          </Tabs.List>
          <Tabs.Panel value="synthese">
            <SyntheseNationale
              cle="synthese-regionale"
              charger={syntheseRegionale}
              chargerStatistiques={statistiquesRegionales}
              chargerParEtablissement={statistiquesRegionalesParEtablissement}
              chargerReussite={reussiteRegionale}
              chargerEvolution={evolutionRegionale}
              telechargerRapport={rapportRegionalPdf}
            />
          </Tabs.Panel>
          <Tabs.Panel value="inspections">
            <TableauDeBordConformite cle="regionale" charger={conformiteRegionale} />
          </Tabs.Panel>
          <Tabs.Panel value="examens">
            <PanneauExamens cle="regionale" charger={rapportExamenRegional} />
          </Tabs.Panel>
          <Tabs.Panel value="plans">
            <PanneauPlansAction cle="regionale" charger={plansActionRegionaux} />
          </Tabs.Panel>
        </Tabs>
      )}
    </Stack>
  )
}
