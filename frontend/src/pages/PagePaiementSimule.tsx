import { Alert, Button, Card, Group, Stack, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router'
import { simulerPaiement } from '../api/endpoints'

/**
 * Faux écran de prestataire de paiement, pour le développement uniquement (le backend ne l'expose
 * qu'avec le profil dev). Un vrai prestataire a sa propre page : l'utilisateur n'arrive jamais ici.
 */
export default function PagePaiementSimule() {
  const { reference } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const simulation = useMutation({
    mutationFn: (succes: boolean) => simulerPaiement(reference as string, succes),
    onSuccess: (_, succes) => {
      void queryClient.invalidateQueries({ queryKey: ['factures'] })
      notifications.show({
        color: succes ? 'green' : 'orange',
        title: succes ? 'Paiement réussi' : 'Paiement échoué',
        message: reference,
      })
      void navigate(-1)
    },
  })

  return (
    <Stack maw={480}>
      <Title order={2}>Prestataire de paiement (simulation)</Title>
      <Alert color="yellow" variant="light">
        Environnement de développement : aucun argent n'est échangé.
      </Alert>
      <Card withBorder padding="lg">
        <Text>Transaction {reference}</Text>
        <Group mt="md">
          <Button color="green" loading={simulation.isPending} onClick={() => simulation.mutate(true)}>
            Simuler un succès
          </Button>
          <Button color="red" variant="light" loading={simulation.isPending} onClick={() => simulation.mutate(false)}>
            Simuler un échec
          </Button>
        </Group>
        {simulation.isError && (
          <Alert color="red" variant="light" mt="md">
            {simulation.error.message}
          </Alert>
        )}
      </Card>
    </Stack>
  )
}
