import { Alert, Stack, Title } from '@mantine/core'
import ResultatsClasse from '../components/ResultatsClasse'
import SelecteursClasse from '../components/SelecteursClasse'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import { useSelectionClasse } from '../hooks/useSelectionClasse'

/** Moyennes et rangs d'une classe pour la direction. */
export default function PageResultats() {
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
      <Title order={2}>Résultats</Title>
      <SelecteursClasse selection={selection} />
      {selection.classe && (
        <ResultatsClasse
          key={selection.classe.id}
          classeId={selection.classe.id}
          anneeScolaireId={selection.classe.anneeScolaireId}
        />
      )}
    </Stack>
  )
}
