import { Alert, Anchor, Loader, Stack, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerCreneauxClasse } from '../api/endpoints'
import GrilleEmploiDuTemps from '../components/GrilleEmploiDuTemps'
import type { Creneau } from '../api/types'

/**
 * Emploi du temps d'une classe (direction ou enseignant).
 * À placer dans frontend/src/pages/ — les imports partent donc de ../api et ../components.
 */
export default function PageEmploiDuTempsClasse({ classeId }: { classeId: number }) {
  const creneaux = useQuery({
    queryKey: ['creneaux-classe', classeId],
    queryFn: () => listerCreneauxClasse(classeId),
  })

  return (
    <Stack>
      <Anchor href="/classes" size="sm">
        ← Classes
      </Anchor>
      <Title order={2}>Emploi du temps</Title>

      {creneaux.isPending && <Loader />}
      {creneaux.isError && (
        <Alert color="red" variant="light">
          {creneaux.error.message}
        </Alert>
      )}

      {creneaux.data && (
        <GrilleEmploiDuTemps
          creneaux={creneaux.data.map((c: Creneau) => ({
            key: String(c.id),
            matiereLibelle: c.matiereLibelle,
            enseignantNom: c.enseignantNom,
            salleNom: c.salleNom,
            jourSemaine: c.jourSemaine,
            heureDebut: c.heureDebut,
            heureFin: c.heureFin,
          }))}
        />
      )}
    </Stack>
  )
}
