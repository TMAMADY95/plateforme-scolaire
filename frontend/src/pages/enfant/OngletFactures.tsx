import { Alert, Badge, Button, Group, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery } from '@tanstack/react-query'
import { facturesDeMonEnfant, mesFactures, payerEnLigne } from '../../api/endpoints'
import { formaterMontant } from '../../utils/monnaie'
import { formaterDate } from '../../utils/dates'

/**
 * Factures d'un élève. Utilisable côté élève (son propre dossier) et côté parent
 * (un de ses enfants) : le paramètre `eleveId` à null sélectionne le mode élève.
 */
export default function OngletFactures({ eleveId }: { eleveId: number | null }) {
  const factures = useQuery({
    queryKey: ['factures', eleveId ?? 'moi'],
    queryFn: () => (eleveId === null ? mesFactures() : facturesDeMonEnfant(eleveId)),
  })

  const paiement = useMutation({
    mutationFn: (factureId: number) => payerEnLigne(factureId),
    // Le payeur termine son paiement chez le prestataire ; la confirmation nous parvient par notification signée.
    onSuccess: (initie) => window.location.assign(initie.urlPaiement),
    onError: (e) => notifications.show({ color: 'red', title: 'Paiement impossible', message: e.message }),
  })

  const donnees = factures.data ?? []
  const totalRestant = donnees.reduce((somme, f) => somme + f.resteAPayer, 0)
  const devise = donnees[0]?.devise

  return (
    <Stack>
      <Title order={4}>Frais scolaires</Title>

      {factures.isPending && <Loader />}
      {factures.isError && (
        <Alert color="red" variant="light">
          {factures.error.message}
        </Alert>
      )}
      {factures.data && donnees.length === 0 && (
        <Alert color="blue" variant="light">
          Aucune facture pour le moment.
        </Alert>
      )}

      {factures.data && donnees.length > 0 && (
        <>
          <Group gap="sm">
            <Badge size="lg" color={totalRestant > 0 ? 'red' : 'green'} variant="light">
              Reste à payer : {formaterMontant(totalRestant, devise)}
            </Badge>
            <Badge size="lg" variant="light">
              {donnees.length} facture(s)
            </Badge>
          </Group>

          <Table.ScrollContainer minWidth={600}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Numéro</Table.Th>
                  <Table.Th>Libellé</Table.Th>
                  <Table.Th>Montant dû</Table.Th>
                  <Table.Th>Déjà payé</Table.Th>
                  <Table.Th>Reste</Table.Th>
                  <Table.Th>Échéance</Table.Th>
                  <Table.Th>Statut</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {donnees.map((f) => (
                  <Table.Tr key={f.id}>
                    <Table.Td>{f.numero}</Table.Td>
                    <Table.Td>{f.libelle}</Table.Td>
                    <Table.Td>{formaterMontant(f.montantDu, f.devise)}</Table.Td>
                    <Table.Td>{formaterMontant(f.montantPaye, f.devise)}</Table.Td>
                    <Table.Td>{formaterMontant(f.resteAPayer, f.devise)}</Table.Td>
                    <Table.Td>{f.echeance ? formaterDate(f.echeance) : '—'}</Table.Td>
                    <Table.Td>
                      <Group gap="xs">
                        <Badge variant="light">{f.statut}</Badge>
                        {f.enRetard && (
                          <Badge color="red" variant="light">
                            En retard
                          </Badge>
                        )}
                      </Group>
                    </Table.Td>
                    <Table.Td>
                      {['IMPAYEE', 'PARTIELLE'].includes(f.statut) && f.resteAPayer > 0 && (
                        <Button
                          size="compact-xs"
                          loading={paiement.isPending && paiement.variables === f.id}
                          onClick={() => paiement.mutate(f.id)}
                        >
                          Payer
                        </Button>
                      )}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>

          <Text size="xs" c="dimmed">
            Le paiement en ligne n'est peut-être pas activé : dans ce cas, rapprochez-vous de
            l'établissement pour connaître les modalités de paiement acceptées.
          </Text>
        </>
      )}
    </Stack>
  )
}
