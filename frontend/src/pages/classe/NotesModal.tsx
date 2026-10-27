import { useState } from 'react'
import { Alert, Button, Checkbox, Group, Loader, Modal, NumberInput, Stack, Table, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { obtenirGrille, saisirNotes } from '../../api/endpoints'
import type { Evaluation, GrilleNotes, LigneNoteRequete } from '../../api/types'

interface Props {
  evaluation: Evaluation
  /** Faux pour l'évaluation d'une matière que l'enseignant n'assure pas : consultation seule. */
  modifiable: boolean
  onFermer: () => void
}

interface Saisie {
  valeur: number | string
  absent: boolean
}

export default function NotesModal({ evaluation, modifiable, onFermer }: Props) {
  const grille = useQuery({ queryKey: ['grille', evaluation.id], queryFn: () => obtenirGrille(evaluation.id) })

  return (
    <Modal opened onClose={onFermer} size="xl" title={`${evaluation.titre} — ${evaluation.matiere}`}>
      {grille.isPending && <Loader />}
      {grille.isError && (
        <Alert color="red" variant="light">
          {grille.error.message}
        </Alert>
      )}
      {grille.data && (
        <GrilleEditable
          key={grille.dataUpdatedAt}
          evaluation={evaluation}
          grille={grille.data}
          modifiable={modifiable}
          onFermer={onFermer}
        />
      )}
    </Modal>
  )
}

function GrilleEditable({
  evaluation,
  grille,
  modifiable,
  onFermer,
}: {
  evaluation: Evaluation
  grille: GrilleNotes
  modifiable: boolean
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const initiales: Record<number, Saisie> = Object.fromEntries(
    grille.lignes.map((l) => [l.inscriptionId, { valeur: l.valeur ?? '', absent: l.absent }]),
  )
  const [saisies, setSaisies] = useState<Record<number, Saisie>>(initiales)

  function modifier(inscriptionId: number, changements: Partial<Saisie>) {
    setSaisies((actuelles) => ({ ...actuelles, [inscriptionId]: { ...actuelles[inscriptionId], ...changements } }))
  }

  const depasse = (saisie: Saisie) => typeof saisie.valeur === 'number' && saisie.valeur > evaluation.bareme
  const invalide = Object.values(saisies).some(depasse)

  const enregistrement = useMutation({
    mutationFn: (lignes: LigneNoteRequete[]) => saisirNotes(evaluation.id, lignes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['grille', evaluation.id] })
      notifications.show({ color: 'green', title: 'Notes enregistrées', message: `« ${evaluation.titre} » a été mise à jour.` })
      onFermer()
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })

  function enregistrer() {
    // On n'envoie que les lignes modifiées : une valeur vide efface la note, « absent » marque l'absence.
    const lignes: LigneNoteRequete[] = []
    for (const ligne of grille.lignes) {
      const avant = initiales[ligne.inscriptionId]
      const apres = saisies[ligne.inscriptionId]
      if (avant.valeur === apres.valeur && avant.absent === apres.absent) continue
      if (apres.absent) lignes.push({ inscriptionId: ligne.inscriptionId, absent: true })
      else if (apres.valeur === '') lignes.push({ inscriptionId: ligne.inscriptionId })
      else lignes.push({ inscriptionId: ligne.inscriptionId, valeur: Number(apres.valeur) })
    }
    if (lignes.length === 0) {
      notifications.show({ color: 'orange', message: 'Aucune modification à enregistrer.' })
      return
    }
    enregistrement.mutate(lignes)
  }

  if (grille.lignes.length === 0) {
    return <Text c="dimmed">Aucun élève n'est encore inscrit dans cette classe.</Text>
  }

  return (
    <Stack>
      <Text size="sm" c="dimmed">
        Note sur {evaluation.bareme} · coefficient {evaluation.coefficient}
        {!modifiable && ' · consultation seule (vous n’enseignez pas cette matière dans cette classe)'}
      </Text>

      <Table.ScrollContainer minWidth={480}>
        <Table striped withTableBorder verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Élève</Table.Th>
              <Table.Th>Note /{evaluation.bareme}</Table.Th>
              <Table.Th>Absent</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {grille.lignes.map((ligne) => {
              const saisie = saisies[ligne.inscriptionId]
              return (
                <Table.Tr key={ligne.inscriptionId}>
                  <Table.Td>
                    {ligne.eleve.nom} {ligne.eleve.prenom}
                  </Table.Td>
                  <Table.Td>
                    <NumberInput
                      size="xs"
                      w={120}
                      min={0}
                      decimalScale={2}
                      hideControls
                      disabled={!modifiable || saisie.absent}
                      value={saisie.absent ? '' : saisie.valeur}
                      error={depasse(saisie) ? `Maximum : ${evaluation.bareme}` : undefined}
                      onChange={(valeur) => modifier(ligne.inscriptionId, { valeur })}
                    />
                  </Table.Td>
                  <Table.Td>
                    <Checkbox
                      disabled={!modifiable}
                      checked={saisie.absent}
                      onChange={(e) =>
                        modifier(ligne.inscriptionId, { absent: e.currentTarget.checked, valeur: '' })
                      }
                      aria-label="Élève absent"
                    />
                  </Table.Td>
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>

      <Group justify="flex-end">
        <Button variant="default" onClick={onFermer}>
          {modifiable ? 'Annuler' : 'Fermer'}
        </Button>
        {modifiable && (
          <Button onClick={enregistrer} loading={enregistrement.isPending} disabled={invalide}>
            Enregistrer les notes
          </Button>
        )}
      </Group>
    </Stack>
  )
}
