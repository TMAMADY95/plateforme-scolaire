import { useState } from 'react'
import { Alert, Badge, Button, Group, Modal, NumberInput, Select, Stack, Table, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { consoliderResultats, notesCandidats, saisirNotesCandidat } from '../api/endpoints'
import type { NotesCandidat, NotesCandidats, StatutCandidat } from '../api/types'

const STATUTS = [
  { value: 'INSCRIT', label: 'À saisir' },
  { value: 'PRESENT', label: 'Présent' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'ELIMINE', label: 'Éliminé (fraude)' },
]

/**
 * Notes individuelles des candidats d'un établissement : statut, note de chaque épreuve, moyenne pondérée par les
 * coefficients et décision proposée. « Établir les résultats » en déduit les chiffres de l'établissement.
 */
export default function NotesCandidatsModal({ examenId, etablissementId, nom, onFermer }: {
  examenId: number
  etablissementId: number
  nom: string
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const cle = ['notes-candidats', examenId, etablissementId]
  const donnees = useQuery({ queryKey: cle, queryFn: () => notesCandidats(examenId, etablissementId) })
  const consolidation = useMutation({
    mutationFn: () => consoliderResultats(examenId, etablissementId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rapport-examen'] })
      notifications.show({ color: 'green', title: 'Résultats établis', message: `Les résultats de ${nom} sont à jour.` })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Impossible d\'établir les résultats', message: e.message }),
  })
  const d = donnees.data
  const clos = d?.statutResultats === 'PROCLAME'
  const remplacer = (c: NotesCandidat) =>
    queryClient.setQueryData<NotesCandidats>(cle, (ancien) =>
      ancien ? { ...ancien, candidats: ancien.candidats.map((x) => (x.id === c.id ? c : x)) } : ancien)

  return (
    <Modal opened onClose={onFermer} size="95%" title={`Notes des candidats : ${nom}`}>
      <Stack gap="sm">
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && d.epreuves.length === 0 && (
          <Alert color="orange" variant="light">Aucune épreuve n'est publiée pour cet examen : ajoutez-les dans « Calendrier et centres ».</Alert>
        )}
        {clos && <Alert color="gray" variant="light">Les résultats sont proclamés : les notes ne sont plus modifiables.</Alert>}
        {d && d.candidats.length === 0 && <Text c="dimmed">Cet établissement n'a aucun candidat inscrit à cet examen.</Text>}
        {d && d.candidats.length > 0 && (
          <Table.ScrollContainer minWidth={700}>
            <Table verticalSpacing={4} highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Candidat</Table.Th>
                  <Table.Th>Statut</Table.Th>
                  {d.epreuves.map((e) => (
                    <Table.Th key={e.id}>{e.nom} <Text span size="xs" c="dimmed">(coef. {e.coefficient})</Text></Table.Th>
                  ))}
                  <Table.Th>Moyenne</Table.Th>
                  <Table.Th>Décision</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.candidats.map((c) => (
                  <LigneCandidat key={`${c.id}-${c.moyenne}-${c.statut}`} donnees={d} candidat={c} clos={clos}
                    examenId={examenId} etablissementId={etablissementId} onEnregistre={remplacer} />
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>Fermer</Button>
          <Button disabled={clos || !d || d.candidats.length === 0} loading={consolidation.isPending} onClick={() => consolidation.mutate()}>
            Établir les résultats de l'établissement
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

function LigneCandidat({ donnees, candidat, clos, examenId, etablissementId, onEnregistre }: {
  donnees: NotesCandidats
  candidat: NotesCandidat
  clos: boolean
  examenId: number
  etablissementId: number
  onEnregistre: (c: NotesCandidat) => void
}) {
  const [statut, setStatut] = useState<StatutCandidat>(candidat.statut)
  const [notes, setNotes] = useState<Record<string, number | string>>(
    Object.fromEntries(donnees.epreuves.map((e) => [String(e.id), candidat.notes[String(e.id)] ?? ''])))
  const [erreur, setErreur] = useState<string | null>(null)

  const enregistrement = useMutation({
    mutationFn: () => {
      const envoyees: Record<string, number> = {}
      for (const [k, v] of Object.entries(notes)) {
        if (v !== '') {
          envoyees[k] = Number(v)
        }
      }
      return saisirNotesCandidat(examenId, etablissementId, candidat.id, { statut, notes: envoyees })
    },
    onSuccess: (c) => {
      setErreur(null)
      onEnregistre(c)
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Table.Tr>
      <Table.Td>
        <Text size="sm" fw={600}>{candidat.nom}</Text>
        <Text size="xs" c="dimmed">{candidat.numeroPv ?? 'PV à attribuer'}</Text>
        {erreur && <Text size="xs" c="red">{erreur}</Text>}
      </Table.Td>
      <Table.Td>
        <Select size="xs" w={140} data={STATUTS} value={statut} allowDeselect={false} disabled={clos}
          onChange={(v) => v && setStatut(v as StatutCandidat)} />
      </Table.Td>
      {donnees.epreuves.map((e) => (
        <Table.Td key={e.id}>
          <NumberInput size="xs" w={80} min={0} max={20} decimalScale={2} disabled={clos || statut !== 'PRESENT'}
            value={notes[String(e.id)]} onChange={(v) => setNotes((x) => ({ ...x, [String(e.id)]: v }))} />
        </Table.Td>
      ))}
      <Table.Td>{candidat.moyenne === null ? '—' : String(candidat.moyenne).replace('.', ',')}</Table.Td>
      <Table.Td>
        {candidat.decision && (
          <Badge color={candidat.decision === 'ADMIS' ? 'green' : 'red'} variant="light">
            {candidat.decision === 'ADMIS' ? 'Admis' : 'Non admis'}
          </Badge>
        )}
      </Table.Td>
      <Table.Td>
        <Button size="compact-xs" variant="light" disabled={clos} loading={enregistrement.isPending} onClick={() => enregistrement.mutate()}>
          Enregistrer
        </Button>
      </Table.Td>
    </Table.Tr>
  )
}
