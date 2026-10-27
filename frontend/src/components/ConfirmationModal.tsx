import { Button, Group, Modal, Stack, Text } from '@mantine/core'

interface Props {
  titre: string
  message: string
  libelleConfirmer: string
  couleur?: string
  enCours?: boolean
  onConfirmer: () => void
  onFermer: () => void
}

/** Fenêtre de confirmation pour les actions qu'on ne peut pas défaire facilement. */
export default function ConfirmationModal({
  titre,
  message,
  libelleConfirmer,
  couleur = 'red',
  enCours = false,
  onConfirmer,
  onFermer,
}: Props) {
  return (
    <Modal opened onClose={onFermer} title={titre}>
      <Stack>
        <Text>{message}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>
            Retour
          </Button>
          <Button color={couleur} loading={enCours} onClick={onConfirmer}>
            {libelleConfirmer}
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
