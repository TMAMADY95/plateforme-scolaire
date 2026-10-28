import { useState } from 'react'
import { Alert, Button, Card, Group, Loader, Select, Stack, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listerClasses, listerCreneauxEtablissement, listerSalles, supprimerCreneau } from '../api/endpoints'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { useAuth } from '../auth/useAuth'
import { etablissementCourant, etablissementsGeres } from '../auth/roles'
import GrilleEmploiDuTemps from '../components/GrilleEmploiDuTemps'
import ConfirmationModal from '../components/ConfirmationModal'
import type { Creneau } from '../api/types'
import CreneauModal from './emplois/CreneauModal'
import GestionSalles from './emplois/GestionSalles'

interface CibleCreneau {
  classeId: number
  creneau?: Creneau
}

/** Gestion des emplois du temps d'un établissement (direction) : créneaux par classe et salles. */
export default function PageEmploisDuTemps() {
  const { utilisateur } = useAuth()
  const annee = useAnneeScolaire()
  const queryClient = useQueryClient()
  const [classeAjout, setClasseAjout] = useState<string | null>(null)
  const [edition, setEdition] = useState<CibleCreneau | null>(null)
  const [aSupprimer, setASupprimer] = useState<Creneau | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

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

  const suppression = useMutation({
    mutationFn: (creneau: Creneau) => supprimerCreneau(creneau.id),
    onSuccess: () => {
      for (const cle of ['creneaux-etab', 'creneaux-classe']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Créneau supprimé', message: '' })
      setASupprimer(null)
    },
    onError: (e) => {
      setASupprimer(null)
      setErreur(e.message)
    },
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
      {erreur !== null && (
        <Alert color="red" variant="light" withCloseButton onClose={() => setErreur(null)}>
          {erreur}
        </Alert>
      )}

      {classes.data && (
        <Group align="flex-end">
          <Select
            label="Ajouter un créneau à la classe"
            placeholder="Choisir une classe"
            searchable
            data={classes.data.map((c) => ({ value: String(c.id), label: c.nom }))}
            value={classeAjout}
            onChange={setClasseAjout}
          />
          <Button
            disabled={classeAjout === null}
            onClick={() => classeAjout !== null && setEdition({ classeId: Number(classeAjout) })}
          >
            Nouveau créneau
          </Button>
        </Group>
      )}

      <Title order={3} mt="md">
        Cours par classe
      </Title>
      {creneaux.data && creneaux.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucun créneau n'a encore été saisi pour cette année. Choisissez une classe ci-dessus pour
          commencer.
        </Alert>
      )}
      {creneaux.data && creneaux.data.length > 0 && (
        <Stack>
          {[...new Set(creneaux.data.map((c) => c.classeId))].map((classeId) => (
            <Card key={classeId} withBorder padding="md">
              <Group justify="space-between" mb="sm">
                <Text fw={600}>{nomClasse(classeId)}</Text>
                <Button size="compact-sm" variant="light" onClick={() => setEdition({ classeId })}>
                  Ajouter un créneau
                </Button>
              </Group>
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
                actions={(key) => {
                  const creneau = creneaux.data.find((c) => String(c.id) === key)
                  if (!creneau) return null
                  return (
                    <Group gap="xs" wrap="nowrap">
                      <Button size="compact-xs" variant="light" onClick={() => setEdition({ classeId, creneau })}>
                        Modifier
                      </Button>
                      <Button size="compact-xs" variant="subtle" color="red" onClick={() => setASupprimer(creneau)}>
                        Supprimer
                      </Button>
                    </Group>
                  )
                }}
              />
            </Card>
          ))}
        </Stack>
      )}

      <Title order={3} mt="md">
        Salles{salles.data ? ` (${salles.data.length})` : ''}
      </Title>
      {salles.data && etablissementId !== undefined && (
        <GestionSalles etablissementId={etablissementId} salles={salles.data} />
      )}

      {edition && etablissementId !== undefined && (
        <CreneauModal
          etablissementId={etablissementId}
          classeId={edition.classeId}
          classeNom={nomClasse(edition.classeId)}
          creneau={edition.creneau}
          onFermer={() => setEdition(null)}
        />
      )}
      {aSupprimer && (
        <ConfirmationModal
          titre="Supprimer le créneau"
          message={`Supprimer ${aSupprimer.matiereLibelle} (${nomClasse(aSupprimer.classeId)}) ?`}
          libelleConfirmer="Supprimer"
          enCours={suppression.isPending}
          onConfirmer={() => suppression.mutate(aSupprimer)}
          onFermer={() => setASupprimer(null)}
        />
      )}
    </Stack>
  )
}
