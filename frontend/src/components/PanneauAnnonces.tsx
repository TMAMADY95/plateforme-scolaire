import { useState } from 'react'
import { Alert, Button, Card, Group, Loader, Stack, Text, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listerAnnonces, publierAnnonce, supprimerAnnonce } from '../api/endpoints'

function formaterPublication(iso: string): string {
  return new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

/** Annonces d'une classe. Avec `peutPublier`, un formulaire permet d'en écrire une (enseignant, direction). */
export default function PanneauAnnonces({ classeId, peutPublier }: { classeId: number | null; peutPublier: boolean }) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const annonces = useQuery({
    queryKey: ['annonces', classeId],
    queryFn: () => listerAnnonces(classeId as number),
    enabled: classeId !== null,
  })

  const formulaire = useForm({
    initialValues: { titre: '', contenu: '' },
    validate: {
      titre: (v) => (v.trim() === '' ? 'Titre requis' : null),
      contenu: (v) => (v.trim() === '' ? 'Texte requis' : null),
    },
  })

  const publication = useMutation({
    mutationFn: (v: { titre: string; contenu: string }) =>
      publierAnnonce(classeId as number, { titre: v.titre.trim(), contenu: v.contenu.trim() }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['annonces', classeId] })
      notifications.show({ color: 'green', title: 'Annonce publiée', message: 'Les familles de la classe peuvent la lire.' })
      formulaire.reset()
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })

  const suppression = useMutation({
    mutationFn: (id: number) => supprimerAnnonce(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['annonces', classeId] }),
    onError: (e) => notifications.show({ color: 'red', title: 'Suppression impossible', message: e.message }),
  })

  if (classeId === null) {
    return (
      <Alert color="blue" variant="light">
        Aucune inscription active : les annonces ne sont pas disponibles.
      </Alert>
    )
  }

  return (
    <Stack>
      {peutPublier && (
        <Card withBorder padding="md">
          <form onSubmit={formulaire.onSubmit((v) => publication.mutate(v))}>
            <Stack gap="xs">
              <Text fw={600}>Nouvelle annonce</Text>
              <TextInput label="Titre" maxLength={200} {...formulaire.getInputProps('titre')} />
              <Textarea
                label="Texte"
                description="Visible par les élèves de la classe et leurs parents"
                autosize
                minRows={3}
                maxRows={8}
                maxLength={4000}
                {...formulaire.getInputProps('contenu')}
              />
              {erreur !== null && (
                <Alert color="red" variant="light">
                  {erreur}
                </Alert>
              )}
              <Group justify="flex-end">
                <Button type="submit" loading={publication.isPending}>
                  Publier
                </Button>
              </Group>
            </Stack>
          </form>
        </Card>
      )}

      {annonces.isPending && <Loader />}
      {annonces.isError && (
        <Alert color="red" variant="light">
          {annonces.error.message}
        </Alert>
      )}
      {annonces.data && annonces.data.length === 0 && <Text c="dimmed">Aucune annonce pour le moment.</Text>}
      {annonces.data?.map((a) => (
        <Card key={a.id} withBorder padding="md">
          <Group justify="space-between" align="flex-start" wrap="nowrap">
            <div>
              <Text fw={600}>{a.titre}</Text>
              <Text size="xs" c="dimmed" mb={6}>
                {a.auteur} · {formaterPublication(a.publieLe)}
              </Text>
              <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>
                {a.contenu}
              </Text>
            </div>
            {a.modifiable && (
              <Button
                size="compact-sm"
                variant="subtle"
                color="red"
                loading={suppression.isPending && suppression.variables === a.id}
                onClick={() => suppression.mutate(a.id)}
              >
                Supprimer
              </Button>
            )}
          </Group>
        </Card>
      ))}
    </Stack>
  )
}
