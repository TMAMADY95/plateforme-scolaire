import { Alert, Anchor, Loader, Stack, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerCreneauxClasse } from '../api/emploisDuTemps'
import { obtenirClasse } from '../api/endpoints'
import GrilleEmploiDuTemps from '../components/GrilleEmploiDuTemps'

/** Emploi du temps d'une classe (direction ou enseignant). */
export default function PageEmploiDuTempsClasse({ classeId }: { classeId: number }) {
  const classe = useQuery({
    queryKey: ['classe', classeId],
    queryFn: () => obtenirClasse(classeId),
  })

  const creneaux = useQuery({
    queryKey: ['creneaux-classe', classeId],
    queryFn: () => listerCreneauxClasse(classeId),
  })

  return (
    <Stack>
      <Anchor href="/classes" size="sm">
        ← Classes
      </Anchor>
      <Title order={2}>
        Emploi du temps{classe.data ? ` — ${classe.data.nom}` : ''}
      </Title>

      {(classe.isPending || creneaux.isPending) && <Loader />}
      {(classe.isError || creneaux.isError) && (
        <Alert color="red" variant="light">
          {(classe.error ?? creneaux.error)?.message}
        </Alert>
      )}

      {creneaux.data && (
        <GrilleEmploiDuTemps
          creneaux={creneaux.data.map((c) => ({
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
