import { Alert, Card, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerCreneauxEtablissement, listerSalles } from '../api/emploisDuTemps'
import { listerClasses } from '../api/endpoints'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { useAuth } from '../auth/useAuth'
import { etablissementCourant, etablissementsGeres } from '../auth/roles'
import GrilleEmploiDuTemps from '../components/GrilleEmploiDuTemps'

/** Vue d'ensemble des emplois du temps d'un établissement (direction). */
export default function PageEmploisDuTemps() {
  const { utilisateur } = useAuth()
  const annee = useAnneeScolaire()

  const etablissementId = utilisateur ? etablissementCourant(utilisateur) : undefined
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const pret = etablissementId !== undefined && anneeId !== null

  const classes = useQuery({
    queryKey: ['classes-etab', etablissementId, anneeId],
    queryFn: () => listerClasses(etablissementId as number, anneeId as number),
    enabled: pret,
  })

  const creneaux = useQuery({
    queryKey: ['creneaux-etab', etablissementId, anneeId],
    queryFn: () => listerCreneauxEtablissement(etablissementId as number, anneeId as number),
    enabled: pret,
  })

  const salles = useQuery({
    queryKey: ['salles', etablissementId],
    queryFn: () => listerSalles(etablissementId as number),
    enabled: etablissementId !== undefined,
  })

  if (utilisateur === null || etablissementsGeres(utilisateur).length === 0) {
    return (
      <Alert color="blue" variant="light">
        Aucun établissement n'est rattaché à votre compte.
      </Alert>
    )
  }

  const nomClasse = (classeId: number) =>
    classes.data?.find((c) => c.id === classeId)?.nom ?? `Classe ${classeId}`

  const libelleAnnee = annee.options.find((o) => o.value === annee.anneeId)?.label

  return (
    <Stack>
      <Title order={2}>Emplois du temps</Title>
      <Text c="dimmed" size="sm">
        {libelleAnnee ? `Année scolaire ${libelleAnnee}` : 'Année scolaire en cours'}
      </Text>

      {(classes.isPending || creneaux.isPending || salles.isPending) && pret && <Loader />}
      {(classes.isError || creneaux.isError || salles.isError) && (
        <Alert color="red" variant="light">
          {(classes.error ?? creneaux.error ?? salles.error)?.message}
        </Alert>
      )}

      <Title order={3} mt="md">
        Cours par classe
      </Title>
      {creneaux.data && creneaux.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucun créneau n'a encore été saisi pour cette année.
        </Alert>
      )}
      {creneaux.data && creneaux.data.length > 0 && (
        <Stack>
          {[...new Set(creneaux.data.map((c) => c.classeId))].map((classeId) => (
            <Card key={classeId} withBorder padding="md">
              <Text fw={600} mb="sm">
                {nomClasse(classeId)}
              </Text>
              <GrilleEmploiDuTemps
                creneaux={creneaux.data
                  .filter((c) => c.classeId === classeId)
                  .map((c) => ({
                    key: String(c.id),
                    matiereLibelle: c.matiereLibelle,
                    enseignantNom: c.enseignantNom,
                    salleNom: c.salleNom,
                    jourSemaine: c.jourSemaine,
                    heureDebut: c.heureDebut,
                    heureFin: c.heureFin,
                  }))}
              />
            </Card>
          ))}
        </Stack>
      )}

      <Title order={3} mt="md">
        Salles{salles.data ? ` (${salles.data.length})` : ''}
      </Title>
      {salles.data && salles.data.length === 0 && <Text c="dimmed">Aucune salle enregistrée.</Text>}
      {salles.data && salles.data.length > 0 && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
          {salles.data.map((s) => (
            <Card key={s.id} withBorder padding="md">
              <Group justify="space-between">
                <Text fw={600}>{s.nom}</Text>
                <Text size="sm" c="dimmed">
                  {s.capacite !== null ? `${s.capacite} places` : '—'}
                </Text>
              </Group>
              <Text size="xs" c="dimmed" mt={4}>
                {s.typeSalle ?? 'Type non précisé'}
              </Text>
            </Card>
          ))}
        </SimpleGrid>
      )}
    </Stack>
  )
}
