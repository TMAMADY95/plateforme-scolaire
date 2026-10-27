import { Alert, Badge, Card, Group, Loader, Stack, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { Navigate } from 'react-router'
import { obtenirEtablissement } from '../api/endpoints'
import { estEnseignant, estParent, etablissementCourant, LIBELLES_ROLES } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'

export default function PageAccueil() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)

  const etablissement = useQuery({
    queryKey: ['etablissement', etablissementId],
    queryFn: () => obtenirEtablissement(etablissementId as number),
    enabled: etablissementId !== undefined,
  })

  if (etablissementId === undefined && estEnseignant(utilisateur)) {
    return <Navigate to="/mes-classes" replace />
  }
  if (etablissementId === undefined && estParent(utilisateur)) {
    return <Navigate to="/mes-enfants" replace />
  }

  if (etablissementId === undefined) {
    const roles = [...new Set(utilisateur.affectations.map((a) => LIBELLES_ROLES[a.role]))].join(', ')
    return (
      <Stack>
        <Title order={2}>Bienvenue</Title>
        <Alert color="blue" variant="light" title="Votre espace arrive bientôt">
          Cette version contient l'espace de la direction d'établissement. L'espace correspondant à votre rôle
          ({roles}) sera ajouté dans une prochaine étape.
        </Alert>
      </Stack>
    )
  }

  return (
    <Stack>
      <Title order={2}>Tableau de bord</Title>
      {etablissement.isPending && <Loader />}
      {etablissement.isError && (
        <Alert color="red" variant="light">
          {etablissement.error.message}
        </Alert>
      )}
      {etablissement.data && (
        <Card withBorder padding="lg" maw={520}>
          <Group justify="space-between" mb="xs">
            <Text fw={600} size="lg">
              {etablissement.data.nom}
            </Text>
            <Badge color={etablissement.data.statut === 'ACTIF' ? 'green' : 'gray'}>{etablissement.data.statut}</Badge>
          </Group>
          <Text c="dimmed" size="sm">
            Code officiel : {etablissement.data.codeOfficiel}
          </Text>
          {etablissement.data.typeEtablissement && (
            <Text c="dimmed" size="sm">
              Type : {etablissement.data.typeEtablissement}
            </Text>
          )}
        </Card>
      )}
    </Stack>
  )
}
