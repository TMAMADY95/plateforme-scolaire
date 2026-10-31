import { useState } from 'react'
import { Alert, Badge, Button, Group, Modal, Select, Stack, Table, Tabs, Text, TextInput, Textarea } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  creerInspection,
  creerSignalement,
  listerInspections,
  listerSignalements,
  modifierAutorisation,
  resoudreSignalement,
} from '../api/endpoints'
import PiecesJointesInspection from './PiecesJointesInspection'
import ExamensEtablissement from './ExamensEtablissement'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import PlansActionEtablissement from './PlansActionEtablissement'
import type { CategorieSecurite, GraviteSecurite, LigneConformite, StatutAutorisation } from '../api/types'

export const LIBELLES_AUTORISATION: Record<StatutAutorisation, string> = {
  EN_ATTENTE: 'En attente',
  AUTORISE: 'Autorisé',
  NON_AUTORISE: 'Non autorisé',
  RETIRE: 'Autorisation retirée',
}
const CATEGORIES: { value: CategorieSecurite; label: string }[] = [
  { value: 'INCENDIE', label: 'Incendie' },
  { value: 'BATIMENT', label: 'Bâtiment' },
  { value: 'EAU_HYGIENE', label: 'Eau et hygiène' },
  { value: 'CLOTURE', label: 'Clôture et accès' },
  { value: 'AUTRE', label: 'Autre' },
]
const GRAVITES: { value: GraviteSecurite; label: string }[] = [
  { value: 'FAIBLE', label: 'Faible' },
  { value: 'MOYENNE', label: 'Moyenne' },
  { value: 'ELEVEE', label: 'Élevée' },
]
const COULEUR_GRAVITE: Record<GraviteSecurite, string> = { FAIBLE: 'gray', MOYENNE: 'orange', ELEVEE: 'red' }
const jour = (iso: string) => new Date(iso).toLocaleDateString('fr-FR')
const aujourdhui = () => new Date().toISOString().slice(0, 10)

interface Props {
  etablissement: LigneConformite
  /** Le ministère décide de l'autorisation ; la direction régionale n'a que les inspections et la sécurité. */
  peutAutoriser: boolean
  onFermer: () => void
}

/** Inspections, problèmes de sécurité et autorisation d'un établissement. */
export default function SuiviEtablissementModal({ etablissement, peutAutoriser, onFermer }: Props) {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? undefined : Number(annee.anneeId)
  return (
    <Modal opened onClose={onFermer} size="xl" title={etablissement.nom}>
      <Stack>
        {etablissement.motifsUrgence.length > 0 && (
          <Alert color={etablissement.urgent ? 'red' : 'yellow'} variant="light" title={etablissement.urgent ? 'Inspection urgente' : 'Points d\'attention'}>
            {etablissement.motifsUrgence.join(' · ')}
          </Alert>
        )}
        <Tabs defaultValue="inspections">
          <Tabs.List>
            <Tabs.Tab value="inspections">Inspections</Tabs.Tab>
            <Tabs.Tab value="securite">Sécurité</Tabs.Tab>
            <Tabs.Tab value="plans">Plan d'action</Tabs.Tab>
            <Tabs.Tab value="examens">Examens</Tabs.Tab>
            <Tabs.Tab value="autorisation">Autorisation</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="inspections" pt="md">
            <Inspections etablissementId={etablissement.id} />
          </Tabs.Panel>
          <Tabs.Panel value="securite" pt="md">
            <Securite etablissementId={etablissement.id} />
          </Tabs.Panel>
          <Tabs.Panel value="plans" pt="md">
            <PlansActionEtablissement etablissementId={etablissement.id} supervision />
          </Tabs.Panel>
          <Tabs.Panel value="examens" pt="md">
            {anneeId !== undefined ? (
              <ExamensEtablissement etablissementId={etablissement.id} anneeScolaireId={anneeId} supervision />
            ) : (
              <Text c="dimmed" size="sm">Aucune année scolaire.</Text>
            )}
          </Tabs.Panel>
          <Tabs.Panel value="autorisation" pt="md">
            <Autorisation etablissement={etablissement} modifiable={peutAutoriser} />
          </Tabs.Panel>
        </Tabs>
      </Stack>
    </Modal>
  )
}

/** Rafraîchit le tableau de bord après chaque saisie. */
function useRafraichir() {
  const queryClient = useQueryClient()
  return () => void queryClient.invalidateQueries({ queryKey: ['conformite'] })
}

function Inspections({ etablissementId }: { etablissementId: number }) {
  const rafraichir = useRafraichir()
  const queryClient = useQueryClient()
  const liste = useQuery({ queryKey: ['inspections', etablissementId], queryFn: () => listerInspections(etablissementId) })
  const [date, setDate] = useState(aujourdhui())
  const [resultat, setResultat] = useState<string | null>('CONFORME')
  const [observations, setObservations] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)

  const creation = useMutation({
    mutationFn: () =>
      creerInspection(etablissementId, {
        dateInspection: date,
        resultat: resultat as 'CONFORME' | 'NON_CONFORME',
        observations: observations.trim() === '' ? null : observations.trim(),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['inspections', etablissementId] })
      rafraichir()
      setObservations('')
      setErreur(null)
      notifications.show({ color: 'green', title: 'Inspection enregistrée', message: 'Le tableau de bord est à jour.' })
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Stack>
      {liste.data && liste.data.length === 0 && <Text c="dimmed">Aucune inspection enregistrée.</Text>}
      {liste.data && liste.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {liste.data.map((i) => (
              <Table.Tr key={i.id}>
                <Table.Td>{jour(i.dateInspection)}</Table.Td>
                <Table.Td>
                  <Badge color={i.resultat === 'CONFORME' ? 'green' : 'red'} variant="light">
                    {i.resultat === 'CONFORME' ? 'Conforme' : 'Non conforme'}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{i.observations ?? '—'}</Text>
                  <Text size="xs" c="dimmed">
                    {i.inspecteur}
                  </Text>
                  <PiecesJointesInspection inspectionId={i.id} modifiable />
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
      <Text fw={600} size="sm">
        Enregistrer une inspection
      </Text>
      <Group grow align="flex-start">
        <TextInput label="Date" type="date" max={aujourdhui()} value={date} onChange={(e) => setDate(e.currentTarget.value)} />
        <Select
          label="Résultat"
          data={[
            { value: 'CONFORME', label: 'Conforme' },
            { value: 'NON_CONFORME', label: 'Non conforme' },
          ]}
          value={resultat}
          onChange={setResultat}
          allowDeselect={false}
        />
      </Group>
      <Textarea label="Observations" autosize minRows={2} value={observations} onChange={(e) => setObservations(e.currentTarget.value)} />
      {erreur && (
        <Alert color="red" variant="light">
          {erreur}
        </Alert>
      )}
      <Group justify="flex-end">
        <Button onClick={() => creation.mutate()} loading={creation.isPending} disabled={date === '' || resultat === null}>
          Enregistrer
        </Button>
      </Group>
    </Stack>
  )
}

function Securite({ etablissementId }: { etablissementId: number }) {
  const rafraichir = useRafraichir()
  const queryClient = useQueryClient()
  const liste = useQuery({ queryKey: ['signalements', etablissementId], queryFn: () => listerSignalements(etablissementId) })
  const [categorie, setCategorie] = useState<string | null>('INCENDIE')
  const [gravite, setGravite] = useState<string | null>('MOYENNE')
  const [description, setDescription] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)

  const apres = () => {
    void queryClient.invalidateQueries({ queryKey: ['signalements', etablissementId] })
    rafraichir()
  }
  const creation = useMutation({
    mutationFn: () =>
      creerSignalement(etablissementId, {
        categorie: categorie as CategorieSecurite,
        gravite: gravite as GraviteSecurite,
        description: description.trim(),
      }),
    onSuccess: () => {
      apres()
      setDescription('')
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })
  const resolution = useMutation({
    mutationFn: resoudreSignalement,
    onSuccess: apres,
    onError: (e) => setErreur(e.message),
  })

  return (
    <Stack>
      {liste.data && liste.data.length === 0 && <Text c="dimmed">Aucun problème de sécurité signalé.</Text>}
      {liste.data && liste.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {liste.data.map((s) => (
              <Table.Tr key={s.id} opacity={s.statut === 'RESOLU' ? 0.6 : 1}>
                <Table.Td>
                  <Badge color={COULEUR_GRAVITE[s.gravite]} variant="light">
                    {GRAVITES.find((g) => g.value === s.gravite)?.label}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Text size="sm" fw={600}>
                    {CATEGORIES.find((c) => c.value === s.categorie)?.label}
                  </Text>
                  <Text size="sm">{s.description}</Text>
                  <Text size="xs" c="dimmed">
                    {s.signalePar} · {jour(s.createdAt)}
                    {s.resoluAt ? ` · résolu le ${jour(s.resoluAt)}` : ''}
                  </Text>
                </Table.Td>
                <Table.Td>
                  {s.statut === 'OUVERT' ? (
                    <Button size="compact-xs" variant="light" color="green" loading={resolution.isPending} onClick={() => resolution.mutate(s.id)}>
                      Marquer résolu
                    </Button>
                  ) : (
                    <Badge color="green" variant="outline">
                      Résolu
                    </Badge>
                  )}
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
      <Text fw={600} size="sm">
        Signaler un problème
      </Text>
      <Group grow align="flex-start">
        <Select label="Catégorie" data={CATEGORIES} value={categorie} onChange={setCategorie} allowDeselect={false} />
        <Select label="Gravité" data={GRAVITES} value={gravite} onChange={setGravite} allowDeselect={false} />
      </Group>
      <Textarea label="Description" autosize minRows={2} value={description} onChange={(e) => setDescription(e.currentTarget.value)} />
      {erreur && (
        <Alert color="red" variant="light">
          {erreur}
        </Alert>
      )}
      <Group justify="flex-end">
        <Button onClick={() => creation.mutate()} loading={creation.isPending} disabled={description.trim() === '' || !categorie || !gravite}>
          Signaler
        </Button>
      </Group>
    </Stack>
  )
}

function Autorisation({ etablissement, modifiable }: { etablissement: LigneConformite; modifiable: boolean }) {
  const rafraichir = useRafraichir()
  const [statut, setStatut] = useState<string | null>(etablissement.autorisation)
  const [reference, setReference] = useState(etablissement.reference ?? '')
  const [date, setDate] = useState('')
  const [motif, setMotif] = useState(etablissement.motifStatut ?? '')
  const [erreur, setErreur] = useState<string | null>(null)

  const modification = useMutation({
    mutationFn: () =>
      modifierAutorisation(etablissement.id, {
        statut: statut as StatutAutorisation,
        reference: reference.trim() === '' ? null : reference.trim(),
        dateAutorisation: date === '' ? null : date,
        motif: motif.trim() === '' ? null : motif.trim(),
      }),
    onSuccess: () => {
      rafraichir()
      setErreur(null)
      notifications.show({ color: 'green', title: 'Autorisation modifiée', message: etablissement.nom })
    },
    onError: (e) => setErreur(e.message),
  })

  if (!modifiable) {
    return (
      <Stack gap="xs">
        <Group>
          <Badge variant="light">{LIBELLES_AUTORISATION[etablissement.autorisation]}</Badge>
          {etablissement.reference && <Text size="sm">Arrêté : {etablissement.reference}</Text>}
        </Group>
        {etablissement.motifStatut && <Text size="sm">{etablissement.motifStatut}</Text>}
        <Text size="xs" c="dimmed">
          La décision d'autorisation appartient au ministère.
        </Text>
      </Stack>
    )
  }

  return (
    <Stack>
      <Select
        label="Statut d'autorisation"
        data={(Object.keys(LIBELLES_AUTORISATION) as StatutAutorisation[]).map((v) => ({ value: v, label: LIBELLES_AUTORISATION[v] }))}
        value={statut}
        onChange={setStatut}
        allowDeselect={false}
      />
      <Group grow align="flex-start">
        <TextInput label="Référence de l'arrêté" value={reference} onChange={(e) => setReference(e.currentTarget.value)} />
        <TextInput label="Date de l'arrêté" type="date" max={aujourdhui()} value={date} onChange={(e) => setDate(e.currentTarget.value)} />
      </Group>
      <Textarea
        label="Motif"
        description="Obligatoire pour un refus ou un retrait d'autorisation."
        autosize
        minRows={2}
        value={motif}
        onChange={(e) => setMotif(e.currentTarget.value)}
      />
      {erreur && (
        <Alert color="red" variant="light">
          {erreur}
        </Alert>
      )}
      <Group justify="flex-end">
        <Button onClick={() => modification.mutate()} loading={modification.isPending} disabled={!statut}>
          Enregistrer
        </Button>
      </Group>
    </Stack>
  )
}
