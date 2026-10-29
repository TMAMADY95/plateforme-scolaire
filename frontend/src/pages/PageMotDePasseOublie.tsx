import { useState } from 'react'
import { Alert, Anchor, Button, Container, Paper, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { Link } from 'react-router'
import { demanderReinitialisation } from '../api/endpoints'
import { ApiError } from '../api/client'

export default function PageMotDePasseOublie() {
  const [envoye, setEnvoye] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)
  const [enCours, setEnCours] = useState(false)

  const formulaire = useForm({
    initialValues: { email: '' },
    validate: { email: (v) => (/^\S+@\S+$/.test(v) ? null : 'Adresse e-mail invalide') },
  })

  async function soumettre(valeurs: { email: string }) {
    setErreur(null)
    setEnCours(true)
    try {
      await demanderReinitialisation(valeurs.email.trim())
      setEnvoye(true)
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Demande impossible.')
    } finally {
      setEnCours(false)
    }
  }

  return (
    <Container size={420} my={80}>
      <Title ta="center" order={2}>
        Mot de passe oublié
      </Title>
      <Text c="dimmed" size="sm" ta="center" mt={5} mb={30}>
        Nous vous envoyons un lien pour en choisir un nouveau
      </Text>
      <Paper withBorder shadow="sm" p={30} radius="md">
        {envoye ? (
          <Stack>
            <Alert color="green" variant="light">
              Si cette adresse correspond à un compte, un e-mail vient de lui être envoyé. Le lien est valable 30 minutes.
              Pensez à regarder dans vos courriers indésirables.
            </Alert>
            <Anchor component={Link} to="/connexion" size="sm" ta="center">
              Retour à la connexion
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
              <TextInput
                label="Adresse e-mail"
                placeholder="vous@exemple.org"
                autoComplete="username"
                {...formulaire.getInputProps('email')}
              />
              <Button type="submit" loading={enCours} fullWidth>
                Envoyer le lien
              </Button>
              <Anchor component={Link} to="/connexion" size="sm" ta="center">
                Retour à la connexion
              </Anchor>
            </Stack>
          </form>
        )}
      </Paper>
    </Container>
  )
}
