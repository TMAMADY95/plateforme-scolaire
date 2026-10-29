import { useEffect, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Grid,
  Group,
  Loader,
  Paper,
  ScrollArea,
  Stack,
  Text,
  Textarea,
  Title,
  UnstyledButton,
} from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import { listerConversations, ouvrirConversation, repondreConversation } from '../api/endpoints'
import type { Conversation } from '../api/types'
import { estParent } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import NouveauMessageModal from '../components/messagerie/NouveauMessageModal'

function formaterHorodatage(iso: string): string {
  const d = new Date(iso)
  return d.toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export default function PageMessages() {
  const utilisateur = useUtilisateur()
  const [params, setParams] = useSearchParams()
  const choisie = params.get('c') === null ? null : Number(params.get('c'))
  const [nouveauOuvert, { open, close }] = useDisclosure()

  const liste = useQuery({ queryKey: ['messagerie', 'liste'], queryFn: listerConversations, refetchInterval: 30_000 })

  const ouvrir = (id: number | null) => setParams(id === null ? {} : { c: String(id) })

  return (
    <Stack>
      <Group justify="space-between" align="flex-end">
        <div>
          <Title order={2}>Messages</Title>
          <Text c="dimmed">
            {estParent(utilisateur)
              ? "Échangez avec les enseignants et la direction de l'école."
              : "Échangez avec les parents. Pour écrire à un parent, utilisez le bouton « Écrire » de la fiche d'un élève."}
          </Text>
        </div>
        {estParent(utilisateur) && <Button onClick={open}>Nouveau message</Button>}
      </Group>

      {liste.isPending && <Loader />}
      {liste.isError && (
        <Alert color="red" variant="light">
          {liste.error.message}
        </Alert>
      )}
      {liste.data && liste.data.length === 0 && <Text c="dimmed">Aucune conversation pour le moment.</Text>}

      {liste.data && liste.data.length > 0 && (
        <Grid gap="md">
          <Grid.Col span={{ base: 12, md: 4 }}>
          <Card withBorder padding={0} style={{ overflow: 'hidden' }}>
            <ScrollArea.Autosize mah={560}>
              {liste.data.map((c) => (
                <UnstyledButton
                  key={c.id}
                  w="100%"
                  p="sm"
                  onClick={() => ouvrir(c.id)}
                  style={{
                    borderBottom: '1px solid var(--mantine-color-gray-2)',
                    background: c.id === choisie ? 'var(--mantine-color-blue-0)' : undefined,
                  }}
                >
                  <Group justify="space-between" wrap="nowrap" gap="xs">
                    <Text fw={c.nonLu ? 700 : 500} truncate>
                      {c.interlocuteur}
                    </Text>
                    <Group gap={6} wrap="nowrap">
                      {c.nonLu && <Badge size="xs" circle color="red" />}
                      <Text size="xs" c="dimmed">
                        {formaterHorodatage(c.dernierMessageLe)}
                      </Text>
                    </Group>
                  </Group>
                  <Text size="sm" fw={c.nonLu ? 600 : 400} truncate>
                    {c.sujet}
                  </Text>
                  <Text size="xs" c="dimmed" truncate>
                    {c.eleve} · {c.apercu}
                  </Text>
                </UnstyledButton>
              ))}
            </ScrollArea.Autosize>
          </Card>
          </Grid.Col>

          <Grid.Col span={{ base: 12, md: 8 }}>
            {choisie === null ? (
              <Text c="dimmed">Choisissez une conversation.</Text>
            ) : (
              <Fil key={choisie} id={choisie} />
            )}
          </Grid.Col>
        </Grid>
      )}

      {nouveauOuvert && (
        <NouveauMessageModal
          onFermer={close}
          onCree={(id) => {
            close()
            ouvrir(id)
          }}
        />
      )}
    </Stack>
  )
}

/** Une conversation ouverte : messages et zone de réponse. Ouvrir la conversation la marque comme lue. */
function Fil({ id }: { id: number }) {
  const queryClient = useQueryClient()
  const [texte, setTexte] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const bas = useRef<HTMLDivElement>(null)

  const conversation = useQuery({
    queryKey: ['messagerie', 'conversation', id],
    queryFn: () => ouvrirConversation(id),
    refetchInterval: 15_000,
  })

  const dernier = conversation.data?.dernierMessageLe
  useEffect(() => {
    if (dernier === undefined) return
    // La lecture est enregistrée côté serveur : on rafraîchit la liste et le compteur.
    void queryClient.invalidateQueries({ queryKey: ['messagerie', 'liste'] })
    void queryClient.invalidateQueries({ queryKey: ['messagerie', 'non-lus'] })
    bas.current?.scrollIntoView({ block: 'end' })
  }, [dernier, queryClient])

  const envoi = useMutation({
    mutationFn: (contenu: string) => repondreConversation(id, contenu),
    onSuccess: (c: Conversation) => {
      queryClient.setQueryData(['messagerie', 'conversation', id], c)
      void queryClient.invalidateQueries({ queryKey: ['messagerie', 'liste'] })
      setTexte('')
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })

  if (conversation.isPending) return <Loader />
  if (conversation.isError) {
    return (
      <Alert color="red" variant="light">
        {conversation.error.message}
      </Alert>
    )
  }
  const c = conversation.data

  return (
    <Card withBorder padding="md">
      <Stack gap="sm">
        <div>
          <Text fw={700}>{c.sujet}</Text>
          <Text size="sm" c="dimmed">
            {c.interlocuteur} ({c.roleInterlocuteur}) · à propos de {c.eleve}
          </Text>
        </div>

        <ScrollArea.Autosize mah={420}>
          <Stack gap="xs" pr="xs">
            {c.messages.map((m) => (
              <Paper
                key={m.id}
                p="sm"
                radius="md"
                bg={m.moi ? 'blue.0' : 'gray.0'}
                style={{ alignSelf: m.moi ? 'flex-end' : 'flex-start', maxWidth: '85%' }}
              >
                <Text size="xs" c="dimmed">
                  {m.moi ? 'Moi' : m.auteur} · {formaterHorodatage(m.envoyeLe)}
                </Text>
                <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>
                  {m.contenu}
                </Text>
              </Paper>
            ))}
            <div ref={bas} />
          </Stack>
        </ScrollArea.Autosize>

        {c.active ? (
          <Stack gap="xs">
            <Textarea
              placeholder="Votre réponse"
              autosize
              minRows={2}
              maxRows={6}
              maxLength={4000}
              value={texte}
              onChange={(e) => setTexte(e.currentTarget.value)}
            />
            {erreur !== null && (
              <Alert color="red" variant="light">
                {erreur}
              </Alert>
            )}
            <Group justify="flex-end">
              <Button
                loading={envoi.isPending}
                disabled={texte.trim() === ''}
                onClick={() => envoi.mutate(texte.trim())}
              >
                Envoyer
              </Button>
            </Group>
          </Stack>
        ) : (
          <Alert color="gray" variant="light">
            Cette conversation n'est plus active : vous pouvez la relire mais plus y répondre.
          </Alert>
        )}
      </Stack>
    </Card>
  )
}
