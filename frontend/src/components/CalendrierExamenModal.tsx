import { useState } from 'react'
import { Alert, Badge, Button, Divider, Group, Modal, NumberInput, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  affecterCentresAutomatiquement,
  ajouterEpreuve,
  centresExamen,
  creerCentreExamen,
  definirPeriodeExamen,
  epreuvesExamen,
  etablissementsCentres,
  repartirCentre,
  repartitionCentre,
  listerMatieres,
  supprimerCentreExamen,
  supprimerEpreuve,
  telechargerListeAppel,
} from '../api/endpoints'
import type { AffectationAutomatique, Examen } from '../api/types'
import { formaterDate } from '../utils/dates'
import DeroulementCentreModal from './DeroulementCentreModal'
import IncidentsCentreModal from './IncidentsCentreModal'
import PersonnelCentreModal from './PersonnelCentreModal'

const heure = (h: string) => h.slice(0, 5)

/** Période officielle, emploi du temps des épreuves et centres d'examen d'un examen. */
export default function CalendrierExamenModal({ examen, onFermer }: { examen: Examen; onFermer: () => void }) {
  return (
    <Modal opened onClose={onFermer} size="xl" title={`Calendrier et centres : ${examen.libelle}`}>
      <Stack gap="lg">
        <Periode examen={examen} />
        <Divider />
        <Epreuves examenId={examen.id} />
        <Divider />
        <Centres examenId={examen.id} />
      </Stack>
    </Modal>
  )
}

function Periode({ examen }: { examen: Examen }) {
  const queryClient = useQueryClient()
  const [debut, setDebut] = useState(examen.dateDebut ?? '')
  const [fin, setFin] = useState(examen.dateFin ?? '')
  const [erreur, setErreur] = useState<string | null>(null)
  const enregistrement = useMutation({
    mutationFn: () => definirPeriodeExamen(examen.id, debut === '' ? null : debut, fin === '' ? null : fin),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['examens'] })
      setErreur(null)
      notifications.show({ color: 'green', title: 'Période enregistrée', message: examen.libelle })
    },
    onError: (e) => setErreur(e.message),
  })
  return (
    <Stack gap="xs">
      <Text fw={600}>Période des épreuves</Text>
      <Group align="flex-end">
        <TextInput label="Début" type="date" value={debut} onChange={(e) => setDebut(e.currentTarget.value)} />
        <TextInput label="Fin" type="date" value={fin} onChange={(e) => setFin(e.currentTarget.value)} />
        <Button loading={enregistrement.isPending} onClick={() => enregistrement.mutate()}>
          Enregistrer
        </Button>
      </Group>
      {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
    </Stack>
  )
}

function Epreuves({ examenId }: { examenId: number }) {
  const queryClient = useQueryClient()
  const liste = useQuery({ queryKey: ['epreuves', examenId], queryFn: () => epreuvesExamen(examenId) })
  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres })
  const [matiere, setMatiere] = useState<string | null>(null)
  const [libelle, setLibelle] = useState('')
  const [date, setDate] = useState('')
  const [debut, setDebut] = useState('08:00')
  const [fin, setFin] = useState('10:00')
  const [coefficient, setCoefficient] = useState<number | string>(1)
  const [erreur, setErreur] = useState<string | null>(null)

  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['epreuves', examenId] })
  const ajout = useMutation({
    mutationFn: () =>
      ajouterEpreuve(examenId, {
        matiereId: matiere === null ? null : Number(matiere),
        libelle: matiere === null ? libelle.trim() : null,
        date,
        heureDebut: debut,
        heureFin: fin,
        coefficient: Number(coefficient) >= 1 ? Number(coefficient) : 1,
      }),
    onSuccess: () => {
      rafraichir()
      setLibelle('')
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })
  const suppression = useMutation({
    mutationFn: (id: number) => supprimerEpreuve(id),
    onSuccess: rafraichir,
    onError: (e) => notifications.show({ color: 'red', title: 'Suppression impossible', message: e.message }),
  })

  return (
    <Stack gap="xs">
      <Text fw={600}>Emploi du temps des épreuves</Text>
      {liste.data && liste.data.length === 0 && <Text size="sm" c="dimmed">Aucune épreuve publiée.</Text>}
      {liste.data && liste.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Date</Table.Th>
              <Table.Th>Horaire</Table.Th>
              <Table.Th>Épreuve</Table.Th>
              <Table.Th>Coef.</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {liste.data.map((e) => (
              <Table.Tr key={e.id}>
                <Table.Td>{formaterDate(e.date)}</Table.Td>
                <Table.Td>
                  {heure(e.heureDebut)} – {heure(e.heureFin)}
                </Table.Td>
                <Table.Td>{e.matiere}</Table.Td>
                <Table.Td>{e.coefficient}</Table.Td>
                <Table.Td ta="right">
                  <Button size="compact-xs" color="red" variant="subtle" onClick={() => suppression.mutate(e.id)}>
                    Supprimer
                  </Button>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
      <Group align="flex-end">
        <Select
          label="Matière"
          placeholder="Du référentiel"
          searchable
          clearable
          data={(matieres.data ?? []).map((m) => ({ value: String(m.id), label: m.libelle }))}
          value={matiere}
          onChange={setMatiere}
          w={200}
        />
        {matiere === null && (
          <TextInput label="Ou libellé" placeholder="Ex. Dictée" value={libelle} onChange={(e) => setLibelle(e.currentTarget.value)} w={160} />
        )}
        <TextInput label="Date" type="date" value={date} onChange={(e) => setDate(e.currentTarget.value)} />
        <TextInput label="De" type="time" value={debut} onChange={(e) => setDebut(e.currentTarget.value)} w={110} />
        <TextInput label="À" type="time" value={fin} onChange={(e) => setFin(e.currentTarget.value)} w={110} />
        <NumberInput label="Coef." min={1} allowDecimal={false} value={coefficient} onChange={setCoefficient} w={80} />
        <Button
          loading={ajout.isPending}
          disabled={date === '' || (matiere === null && libelle.trim() === '')}
          onClick={() => ajout.mutate()}
        >
          Ajouter
        </Button>
      </Group>
      {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
    </Stack>
  )
}

function Centres({ examenId }: { examenId: number }) {
  const queryClient = useQueryClient()
  const centres = useQuery({ queryKey: ['centres-examen', examenId], queryFn: () => centresExamen(examenId) })
  const etablissements = useQuery({ queryKey: ['etablissements-centres'], queryFn: etablissementsCentres })
  const [etablissement, setEtablissement] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [resultat, setResultat] = useState<AffectationAutomatique | null>(null)
  const [centreSalles, setCentreSalles] = useState<{ id: number; nom: string } | null>(null)
  const [centrePersonnel, setCentrePersonnel] = useState<{ id: number; nom: string } | null>(null)
  const [centreIncidents, setCentreIncidents] = useState<{ id: number; nom: string } | null>(null)
  const [centreDeroulement, setCentreDeroulement] = useState<{ id: number; nom: string } | null>(null)

  const rafraichir = () => {
    void queryClient.invalidateQueries({ queryKey: ['centres-examen', examenId] })
    void queryClient.invalidateQueries({ queryKey: ['preparation-examen'] })
    void queryClient.invalidateQueries({ queryKey: ['examens-etablissement'] })
  }
  const ajout = useMutation({
    mutationFn: () =>
      creerCentreExamen(examenId, { nom: null, adresse: null, zoneId: null, dateDebut: null, etablissementId: Number(etablissement) }),
    onSuccess: () => {
      rafraichir()
      setEtablissement(null)
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })
  const suppression = useMutation({
    mutationFn: (id: number) => supprimerCentreExamen(id),
    onSuccess: rafraichir,
    onError: (e) => notifications.show({ color: 'red', title: 'Suppression impossible', message: e.message }),
  })
  const automatique = useMutation({
    mutationFn: () => affecterCentresAutomatiquement(examenId),
    onSuccess: (r) => {
      setResultat(r)
      rafraichir()
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Affectation impossible', message: e.message }),
  })
  const dejaCentres = new Set((centres.data ?? []).map((c) => c.etablissementId))

  return (
    <Stack gap="xs">
      <Text fw={600}>Centres d'examen</Text>
      <Text size="sm" c="dimmed">
        Un centre est un établissement désigné pour accueillir les candidats. L'affectation automatique place les candidats
        dans un centre de la commune de leur établissement.
      </Text>
      {centres.data && centres.data.length === 0 && <Text size="sm" c="dimmed">Aucun centre désigné.</Text>}
      {centres.data && centres.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {centres.data.map((c) => (
              <Table.Tr key={c.id}>
                <Table.Td>{c.nom}</Table.Td>
                <Table.Td>{c.zone ?? '—'}</Table.Td>
                <Table.Td ta="right">
                  <Button size="compact-xs" variant="subtle" onClick={() => setCentreSalles(c)}>
                    Salles et PV
                  </Button>
                  <Button size="compact-xs" variant="subtle" onClick={() => setCentrePersonnel(c)}>
                    Personnel
                  </Button>
                  <Button size="compact-xs" variant="subtle" onClick={() => setCentreDeroulement(c)}>
                    Épreuves
                  </Button>
                  <Button size="compact-xs" variant="subtle" onClick={() => setCentreIncidents(c)}>
                    Incidents
                  </Button>
                  <Button size="compact-xs" color="red" variant="subtle" onClick={() => suppression.mutate(c.id)}>
                    Retirer
                  </Button>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
      <Group align="flex-end">
        <Select
          label="Désigner un établissement éligible comme centre"
          placeholder="Choisir un établissement"
          searchable
          data={(etablissements.data ?? [])
            .filter((e) => e.eligible && !dejaCentres.has(e.id))
            .map((e) => ({ value: String(e.id), label: e.nom }))}
          value={etablissement}
          onChange={setEtablissement}
          w={320}
        />
        <Button disabled={etablissement === null} loading={ajout.isPending} onClick={() => ajout.mutate()}>
          Désigner
        </Button>
        <Button variant="light" loading={automatique.isPending} onClick={() => automatique.mutate()}>
          Affecter automatiquement les candidats
        </Button>
      </Group>
      {centreDeroulement && <DeroulementCentreModal examenId={examenId} centre={centreDeroulement} onFermer={() => setCentreDeroulement(null)} />}
      {centreIncidents && <IncidentsCentreModal examenId={examenId} centre={centreIncidents} onFermer={() => setCentreIncidents(null)} />}
      {centrePersonnel && <PersonnelCentreModal examenId={examenId} centre={centrePersonnel} onFermer={() => setCentrePersonnel(null)} />}
      {centreSalles && <SallesCentre examenId={examenId} centre={centreSalles} onFermer={() => setCentreSalles(null)} />}
      {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
      {resultat && (
        <Alert color={resultat.sansCentre.length === 0 ? 'green' : 'orange'} variant="light">
          {resultat.candidatsAffectes} candidat(s) de {resultat.etablissementsAffectes} établissement(s) affecté(s).
          {resultat.sansCentre.length > 0 &&
            ` Sans centre dans leur commune : ${resultat.sansCentre.join(', ')}. Désignez un centre dans cette commune puis relancez.`}
        </Alert>
      )}
    </Stack>
  )
}

/** Plan d'un centre : salles de 30 candidats au plus, une table chacun, et numéro de PV de chaque candidat. */
function SallesCentre({ examenId, centre, onFermer }: { examenId: number; centre: { id: number; nom: string }; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const plan = useQuery({ queryKey: ['repartition-centre', examenId, centre.id], queryFn: () => repartitionCentre(examenId, centre.id) })
  const generation = useMutation({
    mutationFn: () => repartirCentre(examenId, centre.id),
    onSuccess: (r) => {
      queryClient.setQueryData(['repartition-centre', examenId, centre.id], r)
      void queryClient.invalidateQueries({ queryKey: ['preparation-examen'] })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Répartition impossible', message: e.message }),
  })
  const liste = useMutation({
    mutationFn: () => telechargerListeAppel(examenId, centre.id),
    onError: (e) => notifications.show({ color: 'red', title: 'Liste indisponible', message: e.message }),
  })
  const p = plan.data
  return (
    <Modal opened onClose={onFermer} size="xl" title={`Salles et numéros de PV : ${centre.nom}`}>
      <Stack gap="sm">
        {plan.isError && <Alert color="red" variant="light">{plan.error.message}</Alert>}
        {p && (
          <>
            <Group gap="xs">
              <Badge variant="light">{p.candidats} candidat(s)</Badge>
              <Badge variant="light">{p.salles} salle(s)</Badge>
              <Badge variant="light">{p.surveillants} surveillants (2 par salle)</Badge>
              {p.capacite !== null && (
                <Badge variant="light" color={p.candidats > p.capacite ? 'red' : 'gray'}>capacité {p.capacite}</Badge>
              )}
            </Group>
            <Text size="sm" c="dimmed">
              Règlement général : 30 candidats au plus par salle, un candidat par table-banc. Refaites la répartition après
              l'arrivée de nouveaux candidats : les numéros de PV sont recalculés.
            </Text>
            <Group>
              <Button loading={generation.isPending} disabled={p.candidats === 0} onClick={() => generation.mutate()}>
                {p.repartie ? 'Refaire la répartition' : 'Répartir en salles et numéroter'}
              </Button>
              <Button variant="light" disabled={!p.repartie} loading={liste.isPending} onClick={() => liste.mutate()}>
                Liste d'appel (PDF)
              </Button>
            </Group>
            {p.candidats === 0 && <Text c="dimmed">Aucun candidat affecté à ce centre.</Text>}
            {p.plan.map((s) => (
              <Stack key={s.numero} gap={4}>
                <Text fw={600}>Salle {s.numero} ({s.places.length} candidat(s))</Text>
                <Table verticalSpacing={2}>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Table</Table.Th>
                      <Table.Th>N° de PV</Table.Th>
                      <Table.Th>Candidat</Table.Th>
                      <Table.Th>Établissement</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {s.places.map((x) => (
                      <Table.Tr key={x.numeroPv}>
                        <Table.Td>{x.table}</Table.Td>
                        <Table.Td>{x.numeroPv}</Table.Td>
                        <Table.Td>{x.nom}</Table.Td>
                        <Table.Td>{x.etablissement}</Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Stack>
            ))}
          </>
        )}
      </Stack>
    </Modal>
  )
}
