import { useState } from 'react'
import { Alert, Button, Group, Modal, Select, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { modifierEleve } from '../../api/endpoints'
import type { Eleve } from '../../api/types'
import { aujourdhui } from '../../utils/dates'

interface Props {
  eleve: Eleve
  etablissementId: number
  onFermer: () => void
}

interface Valeurs {
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
}

/** Correction de l'identité d'un élève. Le matricule ne change jamais. */
export default function ModifierEleveModal({ eleve, etablissementId, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: { nom: eleve.nom, prenom: eleve.prenom, dateNaissance: eleve.dateNaissance, sexe: eleve.sexe },
    validate: {
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      prenom: (v) => (v.trim() === '' ? 'Prénom requis' : null),
      dateNaissance: (v) =>
        v === '' ? 'Date de naissance requise' : v >= aujourdhui() ? 'La date de naissance doit être passée' : null,
    },
  })

  const modification = useMutation({
    mutationFn: (v: Valeurs) =>
      modifierEleve(eleve.id, { nom: v.nom.trim(), prenom: v.prenom.trim(), dateNaissance: v.dateNaissance, sexe: v.sexe }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['inscriptions', etablissementId] })
      notifications.show({ color: 'green', title: 'Identité corrigée', message: `Matricule ${eleve.matricule}` })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={`Modifier l'élève ${eleve.matricule}`}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          modification.mutate(valeurs)
        })}
      >
        <Stack>
          <Group grow align="flex-start">
            <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
            <TextInput label="Prénom" {...formulaire.getInputProps('prenom')} />
          </Group>
          <Group grow align="flex-start">
            <TextInput label="Date de naissance" type="date" {...formulaire.getInputProps('dateNaissance')} />
            <Select
              label="Sexe"
              data={[
                { value: 'F', label: 'Féminin' },
                { value: 'M', label: 'Masculin' },
              ]}
              clearable
              {...formulaire.getInputProps('sexe')}
            />
          </Group>
          {erreur !== null && (
            <Alert color="red" variant="light">
              {erreur}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onFermer}>
              Annuler
            </Button>
            <Button type="submit" loading={modification.isPending}>
              Enregistrer
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
