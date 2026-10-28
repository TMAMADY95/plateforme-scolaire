import { useState } from 'react'
import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'

export interface ChampReferentiel {
  nom: string
  label: string
  type?: 'text' | 'date' | 'number'
  /** Le champ peut rester vide. */
  facultatif?: boolean
}

interface Props {
  titre: string
  champs: ChampReferentiel[]
  valeursInitiales: Record<string, string>
  /** Clés de requêtes à rafraîchir après l'enregistrement. */
  cles: string[]
  enregistrer: (valeurs: Record<string, string>) => Promise<unknown>
  onFermer: () => void
}

/** Fenêtre de modification commune aux années, niveaux et matières. Les règles métier restent côté serveur. */
export default function ModifierReferentielModal({
  titre,
  champs,
  valeursInitiales,
  cles,
  enregistrer,
  onFermer,
}: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Record<string, string>>({
    initialValues: valeursInitiales,
    validate: Object.fromEntries(
      champs.map((c) => [c.nom, (v: string) => (!c.facultatif && v.trim() === '' ? `${c.label} requis` : null)]),
    ),
  })

  const modification = useMutation({
    mutationFn: enregistrer,
    onSuccess: () => {
      cles.forEach((cle) => void queryClient.invalidateQueries({ queryKey: [cle] }))
      notifications.show({ color: 'green', title: 'Modifié', message: titre })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={titre}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          modification.mutate(valeurs)
        })}
      >
        <Stack>
          {champs.map((c) => (
            <TextInput key={c.nom} label={c.label} type={c.type ?? 'text'} {...formulaire.getInputProps(c.nom)} />
          ))}
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
