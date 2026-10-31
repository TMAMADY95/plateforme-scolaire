import { useState } from 'react'
import { Alert, Badge, Button, Checkbox, Group, Modal, Select, Stack, Table, Text, Textarea, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { incidentsCentre, signalerIncident, traiterIncident } from '../api/endpoints'
import type { IncidentsCentre, TypeIncident } from '../api/types'

const TYPES: { value: TypeIncident; label: string }[] = [
  { value: 'FRAUDE', label: 'Fraude' },
  { value: 'RETARD', label: 'Retard' },
  { value: 'MALAISE', label: 'Malaise' },
  { value: 'PERTURBATION', label: 'Perturbation / discipline' },
  { value: 'FUITE_SUJET', label: 'Fuite de sujet' },
  { value: 'AUTRE', label: 'Autre' },
]
const LIBELLE = Object.fromEntries(TYPES.map((t) => [t.value, t.label]))

/** Incidents d'un centre : un candidat pris en fraude peut être éliminé (non admis à l'examen). */
export default function IncidentsCentreModal({ examenId, centre, onFermer }: { examenId: number; centre: { id: number; nom: string }; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const cle = ['incidents-centre', examenId, centre.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => incidentsCentre(examenId, centre.id) })
  const [type, setType] = useState<TypeIncident | null>('FRAUDE')
  const [candidat, setCandidat] = useState<string | null>(null)
  const [epreuve, setEpreuve] = useState<string | null>(null)
  const [description, setDescription] = useState('')
  const [signalePar, setSignalePar] = useState('')
  const [eliminer, setEliminer] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  const appliquer = (d: IncidentsCentre) => {
    queryClient.setQueryData(cle, d)
    void queryClient.invalidateQueries({ queryKey: ['notes-candidats'] })
    void queryClient.invalidateQueries({ queryKey: ['rapport-examen'] })
  }
  const signalement = useMutation({
    mutationFn: () =>
      signalerIncident(examenId, centre.id, {
        type: type as TypeIncident,
        candidatId: candidat === null ? null : Number(candidat),
        epreuveId: epreuve === null ? null : Number(epreuve),
        description: description.trim(),
        signalePar: signalePar.trim() === '' ? null : signalePar.trim(),
        eliminer: eliminer && type === 'FRAUDE' && candidat !== null,
      }),
    onSuccess: (d) => {
      appliquer(d)
      setDescription('')
      setEliminer(false)
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })
  const d = donnees.data

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Incidents : ${centre.nom}`}>
      <Stack gap="sm">
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Group gap="xs">
              {Object.entries(d.parType).map(([t, n]) => (
                <Badge key={t} variant="light" color={t === 'FRAUDE' || t === 'FUITE_SUJET' ? 'red' : 'gray'}>{LIBELLE[t] ?? t} : {n}</Badge>
              ))}
              {d.incidents.length === 0 && <Text size="sm" c="dimmed">Aucun incident signalé dans ce centre.</Text>}
            </Group>
            {d.incidents.length > 0 && (
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Incident</Table.Th>
                    <Table.Th>Candidat</Table.Th>
                    <Table.Th>Suites</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.incidents.map((i) => (
                    <LigneIncident key={`${i.id}-${i.statut}`} incident={i} examenId={examenId} centreId={centre.id} onChange={appliquer} />
                  ))}
                </Table.Tbody>
              </Table>
            )}
            <Text fw={600}>Signaler un incident</Text>
            <Group align="flex-end">
              <Select label="Type" data={TYPES} value={type} onChange={(v) => setType(v as TypeIncident | null)} allowDeselect={false} w={200} />
              <Select label="Candidat concerné" placeholder="Aucun" clearable searchable value={candidat} onChange={setCandidat} w={260}
                data={d.candidats.map((c) => ({ value: String(c.id), label: `${c.nom}${c.numeroPv ? ` (${c.numeroPv})` : ''}` }))} />
              <Select label="Épreuve" placeholder="Aucune" clearable value={epreuve} onChange={setEpreuve} w={180}
                data={d.epreuves.map((e) => ({ value: String(e.id), label: e.nom }))} />
            </Group>
            <Textarea label="Description" minRows={2} value={description} onChange={(e) => setDescription(e.currentTarget.value)} />
            <Group align="flex-end">
              <TextInput label="Signalé par" placeholder="Surveillant ou chef de centre" value={signalePar} onChange={(e) => setSignalePar(e.currentTarget.value)} w={260} />
              {type === 'FRAUDE' && candidat !== null && (
                <Checkbox label="Éliminer le candidat (non admis à l'examen)" checked={eliminer} onChange={(e) => setEliminer(e.currentTarget.checked)} />
              )}
              <Button disabled={type === null || description.trim() === ''} loading={signalement.isPending} onClick={() => signalement.mutate()}>
                Signaler
              </Button>
            </Group>
            {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
          </>
        )}
      </Stack>
    </Modal>
  )
}

function LigneIncident({ incident: i, examenId, centreId, onChange }: {
  incident: IncidentsCentre['incidents'][number]
  examenId: number
  centreId: number
  onChange: (d: IncidentsCentre) => void
}) {
  const [decision, setDecision] = useState('')
  const traitement = useMutation({
    mutationFn: () => traiterIncident(examenId, centreId, i.id, decision.trim()),
    onSuccess: onChange,
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })
  return (
    <Table.Tr>
      <Table.Td>
        <Group gap={6}>
          <Badge variant="light" color={i.type === 'FRAUDE' || i.type === 'FUITE_SUJET' ? 'red' : 'gray'}>{LIBELLE[i.type] ?? i.type}</Badge>
          {i.elimine && <Badge color="red">Éliminé</Badge>}
        </Group>
        <Text size="sm">{i.description}</Text>
        <Text size="xs" c="dimmed">
          {new Date(i.survenuLe).toLocaleString('fr-FR')}
          {i.epreuve ? ` · ${i.epreuve}` : ''}
          {i.signalePar ? ` · ${i.signalePar}` : ''}
        </Text>
      </Table.Td>
      <Table.Td>{i.candidat ? <><Text size="sm">{i.candidat}</Text><Text size="xs" c="dimmed">{i.numeroPv}</Text></> : '—'}</Table.Td>
      <Table.Td>
        {i.statut === 'TRAITE' ? (
          <Text size="sm">{i.decision}</Text>
        ) : (
          <Group gap={4} wrap="nowrap">
            <TextInput size="xs" placeholder="Décision prise" value={decision} onChange={(e) => setDecision(e.currentTarget.value)} />
            <Button size="compact-xs" variant="light" disabled={decision.trim() === ''} loading={traitement.isPending} onClick={() => traitement.mutate()}>
              Clore
            </Button>
          </Group>
        )}
      </Table.Td>
    </Table.Tr>
  )
}
