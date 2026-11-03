import { useState } from 'react'
import { Alert, Badge, Button, Checkbox, Group, Modal, Stack, Table, Text, TextInput } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { marquerPreparation, preparationCentre } from '../api/endpoints'
import type { PreparationCentre } from '../api/types'
import { useDroitsExamens } from '../auth/useDroitsExamens'

/** Préparation matérielle d'un centre : sept travaux à cocher ; le centre est « prêt » quand tout est fait. */
export default function PreparationCentreModal({ examenId, centre, onFermer }: { examenId: number; centre: { id: number; nom: string }; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const { terrain } = useDroitsExamens()
  const cle = ['preparation-centre', examenId, centre.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => preparationCentre(examenId, centre.id) })
  const [erreur, setErreur] = useState<string | null>(null)
  const appliquer = (p: PreparationCentre) => {
    queryClient.setQueryData(cle, p)
    void queryClient.invalidateQueries({ queryKey: ['preparation-centres', examenId] })
    setErreur(null)
  }
  const marquage = useMutation({
    mutationFn: (v: { element: string; fait: boolean; observation: string | null }) => marquerPreparation(examenId, centre.id, v),
    onSuccess: appliquer,
    onError: (e) => setErreur(e.message),
  })
  const d = donnees.data

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Préparation du centre : ${centre.nom}`}>
      <Stack gap="sm">
        <Text size="sm" c="dimmed">
          Avant les épreuves, le centre doit être préparé : salles, tables-bancs, affichage, secrétariat, sécurité, personnel et matériel.
          Maximum 30 candidats par salle, un candidat par table-banc.
        </Text>
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Group gap="xs">
              <Badge color={d.pret ? 'green' : 'orange'} variant="light">{d.pret ? 'Centre prêt' : 'Préparation en cours'}</Badge>
              <Text size="sm">{d.faits} travaux sur {d.total}</Text>
            </Group>
            <Table verticalSpacing="xs">
              <Table.Tbody>
                {d.elements.map((e) => (
                  <Ligne key={`${e.code}-${e.fait}-${e.observation}`} element={e} modifiable={terrain} enCours={marquage.isPending}
                    onChange={(fait, observation) => marquage.mutate({ element: e.code, fait, observation })} />
                ))}
              </Table.Tbody>
            </Table>
          </>
        )}
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
        <Group justify="flex-end"><Button variant="default" onClick={onFermer}>Fermer</Button></Group>
      </Stack>
    </Modal>
  )
}

function Ligne({ element: e, modifiable, enCours, onChange }: {
  element: PreparationCentre['elements'][number]
  modifiable: boolean
  enCours: boolean
  onChange: (fait: boolean, observation: string | null) => void
}) {
  const [observation, setObservation] = useState(e.observation ?? '')
  const envoyer = (fait: boolean) => onChange(fait, observation.trim() === '' ? null : observation.trim())
  return (
    <Table.Tr>
      <Table.Td w={40}>
        <Checkbox aria-label={e.libelle} checked={e.fait} disabled={!modifiable || enCours} onChange={(x) => envoyer(x.currentTarget.checked)} />
      </Table.Td>
      <Table.Td>
        <Text fw={600} size="sm">{e.libelle}</Text>
        <Text size="xs" c="dimmed">{e.travaux}{e.fait && e.faitPar ? ` · fait par ${e.faitPar}` : ''}</Text>
      </Table.Td>
      <Table.Td>
        <TextInput size="xs" placeholder="Observation" value={observation} disabled={!modifiable}
          onChange={(x) => setObservation(x.currentTarget.value)}
          onBlur={() => observation !== (e.observation ?? '') && envoyer(e.fait)} />
      </Table.Td>
    </Table.Tr>
  )
}
