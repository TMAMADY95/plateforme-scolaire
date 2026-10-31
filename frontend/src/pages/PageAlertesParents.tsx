import { Alert, Badge, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { alertesParentsEtablissement } from '../api/endpoints'
import { useUtilisateur } from '../auth/useAuth'
import { etablissementCourant } from '../auth/roles'

/**
 * Messages (SMS) envoyés aux parents au sujet de leurs enfants, par exemple en cas d'absence. Ces informations
 * concernent des élèves : elles ne sont visibles que par la direction de l'établissement, jamais par la
 * commune, la région ou le ministère.
 */
export default function PageAlertesParents() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const alertes = useQuery({
    queryKey: ['alertes-parents', etablissementId],
    queryFn: () => alertesParentsEtablissement(etablissementId as number),
    enabled: etablissementId !== undefined,
  })

  if (etablissementId === undefined) {
    return (
      <Alert color="orange" variant="light">
        Aucun établissement n'est associé à votre compte.
      </Alert>
    )
  }

  return (
    <Stack>
      <Title order={2}>Alertes aux parents</Title>
      <Text c="dimmed" size="sm">
        Les 200 derniers messages envoyés aux parents des élèves de votre établissement (absences). Ils ne sont visibles
        que par votre établissement.
      </Text>
      {alertes.isPending && <Loader />}
      {alertes.isError && <Alert color="red" variant="light">{alertes.error.message}</Alert>}
      {alertes.data && alertes.data.length === 0 && <Text c="dimmed">Aucun message envoyé pour le moment.</Text>}
      {alertes.data && alertes.data.length > 0 && (
        <Table verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Date</Table.Th>
              <Table.Th>Canal</Table.Th>
              <Table.Th>Parent</Table.Th>
              <Table.Th>Message</Table.Th>
              <Table.Th>Envoi</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {alertes.data.map((a) => (
              <Table.Tr key={a.id}>
                <Table.Td>{new Date(a.envoyeeLe).toLocaleString('fr-FR')}</Table.Td>
                <Table.Td>{a.canal === 'SMS' ? 'SMS' : 'E-mail'}</Table.Td>
                <Table.Td>{a.destinataire}</Table.Td>
                <Table.Td>{a.resume.replace(/^ABS#\d+\s+/, '')}</Table.Td>
                <Table.Td>
                  <Badge color={a.reussie ? 'green' : 'red'} variant="light">{a.reussie ? 'Envoyé' : 'Échec'}</Badge>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}
