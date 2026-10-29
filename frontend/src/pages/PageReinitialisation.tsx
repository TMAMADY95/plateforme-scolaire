import { useState } from 'react'
import { Alert, Anchor, Button, Container, Paper, PasswordInput, Stack, Text, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { reinitialiserAvecJeton } from '../api/endpoints'
import { ApiError } from '../api/client'

/** Page ouverte depuis le lien reçu par e-mail : /reinitialisation?jeton=... */
export default function PageReinitialisation() {
  const [parametres] = useSearchParams()
  const jeton = parametres.get('jeton') ?? ''
  const navigate = useNavigate()
  const [erreur, setErreur] = useState<string | null>(null)
  const [enCours, setEnCours] = useState(false)

  const formulaire = useForm({
    initialValues: { nouveau: '', confirmation: '' },
    validate: {
      nouveau: (v) => (v.length >= 10 ? null : 'Au moins 10 caractères'),
      confirmation: (v, valeurs) => (v === valeurs.nouveau ? null : 'Les deux mots de passe diffèrent'),
    },
  })

  async function soumettre(valeurs: { nouveau: string }) {
    setErreur(null)
    setEnCours(true)
    try {
      await reinitialiserAvecJeton(jeton, valeurs.nouveau)
      notifications.show({ color: 'green', title: 'Mot de passe modifié', message: 'Vous pouvez vous connecter.' })
      void navigate('/connexion', { replace: true })
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Réinitialisation impossible.')
    } finally {
      setEnCours(false)
    }
  }

  return (
    <Container size={420} my={80}>
      <Title ta="center" order={2}>
        Nouveau mot de passe
      </Title>
      <Text c="dimmed" size="sm" ta="center" mt={5} mb={30}>
        Choisissez un mot de passe d'au moins 10 caractères
      </Text>
      <Paper withBorder shadow="sm" p={30} radius="md">
        {jeton === '' ? (
          <Stack>
            <Alert color="red" variant="light">
              Ce lien est incomplet. Refaites une demande de réinitialisation.
            </Alert>
            <Anchor component={Link} to="/mot-de-passe-oublie" size="sm" ta="center">
              Refaire une demande
            </Anchor>
          </Stack>
        ) : (
          <form onSubmit={formulaire.onSubmit(soumettre)}>
            <Stack>
              {erreur !== null && (
                <Alert color="red" variant="light">
                  {erreur}
                </Alert>
              )}
              <PasswordInput label="Nouveau mot de passe" autoComplete="new-password" {...formulaire.getInputProps('nouveau')} />
              <PasswordInput
                label="Confirmer le mot de passe"
                autoComplete="new-password"
                {...formulaire.getInputProps('confirmation')}
              />
              <Button type="submit" loading={enCours} fullWidth>
                Enregistrer
              </Button>
              <Anchor component={Link} to="/mot-de-passe-oublie" size="sm" ta="center">
                Le lien a expiré ? Refaire une demande
              </Anchor>
            </Stack>
          </form>
        )}
      </Paper>
    </Container>
  )
}
