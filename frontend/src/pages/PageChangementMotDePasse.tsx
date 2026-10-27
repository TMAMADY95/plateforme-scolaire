import { useState } from 'react'
import { Alert, Anchor, Button, Container, Paper, PasswordInput, Stack, Text, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useNavigate } from 'react-router'
import { api, ApiError } from '../api/client'
import { useAuth, useUtilisateur } from '../auth/useAuth'

interface Valeurs {
  motDePasseActuel: string
  nouveauMotDePasse: string
  confirmation: string
}

export default function PageChangementMotDePasse() {
  const utilisateur = useUtilisateur()
  const { deconnexion } = useAuth()
  const navigate = useNavigate()
  const [erreur, setErreur] = useState<string | null>(null)
  const [enCours, setEnCours] = useState(false)

  const formulaire = useForm<Valeurs>({
    initialValues: { motDePasseActuel: '', nouveauMotDePasse: '', confirmation: '' },
    validate: {
      motDePasseActuel: (v) => (v.length > 0 ? null : 'Mot de passe actuel requis'),
      nouveauMotDePasse: (v) => (v.length >= 10 ? null : 'Au moins 10 caractères'),
      confirmation: (v, valeurs) => (v === valeurs.nouveauMotDePasse ? null : 'Les mots de passe ne correspondent pas'),
    },
  })

  async function soumettre(valeurs: Valeurs) {
    setErreur(null)
    setEnCours(true)
    try {
      await api.post('/api/me/mot-de-passe', {
        motDePasseActuel: valeurs.motDePasseActuel,
        nouveauMotDePasse: valeurs.nouveauMotDePasse,
      })
      // Le serveur révoque tous les anciens jetons : il faut se reconnecter.
      deconnexion()
      notifications.show({
        color: 'green',
        title: 'Mot de passe modifié',
        message: 'Reconnectez-vous avec votre nouveau mot de passe.',
      })
      navigate('/connexion', { replace: true })
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Changement impossible.')
    } finally {
      setEnCours(false)
    }
  }

  function annuler() {
    deconnexion()
    navigate('/connexion', { replace: true })
  }

  return (
    <Container size={460} my={80}>
      <Title ta="center" order={2}>
        Changer votre mot de passe
      </Title>
      <Text c="dimmed" size="sm" ta="center" mt={5} mb={30}>
        {utilisateur.changementMotDePasseRequis
          ? 'Votre mot de passe initial doit être remplacé avant de continuer.'
          : 'Choisissez un nouveau mot de passe.'}
      </Text>
      <Paper withBorder shadow="sm" p={30} radius="md">
        <form onSubmit={formulaire.onSubmit(soumettre)}>
          <Stack>
            {erreur !== null && (
              <Alert color="red" variant="light">
                {erreur}
              </Alert>
            )}
            <PasswordInput label="Mot de passe actuel" {...formulaire.getInputProps('motDePasseActuel')} />
            <PasswordInput
              label="Nouveau mot de passe"
              description="10 caractères minimum"
              {...formulaire.getInputProps('nouveauMotDePasse')}
            />
            <PasswordInput label="Confirmer le nouveau mot de passe" {...formulaire.getInputProps('confirmation')} />
            <Button type="submit" loading={enCours} fullWidth>
              Changer le mot de passe
            </Button>
            <Text ta="center" size="sm">
              <Anchor component="button" type="button" onClick={annuler}>
                Se déconnecter
              </Anchor>
            </Text>
          </Stack>
        </form>
      </Paper>
    </Container>
  )
}
