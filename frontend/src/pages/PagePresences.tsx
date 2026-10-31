import { Alert, Stack, Title } from '@mantine/core'
import SelecteursClasse from '../components/SelecteursClasse'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import { useSelectionClasse } from '../hooks/useSelectionClasse'
import JustificationsATraiter from '../components/JustificationsATraiter'
import SortiesATraiter from '../components/SortiesATraiter'
import OngletAppel from './classe/OngletAppel'

/** Consultation (et correction) de l'appel d'une classe par la direction. */
export default function PagePresences() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const selection = useSelectionClasse(etablissementId)

  if (etablissementId === undefined) {
    return (
      <Alert color="orange" variant="light">
        Aucun établissement n'est associé à votre compte.
      </Alert>
    )
  }

  return (
    <Stack>
      <Title order={2}>Présences</Title>
      <JustificationsATraiter etablissementId={etablissementId} />
      <SortiesATraiter etablissementId={etablissementId} />
      <SelecteursClasse selection={selection} />
      {selection.classe && <OngletAppel key={selection.classe.id} classeId={selection.classe.id} />}
    </Stack>
  )
}
