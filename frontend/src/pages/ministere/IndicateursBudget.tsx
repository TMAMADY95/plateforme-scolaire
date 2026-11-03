import { Alert, Button, Group, Loader, Modal, NumberInput, Select, SimpleGrid, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { budgetExamen, enregistrerBudget, indicateursDgecs, listerExamens } from '../../api/endpoints'
import type { BudgetLigne } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'
import { useDroitsExamens } from '../../auth/useDroitsExamens'
import VerificationsDiplomes from './VerificationsDiplomes'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const gnf = (n: number | null) => (n === null ? '—' : `${new Intl.NumberFormat('fr-FR').format(n)} GNF`)
const RUBRIQUES: Record<string, string> = {
  IMPRESSION: 'Impression des sujets',
  TRANSPORT: 'Transport et logistique',
  SURVEILLANCE: 'Surveillance',
  CORRECTION: 'Correction',
  SECURITE: 'Sécurité',
  SANTE: 'Santé',
  COMMUNICATION: 'Communication',
  AUTRE: 'Autres dépenses',
}

/** Indicateurs de performance du directeur général et budget des examens (montants en francs guinéens). */
export default function IndicateursBudget() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const [examenId, setExamenId] = useState<string | null>(null)
  const [ligne, setLigne] = useState<BudgetLigne | null>(null)
  const [prevu, setPrevu] = useState<number | string>(0)
  const [engage, setEngage] = useState<number | string>(0)
  const [paye, setPaye] = useState<number | string>(0)
  const [observation, setObservation] = useState('')

  const indicateurs = useQuery({ queryKey: ['indicateurs-dgecs', anneeId], queryFn: () => indicateursDgecs(anneeId as number), enabled: anneeId !== null })
  const examens = useQuery({ queryKey: ['examens', anneeId], queryFn: () => listerExamens(anneeId as number), enabled: anneeId !== null })
  const budget = useQuery({ queryKey: ['budget', examenId], queryFn: () => budgetExamen(Number(examenId)), enabled: examenId !== null })
  const enregistrer = useMutation({
    mutationFn: () =>
      enregistrerBudget(Number(examenId), {
        rubrique: (ligne as BudgetLigne).rubrique,
        montantPrevu: Number(prevu) || 0,
        montantEngage: Number(engage) || 0,
        montantPaye: Number(paye) || 0,
        observation: observation.trim() || null,
      }),
    onSuccess: () => {
      setLigne(null)
      void queryClient.invalidateQueries({ queryKey: ['budget'] })
      void queryClient.invalidateQueries({ queryKey: ['indicateurs-dgecs'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const modifier = (l: BudgetLigne) => {
    setLigne(l)
    setPrevu(l.prevu)
    setEngage(l.engage)
    setPaye(l.paye)
    setObservation(l.observation ?? '')
  }
  const i = indicateurs.data
  const b = budget.data

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={3}>Indicateurs de performance et budget</Title>
          <Text c="dimmed" size="sm">Tableau de bord du directeur général pour l'année choisie.</Text>
        </div>
        <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
      </Group>
      {indicateurs.isLoading && <Loader />}
      {indicateurs.isError && <Alert color="red">Indicateurs indisponibles.</Alert>}
      {i && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
          <CarteChiffre libelle="Listes de candidats validées" valeur={pourcent(i.tauxListesValidees)} icone="notes" />
          <CarteChiffre libelle="Dossiers complets" valeur={pourcent(i.tauxDossiersCompletes)} icone="eleves" />
          <CarteChiffre libelle="Centres prêts" valeur={pourcent(i.tauxCentresPrets)} icone="classes" couleur="green" />
          <CarteChiffre libelle="Postes de surveillance pourvus" valeur={pourcent(i.tauxPostesPourvus)} icone="eleves" />
          <CarteChiffre libelle="Taux de réussite" valeur={pourcent(i.tauxReussite)} icone="resultats" couleur="green" detail={`${nombre(i.candidats)} candidats, ${nombre(i.examens)} examens`} />
          <CarteChiffre libelle="Incidents pour 10 000 candidats" valeur={i.incidentsPour10000 === null ? '—' : String(i.incidentsPour10000).replace('.', ',')} icone="alerte" couleur="orange" />
          <CarteChiffre libelle="Délai fin des épreuves → proclamation" valeur={i.delaiProclamationJours === null ? '—' : `${i.delaiProclamationJours} j`} icone="presences" detail="moyenne des examens proclamés" />
          <CarteChiffre libelle="Réclamations traitées dans le délai" valeur={pourcent(i.tauxReclamationsDansLeDelai)} icone="alerte" />
          <CarteChiffre libelle="Diplômes délivrés (admis)" valeur={pourcent(i.tauxDiplomesDelivres)} icone="resultats" />
          <CarteChiffre libelle="Exécution du budget" valeur={pourcent(i.tauxExecutionBudget)} icone="notes" detail="engagé / prévu" />
          <CarteChiffre libelle="Coût par candidat" valeur={gnf(i.coutParCandidat)} icone="eleves" detail="montants engagés" />
        </SimpleGrid>
      )}

      <Title order={4}>Budget d'un examen</Title>
      <Select aria-label="Examen" placeholder="Choisir un examen" data={(examens.data ?? []).map((x) => ({ value: String(x.id), label: x.libelle }))}
        value={examenId} onChange={setExamenId} w={320} />
      {budget.isError && <Alert color="red">Budget indisponible.</Alert>}
      {b && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Prévu" valeur={gnf(b.total.prevu)} icone="notes" />
            <CarteChiffre libelle="Engagé" valeur={gnf(b.total.engage)} icone="notes" detail={`${pourcent(b.tauxEngagement)} du prévu`} />
            <CarteChiffre libelle="Payé" valeur={gnf(b.total.paye)} icone="notes" couleur="green" detail={`${pourcent(b.tauxPaiement)} de l'engagé`} />
            <CarteChiffre libelle="Coût par candidat" valeur={gnf(b.coutParCandidat)} icone="eleves" detail={`${gnf(b.coutParCentre)} par centre`} />
          </SimpleGrid>
          <Table.ScrollContainer minWidth={640}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Rubrique</Table.Th><Table.Th ta="right">Prévu</Table.Th><Table.Th ta="right">Engagé</Table.Th><Table.Th ta="right">Payé</Table.Th>
                  <Table.Th ta="right">Écart (engagé − prévu)</Table.Th><Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {b.lignes.map((l) => (
                  <Table.Tr key={l.rubrique}>
                    <Table.Td>{RUBRIQUES[l.rubrique] ?? l.rubrique}</Table.Td>
                    <Table.Td ta="right">{nombre(l.prevu)}</Table.Td>
                    <Table.Td ta="right">{nombre(l.engage)}</Table.Td>
                    <Table.Td ta="right">{nombre(l.paye)}</Table.Td>
                    <Table.Td ta="right"><Text span c={l.ecart > 0 ? 'red' : undefined}>{nombre(l.ecart)}</Text></Table.Td>
                    <Table.Td>{dgecs && <Button size="xs" variant="light" onClick={() => modifier(l)}>Modifier</Button>}</Table.Td>
                  </Table.Tr>
                ))}
                <Table.Tr fw={700}>
                  <Table.Td>Total</Table.Td>
                  <Table.Td ta="right">{nombre(b.total.prevu)}</Table.Td>
                  <Table.Td ta="right">{nombre(b.total.engage)}</Table.Td>
                  <Table.Td ta="right">{nombre(b.total.paye)}</Table.Td>
                  <Table.Td ta="right">{nombre(b.total.ecart)}</Table.Td>
                  <Table.Td />
                </Table.Tr>
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </>
      )}
      <Modal opened={ligne !== null} onClose={() => setLigne(null)} title={ligne ? (RUBRIQUES[ligne.rubrique] ?? ligne.rubrique) : ''}>
        <Stack>
          <NumberInput label="Montant prévu (GNF)" min={0} thousandSeparator=" " value={prevu} onChange={setPrevu} />
          <NumberInput label="Montant engagé (GNF)" min={0} thousandSeparator=" " value={engage} onChange={setEngage} />
          <NumberInput label="Montant payé (GNF)" min={0} thousandSeparator=" " value={paye} onChange={setPaye}
            error={Number(paye) > Number(engage) ? 'Le payé ne peut pas dépasser l\'engagé' : undefined} />
          <TextInput label="Observation" value={observation} onChange={(e) => setObservation(e.currentTarget.value)} maxLength={500} />
          <Button loading={enregistrer.isPending} disabled={Number(paye) > Number(engage)} onClick={() => enregistrer.mutate()}>Enregistrer</Button>
        </Stack>
      </Modal>
      <VerificationsDiplomes />
    </Stack>
  )
}
