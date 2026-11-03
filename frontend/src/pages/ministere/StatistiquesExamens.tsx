import { Alert, Group, Loader, Select, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { statistiquesExamens } from '../../api/endpoints'
import type { StatsExamenResultats } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)

function TableauResultats({ lignes, premiere }: { lignes: StatsExamenResultats[]; premiere: string }) {
  return (
    <Table.ScrollContainer minWidth={640}>
      <Table striped withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{premiere}</Table.Th>
            <Table.Th ta="right">Établissements</Table.Th>
            <Table.Th ta="right">Inscrits</Table.Th>
            <Table.Th ta="right">Présents</Table.Th>
            <Table.Th ta="right">Admis</Table.Th>
            <Table.Th ta="right">Absents</Table.Th>
            <Table.Th ta="right">Éliminés</Table.Th>
            <Table.Th ta="right">Taux de réussite</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {lignes.map((l) => (
            <Table.Tr key={l.libelle}>
              <Table.Td>{l.libelle}</Table.Td>
              <Table.Td ta="right">{nombre(l.etablissements)}</Table.Td>
              <Table.Td ta="right">{nombre(l.inscrits)}</Table.Td>
              <Table.Td ta="right">{nombre(l.presentes)}</Table.Td>
              <Table.Td ta="right">{nombre(l.admis)}</Table.Td>
              <Table.Td ta="right">{nombre(l.absents)}</Table.Td>
              <Table.Td ta="right">{nombre(l.elimines)}</Table.Td>
              <Table.Td ta="right">{pourcent(l.taux)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  )
}

/** Statistiques des examens : salles et centres d'examen, résultats par niveau d'examen et par type d'établissement. */
export default function StatistiquesExamens() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const requete = useQuery({
    queryKey: ['statistiques-examens', anneeId],
    queryFn: () => statistiquesExamens(anneeId as number),
    enabled: anneeId !== null,
  })
  const d = requete.data

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={3}>Statistiques des examens</Title>
          <Text c="dimmed" size="sm">Salles et centres d'examen, niveaux d'examen et types d'établissement.</Text>
        </div>
        <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
      </Group>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">Statistiques indisponibles.</Alert>}
      {d && (
        <>
          <Title order={4}>Salles d'examen</Title>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Centres d'examen" valeur={nombre(d.salles.centres)} icone="notes" />
            <CarteChiffre libelle="Salles utilisées" valeur={nombre(d.salles.sallesUtilisees)} icone="classes" detail={`${nombre(d.salles.sallesDisponibles)} salles disponibles`} />
            <CarteChiffre libelle="Candidats affectés" valeur={nombre(d.salles.candidatsAffectes)} icone="eleves" couleur="green" detail={`${nombre(d.salles.candidatsSansCentre)} sans centre`} />
            <CarteChiffre libelle="Remplissage des salles" valeur={pourcent(d.salles.remplissage)} icone="resultats" detail="30 candidats par salle" />
          </SimpleGrid>
          <Table.ScrollContainer minWidth={720}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Centre</Table.Th>
                  <Table.Th>Zone</Table.Th>
                  <Table.Th>Examen</Table.Th>
                  <Table.Th ta="right">Candidats</Table.Th>
                  <Table.Th ta="right">Salles utilisées</Table.Th>
                  <Table.Th ta="right">Salles disponibles</Table.Th>
                  <Table.Th ta="right">Capacité</Table.Th>
                  <Table.Th ta="right">Remplissage</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.centres.length === 0 && (
                  <Table.Tr><Table.Td colSpan={8}><Text c="dimmed">Aucun centre d'examen.</Text></Table.Td></Table.Tr>
                )}
                {d.centres.map((c) => (
                  <Table.Tr key={c.centreId}>
                    <Table.Td>{c.centre}</Table.Td>
                    <Table.Td>{c.zone ?? '—'}</Table.Td>
                    <Table.Td>{c.examen}</Table.Td>
                    <Table.Td ta="right">{nombre(c.candidats)}</Table.Td>
                    <Table.Td ta="right">{nombre(c.sallesUtilisees)}</Table.Td>
                    <Table.Td ta="right">{nombre(c.sallesDisponibles)}</Table.Td>
                    <Table.Td ta="right">{nombre(c.capaciteDisponible)}</Table.Td>
                    <Table.Td ta="right">{pourcent(c.remplissage)}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>

          <Title order={4}>Par niveau d'examen</Title>
          {d.niveaux.length === 0 && <Text c="dimmed">Aucun examen cette année.</Text>}
          {d.niveaux.map((n) => (
            <Stack key={n.niveau} gap="xs">
              <Text fw={600}>
                {n.niveau} <Text span c="dimmed" fw={400}>({n.examens} examen{n.examens > 1 ? 's' : ''}) : {pourcent(n.total.taux)} de réussite</Text>
              </Text>
              <TableauResultats lignes={[{ ...n.total, libelle: 'Tous types' }, ...n.parType]} premiere="Type d'établissement" />
              {n.parSerie.length > 0 && (
                <Table.ScrollContainer minWidth={480}>
                  <Table striped withTableBorder>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th>Série / type d'établissement</Table.Th>
                        <Table.Th ta="right">Inscrits</Table.Th>
                        <Table.Th ta="right">Présents</Table.Th>
                        <Table.Th ta="right">Admis</Table.Th>
                        <Table.Th ta="right">Taux de réussite</Table.Th>
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {n.parSerie.flatMap((s) => [
                        <Table.Tr key={s.libelle}>
                          <Table.Td fw={600}>{s.libelle}</Table.Td>
                          <Table.Td ta="right">{nombre(s.inscrits)}</Table.Td>
                          <Table.Td ta="right">{nombre(s.presentes)}</Table.Td>
                          <Table.Td ta="right">{nombre(s.admis)}</Table.Td>
                          <Table.Td ta="right">{pourcent(s.taux)}</Table.Td>
                        </Table.Tr>,
                        ...s.parType.map((t) => (
                          <Table.Tr key={`${s.libelle}|${t.libelle}`}>
                            <Table.Td pl="xl">{t.libelle}</Table.Td>
                            <Table.Td ta="right">{nombre(t.inscrits)}</Table.Td>
                            <Table.Td ta="right">{nombre(t.presentes)}</Table.Td>
                            <Table.Td ta="right">{nombre(t.admis)}</Table.Td>
                            <Table.Td ta="right">{pourcent(t.taux)}</Table.Td>
                          </Table.Tr>
                        )),
                      ])}
                    </Table.Tbody>
                  </Table>
                </Table.ScrollContainer>
              )}
            </Stack>
          ))}

          <Title order={4}>Lycée : élèves par niveau et série</Title>
          {d.lycee.length === 0 ? (
            <Text c="dimmed">Aucune classe de lycée avec série renseignée.</Text>
          ) : (
            <Table.ScrollContainer minWidth={420}>
              <Table striped withTableBorder>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Niveau</Table.Th>
                    <Table.Th>Série</Table.Th>
                    <Table.Th ta="right">Classes</Table.Th>
                    <Table.Th ta="right">Élèves</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.lycee.map((l) => (
                    <Table.Tr key={`${l.niveau}|${l.serie}`}>
                      <Table.Td>{l.niveau}</Table.Td>
                      <Table.Td>{l.serie}</Table.Td>
                      <Table.Td ta="right">{nombre(l.classes)}</Table.Td>
                      <Table.Td ta="right">{nombre(l.eleves)}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}

          <Title order={4}>Par type d'établissement (tous niveaux d'examen)</Title>
          <TableauResultats lignes={d.types} premiere="Type d'établissement" />
        </>
      )}
    </Stack>
  )
}
