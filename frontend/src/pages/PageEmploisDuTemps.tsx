import { Alert, Card, Group, Loader, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerCreneauxEtablissement, listerSalles } from '../api/emploisDuTemps'
import { listerClasses } from '../api/endpoints'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { useAuth } from '../auth/useAuth'
import { etablissementCourant } from '../auth/roles'
import GrilleEmploiDuTemps from '../components/GrilleEmploiDuTemps'
import { formaterPlage } from '../utils/horaires'

/** Vue d'ensemble des emplois du temps d'un établissement (direction). */
export default function PageEmploisDuTemps() {
  const { utilisateur } = useAuth()
  const annee = useAnneeScolaire()
  const etablissementId = utilisateur ? etablissementCourant(utilisateur) : undefined

  const classes = useQuery({
    queryKey: ['classes-etab', etablissementId, annee.anneeId],
    queryFn: () => listerClasses(etablissementId as number, annee.anneeId as number),
    enabled: etablissementId !== undefined && annee.anneeId !== null,
  })

  const creneaux = useQuery({
    queryKey: ['creneaux-etab', etablissementId, annee.anneeId],
    queryFn: () => listerCreneauxEtablissement(etablissementId as number, annee.anneeId as number),
    enabled: etablissementId !== undefined && annee.anneeId !== null,
  })

  const salles = useQuery({
    queryKey: ['salles', etablissementId],
    queryFn: () => listerSalles(etablissementId as number),
    enabled: etablissementId !== undefined,
  })

  if (etablissementId === undefined) {
    return (
      <Alert color="blue" variant="light">
        Aucun établissement n'est rattaché à votre compte.
      </Alert>
    )
  }

  const nomClasse = (classeId: number) =>
    classes.data?.find((c) => c.id === classeId)?.nom ?? `Classe ${classeId}`

  return (
    <Stack>
      <Title order={2}>Emplois du temps</Title>
      <Text c="dimmed" size="sm">
        {annee.libelle !== null ? `Année scolaire ${annee.libelle}` : 'Année scolaire en cours'}
      </Text>

      {(classes.isPending || creneaux.isPending || salles.isPending) && <Loader />}
      {(classes.isError || creneaux.isError || salles.isError) && (
        <Alert color="red" variant="light">
          {(classes.error ?? creneaux.error ?? salles.error)?.message}
        </Alert>
      )}

      {creneaux.data && (
        <Title order={3} mt="md">
          Cours par classe
        </Title>
      )}
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

      {salles.data && (
        <>
          <Title order={3} mt="md">
            Salles ({salles.data.length})
          </Title>
          {salles.data.length === 0 ? (
            <Text c="dimmed">Aucune salle enregistrée.</Text>
          ) : (
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
        </>
      )}

      <Text size="xs" c="dimmed" mt="md">
        Pour modifier un emploi du temps, ouvrez la fiche d'une classe.
      </Text>
      {creneaux.data && creneaux.data.length > 0 && (
        <Table>
          <Table.Tbody>
            <Table.Tr>
              <Table.Td c="dimmed" w={120}>
                Premier cours
              </Table.Td>
              <Table.Td>{formaterPlage(creneaux.data[0].heureDebut, creneaux.data[0].heureFin)}</Table.Td>
            </Table.Tr>
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}
