import { useState } from 'react'
import { Alert, Anchor, Box, Button, Group, Paper, PasswordInput, SimpleGrid, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import Icone from '../components/Icone'
import type { NomIcone } from '../components/Icone'
import Logo from '../components/Logo'
import { SOUS_TITRE_APPLICATION } from '../theme'

interface EtatNavigation {
  depuis?: string
}

const ATOUTS: { icone: NomIcone; titre: string; texte: string }[] = [
  { icone: 'ecole', titre: 'Gestion centralisée', texte: 'Tous les établissements au même endroit.' },
  { icone: 'presences', titre: 'Suivi en temps réel', texte: 'Présences, notes et bulletins à jour.' },
  { icone: 'finances', titre: 'Paiement sécurisé', texte: 'Frais scolaires et reçus en ligne.' },
  { icone: 'comptes', titre: 'Accès protégés', texte: 'Chaque acteur voit uniquement ses données.' },
]

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
    <SimpleGrid cols={{ base: 1, md: 2 }} spacing={0} mih="100vh">
      <Box className="connexion-panneau" p={{ base: 'xl', md: 60 }}>
        <Stack justify="space-between" h="100%" gap={40}>
          <Logo taille={26} />
          <Stack gap="lg">
            <Title order={1} c="white" fz={{ base: 30, md: 40 }} lh={1.2}>
              Une éducation de qualité pour un meilleur avenir
            </Title>
            <Text c="rgba(255,255,255,0.85)" maw={460}>
              {SOUS_TITRE_APPLICATION} : établissements, élèves, enseignants et familles réunis sur une même
              plateforme.
            </Text>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" mt="md" visibleFrom="sm">
              {ATOUTS.map((a) => (
                <Group key={a.titre} wrap="nowrap" align="flex-start" gap="sm">
                  <Box c="white" style={{ opacity: 0.9 }}>
                    <Icone nom={a.icone} taille={24} />
                  </Box>
                  <div>
                    <Text fw={600} c="white" size="sm">
                      {a.titre}
                    </Text>
                    <Text c="rgba(255,255,255,0.75)" size="xs">
                      {a.texte}
                    </Text>
                  </div>
                </Group>
              ))}
            </SimpleGrid>
          </Stack>
          <Text c="rgba(255,255,255,0.6)" size="xs">
            Ensemble pour une éducation accessible, moderne et performante.
          </Text>
        </Stack>
      </Box>

      <Stack justify="center" align="center" p="xl" bg="white">
        <Paper w="100%" maw={400} p={{ base: 0, sm: 'md' }}>
          <Title order={2} mb={4}>
            Connexion
          </Title>
          <Text c="dimmed" size="sm" mb="xl">
            Connectez-vous pour accéder à votre espace
          </Text>
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
                size="md"
                {...formulaire.getInputProps('email')}
              />
              <PasswordInput
                label="Mot de passe"
                autoComplete="current-password"
                size="md"
                {...formulaire.getInputProps('motDePasse')}
              />
              <Button type="submit" loading={enCours} fullWidth size="md">
                Se connecter
              </Button>
              <Anchor component={Link} to="/mot-de-passe-oublie" size="sm" ta="center">
                Mot de passe oublié ?
              </Anchor>
            </Stack>
          </form>
        </Paper>
      </Stack>
    </SimpleGrid>
  )
}
