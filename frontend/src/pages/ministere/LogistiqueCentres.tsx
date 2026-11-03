import { Alert, Badge, Button, Group, Loader, Modal, NumberInput, Select, SimpleGrid, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { enregistrerLivraison, listerExamens, logistiqueExamen } from '../../api/endpoints'
import type { LogistiqueCentre } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'
import { useDroitsExamens } from '../../auth/useDroitsExamens'
import EtatCentres from './EtatCentres'
import BesoinsEpreuves from './BesoinsEpreuves'
import PersonnelNational from './PersonnelNational'
import TransportSujets from './TransportSujets'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const ARTICLES = [
  { value: 'SUJETS', label: 'Sujets' },
  { value: 'COPIES', label: 'Copies' },
  { value: 'ENVELOPPES', label: 'Enveloppes' },
  { value: 'FOURNITURES', label: 'Fournitures' },
]
const STATUTS = [
  { value: 'PREVUE', label: 'Prévue' },
  { value: 'EXPEDIEE', label: 'Expédiée' },
  { value: 'RECUE', label: 'Reçue' },
]
const COULEUR: Record<string, string> = { RECUE: 'green', EXPEDIEE: 'blue', PREVUE: 'gray', NON_PREVUE: 'gray' }
const LIBELLE: Record<string, string> = { RECUE: 'Reçue', EXPEDIEE: 'Expédiée', PREVUE: 'Prévue', NON_PREVUE: '—' }

/** Préparation nationale des centres d'un examen : personnel requis et pourvu, fournitures livrées, centres prêts. */
export default function LogistiqueCentres() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const [examenId, setExamenId] = useState<string | null>(null)
  const [centre, setCentre] = useState<LogistiqueCentre | null>(null)
  const [article, setArticle] = useState<string | null>('SUJETS')
  const [prevue, setPrevue] = useState<number | string>(0)
  const [livree, setLivree] = useState<number | string>('')
  const [statut, setStatut] = useState<string | null>('PREVUE')
  const [observation, setObservation] = useState('')

  const examens = useQuery({ queryKey: ['examens', anneeId], queryFn: () => listerExamens(anneeId as number), enabled: anneeId !== null })
  const suivi = useQuery({ queryKey: ['logistique', examenId], queryFn: () => logistiqueExamen(Number(examenId)), enabled: examenId !== null })
  const enregistrer = useMutation({
    mutationFn: () =>
      enregistrerLivraison(Number(examenId), (centre as LogistiqueCentre).centreId, {
        article: article ?? 'SUJETS',
        quantitePrevue: Number(prevue) || 0,
        quantiteLivree: livree === '' ? null : Number(livree),
        statut: statut ?? 'PREVUE',
        observation: observation.trim() || null,
      }),
    onSuccess: () => {
      setCentre(null)
      void queryClient.invalidateQueries({ queryKey: ['logistique'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const r = suivi.data?.resume

  return (
    <Stack>
      <Group justify="space-between" align="end">
        <div>
          <Title order={3}>Logistique des centres</Title>
          <Text c="dimmed" size="sm">Personnel requis (président, chef de centre, secrétariat, 2 surveillants par salle) et fournitures livrées.</Text>
        </div>
        <Group align="end">
          <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={170} />
          <Select aria-label="Examen" placeholder="Choisir un examen" data={(examens.data ?? []).map((x) => ({ value: String(x.id), label: x.libelle }))}
            value={examenId} onChange={setExamenId} w={280} />
        </Group>
      </Group>
      {suivi.isLoading && <Loader />}
      {suivi.isError && <Alert color="red">Suivi indisponible.</Alert>}
      {r && suivi.data && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Centres prêts" valeur={pourcent(r.tauxPrets)} icone="classes" couleur="green" detail={`${nombre(r.prets)} sur ${nombre(r.centres)}`} />
            <CarteChiffre libelle="Postes de surveillance pourvus" valeur={pourcent(r.tauxPostesPourvus)} icone="eleves" detail={`${nombre(r.postesPourvus)} sur ${nombre(r.postesRequis)}`} />
            <CarteChiffre libelle="Centres approvisionnés" valeur={pourcent(r.tauxApprovisionnes)} icone="notes" detail={`${nombre(r.approvisionnes)} sur ${nombre(r.centres)}`} />
            <CarteChiffre libelle="Centres en retard" valeur={nombre(r.enRetard)} icone="alerte" couleur="red" detail="début des épreuves atteint, centre non prêt" />
          </SimpleGrid>
          <Table.ScrollContainer minWidth={900}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Centre</Table.Th><Table.Th>Zone</Table.Th><Table.Th ta="right">Salles</Table.Th><Table.Th ta="right">Personnel</Table.Th>
                  {ARTICLES.map((a) => <Table.Th key={a.value}>{a.label}</Table.Th>)}
                  <Table.Th>État</Table.Th><Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {suivi.data.centres.map((c) => (
                  <Table.Tr key={c.centreId}>
                    <Table.Td>{c.nom}</Table.Td>
                    <Table.Td>{c.zone ?? '—'}</Table.Td>
                    <Table.Td ta="right">{nombre(c.salles)}</Table.Td>
                    <Table.Td ta="right"><Text span c={c.personnelComplet ? 'green' : 'red'}>{c.postesPourvus}/{c.postesRequis}</Text></Table.Td>
                    {c.livraisons.map((l) => (
                      <Table.Td key={l.article}>
                        <Badge color={COULEUR[l.statut]} variant="light">{LIBELLE[l.statut]}</Badge>
                        {l.ecart !== null && l.ecart !== 0 && <Text size="xs" c="red">{l.ecart > 0 ? '+' : ''}{l.ecart}</Text>}
                      </Table.Td>
                    ))}
                    <Table.Td><Badge color={c.pret ? 'green' : c.enRetard ? 'red' : 'orange'}>{c.pret ? 'Prêt' : c.enRetard ? 'En retard' : 'En préparation'}</Badge></Table.Td>
                    <Table.Td>{dgecs && <Button size="xs" variant="light" onClick={() => setCentre(c)}>Fournitures</Button>}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </>
      )}
      {examenId !== null && <PersonnelNational examenId={Number(examenId)} />}
      {examenId !== null && <BesoinsEpreuves examenId={Number(examenId)} />}
      {examenId !== null && suivi.data && <TransportSujets examenId={Number(examenId)} centres={suivi.data.centres.map((c) => ({ value: String(c.centreId), label: c.nom }))} />}
      <EtatCentres />
      <Modal opened={centre !== null} onClose={() => setCentre(null)} title={`Fournitures : ${centre?.nom ?? ''}`}>
        <Stack>
          <Select label="Article" data={ARTICLES} value={article} onChange={(v) => {
            setArticle(v)
            const l = centre?.livraisons.find((x) => x.article === v)
            setPrevue(l?.quantitePrevue ?? 0)
            setLivree(l?.quantiteLivree ?? '')
            setStatut(l && l.statut !== 'NON_PREVUE' ? l.statut : 'PREVUE')
          }} allowDeselect={false} />
          <NumberInput label="Quantité prévue" min={0} value={prevue} onChange={setPrevue} />
          <Select label="Statut" data={STATUTS} value={statut} onChange={setStatut} allowDeselect={false} />
          {statut === 'RECUE' && <NumberInput label="Quantité reçue (par défaut : la quantité prévue)" min={0} value={livree} onChange={setLivree} />}
          <TextInput label="Observation" value={observation} onChange={(e) => setObservation(e.currentTarget.value)} maxLength={500} />
          <Button loading={enregistrer.isPending} onClick={() => enregistrer.mutate()}>Enregistrer</Button>
        </Stack>
      </Modal>
    </Stack>
  )
}
