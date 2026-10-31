import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, Select, Stack, Table, Text, TextInput, Textarea, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  annulerRendezVous,
  demanderRendezVous,
  demanderSortie,
  enseignantsDeLEleve,
  rendezVousEleve,
  sortiesEleve,
} from '../../api/endpoints'
import { aujourdhui, formaterDate } from '../../utils/dates'

const COULEURS: Record<string, string> = { DEMANDE: 'blue', DEMANDEE: 'blue', CONFIRME: 'green', ACCEPTEE: 'green', REFUSE: 'red', REFUSEE: 'red', ANNULE: 'gray' }
const LIBELLES: Record<string, string> = {
  DEMANDE: 'En attente', DEMANDEE: 'En attente', CONFIRME: 'Confirmé', ACCEPTEE: 'Accordée', REFUSE: 'Refusé', REFUSEE: 'Refusée', ANNULE: 'Annulé',
}
const formaterDateHeure = (iso: string) => `${formaterDate(iso.slice(0, 10))} à ${iso.slice(11, 16).replace(':', ' h ')}`

/** Rendez-vous avec les enseignants et demandes de sortie anticipée. */
export default function OngletLiaison({ eleveId }: { eleveId: number }) {
  return (
    <Stack gap="xl">
      <RendezVousPanneau eleveId={eleveId} />
      <SortiesPanneau eleveId={eleveId} />
    </Stack>
  )
}

function RendezVousPanneau({ eleveId }: { eleveId: number }) {
  const queryClient = useQueryClient()
  const enseignants = useQuery({ queryKey: ['enseignants-eleve', eleveId], queryFn: () => enseignantsDeLEleve(eleveId) })
  const rendezVous = useQuery({ queryKey: ['rendez-vous', eleveId], queryFn: () => rendezVousEleve(eleveId) })
  const [enseignant, setEnseignant] = useState<string | null>(null)
  const [jour, setJour] = useState('')
  const [heure, setHeure] = useState('')
  const [motif, setMotif] = useState('')
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['rendez-vous', eleveId] })

  const demande = useMutation({
    mutationFn: () => demanderRendezVous(eleveId, { enseignantId: Number(enseignant), dateHeure: `${jour}T${heure}`, motif: motif.trim() }),
    onSuccess: () => {
      rafraichir()
      setMotif('')
      notifications.show({ color: 'green', title: 'Demande envoyée', message: "L'enseignant va confirmer ou proposer une autre date." })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Envoi impossible', message: e.message }),
  })
  const annulation = useMutation({ mutationFn: annulerRendezVous, onSuccess: rafraichir })

  return (
    <Stack>
      <Title order={4}>Rendez-vous avec un enseignant</Title>
      <Card withBorder padding="md">
        <Stack gap="xs">
          <Group align="flex-end">
            <Select
              label="Enseignant"
              data={(enseignants.data ?? []).map((e) => ({ value: String(e.enseignantId), label: `${e.nom} (${e.matieres.join(', ')})` }))}
              value={enseignant}
              onChange={setEnseignant}
              miw={260}
            />
            <TextInput label="Jour" type="date" min={aujourdhui()} value={jour} onChange={(e) => setJour(e.currentTarget.value)} />
            <TextInput label="Heure" type="time" value={heure} onChange={(e) => setHeure(e.currentTarget.value)} />
          </Group>
          <Textarea label="Motif" placeholder="Résultats, comportement, orientation…" maxLength={500} autosize minRows={2} value={motif} onChange={(e) => setMotif(e.currentTarget.value)} />
          <Group justify="flex-end">
            <Button disabled={enseignant === null || jour === '' || heure === '' || motif.trim() === ''} loading={demande.isPending} onClick={() => demande.mutate()}>
              Demander un rendez-vous
            </Button>
          </Group>
        </Stack>
      </Card>
      {rendezVous.isPending && <Loader />}
      {rendezVous.isError && <Alert color="red" variant="light">{rendezVous.error.message}</Alert>}
      {rendezVous.data && rendezVous.data.length === 0 && <Text c="dimmed">Aucun rendez-vous.</Text>}
      {rendezVous.data && rendezVous.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {rendezVous.data.map((r) => (
              <Table.Tr key={r.id}>
                <Table.Td>
                  {formaterDateHeure(r.dateHeure)}
                  <Text size="xs" c="dimmed">{r.enseignant}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{r.motif}</Text>
                  {r.reponse && <Text size="xs" c="dimmed">Réponse : {r.reponse}</Text>}
                </Table.Td>
                <Table.Td><Badge color={COULEURS[r.statut]} variant="light">{LIBELLES[r.statut]}</Badge></Table.Td>
                <Table.Td>
                  {(r.statut === 'DEMANDE' || r.statut === 'CONFIRME') && (
                    <Button size="compact-xs" variant="subtle" color="red" onClick={() => annulation.mutate(r.id)}>Annuler</Button>
                  )}
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}

function SortiesPanneau({ eleveId }: { eleveId: number }) {
  const queryClient = useQueryClient()
  const sorties = useQuery({ queryKey: ['sorties', eleveId], queryFn: () => sortiesEleve(eleveId) })
  const [jour, setJour] = useState(aujourdhui())
  const [heure, setHeure] = useState('')
  const [motif, setMotif] = useState('')
  const [accompagnateur, setAccompagnateur] = useState('')

  const demande = useMutation({
    mutationFn: () => demanderSortie(eleveId, { dateSortie: jour, heureSortie: heure, motif: motif.trim(), accompagnateur: accompagnateur.trim() || null }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['sorties', eleveId] })
      setMotif('')
      setAccompagnateur('')
      notifications.show({ color: 'green', title: 'Demande envoyée', message: "La direction de l'école va répondre." })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Envoi impossible', message: e.message }),
  })

  return (
    <Stack>
      <Title order={4}>Autorisation de sortie</Title>
      <Card withBorder padding="md">
        <Stack gap="xs">
          <Group align="flex-end">
            <TextInput label="Jour" type="date" min={aujourdhui()} value={jour} onChange={(e) => setJour(e.currentTarget.value)} />
            <TextInput label="Heure de sortie" type="time" value={heure} onChange={(e) => setHeure(e.currentTarget.value)} />
            <TextInput label="Personne qui vient le chercher" maxLength={150} value={accompagnateur} onChange={(e) => setAccompagnateur(e.currentTarget.value)} />
          </Group>
          <Textarea label="Motif" maxLength={500} autosize minRows={2} value={motif} onChange={(e) => setMotif(e.currentTarget.value)} />
          <Group justify="flex-end">
            <Button disabled={jour === '' || heure === '' || motif.trim() === ''} loading={demande.isPending} onClick={() => demande.mutate()}>
              Demander une autorisation
            </Button>
          </Group>
        </Stack>
      </Card>
      {sorties.data && sorties.data.length === 0 && <Text c="dimmed">Aucune demande de sortie.</Text>}
      {sorties.data && sorties.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {sorties.data.map((s) => (
              <Table.Tr key={s.id}>
                <Table.Td>{formaterDate(s.dateSortie)} à {s.heureSortie.slice(0, 5).replace(':', ' h ')}</Table.Td>
                <Table.Td>
                  <Text size="sm">{s.motif}</Text>
                  {s.reponse && <Text size="xs" c="dimmed">Réponse : {s.reponse}</Text>}
                </Table.Td>
                <Table.Td><Badge color={COULEURS[s.statut]} variant="light">{LIBELLES[s.statut]}</Badge></Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}
