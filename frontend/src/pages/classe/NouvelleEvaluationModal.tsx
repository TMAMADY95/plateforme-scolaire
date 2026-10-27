import { useState } from 'react'
import { Alert, Button, Group, Modal, NumberInput, Select, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { creerEvaluation } from '../../api/endpoints'
import type { Periode } from '../../api/types'
import { aujourdhui } from '../../utils/dates'

interface Props {
  classeId: number
  periodes: Periode[]
  mesMatieres: { id: number; libelle: string }[]
  onFermer: () => void
}

interface Valeurs {
  matiereId: string | null
  periodeId: string | null
  titre: string
  dateEvaluation: string
  coefficient: number | string
  bareme: number | string
}

export default function NouvelleEvaluationModal({ classeId, periodes, mesMatieres, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  // Par défaut : la période en cours, et la matière si l'enseignant n'en assure qu'une
  const jour = aujourdhui()
  const enCours = periodes.find((p) => p.dateDebut <= jour && jour <= p.dateFin)

  const formulaire = useForm<Valeurs>({
    initialValues: {
      matiereId: mesMatieres.length === 1 ? String(mesMatieres[0].id) : null,
      periodeId: enCours ? String(enCours.id) : null,
      titre: '',
      dateEvaluation: enCours ? jour : '',
      coefficient: 1,
      bareme: 20,
    },
    validate: (v) => {
      const periode = periodes.find((p) => String(p.id) === v.periodeId)
      return {
        matiereId: v.matiereId === null ? 'Matière requise' : null,
        periodeId: v.periodeId === null ? 'Période requise' : null,
        titre: v.titre.trim() === '' ? 'Titre requis' : null,
        dateEvaluation:
          v.dateEvaluation === ''
            ? 'Date requise'
            : periode && (v.dateEvaluation < periode.dateDebut || v.dateEvaluation > periode.dateFin)
              ? 'La date doit être comprise dans la période'
              : null,
        coefficient: Number(v.coefficient) > 0 ? null : 'Le coefficient doit être positif',
        bareme: Number(v.bareme) > 0 ? null : 'Le barème doit être positif',
      }
    },
  })

  const periodeChoisie = periodes.find((p) => String(p.id) === formulaire.values.periodeId)

  const creation = useMutation({
    mutationFn: (v: Valeurs) =>
      creerEvaluation(classeId, {
        matiereId: Number(v.matiereId),
        periodeId: Number(v.periodeId),
        titre: v.titre.trim(),
        dateEvaluation: v.dateEvaluation,
        coefficient: Number(v.coefficient),
        bareme: Number(v.bareme),
      }),
    onSuccess: (evaluation) => {
      void queryClient.invalidateQueries({ queryKey: ['evaluations', classeId] })
      notifications.show({ color: 'green', title: 'Évaluation créée', message: `« ${evaluation.titre} » est prête pour la saisie des notes.` })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title="Nouvelle évaluation" size="lg">
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          creation.mutate(valeurs)
        })}
      >
        <Stack>
          <TextInput label="Titre" placeholder="Contrôle 1" {...formulaire.getInputProps('titre')} />
          <Group grow align="flex-start">
            <Select
              label="Matière"
              data={mesMatieres.map((m) => ({ value: String(m.id), label: m.libelle }))}
              {...formulaire.getInputProps('matiereId')}
            />
            <Select
              label="Période"
              data={periodes.map((p) => ({ value: String(p.id), label: p.libelle }))}
              {...formulaire.getInputProps('periodeId')}
            />
          </Group>
          <TextInput
            label="Date"
            type="date"
            min={periodeChoisie?.dateDebut}
            max={periodeChoisie?.dateFin}
            {...formulaire.getInputProps('dateEvaluation')}
          />
          <Group grow align="flex-start">
            <NumberInput
              label="Coefficient"
              min={0.01}
              step={0.5}
              decimalScale={2}
              {...formulaire.getInputProps('coefficient')}
            />
            <NumberInput
              label="Barème (note maximale)"
              min={1}
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
            <Button type="submit" loading={creation.isPending}>
              Créer
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
