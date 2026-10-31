import { Card, SimpleGrid, Table, Text } from '@mantine/core'
import type { RepartitionExamen, StatistiquesExamen as Stats, SyntheseExamen } from '../api/types'
import CarteChiffre from './CarteChiffre'
import EnteteSection from './EnteteSection'

const nombre = (n: number) => new Intl.NumberFormat('fr-FR').format(n)
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)

const NOMS_SERIES: Record<string, string> = {
  SCIENCES_MATHEMATIQUES: 'Sciences mathématiques',
  SCIENCES_SOCIALES: 'Sciences sociales',
  SCIENCES_EXPERIMENTALES: 'Sciences expérimentales',
}

function TableauZones({ titre, lignes }: { titre: string; lignes: SyntheseExamen[] }) {
  if (lignes.length === 0) {
    return null
  }
  return (
    <Card withBorder padding="lg">
      <EnteteSection titre={titre} />
      <Table.ScrollContainer minWidth={420} mt="md">
        <Table verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Zone</Table.Th>
              <Table.Th ta="right">Présentés</Table.Th>
              <Table.Th ta="right">Admis</Table.Th>
              <Table.Th ta="right">Taux</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {lignes.map((z) => (
              <Table.Tr key={z.libelle}>
                <Table.Td>{z.libelle}</Table.Td>
                <Table.Td ta="right">{nombre(z.presentes)}</Table.Td>
                <Table.Td ta="right">{nombre(z.admis)}</Table.Td>
                <Table.Td ta="right">{pourcent(z.taux)}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Card>
  )
}

function TableauParts({ titre, lignes, noms }: { titre: string; lignes: RepartitionExamen[]; noms?: Record<string, string> }) {
  const utiles = lignes.filter((l) => l.presentes > 0)
  if (utiles.length === 0) {
    return null
  }
  return (
    <Card withBorder padding="lg">
      <EnteteSection titre={titre} />
      <Table mt="md" verticalSpacing="xs">
        <Table.Thead>
          <Table.Tr>
            <Table.Th />
            <Table.Th ta="right">Présentés</Table.Th>
            <Table.Th ta="right">Admis</Table.Th>
            <Table.Th ta="right">Taux</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {utiles.map((l) => (
            <Table.Tr key={l.libelle}>
              <Table.Td>{noms?.[l.libelle] ?? l.libelle}</Table.Td>
              <Table.Td ta="right">{nombre(l.presentes)}</Table.Td>
              <Table.Td ta="right">{nombre(l.admis)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.taux)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Card>
  )
}

/** Statistiques officielles d'un examen : absents, éliminés, sexe, région, préfecture et séries du baccalauréat. */
export default function StatistiquesExamen({ statistiques }: { statistiques: Stats | null }) {
  if (statistiques === null) {
    return null
  }
  const filles = statistiques.parSexe.find((x) => x.libelle === 'Filles')
  const garcons = statistiques.parSexe.find((x) => x.libelle === 'Garçons')
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2, md: 4 }}>
        <CarteChiffre libelle="Absents" valeur={nombre(statistiques.absents)} icone="presences" couleur="orange" />
        <CarteChiffre libelle="Éliminés (fraude)" valeur={nombre(statistiques.elimines)} icone="alerte" couleur="red" />
        <CarteChiffre libelle="Réussite des filles" valeur={pourcent(filles?.taux ?? null)} icone="resultats" couleur="grape" />
        <CarteChiffre libelle="Réussite des garçons" valeur={pourcent(garcons?.taux ?? null)} icone="resultats" couleur="blue" />
      </SimpleGrid>
      <Text size="xs" c="dimmed">
        Le taux de réussite se calcule sur les candidats présents. Les chiffres par sexe, par série, les absents et les éliminés ne
        comptent que les établissements qui les ont renseignés.
      </Text>
      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <TableauParts titre="Par sexe" lignes={statistiques.parSexe} />
        <TableauParts titre="Par série du baccalauréat" lignes={statistiques.parSerie} noms={NOMS_SERIES} />
        <TableauZones titre="Par région" lignes={statistiques.parRegion} />
        <TableauZones titre="Par préfecture" lignes={statistiques.parPrefecture} />
      </SimpleGrid>
    </>
  )
}
