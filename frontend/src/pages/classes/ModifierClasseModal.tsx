import { useState } from 'react'
import { Alert, Button, Group, Modal, NumberInput, Select, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { modifierClasse } from '../../api/endpoints'
import type { Classe, Niveau } from '../../api/types'

interface Valeurs {
  niveauId: string | null
  nom: string
  capacite: number | string
}

/** Modification d'une classe : niveau, nom et capacité (l'année ne change pas). */
export default function ModifierClasseModal({
  classe,
  niveaux,
  onFermer,
}: {
  classe: Classe
  niveaux: Niveau[]
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: { niveauId: String(classe.niveauId), nom: classe.nom, capacite: classe.capacite ?? '' },
    validate: {
      niveauId: (v) => (v === null ? 'Niveau requis' : null),
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      capacite: (v) => (v === '' || Number(v) >= 1 ? null : 'Au moins 1'),
    },
  })

  const modification = useMutation({
    mutationFn: (v: Valeurs) =>
      modifierClasse(classe.id, {
        niveauId: Number(v.niveauId),
        nom: v.nom.trim(),
        capacite: v.capacite === '' ? null : Number(v.capacite),
      }),
    onSuccess: (c) => {
      for (const cle of ['classes', 'classes-etab']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Classe modifiée', message: c.nom })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={`Modifier la classe ${classe.nom}`}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          modification.mutate(valeurs)
        })}
      >
        <Stack>
          <Select
            label="Niveau"
            allowDeselect={false}
            data={niveaux.map((n) => ({ value: String(n.id), label: n.libelle }))}
            {...formulaire.getInputProps('niveauId')}
          />
          <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
          <NumberInput
            label="Capacité"
            description="Laisser vide si illimitée"
            min={1}
            allowDecimal={false}
            {...formulaire.getInputProps('capacite')}
          />
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
