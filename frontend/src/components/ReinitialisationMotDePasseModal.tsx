import { useState } from 'react'
import { Alert, Button, Code, CopyButton, Group, Modal, Stack, Text, TextInput } from '@mantine/core'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { reinitialiserMotDePasse } from '../api/endpoints'
import { genererMotDePasse } from '../utils/motDePasse'

interface Props {
  compte: { id: number; email: string; nom: string; prenom: string }
  onFermer: () => void
}

/**
 * Réinitialisation du mot de passe d'une personne par son administrateur.
 * Le mot de passe temporaire n'est affiché qu'une fois, ici : il n'est ni stocké ni journalisé.
 */
export default function ReinitialisationMotDePasseModal({ compte, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [motDePasse, setMotDePasse] = useState(() => genererMotDePasse())
  const [valide, setValide] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const reinitialisation = useMutation({
    mutationFn: () => reinitialiserMotDePasse(compte.id, motDePasse),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['comptes'] })
      setValide(motDePasse)
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={`Mot de passe de ${compte.prenom} ${compte.nom}`}>
      <Stack>
        {valide === null ? (
          <>
            <Text size="sm" c="dimmed">
              {compte.email}. Ses sessions ouvertes seront fermées et il ou elle devra choisir un nouveau mot
              de passe à la prochaine connexion.
            </Text>
            <TextInput
              label="Mot de passe temporaire"
              value={motDePasse}
              onChange={(e) => setMotDePasse(e.currentTarget.value)}
              error={motDePasse.length < 10 ? 'Au moins 10 caractères' : null}
              rightSectionWidth={110}
              rightSection={
                <Button size="compact-xs" variant="light" onClick={() => setMotDePasse(genererMotDePasse())}>
                  Régénérer
                </Button>
              }
            />
            {erreur !== null && (
              <Alert color="red" variant="light">
                {erreur}
              </Alert>
            )}
            <Group justify="flex-end">
              <Button variant="default" onClick={onFermer}>
                Annuler
              </Button>
              <Button
                disabled={motDePasse.length < 10}
                loading={reinitialisation.isPending}
                onClick={() => {
                  setErreur(null)
                  reinitialisation.mutate()
                }}
              >
                Réinitialiser
              </Button>
            </Group>
          </>
        ) : (
          <>
            <Alert color="green" variant="light">
              Mot de passe réinitialisé. Communiquez-le à la personne par un canal sûr : il ne sera plus affiché.
            </Alert>
            <Code block fz="lg">
              {valide}
            </Code>
            <Group justify="flex-end">
              <CopyButton value={valide}>
                {({ copied, copy }) => (
                  <Button variant="light" onClick={copy}>
                    {copied ? 'Copié' : 'Copier'}
                  </Button>
                )}
              </CopyButton>
              <Button onClick={onFermer}>Terminer</Button>
            </Group>
          </>
        )}
      </Stack>
    </Modal>
  )
}
