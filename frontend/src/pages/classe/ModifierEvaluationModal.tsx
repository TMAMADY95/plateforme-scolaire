import { useState } from 'react'
import { Alert, Button, Group, Modal, NumberInput, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { modifierEvaluation } from '../../api/endpoints'
import type { Evaluation } from '../../api/types'

interface Valeurs {
  titre: string
  dateEvaluation: string
  coefficient: number | string
  bareme: number | string
}

/** Modification du titre, de la date, du coefficient et du barème (la matière et la période ne changent pas). */
export default function ModifierEvaluationModal({
  evaluation,
  onFermer,
}: {
  evaluation: Evaluation
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: {
      titre: evaluation.titre,
      dateEvaluation: evaluation.dateEvaluation,
      coefficient: evaluation.coefficient,
      bareme: evaluation.bareme,
    },
    validate: {
      titre: (v) => (v.trim() === '' ? 'Titre requis' : null),
      dateEvaluation: (v) => (v === '' ? 'Date requise' : null),
      coefficient: (v) => (Number(v) >= 0.01 ? null : 'Au moins 0,01'),
      bareme: (v) => (Number(v) >= 0.01 ? null : 'Au moins 0,01'),
    },
  })

  const modification = useMutation({
    mutationFn: (v: Valeurs) =>
      modifierEvaluation(evaluation.id, {
        titre: v.titre.trim(),
        dateEvaluation: v.dateEvaluation,
        coefficient: Number(v.coefficient),
        bareme: Number(v.bareme),
      }),
    onSuccess: () => {
      for (const cle of ['evaluations', 'resultats', 'bulletin']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Évaluation modifiée', message: evaluation.titre })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title="Modifier l'évaluation">
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          modification.mutate(valeurs)
        })}
      >
        <Stack>
          <Text size="sm" c="dimmed">
            {evaluation.matiere} · {evaluation.periode}
          </Text>
          <TextInput label="Titre" {...formulaire.getInputProps('titre')} />
          <TextInput label="Date" type="date" {...formulaire.getInputProps('dateEvaluation')} />
          <Group grow align="flex-start">
            <NumberInput label="Coefficient" min={0.01} decimalScale={2} {...formulaire.getInputProps('coefficient')} />
            <NumberInput
              label="Barème"
              description="Pas en dessous d'une note déjà saisie"
              min={0.01}
              decimalScale={2}
              {...formulaire.getInputProps('bareme')}
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
