import { useState } from 'react'
import { Alert, Button, Container, Paper, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

interface EtatNavigation {
  depuis?: string
}

export default function PageConnexion() {
  const { utilisateur, connexion } = useAuth()
  const navigate = useNavigate()
  const lieu = useLocation()
  const [erreur, setErreur] = useState<string | null>(null)
  const [enCours, setEnCours] = useState(false)

  const formulaire = useForm({
    initialValues: { email: '', motDePasse: '' },
    validate: {
      email: (valeur) => (/^\S+@\S+$/.test(valeur) ? null : 'Adresse e-mail invalide'),
      motDePasse: (valeur) => (valeur.length > 0 ? null : 'Mot de passe requis'),
    },
  })

  if (utilisateur !== null) {
    return <Navigate to={utilisateur.changementMotDePasseRequis ? '/changer-mot-de-passe' : '/'} replace />
  }

  const destination = (lieu.state as EtatNavigation | null)?.depuis ?? '/'

  async function soumettre(valeurs: { email: string; motDePasse: string }) {
    setErreur(null)
    setEnCours(true)
    try {
      const moi = await connexion(valeurs.email.trim(), valeurs.motDePasse)
      navigate(moi.changementMotDePasseRequis ? '/changer-mot-de-passe' : destination, { replace: true })
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Connexion impossible.')
    } finally {
      setEnCours(false)
    }
  }

  return (
    <Container size={420} my={80}>
      <Title ta="center" order={2}>
        Gestion scolaire
      </Title>
      <Text c="dimmed" size="sm" ta="center" mt={5} mb={30}>
        Connectez-vous pour accéder à votre espace
      </Text>
      <Paper withBorder shadow="sm" p={30} radius="md">
        <form onSubmit={formulaire.onSubmit(soumettre)}>
          <Stack>
            {erreur !== null && (
              <Alert color="red" variant="light">
                {erreur}
              </Alert>
            )}
            <TextInput
              label="Adresse e-mail"
              placeholder="vous@exemple.org"
              autoComplete="username"
              {...formulaire.getInputProps('email')}
            />
            <PasswordInput
              label="Mot de passe"
              autoComplete="current-password"
              {...formulaire.getInputProps('motDePasse')}
            />
            <Button type="submit" loading={enCours} fullWidth>
              Se connecter
            </Button>
          </Stack>
        </form>
      </Paper>
    </Container>
  )
}
