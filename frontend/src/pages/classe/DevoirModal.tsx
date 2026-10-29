import { useState } from 'react'
import { Alert, Button, Group, Modal, Select, Stack, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { creerDevoir, modifierDevoir } from '../../api/endpoints'
import type { Devoir } from '../../api/types'
import { aujourdhui } from '../../utils/dates'

interface Valeurs {
  matiereId: string | null
  titre: string
  consigne: string
  dateLimite: string
}

/** Création (sans `devoir`) ou modification d'un devoir ; la matière ne change plus après la création. */
export default function DevoirModal({
  classeId,
  mesMatieres,
  devoir,
  onFermer,
}: {
  classeId: number
  mesMatieres: { id: number; libelle: string }[]
  devoir?: Devoir
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: {
      matiereId: devoir ? String(devoir.matiereId) : mesMatieres.length === 1 ? String(mesMatieres[0].id) : null,
      titre: devoir?.titre ?? '',
      consigne: devoir?.consigne ?? '',
      dateLimite: devoir?.dateLimite ?? aujourdhui(),
    },
    validate: {
      matiereId: (v) => (v === null ? 'Matière requise' : null),
      titre: (v) => (v.trim() === '' ? 'Titre requis' : null),
      dateLimite: (v) => (v === '' ? 'Date requise' : null),
    },
  })

  const enregistrement = useMutation({
    mutationFn: (v: Valeurs) => {
      const corps = { titre: v.titre.trim(), consigne: v.consigne.trim(), dateLimite: v.dateLimite }
      return devoir
        ? modifierDevoir(devoir.id, corps)
        : creerDevoir(classeId, { ...corps, matiereId: Number(v.matiereId) })
    },
    onSuccess: (resultat) => {
      void queryClient.invalidateQueries({ queryKey: ['devoirs'] })
      notifications.show({
        color: 'green',
        title: devoir ? 'Devoir modifié' : 'Devoir publié',
        message: resultat.titre,
      })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={devoir ? 'Modifier le devoir' : 'Nouveau devoir'}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          enregistrement.mutate(valeurs)
        })}
      >
        <Stack>
          <Select
            label="Matière"
            data={mesMatieres.map((m) => ({ value: String(m.id), label: m.libelle }))}
            disabled={devoir !== undefined}
            allowDeselect={false}
            {...formulaire.getInputProps('matiereId')}
          />
          <TextInput label="Titre" maxLength={200} {...formulaire.getInputProps('titre')} />
          <Textarea
            label="Consigne"
            description="Facultative"
            autosize
            minRows={3}
            maxRows={8}
            maxLength={4000}
            {...formulaire.getInputProps('consigne')}
          />
          <TextInput
            label="À rendre pour le"
            type="date"
            min={devoir ? undefined : aujourdhui()}
            {...formulaire.getInputProps('dateLimite')}
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
            <Button type="submit" loading={enregistrement.isPending}>
              {devoir ? 'Enregistrer' : 'Publier'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
