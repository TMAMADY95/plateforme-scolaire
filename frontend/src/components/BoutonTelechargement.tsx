import { Button } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation } from '@tanstack/react-query'

/** Bouton qui lance un téléchargement (PDF...) et signale l'échec par une notification. */
export default function BoutonTelechargement({
  telecharger,
  children,
  compact = false,
}: {
  telecharger: () => Promise<void>
  children: string
  compact?: boolean
}) {
  const telechargement = useMutation({
    mutationFn: telecharger,
    onError: (e) => notifications.show({ color: 'red', title: 'Téléchargement impossible', message: e.message }),
  })
  return (
    <Button
      variant="light"
      size={compact ? 'compact-xs' : 'sm'}
      loading={telechargement.isPending}
      onClick={() => telechargement.mutate()}
    >
      {children}
    </Button>
  )
}
