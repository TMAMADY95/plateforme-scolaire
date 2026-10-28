import { useState } from 'react'
import { Alert, Button, Group, Modal, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation } from '@tanstack/react-query'
import type { ComptePersonnel, DirectionRequete } from '../../api/types'
import { genererMotDePasse } from '../../utils/motDePasse'

interface Valeurs {
  prenom: string
  nom: string
  email: string
  telephone: string
  motDePasseInitial: string
}

interface Props {
  titre: string
  /** Appel d'API qui crée le compte (direction d'établissement ou direction régionale). */
  creer: (requete: DirectionRequete) => Promise<ComptePersonnel>
  /** Titre de la notification de succès. */
  messageSucces?: string
  onFermer: () => void
}

export default function DirectionModal({ titre, creer, messageSucces = 'Compte de direction créé', onFermer }: Props) {
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: { prenom: '', nom: '', email: '', telephone: '', motDePasseInitial: '' },
    validate: {
      prenom: (v) => (v.trim() === '' ? 'Prénom requis' : null),
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      email: (v) => (/^\S+@\S+$/.test(v.trim()) ? null : 'Adresse e-mail invalide'),
      motDePasseInitial: (v) => (v.length >= 10 ? null : 'Au moins 10 caractères'),
    },
  })

  const creation = useMutation({
    mutationFn: (v: Valeurs) =>
      creer({
        email: v.email.trim(),
        nom: v.nom.trim(),
        prenom: v.prenom.trim(),
        telephone: v.telephone.trim() || undefined,
        motDePasseInitial: v.motDePasseInitial,
      }),
    onSuccess: (compte) => {
      notifications.show({
        color: 'green',
        title: messageSucces,
        message: `${compte.prenom} ${compte.nom} devra changer son mot de passe à sa première connexion.`,
      })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={titre} size="lg">
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          creation.mutate(valeurs)
        })}
      >
        <Stack>
          <Group grow align="flex-start">
            <TextInput label="Prénom" {...formulaire.getInputProps('prenom')} />
            <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
          </Group>
          <TextInput label="Adresse e-mail" description="Elle sert d'identifiant de connexion" {...formulaire.getInputProps('email')} />
          <TextInput label="Téléphone (facultatif)" {...formulaire.getInputProps('telephone')} />
          <TextInput
            label="Mot de passe initial"
            description="À communiquer à la personne : elle devra le remplacer à sa première connexion"
            {...formulaire.getInputProps('motDePasseInitial')}
            rightSectionWidth={110}
            rightSection={
              <Button
                size="compact-xs"
                variant="light"
                onClick={() => formulaire.setFieldValue('motDePasseInitial', genererMotDePasse())}
              >
                Générer
              </Button>
            }
          />
          <Text size="xs" c="dimmed">
            Ce mot de passe n'est affiché qu'ici : notez-le avant de valider.
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
            <Button type="submit" loading={creation.isPending}>
              Créer le compte
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
