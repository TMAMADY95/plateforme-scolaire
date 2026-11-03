import { ActionIcon, Alert, Badge, Button, Checkbox, Group, Loader, Modal, NumberInput, Select, SimpleGrid, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { creerConvoi, modifierConvoi, sujetsAImprimer, supprimerConvoi, transportExamen } from '../../api/endpoints'
import type { ConvoiExamen } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useDroitsExamens } from '../../auth/useDroitsExamens'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const STATUTS = [
  { value: 'PREVU', label: 'Prévu' },
  { value: 'EN_ROUTE', label: 'En route' },
  { value: 'LIVRE', label: 'Livré' },
]
const INCIDENTS = [
  { value: 'PERTE', label: 'Perte' },
  { value: 'RETARD', label: 'Retard' },
  { value: 'ERREUR_DISTRIBUTION', label: 'Erreur de distribution' },
  { value: 'RUPTURE_MATERIEL', label: 'Rupture de matériel' },
  { value: 'ACCIDENT', label: 'Accident' },
  { value: 'AUTRE', label: 'Autre' },
]
const COULEUR: Record<string, string> = { LIVRE: 'green', EN_ROUTE: 'blue', PREVU: 'gray' }

const affiche = (iso: string | null) =>
  iso === null ? '—' : new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', timeZone: 'Africa/Conakry' })
/** ISO → valeur d'un champ datetime-local (heure locale du navigateur). */
const versChamp = (iso: string | null) => {
  if (iso === null) return ''
  const d = new Date(iso)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}
const versIso = (v: string) => (v === '' ? null : new Date(v).toISOString())

/** Transport des sujets et du matériel vers les centres, et besoins d'impression de l'examen choisi. */
export default function TransportSujets({ examenId, centres }: { examenId: number; centres: { value: string; label: string }[] }) {
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const [edition, setEdition] = useState<ConvoiExamen | 'nouveau' | null>(null)
  const [centreId, setCentreId] = useState<string | null>(null)
  const [vehicule, setVehicule] = useState('')
  const [chauffeur, setChauffeur] = useState('')
  const [itineraire, setItineraire] = useState('')
  const [escorte, setEscorte] = useState(false)
  const [departPrevu, setDepartPrevu] = useState('')
  const [arriveePrevue, setArriveePrevue] = useState('')
  const [departReel, setDepartReel] = useState('')
  const [arriveeReelle, setArriveeReelle] = useState('')
  const [carburant, setCarburant] = useState<number | string>('')
  const [statut, setStatut] = useState<string | null>('PREVU')
  const [incident, setIncident] = useState<string | null>(null)
  const [incidentDescription, setIncidentDescription] = useState('')
  const [observation, setObservation] = useState('')

  const transport = useQuery({ queryKey: ['transport', examenId], queryFn: () => transportExamen(examenId) })
  const sujets = useQuery({ queryKey: ['sujets-a-imprimer', examenId], queryFn: () => sujetsAImprimer(examenId) })

  const ouvrir = (c: ConvoiExamen | 'nouveau') => {
    const x = c === 'nouveau' ? null : c
    setCentreId(x ? String(x.centreId) : null)
    setVehicule(x?.vehicule ?? '')
    setChauffeur(x?.chauffeur ?? '')
    setItineraire(x?.itineraire ?? '')
    setEscorte(x?.escorte ?? false)
    setDepartPrevu(versChamp(x?.departPrevu ?? null))
    setArriveePrevue(versChamp(x?.arriveePrevue ?? null))
    setDepartReel(versChamp(x?.departReel ?? null))
    setArriveeReelle(versChamp(x?.arriveeReelle ?? null))
    setCarburant(x?.carburantLitres ?? '')
    setStatut(x?.statut ?? 'PREVU')
    setIncident(x?.incidentType ?? null)
    setIncidentDescription(x?.incidentDescription ?? '')
    setObservation(x?.observation ?? '')
    setEdition(c)
  }
  const enregistrer = useMutation({
    mutationFn: () => {
      const requete = {
        centreId: Number(centreId),
        vehicule: vehicule.trim(),
        chauffeur: chauffeur.trim() || null,
        itineraire: itineraire.trim() || null,
        escorte,
        departPrevu: versIso(departPrevu),
        arriveePrevue: versIso(arriveePrevue),
        departReel: versIso(departReel),
        arriveeReelle: versIso(arriveeReelle),
        carburantLitres: carburant === '' ? null : Number(carburant),
        statut: statut ?? 'PREVU',
        incidentType: incident,
        incidentDescription: incidentDescription.trim() || null,
        observation: observation.trim() || null,
      }
      return edition !== null && edition !== 'nouveau' ? modifierConvoi(examenId, edition.id, requete) : creerConvoi(examenId, requete)
    },
    onSuccess: () => {
      setEdition(null)
      void queryClient.invalidateQueries({ queryKey: ['transport'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const supprimer = useMutation({
    mutationFn: (id: number) => supprimerConvoi(examenId, id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['transport'] }),
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const r = transport.data?.resume
  const s = sujets.data

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={4}>Transport des sujets et du matériel</Title>
          <Text c="dimmed" size="sm">Véhicules, itinéraires, horaires prévus et réels, carburant et incidents par centre.</Text>
        </div>
        {dgecs && <Button onClick={() => ouvrir('nouveau')}>Ajouter un convoi</Button>}
      </Group>
      {transport.isLoading && <Loader />}
      {transport.isError && <Alert color="red">Transport indisponible.</Alert>}
      {r && transport.data && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Centres desservis" valeur={`${nombre(r.centresDesservis)} / ${nombre(r.centres)}`} icone="classes" couleur="blue" detail={`${nombre(r.centresSansConvoi)} sans convoi`} />
            <CarteChiffre libelle="Livrés à temps" valeur={pourcent(r.tauxLivresATemps)} icone="classes" couleur="green" detail={`${nombre(r.livres)} livrés, ${nombre(r.enRoute)} en route`} />
            <CarteChiffre libelle="Retards / incidents" valeur={`${nombre(r.enRetard)} / ${nombre(r.avecIncident)}`} icone="classes" couleur="red" detail={`${nombre(r.convois)} convois`} />
            <CarteChiffre libelle="Véhicules et carburant" valeur={nombre(r.vehicules)} icone="classes" couleur="gray" detail={`${nombre(r.carburantLitres)} L, ${nombre(r.escortes)} escortes`} />
          </SimpleGrid>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Centre</Table.Th><Table.Th>Véhicule</Table.Th><Table.Th>Itinéraire</Table.Th><Table.Th>Départ prévu</Table.Th>
                <Table.Th>Arrivée prévue</Table.Th><Table.Th>Arrivée réelle</Table.Th><Table.Th>Statut</Table.Th><Table.Th>Incident</Table.Th><Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {transport.data.convois.map((c) => (
                <Table.Tr key={c.id}>
                  <Table.Td>{c.centre}</Table.Td>
                  <Table.Td>{c.vehicule}{c.escorte ? ' (escorte)' : ''}<Text size="xs" c="dimmed">{c.chauffeur ?? ''}</Text></Table.Td>
                  <Table.Td>{c.itineraire ?? '—'}</Table.Td>
                  <Table.Td>{affiche(c.departPrevu)}</Table.Td>
                  <Table.Td>{affiche(c.arriveePrevue)}</Table.Td>
                  <Table.Td>{affiche(c.arriveeReelle)}</Table.Td>
                  <Table.Td>
                    <Badge color={COULEUR[c.statut] ?? 'gray'}>{STATUTS.find((x) => x.value === c.statut)?.label ?? c.statut}</Badge>
                    {c.enRetard && <Badge color="red" ml={4}>Retard</Badge>}
                  </Table.Td>
                  <Table.Td>{c.incidentType ? INCIDENTS.find((x) => x.value === c.incidentType)?.label ?? c.incidentType : '—'}</Table.Td>
                  <Table.Td>
                    {dgecs && (
                      <Group gap={4} wrap="nowrap">
                        <Button size="compact-xs" variant="light" onClick={() => ouvrir(c)}>Modifier</Button>
                        <ActionIcon color="red" variant="subtle" aria-label="Supprimer le convoi" onClick={() => supprimer.mutate(c.id)}>×</ActionIcon>
                      </Group>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
              {transport.data.convois.length === 0 && <Table.Tr><Table.Td colSpan={9}><Text c="dimmed" ta="center">Aucun convoi enregistré.</Text></Table.Td></Table.Tr>}
            </Table.Tbody>
          </Table>
        </>
      )}

      <Title order={4}>Sujets à imprimer</Title>
      {sujets.isLoading && <Loader />}
      {s && (
        <>
          <Text c="dimmed" size="sm">
            {nombre(s.candidats)} candidats, {nombre(s.salles)} salles. Un sujet par candidat plus {s.margePourcent} % de marge, {s.feuillesParCandidat} feuilles de composition par
            candidat, une enveloppe par salle (hypothèses à confirmer avec la DGECS). Seuls les effectifs sont affichés, jamais le contenu des sujets.
          </Text>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Épreuve</Table.Th><Table.Th>Date</Table.Th><Table.Th ta="right">Sujets à imprimer</Table.Th><Table.Th ta="right">Feuilles</Table.Th>
                <Table.Th ta="right">Enveloppes</Table.Th><Table.Th ta="right">Conditionnés</Table.Th><Table.Th ta="right">Écart</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {s.epreuves.map((e) => (
                <Table.Tr key={e.epreuveId}>
                  <Table.Td>{e.discipline}</Table.Td>
                  <Table.Td>{e.date ?? '—'}</Table.Td>
                  <Table.Td ta="right">{nombre(e.sujetsAImprimer)}</Table.Td>
                  <Table.Td ta="right">{nombre(e.feuilles)}</Table.Td>
                  <Table.Td ta="right">{nombre(e.enveloppes)}</Table.Td>
                  <Table.Td ta="right">{nombre(e.exemplairesConditionnes)}</Table.Td>
                  <Table.Td ta="right"><Text c={e.ecart < 0 ? 'red' : 'green'} span>{nombre(e.ecart)}</Text></Table.Td>
                </Table.Tr>
              ))}
              {s.epreuves.length === 0 && <Table.Tr><Table.Td colSpan={7}><Text c="dimmed" ta="center">Aucune épreuve programmée.</Text></Table.Td></Table.Tr>}
            </Table.Tbody>
          </Table>
          {s.options.length > 0 && (
            <Table withTableBorder w={420}>
              <Table.Thead><Table.Tr><Table.Th>Série / option</Table.Th><Table.Th ta="right">Candidats</Table.Th><Table.Th ta="right">Sujets</Table.Th></Table.Tr></Table.Thead>
              <Table.Tbody>
                {s.options.map((o) => (
                  <Table.Tr key={o.option}><Table.Td>{o.option}</Table.Td><Table.Td ta="right">{nombre(o.candidats)}</Table.Td><Table.Td ta="right">{nombre(o.sujetsAImprimer)}</Table.Td></Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </>
      )}

      <Modal opened={edition !== null} onClose={() => setEdition(null)} title={edition === 'nouveau' ? 'Nouveau convoi' : 'Modifier le convoi'} size="lg">
        <Stack>
          <Select label="Centre" data={centres} value={centreId} onChange={setCentreId} searchable />
          <Group grow>
            <TextInput label="Véhicule" value={vehicule} onChange={(e) => setVehicule(e.currentTarget.value)} maxLength={60} />
            <TextInput label="Chauffeur" value={chauffeur} onChange={(e) => setChauffeur(e.currentTarget.value)} maxLength={100} />
          </Group>
          <TextInput label="Itinéraire" value={itineraire} onChange={(e) => setItineraire(e.currentTarget.value)} maxLength={300} />
          <Group grow>
            <TextInput type="datetime-local" label="Départ prévu" value={departPrevu} onChange={(e) => setDepartPrevu(e.currentTarget.value)} />
            <TextInput type="datetime-local" label="Arrivée prévue" value={arriveePrevue} onChange={(e) => setArriveePrevue(e.currentTarget.value)} />
          </Group>
          <Group grow>
            <TextInput type="datetime-local" label="Départ réel" value={departReel} onChange={(e) => setDepartReel(e.currentTarget.value)} />
            <TextInput type="datetime-local" label="Arrivée réelle" value={arriveeReelle} onChange={(e) => setArriveeReelle(e.currentTarget.value)} />
          </Group>
          <Group grow align="end">
            <Select label="Statut" data={STATUTS} value={statut} onChange={setStatut} allowDeselect={false} />
            <NumberInput label="Carburant (litres)" min={0} value={carburant} onChange={setCarburant} />
            <Checkbox label="Escorte" checked={escorte} onChange={(e) => setEscorte(e.currentTarget.checked)} />
          </Group>
          <Select label="Incident" placeholder="Aucun" data={INCIDENTS} value={incident} onChange={setIncident} clearable />
          {incident !== null && <TextInput label="Description de l'incident" value={incidentDescription} onChange={(e) => setIncidentDescription(e.currentTarget.value)} maxLength={500} />}
          <TextInput label="Observation" value={observation} onChange={(e) => setObservation(e.currentTarget.value)} maxLength={500} />
          <Group justify="end">
            <Button variant="default" onClick={() => setEdition(null)}>Annuler</Button>
            <Button loading={enregistrer.isPending} disabled={centreId === null || vehicule.trim() === ''} onClick={() => enregistrer.mutate()}>Enregistrer</Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
