import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, NumberInput, Select, Stack, Switch, Table, Text, TextInput, Textarea, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { classerIncident, disciplineEtablissement, sanctionnerIncident, signalerIncidentDisciplinaire } from '../api/endpoints'
import type { IncidentDisciplinaire } from '../api/types'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { formaterDate } from '../utils/dates'

const TYPES = [
  { value: 'INSOLENCE', label: 'Insolence / indiscipline' },
  { value: 'VIOLENCE', label: 'Violence' },
  { value: 'HARCELEMENT', label: 'Harcèlement' },
  { value: 'FRAUDE', label: 'Fraude / tricherie' },
  { value: 'DEGRADATION', label: 'Dégradation de matériel' },
  { value: 'VOL', label: 'Vol' },
  { value: 'TELEPHONE', label: 'Téléphone en classe' },
  { value: 'TENUE', label: 'Tenue non conforme' },
  { value: 'AUTRE', label: 'Autre' },
]
const GRAVITES = [
  { value: 'LEGERE', label: 'Légère' },
  { value: 'GRAVE', label: 'Grave' },
  { value: 'TRES_GRAVE', label: 'Très grave' },
]
const SANCTIONS: Record<string, { value: string; label: string }[]> = {
  LEGERE: [{ value: 'AVERTISSEMENT', label: 'Avertissement' }, { value: 'BLAME', label: 'Blâme' }, { value: 'RETENUE', label: 'Retenue' }],
  GRAVE: [
    { value: 'AVERTISSEMENT', label: 'Avertissement' }, { value: 'BLAME', label: 'Blâme' }, { value: 'RETENUE', label: 'Retenue' },
    { value: 'EXCLUSION_TEMPORAIRE', label: 'Exclusion temporaire' }, { value: 'CONSEIL_DISCIPLINE', label: 'Renvoi en conseil de discipline' },
  ],
  TRES_GRAVE: [
    { value: 'BLAME', label: 'Blâme' }, { value: 'EXCLUSION_TEMPORAIRE', label: 'Exclusion temporaire' },
    { value: 'CONSEIL_DISCIPLINE', label: 'Renvoi en conseil de discipline' }, { value: 'EXCLUSION_DEFINITIVE', label: 'Exclusion définitive' },
  ],
}
const libelle = (liste: { value: string; label: string }[], v: string | null) => liste.find((x) => x.value === v)?.label ?? v ?? '—'
const COULEUR_GRAVITE: Record<string, string> = { LEGERE: 'gray', GRAVE: 'orange', TRES_GRAVE: 'red' }
const COULEUR_STATUT: Record<string, string> = { OUVERT: 'yellow', SANCTIONNE: 'blue', CLASSE: 'gray' }
const LIBELLE_STATUT: Record<string, string> = { OUVERT: 'À traiter', SANCTIONNE: 'Sanctionné', CLASSE: 'Classé sans suite' }

/** Discipline scolaire : incidents, sanctions graduées selon la gravité, suivi des récidives. Visible de la direction de l'établissement uniquement. */
export default function PageDiscipline() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const queryClient = useQueryClient()
  const { annees, anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const [signalement, setSignalement] = useState(false)
  const [aSanctionner, setASanctionner] = useState<IncidentDisciplinaire | null>(null)
  const [aClasser, setAClasser] = useState<IncidentDisciplinaire | null>(null)

  const liste = useQuery({
    queryKey: ['discipline', etablissementId, anneeId],
    queryFn: () => disciplineEtablissement(etablissementId as number, Number(anneeId)),
    enabled: etablissementId !== undefined && anneeId !== null,
  })
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['discipline'] })
  const erreur = (e: Error) => notifications.show({ color: 'red', title: 'Action impossible', message: e.message })

  // Signalement
  const [matricule, setMatricule] = useState('')
  const [dateFaits, setDateFaits] = useState(new Date().toISOString().slice(0, 10))
  const [type, setType] = useState<string | null>('INSOLENCE')
  const [gravite, setGravite] = useState<string | null>('LEGERE')
  const [description, setDescription] = useState('')
  const signaler = useMutation({
    mutationFn: () => signalerIncidentDisciplinaire(etablissementId as number, { matriculeEleve: matricule.trim(), anneeId: Number(anneeId), dateFaits, type: type ?? 'AUTRE', gravite: gravite ?? 'LEGERE', description: description.trim() }),
    onSuccess: () => {
      notifications.show({ color: 'green', message: 'Incident enregistré.' })
      setSignalement(false)
      setMatricule('')
      setDescription('')
      rafraichir()
    },
    onError: erreur,
  })

  // Sanction
  const [sanction, setSanction] = useState<string | null>(null)
  const [jours, setJours] = useState<number | string>(1)
  const [debut, setDebut] = useState(new Date().toISOString().slice(0, 10))
  const [observation, setObservation] = useState('')
  const [parents, setParents] = useState(true)
  const sanctionner = useMutation({
    mutationFn: () => sanctionnerIncident(aSanctionner!.id, {
      sanction: sanction ?? '',
      exclusionJours: sanction === 'EXCLUSION_TEMPORAIRE' ? Number(jours) : null,
      exclusionDebut: sanction === 'EXCLUSION_TEMPORAIRE' ? debut : null,
      observation: observation.trim() || null,
      parentsInformes: parents,
    }),
    onSuccess: () => {
      notifications.show({ color: 'green', message: 'Sanction enregistrée.' })
      if (sanction === 'EXCLUSION_DEFINITIVE') {
        notifications.show({ color: 'orange', autoClose: false, message: "L'exclusion définitive n'annule pas l'inscription automatiquement : annulez-la dans Inscriptions si nécessaire." })
      }
      setASanctionner(null)
      setSanction(null)
      setObservation('')
      rafraichir()
    },
    onError: erreur,
  })

  // Classement
  const [motifClassement, setMotifClassement] = useState('')
  const classer = useMutation({
    mutationFn: () => classerIncident(aClasser!.id, motifClassement.trim()),
    onSuccess: () => {
      setAClasser(null)
      setMotifClassement('')
      rafraichir()
    },
    onError: erreur,
  })

  if (etablissementId === undefined) return <Alert color="orange" variant="light">Aucun établissement n'est associé à votre compte.</Alert>
  const d = liste.data

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Discipline</Title>
        <Button onClick={() => setSignalement(true)} disabled={annees.isPending}>Signaler un incident</Button>
      </Group>
      <Text c="dimmed" size="sm">Ces informations concernent des élèves : elles ne sont visibles que de votre direction. Les parents voient seulement les sanctions prononcées contre leur enfant.</Text>
      <Select label="Année scolaire" data={options} value={anneeId} onChange={setAnneeChoisie} allowDeselect={false} w={200} />
      {liste.isPending && anneeId !== null && <Loader />}
      {liste.isError && <Alert color="red" variant="light">{liste.error.message}</Alert>}
      {d && (
        <>
          <Group gap="xs">
            <Badge size="lg" variant="light">{d.resume.incidents} incidents</Badge>
            <Badge size="lg" variant="light" color={d.resume.ouverts ? 'yellow' : 'gray'}>{d.resume.ouverts} à traiter</Badge>
            <Badge size="lg" variant="light" color="blue">{d.resume.sanctionnes} sanctionnés</Badge>
            <Badge size="lg" variant="light" color="red">{d.resume.exclusions} exclusions</Badge>
            <Badge size="lg" variant="light" color={d.resume.recidivistes ? 'orange' : 'gray'}>{d.resume.recidivistes} élève(s) en récidive (3 incidents ou plus)</Badge>
          </Group>
          {d.incidents.length === 0 ? <Text c="dimmed">Aucun incident cette année.</Text> : (
            <Table.ScrollContainer minWidth={900}>
              <Table striped withTableBorder>
                <Table.Thead>
                  <Table.Tr><Table.Th>Date</Table.Th><Table.Th>Élève</Table.Th><Table.Th>Faits</Table.Th><Table.Th>Gravité</Table.Th><Table.Th>Statut</Table.Th><Table.Th>Sanction</Table.Th><Table.Th>Actions</Table.Th></Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.incidents.map((i) => (
                    <Table.Tr key={i.id}>
                      <Table.Td>{formaterDate(i.dateFaits)}</Table.Td>
                      <Table.Td>{i.eleve}<Text size="xs" c="dimmed">{i.matricule}</Text>{i.recidiviste && <Badge size="xs" color="orange" variant="light">Récidive ({i.incidentsAnnee})</Badge>}</Table.Td>
                      <Table.Td>{libelle(TYPES, i.type)}<Text size="xs" c="dimmed">{i.description}</Text></Table.Td>
                      <Table.Td><Badge variant="light" color={COULEUR_GRAVITE[i.gravite]}>{libelle(GRAVITES, i.gravite)}</Badge></Table.Td>
                      <Table.Td><Badge variant="light" color={COULEUR_STATUT[i.statut]}>{LIBELLE_STATUT[i.statut]}</Badge></Table.Td>
                      <Table.Td>
                        {i.sanction ? libelle(SANCTIONS.TRES_GRAVE.concat(SANCTIONS.GRAVE), i.sanction) : '—'}
                        {i.exclusionJours && <Text size="xs" c="dimmed">{i.exclusionJours} jour(s) à partir du {formaterDate(i.exclusionDebut)}</Text>}
                        {i.observation && <Text size="xs" c="dimmed">{i.observation}</Text>}
                      </Table.Td>
                      <Table.Td>
                        {i.statut === 'OUVERT' && (
                          <Group gap="xs" wrap="nowrap">
                            <Button size="compact-sm" variant="light" onClick={() => { setSanction(null); setASanctionner(i) }}>Sanctionner</Button>
                            <Button size="compact-sm" variant="subtle" color="gray" onClick={() => setAClasser(i)}>Classer</Button>
                          </Group>
                        )}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </>
      )}

      <Modal opened={signalement} onClose={() => setSignalement(false)} title="Signaler un incident">
        <Stack>
          <TextInput label="Matricule ou identifiant national de l'élève" value={matricule} onChange={(e) => setMatricule(e.currentTarget.value)} />
          <TextInput label="Date des faits" type="date" value={dateFaits} onChange={(e) => setDateFaits(e.currentTarget.value)} />
          <Select label="Nature" data={TYPES} value={type} onChange={setType} allowDeselect={false} />
          <Select label="Gravité" data={GRAVITES} value={gravite} onChange={setGravite} allowDeselect={false} />
          <Textarea label="Description des faits" maxLength={1000} value={description} onChange={(e) => setDescription(e.currentTarget.value)} />
          <Button disabled={matricule.trim() === '' || description.trim() === ''} loading={signaler.isPending} onClick={() => signaler.mutate()}>Enregistrer</Button>
        </Stack>
      </Modal>

      <Modal opened={aSanctionner !== null} onClose={() => setASanctionner(null)} title="Prononcer une sanction">
        {aSanctionner && (
          <Stack>
            <Text size="sm">{aSanctionner.eleve} — {libelle(TYPES, aSanctionner.type)} ({libelle(GRAVITES, aSanctionner.gravite)})</Text>
            <Select label="Sanction" data={SANCTIONS[aSanctionner.gravite]} value={sanction} onChange={setSanction} />
            {sanction === 'EXCLUSION_TEMPORAIRE' && (
              <Group grow>
                <NumberInput label="Jours (1 à 8)" min={1} max={8} value={jours} onChange={setJours} />
                <TextInput label="À partir du" type="date" value={debut} onChange={(e) => setDebut(e.currentTarget.value)} />
              </Group>
            )}
            <Textarea label={sanction === 'EXCLUSION_DEFINITIVE' || sanction === 'CONSEIL_DISCIPLINE' ? 'Observation (obligatoire)' : 'Observation'} maxLength={500} value={observation} onChange={(e) => setObservation(e.currentTarget.value)} />
            <Switch label="Parents informés" checked={parents} onChange={(e) => setParents(e.currentTarget.checked)} />
            <Button disabled={sanction === null} loading={sanctionner.isPending} onClick={() => sanctionner.mutate()}>Enregistrer la sanction</Button>
          </Stack>
        )}
      </Modal>

      <Modal opened={aClasser !== null} onClose={() => setAClasser(null)} title="Classer sans suite">
        <Stack>
          <Textarea label="Motif (obligatoire)" maxLength={500} value={motifClassement} onChange={(e) => setMotifClassement(e.currentTarget.value)} />
          <Button color="gray" disabled={motifClassement.trim() === ''} loading={classer.isPending} onClick={() => classer.mutate()}>Classer</Button>
        </Stack>
      </Modal>
    </Stack>
  )
}
