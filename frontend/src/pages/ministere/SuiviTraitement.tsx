import { Alert, Group, Loader, Select, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { suiviTraitement } from '../../api/endpoints'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const note = (n: number | null) => (n === null ? '—' : String(n).replace('.', ','))
const TYPES: Record<string, string> = {
  FRAUDE: 'Fraudes',
  RETARD: 'Retards',
  MALAISE: 'Malaises',
  PERTURBATION: 'Perturbations',
  FUITE_SUJET: 'Fuites de sujet',
  AUTRE: 'Autres',
}

/** Correction des copies, fraudes, doublons, notes manquantes, moyennes par discipline et distribution des moyennes. */
export default function SuiviTraitement() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const requete = useQuery({ queryKey: ['suivi-traitement', anneeId], queryFn: () => suiviTraitement(anneeId as number), enabled: anneeId !== null })
  const d = requete.data

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={3}>Copies, notes et fiabilité</Title>
          <Text c="dimmed" size="sm">Correction, fraudes, doublons, notes manquantes et moyennes par discipline.</Text>
        </div>
        <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
      </Group>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">Suivi indisponible.</Alert>}
      {d && (
        <>
          <Title order={4}>Correction</Title>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Copies corrigées" valeur={pourcent(d.correction.tauxCorrection)} icone="notes" couleur="green"
              detail={`${nombre(d.correction.corrigees)} sur ${nombre(d.correction.copies)}`} />
            <CarteChiffre libelle="Commissions de correction" valeur={nombre(d.correction.commissions)} icone="classes" detail={`${nombre(d.correction.cloturees)} clôturées`} />
            <CarteChiffre libelle="Correcteurs mobilisés" valeur={nombre(d.correction.correcteurs)} icone="eleves" />
            <CarteChiffre libelle="Présidents et secrétaires" valeur={`${nombre(d.correction.presidents)} / ${nombre(d.correction.secretaires)}`} icone="eleves" />
          </SimpleGrid>
          {d.correction.correcteursParDiscipline.length > 0 && (
            <Table withTableBorder w={420}>
              <Table.Thead><Table.Tr><Table.Th>Discipline</Table.Th><Table.Th ta="right">Correcteurs</Table.Th></Table.Tr></Table.Thead>
              <Table.Tbody>
                {d.correction.correcteursParDiscipline.map((c) => (
                  <Table.Tr key={c.libelle}><Table.Td>{c.libelle}</Table.Td><Table.Td ta="right">{nombre(c.nombre)}</Table.Td></Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}

          <Title order={4}>Fraudes et incidents</Title>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Incidents signalés" valeur={nombre(d.fraudes.incidents)} icone="alerte" couleur="orange" detail={`${nombre(d.fraudes.ouverts)} ouverts, ${nombre(d.fraudes.traites)} traités`} />
            <CarteChiffre libelle="Candidats éliminés pour fraude" valeur={nombre(d.fraudes.eliminesPourFraude)} icone="alerte" couleur="red" />
            {d.fraudes.parType.map((t) => (
              <CarteChiffre key={t.libelle} libelle={TYPES[t.libelle] ?? t.libelle} valeur={nombre(t.nombre)} icone="alerte" />
            ))}
          </SimpleGrid>

          <Title order={4}>Fiabilité des données</Title>
          <SimpleGrid cols={{ base: 1, sm: 3 }}>
            <CarteChiffre libelle="Identités en double" valeur={nombre(d.fiabilite.groupesIdentiteEnDouble)} icone="eleves" couleur="orange" detail="même nom, prénom et date de naissance, élèves distincts" />
            <CarteChiffre libelle="Inscriptions multiples" valeur={nombre(d.fiabilite.inscriptionsMultiples)} icone="eleves" couleur="orange" detail="même élève inscrit plusieurs fois à un examen" />
            <CarteChiffre libelle="Notes manquantes" valeur={nombre(d.fiabilite.notesManquantes)} icone="notes" couleur="red" detail="candidats présents sans note à une épreuve" />
          </SimpleGrid>

          <Title order={4}>Notes par discipline</Title>
          {d.disciplines.length === 0 ? <Text c="dimmed">Aucune note saisie.</Text> : (
            <Table.ScrollContainer minWidth={560}>
              <Table striped withTableBorder>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Discipline</Table.Th><Table.Th ta="right">Notes</Table.Th><Table.Th ta="right">Moyenne</Table.Th>
                    <Table.Th ta="right">Minimum</Table.Th><Table.Th ta="right">Maximum</Table.Th><Table.Th ta="right">Manquantes</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.disciplines.map((x) => (
                    <Table.Tr key={x.libelle}>
                      <Table.Td>{x.libelle}</Table.Td><Table.Td ta="right">{nombre(x.notes)}</Table.Td><Table.Td ta="right">{note(x.moyenne)}</Table.Td>
                      <Table.Td ta="right">{note(x.minimum)}</Table.Td><Table.Td ta="right">{note(x.maximum)}</Table.Td>
                      <Table.Td ta="right"><Text span c={x.manquantes > 0 ? 'red' : undefined}>{nombre(x.manquantes)}</Text></Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}

          <Title order={4}>Distribution des moyennes des présents</Title>
          <Table withTableBorder w={460}>
            <Table.Tbody>
              {d.moyennes.map((m) => (
                <Table.Tr key={m.libelle}><Table.Td>{m.libelle}</Table.Td><Table.Td ta="right">{nombre(m.nombre)}</Table.Td><Table.Td ta="right">{pourcent(m.pourcentage)}</Table.Td></Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </>
      )}
    </Stack>
  )
}
