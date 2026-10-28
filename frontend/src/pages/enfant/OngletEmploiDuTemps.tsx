import { Alert, Loader, Stack, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { emploiDuTempsClasse } from '../../api/endpoints'
import GrilleEmploiDuTemps from '../../components/GrilleEmploiDuTemps'
import type { CreneauEleve } from '../../api/types'

/** Bloc « Emploi du temps » réutilisable dans l'espace élève et l'espace parent. */
export default function OngletEmploiDuTemps({ classeId }: { classeId: number | null }) {
  const creneaux = useQuery({
    queryKey: ['emploi-du-temps-classe', classeId],
    queryFn: () => emploiDuTempsClasse(classeId as number),
    enabled: classeId !== null,
  })

  if (classeId === null) {
    return (
      <Alert color="blue" variant="light">
        Aucune inscription active : l'emploi du temps n'est pas disponible.
      </Alert>
    )
  }

  return (
    <Stack>
      <Title order={4}>Emploi du temps</Title>
      {creneaux.isPending && <Loader />}
      {creneaux.isError && (
        <Alert color="red" variant="light">
          {creneaux.error.message}
        </Alert>
      )}
      {creneaux.data && (
        <GrilleEmploiDuTemps
          creneaux={creneaux.data.map((c: CreneauEleve, i: number) => ({
            key: `${c.jourSemaine}-${c.heureDebut}-${i}`,
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
