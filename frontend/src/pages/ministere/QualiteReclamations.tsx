import { Alert, Badge, Button, Group, Loader, Modal, NumberInput, Select, SimpleGrid, Stack, Table, Text, Textarea, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { annulerRectification, deposerReclamation, epreuvesExamen, listerExamens, listerReclamations, qualiteDonnees, rectifierNote, traiterReclamation } from '../../api/endpoints'
import type { ReclamationExamen } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'
import { useDroitsExamens } from '../../auth/useDroitsExamens'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const date = (iso: string) => new Date(iso).toLocaleDateString('fr-FR')
const OBJETS = [
  { value: 'NOTE', label: 'Note' },
  { value: 'IDENTITE', label: 'Identité' },
  { value: 'OMISSION', label: 'Omission' },
  { value: 'AUTRE', label: 'Autre' },
]

/** Qualité des données des examens et suivi des réclamations (dépôt et traitement par la DGECS). */
export default function QualiteReclamations() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const [examenId, setExamenId] = useState<string | null>(null)
  const [depot, setDepot] = useState(false)
  const [numero, setNumero] = useState('')
  const [objet, setObjet] = useState<string | null>('NOTE')
  const [description, setDescription] = useState('')
  const [aTraiter, setATraiter] = useState<ReclamationExamen | null>(null)
  const [aAnnuler, setAAnnuler] = useState<ReclamationExamen | null>(null)
  const [motifAnnulation, setMotifAnnulation] = useState('')
  const [decision, setDecision] = useState('')
  const [epreuveId, setEpreuveId] = useState<string | null>(null)
  const [nouvelleNote, setNouvelleNote] = useState<number | string>('')

  const qualite = useQuery({ queryKey: ['qualite-donnees', anneeId], queryFn: () => qualiteDonnees(anneeId as number), enabled: anneeId !== null })
  const examens = useQuery({ queryKey: ['examens', anneeId], queryFn: () => listerExamens(anneeId as number), enabled: anneeId !== null })
  const reclamations = useQuery({
    queryKey: ['reclamations', examenId],
    queryFn: () => listerReclamations(Number(examenId)),
    enabled: examenId !== null,
  })
  const rafraichir = () => {
    void queryClient.invalidateQueries({ queryKey: ['reclamations'] })
    void queryClient.invalidateQueries({ queryKey: ['qualite-donnees'] })
  }
  const deposer = useMutation({
    mutationFn: () => deposerReclamation(Number(examenId), { numeroCandidat: numero, objet: objet ?? 'NOTE', description }),
    onSuccess: () => {
      setDepot(false)
      setNumero('')
      setDescription('')
      rafraichir()
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const traiter = useMutation({
    mutationFn: (statut: string) => traiterReclamation(Number(examenId), (aTraiter as ReclamationExamen).id, { statut, decision }),
    onSuccess: () => {
      setATraiter(null)
      setDecision('')
      rafraichir()
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const annuler = useMutation({
    mutationFn: () => annulerRectification(Number(examenId), (aAnnuler as ReclamationExamen).id, { motif: motifAnnulation.trim() }),
    onSuccess: () => {
      setAAnnuler(null)
      setMotifAnnulation('')
      void queryClient.invalidateQueries({ queryKey: ['reclamations'] })
      void queryClient.invalidateQueries({ queryKey: ['qualite-donnees'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const epreuves = useQuery({ queryKey: ['epreuves', examenId], queryFn: () => epreuvesExamen(Number(examenId)), enabled: aTraiter?.objet === 'NOTE' && examenId !== null })
  const rectifier = useMutation({
    mutationFn: () => rectifierNote(Number(examenId), (aTraiter as ReclamationExamen).id, { epreuveId: Number(epreuveId), nouvelleNote: Number(nouvelleNote), decision }),
    onSuccess: () => {
      setATraiter(null)
      setDecision('')
      setEpreuveId(null)
      setNouvelleNote('')
      rafraichir()
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const d = qualite.data

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={3}>Qualité des données et réclamations</Title>
          <Text c="dimmed" size="sm">Dossiers incomplets, notes manquantes, listes validées et suivi des réclamations.</Text>
        </div>
        <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
      </Group>
      {qualite.isLoading && <Loader />}
      {qualite.isError && <Alert color="red">Indicateurs indisponibles.</Alert>}
      {d && (
        <>
          <Title order={4}>Dossiers des candidats</Title>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Dossiers complets" valeur={pourcent(d.dossiers.tauxDossiersCompletes)} icone="eleves" detail="centre, identité et photo renseignés" />
            <CarteChiffre libelle="Listes validées" valeur={pourcent(d.dossiers.tauxListesValidees)} icone="notes" detail={`${nombre(d.dossiers.listesValidees)} sur ${nombre(d.dossiers.listesTotal)}`} />
            <CarteChiffre libelle="Sans centre" valeur={nombre(d.dossiers.sansCentre)} icone="classes" couleur="orange" />
            <CarteChiffre libelle="Identité incomplète" valeur={nombre(d.dossiers.identiteIncomplete)} icone="alerte" couleur="orange" detail="naissance ou filiation manquante" />
            <CarteChiffre libelle="Sans photo" valeur={nombre(d.dossiers.sansPhoto)} icone="eleves" couleur="orange" />
            <CarteChiffre libelle="Présents sans moyenne" valeur={nombre(d.dossiers.presentsSansMoyenne)} icone="notes" couleur="red" detail="notes à saisir" />
            <CarteChiffre libelle="Présents sans décision" valeur={nombre(d.dossiers.presentsSansDecision)} icone="resultats" couleur="red" />
          </SimpleGrid>
          <Title order={4}>Réclamations (délai de traitement : {d.delaiReclamationJours} jours)</Title>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Réclamations" valeur={nombre(d.reclamations.total)} icone="alerte" detail={`${nombre(d.reclamations.ouvertes)} ouvertes`} />
            <CarteChiffre libelle="En retard" valeur={nombre(d.reclamations.enRetard)} icone="alerte" couleur="red" />
            <CarteChiffre libelle="Traitées dans le délai" valeur={pourcent(d.reclamations.tauxTraiteesDansLeDelai)} icone="presences" couleur="green" />
            <CarteChiffre libelle="Taux d'acceptation" valeur={pourcent(d.reclamations.tauxAcceptation)} icone="resultats" detail={`${nombre(d.reclamations.acceptees)} acceptées, ${nombre(d.reclamations.rejetees)} rejetées`} />
            <CarteChiffre libelle="Résultats rectifiés" valeur={nombre(d.resultatsRectifies)} icone="notes" detail="notes corrigées après vérification" />
          </SimpleGrid>
        </>
      )}

      <Group justify="space-between" align="end">
        <Select label="Examen" placeholder="Choisir un examen" data={(examens.data ?? []).map((x) => ({ value: String(x.id), label: x.libelle }))}
          value={examenId} onChange={setExamenId} w={320} />
        {dgecs && examenId !== null && <Button onClick={() => setDepot(true)}>Déposer une réclamation</Button>}
      </Group>
      {reclamations.isError && <Alert color="red">Réclamations indisponibles.</Alert>}
      {reclamations.data && (reclamations.data.length === 0 ? <Text c="dimmed">Aucune réclamation pour cet examen.</Text> : (
        <Table.ScrollContainer minWidth={760}>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Candidat</Table.Th><Table.Th>Objet</Table.Th><Table.Th>Description</Table.Th><Table.Th>Déposée</Table.Th>
                <Table.Th>Échéance</Table.Th><Table.Th>Statut</Table.Th><Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {reclamations.data.map((r) => (
                <Table.Tr key={r.id}>
                  <Table.Td>{r.numero ? `${r.numero} · ` : ''}{r.candidat}</Table.Td>
                  <Table.Td>{OBJETS.find((o) => o.value === r.objet)?.label ?? r.objet}</Table.Td>
                  <Table.Td>{r.description}{r.decision ? <Text size="xs" c="dimmed">Décision : {r.decision}</Text> : null}</Table.Td>
                  <Table.Td>{date(r.deposeeLe)}</Table.Td>
                  <Table.Td>{date(r.echeanceLe)}</Table.Td>
                  <Table.Td>
                    <Badge color={r.statut === 'ACCEPTEE' ? 'green' : r.statut === 'REJETEE' ? 'gray' : r.enRetard ? 'red' : 'blue'}>
                      {r.statut === 'OUVERTE' ? (r.enRetard ? 'En retard' : 'Ouverte') : r.statut === 'ACCEPTEE' ? 'Acceptée' : 'Rejetée'}
                    </Badge>
                  </Table.Td>
                  <Table.Td>
                    {dgecs && r.statut === 'OUVERTE' && <Button size="xs" variant="light" onClick={() => setATraiter(r)}>Traiter</Button>}
                    {dgecs && r.statut === 'ACCEPTEE' && r.objet === 'NOTE' && <Button size="xs" variant="subtle" color="red" onClick={() => setAAnnuler(r)}>Annuler la rectification</Button>}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      ))}

      <Modal opened={depot} onClose={() => setDepot(false)} title="Déposer une réclamation">
        <Stack>
          <TextInput label="Numéro du candidat" value={numero} onChange={(e) => setNumero(e.currentTarget.value)} required />
          <Select label="Objet" data={OBJETS} value={objet} onChange={setObjet} allowDeselect={false} />
          <Textarea label="Description" value={description} onChange={(e) => setDescription(e.currentTarget.value)} minRows={3} maxLength={2000} required />
          <Button loading={deposer.isPending} disabled={!numero.trim() || !description.trim()} onClick={() => deposer.mutate()}>Enregistrer</Button>
        </Stack>
      </Modal>
      <Modal opened={aTraiter !== null} onClose={() => setATraiter(null)} title="Traiter la réclamation">
        <Stack>
          <Text size="sm">{aTraiter?.candidat} : {aTraiter?.description}</Text>
          <Textarea label="Motivation de la décision" value={decision} onChange={(e) => setDecision(e.currentTarget.value)} minRows={3} maxLength={1000} required />
          {aTraiter?.objet === 'NOTE' && (
            <Stack gap="xs">
              <Text size="sm" fw={600}>Rectifier la note (le résultat du candidat et les totaux sont recalculés)</Text>
              <Select label="Épreuve" placeholder="Choisir l'épreuve" data={(epreuves.data ?? []).map((e) => ({ value: String(e.id), label: e.matiere }))} value={epreuveId} onChange={setEpreuveId} />
              <NumberInput label="Nouvelle note" min={0} max={20} decimalScale={2} value={nouvelleNote} onChange={setNouvelleNote} />
              <Button color="green" variant="outline" loading={rectifier.isPending} disabled={!decision.trim() || epreuveId === null || nouvelleNote === ''} onClick={() => rectifier.mutate()}>
                Accepter et rectifier la note
              </Button>
            </Stack>
          )}
          <Group>
            <Button color="green" variant="light" loading={traiter.isPending} disabled={!decision.trim()} onClick={() => traiter.mutate('ACCEPTEE')}>Accepter sans modifier la note</Button>
            <Button color="gray" loading={traiter.isPending} disabled={!decision.trim()} onClick={() => traiter.mutate('REJETEE')}>Rejeter</Button>
          </Group>
        </Stack>
      </Modal>
      <Modal opened={aAnnuler !== null} onClose={() => setAAnnuler(null)} title="Annuler la rectification">
        <Stack>
          <Text size="sm">{aAnnuler?.candidat} : la note, la moyenne et la décision d'origine seront rétablies, les totaux de l'établissement recalculés et la réclamation rouverte.</Text>
          <TextInput label="Motif de l'annulation" value={motifAnnulation} onChange={(e) => setMotifAnnulation(e.currentTarget.value)} maxLength={500} />
          <Button color="red" loading={annuler.isPending} disabled={!motifAnnulation.trim()} onClick={() => annuler.mutate()}>Annuler la rectification</Button>
        </Stack>
      </Modal>
    </Stack>
  )
}
