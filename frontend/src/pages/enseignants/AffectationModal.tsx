import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Select, Stack, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { affecterEnseignant, listerEnseignants, listerMatieres } from '../../api/endpoints'
import type { Classe } from '../../api/types'

interface Props {
  classe: Classe
  onFermer: () => void
}

/** Affecte un enseignant de l'établissement à la classe pour une matière. */
export default function AffectationModal({ classe, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [enseignantId, setEnseignantId] = useState<string | null>(null)
  const [matiereId, setMatiereId] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const enseignants = useQuery({
    queryKey: ['enseignants', classe.etablissementId],
    queryFn: () => listerEnseignants(classe.etablissementId),
  })
  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres })

  const affectation = useMutation({
    mutationFn: () => affecterEnseignant(classe.id, { enseignantId: Number(enseignantId), matiereId: Number(matiereId) }),
    onSuccess: (resultat) => {
      void queryClient.invalidateQueries({ queryKey: ['affectations', classe.id] })
      notifications.show({
        color: 'green',
        title: 'Affectation enregistrée',
        message: `${resultat.enseignantNom} enseigne ${resultat.matiereLibelle} en ${classe.nom}.`,
      })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  const optionsEnseignants = (enseignants.data ?? []).map((e) => ({
    value: String(e.id),
    label: `${e.prenom} ${e.nom}`,
  }))
  const optionsMatieres = (matieres.data ?? []).map((m) => ({ value: String(m.id), label: m.libelle }))
  const chargement = enseignants.isPending || matieres.isPending

  return (
    <Modal opened onClose={onFermer} title={`Affecter un enseignant à la classe ${classe.nom}`}>
      <Stack>
        {chargement && <Loader size="sm" />}
        {(enseignants.isError || matieres.isError) && (
          <Alert color="red" variant="light">
            {(enseignants.error ?? matieres.error)?.message}
          </Alert>
        )}
        {enseignants.data && enseignants.data.length === 0 && (
          <Alert color="orange" variant="light">
            Aucun enseignant dans cet établissement : créez d'abord un compte dans l'onglet « Enseignants ».
          </Alert>
        )}
        {!chargement && (
          <>
            <Select
              label="Enseignant"
              placeholder="Choisir un enseignant"
              data={optionsEnseignants}
              value={enseignantId}
              onChange={setEnseignantId}
              searchable
            />
            <Select
              label="Matière"
              placeholder="Choisir une matière"
              data={optionsMatieres}
              value={matiereId}
              onChange={setMatiereId}
            />
          </>
        )}
        <Text size="xs" c="dimmed">
          Un enseignant ne peut saisir l'appel et les notes que pour les classes et matières qui lui sont affectées.
        </Text>
        {erreur !== null && (
          <Alert color="red" variant="light">
            {erreur}
          </Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>
            Annuler
          </Button>
          <Button
            disabled={enseignantId === null || matiereId === null}
            loading={affectation.isPending}
            onClick={() => {
              setErreur(null)
              affectation.mutate()
            }}
          >
            Affecter
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
