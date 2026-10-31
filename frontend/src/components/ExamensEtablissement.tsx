import { useState } from 'react'
import { Alert, Badge, Button, Group, Select, Stack, Table, Text, MultiSelect } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  affecterCentreExamen,
  centresExamen,
  epreuvesExamen,
  examensEtablissement,
  inscrireCandidats,
  preparationExamen,
  retirerCandidat,
  telechargerConvocations,
  telechargerDiplomes,
  telechargerReleves,
} from '../api/endpoints'
import type { ExamenEtablissement } from '../api/types'
import { libelleOrganisation } from '../utils/organisationExamen'
import CandidatLibreForm from './CandidatLibreForm'
import DossiersCandidaturesModal from './DossiersCandidaturesModal'
import ListeCandidatsPanneau from './ListeCandidatsPanneau'

interface Props {
  etablissementId: number
  anneeScolaireId: number
  /** Ministère et direction régionale : gèrent les centres et les attribuent ; la direction inscrit les candidats. */
  supervision: boolean
}

/** Examens de l'année : inscription des candidats, centre d'examen et convocations. */
export default function ExamensEtablissement({ etablissementId, anneeScolaireId, supervision }: Props) {
  const liste = useQuery({
    queryKey: ['examens-etablissement', etablissementId, anneeScolaireId],
    queryFn: () => examensEtablissement(etablissementId, anneeScolaireId),
  })
  const [choisi, setChoisi] = useState<number | null>(null)

  if (liste.isError) {
    return <Alert color="red" variant="light">{liste.error.message}</Alert>
  }
  if (!liste.data || liste.data.length === 0) {
    return <Text c="dimmed" size="sm">Aucun examen n'est défini pour cette année.</Text>
  }
  return (
    <Stack gap="md">
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Examen</Table.Th>
            <Table.Th>Organisé par</Table.Th>
            <Table.Th>Candidats</Table.Th>
            <Table.Th />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {liste.data.map((e: ExamenEtablissement) => (
            <Table.Tr key={e.examen.id}>
              <Table.Td>{e.examen.libelle}</Table.Td>
              <Table.Td>{libelleOrganisation(e.examen.niveauOrganisation)}</Table.Td>
              <Table.Td><Badge variant="light">{e.candidats}</Badge></Table.Td>
              <Table.Td>
                <Button size="xs" variant="light" onClick={() => setChoisi(choisi === e.examen.id ? null : e.examen.id)}>
                  {choisi === e.examen.id ? 'Fermer' : supervision ? 'Centres et candidats' : 'Candidats'}
                </Button>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      {choisi !== null && (
        <DetailExamen etablissementId={etablissementId} examenId={choisi} anneeScolaireId={anneeScolaireId} supervision={supervision} />
      )}
    </Stack>
  )
}

function DetailExamen({ etablissementId, examenId, anneeScolaireId, supervision }: Props & { examenId: number }) {
  const queryClient = useQueryClient()
  const preparation = useQuery({
    queryKey: ['preparation-examen', etablissementId, examenId],
    queryFn: () => preparationExamen(etablissementId, examenId),
  })
  const [selection, setSelection] = useState<string[]>([])
  const [dossiersOuverts, setDossiersOuverts] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  const rafraichir = () => {
    void queryClient.invalidateQueries({ queryKey: ['preparation-examen', etablissementId, examenId] })
    void queryClient.invalidateQueries({ queryKey: ['examens-etablissement', etablissementId, anneeScolaireId] })
  }

  const inscription = useMutation({
    mutationFn: () => inscrireCandidats(etablissementId, examenId, selection.map(Number)),
    onSuccess: () => {
      rafraichir()
      setSelection([])
      setErreur(null)
      notifications.show({ color: 'green', title: 'Candidats inscrits', message: 'Les numéros de candidat ont été attribués.' })
    },
    onError: (e) => setErreur(e.message),
  })
  const retrait = useMutation({
    mutationFn: (candidatId: number) => retirerCandidat(candidatId),
    onSuccess: rafraichir,
    onError: (e) => notifications.show({ color: 'red', title: 'Retrait impossible', message: e.message }),
  })
  const convocations = useMutation({
    mutationFn: () => telechargerConvocations(etablissementId, examenId),
    onError: (e) => notifications.show({ color: 'red', title: 'Convocations indisponibles', message: e.message }),
  })

  const diplomes = useMutation({
    mutationFn: () => telechargerDiplomes(etablissementId, examenId),
    onError: (e) => notifications.show({ color: 'red', title: 'Diplômes indisponibles', message: e.message }),
  })

  const releves = useMutation({
    mutationFn: () => telechargerReleves(etablissementId, examenId),
    onError: (e) => notifications.show({ color: 'red', title: 'Relevés indisponibles', message: e.message }),
  })

  if (!preparation.data) {
    return <Text c="dimmed" size="sm">Chargement…</Text>
  }
  const { candidats, possibles } = preparation.data

  return (
    <Stack gap="md">
      <Calendrier examenId={examenId} dateDebut={preparation.data.examen.dateDebut} dateFin={preparation.data.examen.dateFin} />
      <ListeCandidatsPanneau etablissementId={etablissementId} examenId={examenId} supervision={supervision} />
      {supervision && <Centres etablissementId={etablissementId} examenId={examenId} onAffecte={rafraichir} />}
      {!supervision && (
        <Group align="flex-end">
          <MultiSelect
            style={{ flex: 1 }}
            label="Élèves à inscrire"
            placeholder={possibles.length === 0 ? 'Aucun élève disponible' : 'Choisir des élèves'}
            searchable
            data={possibles.map((p) => ({
              value: String(p.eleveId),
              label: `${p.nom} — ${p.classe}${p.eligible ? '' : ` (non éligible : ${p.tauxAbsences ?? 0} % d'absences)`}`,
              disabled: !p.eligible,
            }))}
            value={selection}
            onChange={setSelection}
          />
          <Button disabled={selection.length === 0} loading={inscription.isPending} onClick={() => inscription.mutate()}>
            Inscrire
          </Button>
        </Group>
      )}
      {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
      {!supervision && ['REGION', 'NATIONAL'].includes(preparation.data.examen.niveauOrganisation) && preparation.data.examen.statutResultats === 'EN_COURS' && (
        <CandidatLibreForm etablissementId={etablissementId} examenId={examenId} onInscrit={rafraichir} />
      )}
      {candidats.length === 0 ? (
        <Text c="dimmed" size="sm">Aucun candidat inscrit.</Text>
      ) : (
        <>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>N° candidat</Table.Th>
                <Table.Th>Nom</Table.Th>
                <Table.Th>Classe</Table.Th>
                <Table.Th>Centre</Table.Th>
                <Table.Th>PV / salle / table</Table.Th>
                {!supervision && <Table.Th />}
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {candidats.map((c) => (
                <Table.Tr key={c.id}>
                  <Table.Td>{c.numero ?? '—'}</Table.Td>
                  <Table.Td>{c.nom}</Table.Td>
                  <Table.Td>{c.libre ? <Badge variant="light" color="grape">Candidat libre</Badge> : (c.classe ?? '—')}</Table.Td>
                  <Table.Td>{c.centre ?? <Text size="sm" c="dimmed">À attribuer</Text>}</Table.Td>
                  <Table.Td>{c.numeroPv ? `${c.numeroPv} · salle ${c.salle} · table ${c.table}` : '—'}</Table.Td>
                  {!supervision && (
                    <Table.Td>
                      <Button size="xs" color="red" variant="subtle" onClick={() => retrait.mutate(c.id)}>
                        Retirer
                      </Button>
                    </Table.Td>
                  )}
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
          <Group>
            <Button variant="light" loading={convocations.isPending} onClick={() => convocations.mutate()}>
              Télécharger les convocations (PDF)
            </Button>
            <Button variant="light" onClick={() => setDossiersOuverts(true)}>
              Dossiers de candidature
            </Button>
            {preparation.data.examen.statutResultats === 'PROCLAME' && (
              <Button variant="light" color="green" loading={releves.isPending} onClick={() => releves.mutate()}>
                Relevés de notes et attestations (PDF)
              </Button>
            )}
            {preparation.data.examen.statutResultats === 'PROCLAME' && (
              <Button variant="light" color="green" loading={diplomes.isPending} onClick={() => diplomes.mutate()}>
                Diplômes des admis (PDF)
              </Button>
            )}
          </Group>
        </>
      )}
      {dossiersOuverts && preparation.data && (
        <DossiersCandidaturesModal
          etablissementId={etablissementId}
          examenId={examenId}
          titre={preparation.data.examen.libelle}
          peutModifier={preparation.data.examen.statutResultats !== 'PROCLAME'}
          onFermer={() => setDossiersOuverts(false)}
        />
      )}
    </Stack>
  )
}

function Calendrier({ examenId, dateDebut, dateFin }: { examenId: number; dateDebut: string | null; dateFin: string | null }) {
  const epreuves = useQuery({ queryKey: ['epreuves', examenId], queryFn: () => epreuvesExamen(examenId) })
  const jour = (iso: string) => new Date(iso).toLocaleDateString('fr-FR')
  return (
    <Stack gap="xs">
      <Text fw={600} size="sm">Calendrier officiel</Text>
      <Text size="sm" c="dimmed">
        {dateDebut ? `Épreuves du ${jour(dateDebut)}${dateFin ? ` au ${jour(dateFin)}` : ''}.` : 'La période n\'est pas encore publiée.'}
      </Text>
      {epreuves.data && epreuves.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {epreuves.data.map((e) => (
              <Table.Tr key={e.id}>
                <Table.Td>{jour(e.date)}</Table.Td>
                <Table.Td>
                  {e.heureDebut.slice(0, 5)} – {e.heureFin.slice(0, 5)}
                </Table.Td>
                <Table.Td>{e.matiere}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}

/** Attribue un centre aux candidats de l'établissement (les centres se désignent depuis l'onglet Examens). */
function Centres({ etablissementId, examenId, onAffecte }: { etablissementId: number; examenId: number; onAffecte: () => void }) {
  const centres = useQuery({ queryKey: ['centres-examen', examenId], queryFn: () => centresExamen(examenId) })
  const [centreChoisi, setCentreChoisi] = useState<string | null>(null)
  const affectation = useMutation({
    mutationFn: () => affecterCentreExamen(examenId, etablissementId, Number(centreChoisi)),
    onSuccess: (nombre) => {
      onAffecte()
      notifications.show({ color: 'green', title: 'Centre attribué', message: `${nombre} candidat(s) affecté(s).` })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Attribution impossible', message: e.message }),
  })
  return (
    <Group align="flex-end">
      <Select
        label="Centre pour les candidats de cet établissement"
        placeholder={centres.data && centres.data.length === 0 ? 'Aucun centre désigné' : 'Choisir un centre'}
        data={(centres.data ?? []).map((c) => ({ value: String(c.id), label: c.nom }))}
        value={centreChoisi}
        onChange={setCentreChoisi}
      />
      <Button disabled={centreChoisi === null} loading={affectation.isPending} onClick={() => affectation.mutate()}>
        Attribuer
      </Button>
    </Group>
  )
}
