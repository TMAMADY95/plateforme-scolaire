import { Alert, Stack, Tabs, Title } from '@mantine/core'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import AffectationsClasse from './enseignants/AffectationsClasse'
import ListeEnseignants from './enseignants/ListeEnseignants'

export default function PageEnseignants() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)

  if (etablissementId === undefined) {
    return (
      <Alert color="orange" variant="light">
        Aucun établissement n'est associé à votre compte.
      </Alert>
    )
  }

  return (
    <Stack>
      <Title order={2}>Enseignants</Title>
      <Tabs defaultValue="enseignants">
        <Tabs.List>
          <Tabs.Tab value="enseignants">Enseignants</Tabs.Tab>
          <Tabs.Tab value="affectations">Affectations par classe</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="enseignants">
          <ListeEnseignants etablissementId={etablissementId} />
        </Tabs.Panel>
        <Tabs.Panel value="affectations">
          <AffectationsClasse etablissementId={etablissementId} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
