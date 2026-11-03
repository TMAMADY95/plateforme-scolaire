import { Alert, Badge, Button, Group, Loader, Modal, Select, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { participationCentres, participationExamens } from '../../api/endpoints'
import type { ParticipationEpreuve } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const jour = (iso: string) => new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
const heure = (h: string) => h.slice(0, 5)
const ponctuel = (v: boolean | null) => (v === null ? <Text span c="dimmed">—</Text> : <Badge color={v ? 'green' : 'red'} variant="light">{v ? 'À l\'heure' : 'En retard'}</Badge>)

/** Participation épreuve par épreuve puis centre par centre, d'après les rapports de surveillance des centres. */
export default function ParticipationEpreuves() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const [epreuve, setEpreuve] = useState<ParticipationEpreuve | null>(null)
  const requete = useQuery({ queryKey: ['participation', anneeId], queryFn: () => participationExamens(anneeId as number), enabled: anneeId !== null })
  const centres = useQuery({
    queryKey: ['participation-centres', epreuve?.epreuveId],
    queryFn: () => participationCentres((epreuve as ParticipationEpreuve).epreuveId),
    enabled: epreuve !== null,
  })
  const r = requete.data?.resume

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={3}>Participation aux épreuves</Title>
          <Text c="dimmed" size="sm">Présents (ayant composé), absents, copies ramassées (en rouge si elles diffèrent des présents sur des rapports clos) et ponctualité, d'après les rapports de surveillance (tolérance de 15 minutes).</Text>
        </div>
        <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
      </Group>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">Participation indisponible.</Alert>}
      {r && requete.data && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Taux de présence" valeur={pourcent(r.tauxPresence)} icone="presences" couleur="green" detail={`${nombre(r.presents)} présents sur ${nombre(r.attendus)} attendus`} />
            <CarteChiffre libelle="Taux d'absence" valeur={pourcent(r.tauxAbsence)} icone="presences" couleur="orange" detail={`${nombre(r.absents)} absents, ${nombre(r.elimines)} éliminés`} />
            <CarteChiffre libelle="Rapports de surveillance reçus" valeur={`${nombre(r.rapportsRecus)} / ${nombre(r.rapportsAttendus)}`} icone="notes" detail={`${nombre(r.rapportsClos)} clos`} />
            <CarteChiffre libelle="Épreuves démarrées à l'heure" valeur={pourcent(r.tauxDebutsALHeure)} icone="classes" detail={`terminées à l'heure : ${pourcent(r.tauxFinsALHeure)}`} />
          </SimpleGrid>
          {requete.data.epreuves.length === 0 ? <Text c="dimmed">Aucune épreuve programmée pour cette année.</Text> : (
            <Table.ScrollContainer minWidth={900}>
              <Table striped withTableBorder highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Date</Table.Th><Table.Th>Examen</Table.Th><Table.Th>Épreuve</Table.Th><Table.Th ta="right">Rapports</Table.Th>
                    <Table.Th ta="right">Attendus</Table.Th><Table.Th ta="right">Présents</Table.Th><Table.Th ta="right">Absents</Table.Th>
                    <Table.Th ta="right">Éliminés</Table.Th><Table.Th ta="right">Copies</Table.Th><Table.Th ta="right">Présence</Table.Th><Table.Th ta="right">Début à l'heure</Table.Th><Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {requete.data.epreuves.map((e) => (
                    <Table.Tr key={e.epreuveId}>
                      <Table.Td>{jour(e.date)}<Text size="xs" c="dimmed">{heure(e.heureDebut)} – {heure(e.heureFin)}</Text></Table.Td>
                      <Table.Td>{e.examen}</Table.Td><Table.Td>{e.discipline}</Table.Td>
                      <Table.Td ta="right">{e.rapportsRecus} / {e.centres}</Table.Td>
                      <Table.Td ta="right">{nombre(e.attendus)}</Table.Td><Table.Td ta="right">{nombre(e.presents)}</Table.Td><Table.Td ta="right">{nombre(e.absents)}</Table.Td>
                      <Table.Td ta="right">{nombre(e.elimines)}</Table.Td><Table.Td ta="right"><Text span size="sm" c={e.rapportsClos > 0 && e.copiesRamassees !== e.presents ? 'red' : undefined} title={e.rapportsClos > 0 && e.copiesRamassees !== e.presents ? `Écart de ${nombre(e.copiesRamassees - e.presents)} avec les présents (ayant composé)` : undefined}>{nombre(e.copiesRamassees)}</Text></Table.Td>
                      <Table.Td ta="right">{pourcent(e.tauxPresence)}</Table.Td>
                      <Table.Td ta="right">{e.debutsRenseignes === 0 ? '—' : `${e.debutsALHeure} / ${e.debutsRenseignes}`}</Table.Td>
                      <Table.Td><Button size="xs" variant="light" onClick={() => setEpreuve(e)}>Centres</Button></Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </>
      )}
      <Modal opened={epreuve !== null} onClose={() => setEpreuve(null)} size="xl" title={epreuve ? `${epreuve.examen} : ${epreuve.discipline}, ${jour(epreuve.date)}` : ''}>
        {centres.isLoading && <Loader />}
        {centres.isError && <Alert color="red">Centres indisponibles.</Alert>}
        {centres.data && (
          <Table.ScrollContainer minWidth={760}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Centre</Table.Th><Table.Th>Zone</Table.Th><Table.Th ta="right">Attendus</Table.Th><Table.Th ta="right">Présents</Table.Th>
                  <Table.Th ta="right">Absents</Table.Th><Table.Th ta="right">Absence</Table.Th><Table.Th>Début</Table.Th><Table.Th>Fin</Table.Th><Table.Th>Rapport</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {centres.data.map((c) => (
                  <Table.Tr key={c.centreId}>
                    <Table.Td>{c.centre}</Table.Td><Table.Td>{c.zone ?? '—'}</Table.Td><Table.Td ta="right">{nombre(c.attendus)}</Table.Td>
                    <Table.Td ta="right">{nombre(c.presents)}</Table.Td><Table.Td ta="right">{nombre(c.absents)}</Table.Td>
                    <Table.Td ta="right">{pourcent(c.tauxAbsence)}</Table.Td>
                    <Table.Td>{c.debutReel ? `${c.debutReel} ` : ''}{ponctuel(c.debutALHeure)}</Table.Td><Table.Td>{ponctuel(c.finALHeure)}</Table.Td>
                    <Table.Td><Badge color={c.rapportClos ? 'green' : c.rapportRecu ? 'blue' : 'gray'}>{c.rapportClos ? 'Clos' : c.rapportRecu ? 'En cours' : 'Manquant'}</Badge></Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
      </Modal>
    </Stack>
  )
}
