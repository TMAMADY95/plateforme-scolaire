import { useState } from 'react'
import { Alert, Button, Card, Group, NumberInput, SimpleGrid, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { creerSalle, supprimerSalle } from '../../api/endpoints'
import type { Salle } from '../../api/types'
import ConfirmationModal from '../../components/ConfirmationModal'

interface Valeurs {
  nom: string
  capacite: number | string
  typeSalle: string
}

/** Liste des salles d'un établissement, avec ajout et suppression. */
export default function GestionSalles({ etablissementId, salles }: { etablissementId: number; salles: Salle[] }) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const [aSupprimer, setASupprimer] = useState<Salle | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: { nom: '', capacite: '', typeSalle: '' },
    validate: { nom: (v) => (v.trim() === '' ? 'Nom requis' : null) },
  })

  const ajout = useMutation({
    mutationFn: (v: Valeurs) =>
      creerSalle(etablissementId, {
        nom: v.nom.trim(),
        capacite: v.capacite === '' ? undefined : Number(v.capacite),
        typeSalle: v.typeSalle.trim() || undefined,
      }),
    onSuccess: (salle) => {
      void queryClient.invalidateQueries({ queryKey: ['salles'] })
      notifications.show({ color: 'green', title: 'Salle ajoutée', message: salle.nom })
      formulaire.reset()
    },
    onError: (e) => setErreur(e.message),
  })

  const suppression = useMutation({
    mutationFn: (salle: Salle) => supprimerSalle(etablissementId, salle.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['salles'] })
      void queryClient.invalidateQueries({ queryKey: ['creneaux-etab'] })
      setASupprimer(null)
    },
    onError: (e) => {
      setASupprimer(null)
      setErreur(e.message)
    },
  })

  return (
    <Stack>
      {salles.length === 0 && <Text c="dimmed">Aucune salle enregistrée.</Text>}
      {salles.length > 0 && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
          {salles.map((s) => (
            <Card key={s.id} withBorder padding="md">
              <Group justify="space-between">
                <Text fw={600}>{s.nom}</Text>
                <Text size="sm" c="dimmed">
                  {s.capacite !== null ? `${s.capacite} places` : '—'}
                </Text>
              </Group>
              <Group justify="space-between" mt={4}>
                <Text size="xs" c="dimmed">
                  {s.typeSalle ?? 'Type non précisé'}
                </Text>
                <Button size="compact-xs" color="red" variant="subtle" onClick={() => setASupprimer(s)}>
                  Supprimer
                </Button>
              </Group>
            </Card>
          ))}
        </SimpleGrid>
      )}

      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          ajout.mutate(valeurs)
        })}
      >
        <Group align="flex-end">
          <TextInput label="Nouvelle salle" placeholder="Salle 12" {...formulaire.getInputProps('nom')} />
          <NumberInput label="Capacité" min={1} allowDecimal={false} w={120} {...formulaire.getInputProps('capacite')} />
          <TextInput label="Type" placeholder="Laboratoire…" {...formulaire.getInputProps('typeSalle')} />
          <Button type="submit" loading={ajout.isPending}>
            Ajouter
          </Button>
        </Group>
      </form>

      {erreur !== null && (
        <Alert color="red" variant="light" withCloseButton onClose={() => setErreur(null)}>
          {erreur}
        </Alert>
      )}

      {aSupprimer && (
        <ConfirmationModal
          titre="Supprimer la salle"
          message={`Supprimer « ${aSupprimer.nom} » ? Impossible tant qu'un créneau l'utilise.`}
          libelleConfirmer="Supprimer"
          enCours={suppression.isPending}
          onConfirmer={() => suppression.mutate(aSupprimer)}
          onFermer={() => setASupprimer(null)}
        />
      )}
    </Stack>
  )
}
