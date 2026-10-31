import { useRef, useState } from 'react'
import { Alert, Anchor, Button, Group, Stack, Text } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ajouterPieceJointe, listerPiecesJointes, supprimerPieceJointe, telechargerPieceJointe } from '../api/endpoints'

const taille = (octets: number) => (octets >= 1_048_576 ? `${(octets / 1_048_576).toFixed(1)} Mo` : `${Math.max(1, Math.round(octets / 1024))} Ko`)

/** Photos et PDF joints à une inspection (5 Mo au plus chacun, 10 au plus). */
export default function PiecesJointesInspection({ inspectionId, modifiable }: { inspectionId: number; modifiable: boolean }) {
  const queryClient = useQueryClient()
  const champ = useRef<HTMLInputElement>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const pieces = useQuery({ queryKey: ['pieces-jointes', inspectionId], queryFn: () => listerPiecesJointes(inspectionId) })
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['pieces-jointes', inspectionId] })

  const ajout = useMutation({
    mutationFn: (fichier: File) => ajouterPieceJointe(inspectionId, fichier),
    onSuccess: () => {
      setErreur(null)
      rafraichir()
    },
    onError: (e) => setErreur(e.message),
  })
  const suppression = useMutation({
    mutationFn: supprimerPieceJointe,
    onSuccess: rafraichir,
    onError: (e) => setErreur(e.message),
  })

  return (
    <Stack gap={4}>
      {pieces.data?.map((p) => (
        <Group key={p.id} gap="xs" wrap="nowrap">
          <Anchor size="xs" component="button" type="button" onClick={() => telechargerPieceJointe(p).catch((e: Error) => setErreur(e.message))}>
            {p.nom}
          </Anchor>
          <Text size="xs" c="dimmed">{taille(p.taille)}</Text>
          {modifiable && (
            <Anchor size="xs" c="red" component="button" type="button" onClick={() => suppression.mutate(p.id)}>
              Retirer
            </Anchor>
          )}
        </Group>
      ))}
      {erreur && <Alert color="red" variant="light" p="xs">{erreur}</Alert>}
      {modifiable && (
        <>
          <input
            ref={champ}
            type="file"
            accept="application/pdf,image/jpeg,image/png"
            hidden
            onChange={(e) => {
              const fichier = e.currentTarget.files?.[0]
              e.currentTarget.value = ''
              if (fichier) ajout.mutate(fichier)
            }}
          />
          <div>
            <Button size="compact-xs" variant="subtle" loading={ajout.isPending} onClick={() => champ.current?.click()}>
              Joindre un PDF ou une photo
            </Button>
          </div>
        </>
      )}
    </Stack>
  )
}
