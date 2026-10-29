import { useState } from 'react'
import { Alert, Button, Group, Modal, Stack, Text, Textarea } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { annulerPaiement } from '../../api/endpoints'
import type { Paiement } from '../../api/types'
import { formaterMontant } from '../../utils/monnaie'

/** Annule un encaissement hors ligne. Le paiement reste visible dans l'historique, avec son motif. */
export default function AnnulerPaiementModal({
  paiement,
  onFermer,
  onAnnule,
}: {
  paiement: Paiement
  onFermer: () => void
  onAnnule: () => void
}) {
  const queryClient = useQueryClient()
  const [motif, setMotif] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)

  const annulation = useMutation({
    mutationFn: () => annulerPaiement(paiement.factureId, paiement.id, motif.trim()),
    onSuccess: () => {
      for (const cle of ['paiements', 'factures-etab', 'bilan-financier', 'factures-retard']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Paiement annulé', message: paiement.referenceExterne ?? paiement.referenceInterne })
      onAnnule()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title="Annuler ce paiement ?">
      <Stack>
        <Text size="sm">
          {formaterMontant(paiement.montant, paiement.devise)} ({paiement.mode}) sera retiré du solde de la facture. Le
          paiement reste visible dans l'historique avec son motif.
        </Text>
        <Textarea
          label="Motif"
          description="Obligatoire, conservé dans l'historique et le journal d'audit"
          maxLength={255}
          value={motif}
          onChange={(e) => setMotif(e.currentTarget.value)}
        />
        {erreur !== null && (
          <Alert color="red" variant="light">
            {erreur}
          </Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>
            Retour
          </Button>
          <Button color="red" disabled={motif.trim() === ''} loading={annulation.isPending} onClick={() => annulation.mutate()}>
            Annuler le paiement
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
