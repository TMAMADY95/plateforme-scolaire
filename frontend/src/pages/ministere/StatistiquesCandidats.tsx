import { Alert, Group, Loader, Select, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { statistiquesCandidats } from '../../api/endpoints'
import type { StatCandidatsLigne } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const evolution = (t: number | null) => (t === null ? '—' : `${t > 0 ? '+' : ''}${String(t).replace('.', ',')} %`)
const MENTIONS: Record<string, string> = { TRES_BIEN: 'Très bien', BIEN: 'Bien', ASSEZ_BIEN: 'Assez bien', PASSABLE: 'Passable' }

function Tableau({ lignes, premiere, comparaison }: { lignes: StatCandidatsLigne[]; premiere: string; comparaison?: boolean }) {
  return (
    <Table.ScrollContainer minWidth={900}>
      <Table striped withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{premiere}</Table.Th>
            <Table.Th ta="right">Inscrits</Table.Th>
            <Table.Th ta="right">% filles</Table.Th>
            <Table.Th ta="right">% libres</Table.Th>
            <Table.Th ta="right">Présents</Table.Th>
            <Table.Th ta="right">Absents</Table.Th>
            <Table.Th ta="right">Éliminés</Table.Th>
            <Table.Th ta="right">Admis</Table.Th>
            <Table.Th ta="right">Non admis</Table.Th>
            <Table.Th ta="right">Réussite (présents)</Table.Th>
            <Table.Th ta="right">Réussite (inscrits)</Table.Th>
            <Table.Th ta="right">Filles</Table.Th>
            <Table.Th ta="right">Garçons</Table.Th>
            {comparaison && <Table.Th ta="right">Inscrits N-1</Table.Th>}
            {comparaison && <Table.Th ta="right">Évolution</Table.Th>}
            {comparaison && <Table.Th ta="right">Réussite N-1</Table.Th>}
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {lignes.map((l) => (
            <Table.Tr key={l.libelle}>
              <Table.Td>{l.libelle}</Table.Td>
              <Table.Td ta="right">{nombre(l.inscrits)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.pctFilles)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.pctLibres)}</Table.Td>
              <Table.Td ta="right">{nombre(l.presents)}</Table.Td>
              <Table.Td ta="right">{nombre(l.absents)}</Table.Td>
              <Table.Td ta="right">{nombre(l.elimines)}</Table.Td>
              <Table.Td ta="right">{nombre(l.admis)}</Table.Td>
              <Table.Td ta="right">{nombre(l.presents - l.admis)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.tauxReussite)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.tauxReussiteInscrits)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.tauxReussiteFilles)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.tauxReussiteGarcons)}</Table.Td>
              {comparaison && <Table.Td ta="right">{nombre(l.inscritsPrecedent)}</Table.Td>}
              {comparaison && <Table.Td ta="right">{evolution(l.evolutionInscrits)}</Table.Td>}
              {comparaison && <Table.Td ta="right">{pourcent(l.tauxReussitePrecedent)}</Table.Td>}
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  )
}

/** Profil des candidats, participation, réussite, mentions et évolution annuelle (DGECS, consultation ministère). */
export default function StatistiquesCandidats() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const requete = useQuery({
    queryKey: ['statistiques-candidats', anneeId],
    queryFn: () => statistiquesCandidats(anneeId as number),
    enabled: anneeId !== null,
  })
  const d = requete.data
  const t = d?.total

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={3}>Candidats et résultats</Title>
          <Text c="dimmed" size="sm">Profil, participation, réussite et comparaison avec l'année précédente.</Text>
        </div>
        <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
      </Group>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">Statistiques indisponibles.</Alert>}
      {d && t && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Candidats inscrits" valeur={nombre(t.inscrits)} icone="eleves" detail={`${pourcent(t.pctFilles)} de filles`} />
            <CarteChiffre libelle="Candidats libres" valeur={nombre(t.libres)} icone="eleves" detail={`${pourcent(t.pctLibres)} des inscrits`} />
            <CarteChiffre libelle="Taux de présence" valeur={pourcent(t.tauxPresence)} icone="presences" detail={`${nombre(t.absents)} absents, ${nombre(t.elimines)} éliminés`} />
            <CarteChiffre libelle="Taux de réussite" valeur={pourcent(t.tauxReussite)} icone="resultats" couleur="green" detail={`${pourcent(t.tauxReussiteInscrits)} des inscrits`} />
          </SimpleGrid>
          <Title order={4}>Par niveau d'examen</Title>
          <Tableau lignes={[t, ...d.parNiveau]} premiere="Niveau d'examen" comparaison={d.anneePrecedenteId !== null} />
          <Title order={4}>Mentions des admis</Title>
          <Table withTableBorder w={360}>
            <Table.Tbody>
              {d.mentions.map((m) => (
                <Table.Tr key={m.mention}>
                  <Table.Td>{MENTIONS[m.mention] ?? m.mention}</Table.Td>
                  <Table.Td ta="right">{nombre(m.nombre)}</Table.Td>
                  <Table.Td ta="right">{pourcent(m.pourcentage)}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
          <Title order={4}>Par région</Title>
          <Tableau lignes={d.parRegion} premiere="Région" />
          <Title order={4}>Par préfecture</Title>
          <Tableau lignes={d.parPrefecture} premiere="Préfecture" />
          <Title order={4}>Par zone de l'établissement</Title>
          <Tableau lignes={d.parZone} premiere="Zone" />
        </>
      )}
    </Stack>
  )
}
