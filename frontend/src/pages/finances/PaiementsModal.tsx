import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, NumberInput, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { enregistrerPaiement, listerPaiements } from '../../api/endpoints'
import type { Facture } from '../../api/types'
import { formaterMontant } from '../../utils/monnaie'

const MODES = [
  { value: 'ESPECES', label: 'Espèces' },
  { value: 'VIREMENT', label: 'Virement' },
  { value: 'CHEQUE', label: 'Chèque' },
]

const COULEURS: Record<string, string> = { VALIDE: 'green', EN_ATTENTE: 'orange', ECHEC: 'red', ANNULE: 'gray' }

interface Valeurs {
  montant: number | string
  mode: string
  reference: string
}

/** Historique des paiements d'une facture et saisie d'un encaissement (direction, personnel administratif). */
export default function PaiementsModal({ facture, onFermer }: { facture: Facture; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const paiements = useQuery({ queryKey: ['paiements', facture.id], queryFn: () => listerPaiements(facture.id) })

  const peutEncaisser = ['IMPAYEE', 'PARTIELLE'].includes(facture.statut) && facture.resteAPayer > 0

  const formulaire = useForm<Valeurs>({
    initialValues: { montant: facture.resteAPayer, mode: 'ESPECES', reference: '' },
    validate: {
      montant: (v) =>
        Number(v) > 0 && Number(v) <= facture.resteAPayer ? null : `Entre 0,01 et ${facture.resteAPayer}`,
      mode: (v) => (v ? null : 'Mode requis'),
    },
  })

  const encaissement = useMutation({
    mutationFn: (v: Valeurs) =>
      enregistrerPaiement(facture.id, {
        montant: Number(v.montant),
        mode: v.mode as 'ESPECES' | 'VIREMENT' | 'CHEQUE',
        reference: v.reference.trim() || undefined,
      }),
    onSuccess: () => {
      for (const cle of ['paiements', 'factures-etab', 'bilan-financier', 'factures-retard']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Paiement enregistré', message: facture.numero })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={`Paiements — ${facture.numero}`} size="lg">
      <Stack>
        <Text size="sm" c="dimmed">
          {facture.elevePrenom} {facture.eleveNom} · {facture.libelle} · reste{' '}
          {formaterMontant(facture.resteAPayer, facture.devise)}
        </Text>

        {paiements.isPending && <Loader />}
        {paiements.isError && (
          <Alert color="red" variant="light">
            {paiements.error.message}
          </Alert>
        )}
        {paiements.data && paiements.data.length === 0 && <Text c="dimmed">Aucun paiement pour le moment.</Text>}
        {paiements.data && paiements.data.length > 0 && (
          <Table.ScrollContainer minWidth={420}>
            <Table withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Mode</Table.Th>
                  <Table.Th>Montant</Table.Th>
                  <Table.Th>Référence</Table.Th>
                  <Table.Th>Statut</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {paiements.data.map((p) => (
                  <Table.Tr key={p.id}>
                    <Table.Td>{new Date(p.createdAt).toLocaleDateString('fr-FR')}</Table.Td>
                    <Table.Td>{p.mode}</Table.Td>
                    <Table.Td>{formaterMontant(p.montant, p.devise)}</Table.Td>
                    <Table.Td>{p.referenceExterne ?? p.referenceInterne}</Table.Td>
                    <Table.Td>
                      <Badge color={COULEURS[p.statut] ?? 'gray'} variant="light">
                        {p.statut}
                      </Badge>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}

        {peutEncaisser && (
          <form
            onSubmit={formulaire.onSubmit((valeurs) => {
              setErreur(null)
              encaissement.mutate(valeurs)
            })}
          >
            <Stack gap="xs">
              <Text fw={600}>Enregistrer un encaissement</Text>
              <Group grow align="flex-start">
                <NumberInput label="Montant" min={0.01} decimalScale={2} {...formulaire.getInputProps('montant')} />
                <Select label="Mode" data={MODES} allowDeselect={false} {...formulaire.getInputProps('mode')} />
              </Group>
              <TextInput
                label="Référence (facultatif)"
                description="Numéro de chèque ou de virement : une même référence ne peut pas être saisie deux fois"
                {...formulaire.getInputProps('reference')}
              />
              {erreur !== null && (
                <Alert color="red" variant="light">
                  {erreur}
                </Alert>
              )}
              <Group justify="flex-end">
                <Button variant="default" onClick={onFermer}>
                  Fermer
                </Button>
                <Button type="submit" loading={encaissement.isPending}>
                  Enregistrer
                </Button>
              </Group>
            </Stack>
          </form>
        )}
      </Stack>
    </Modal>
  )
}
