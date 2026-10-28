import { useState } from 'react'
import { Alert, Button, Group, Modal, NumberInput, Select, Stack, Text, Textarea } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { exonererFacture } from '../../api/endpoints'
import type { Facture } from '../../api/types'
import { formaterMontant } from '../../utils/monnaie'

const TYPES = [
  { value: 'BOURSE', label: 'Bourse' },
  { value: 'REMISE', label: 'Remise' },
  { value: 'AIDE_SOCIALE', label: 'Aide sociale' },
  { value: 'CORRECTION', label: 'Correction' },
]

interface Valeurs {
  montant: number | string
  motif: string
  type: string
}

/** Exonération (totale ou partielle) d'une facture sur laquelle aucun paiement n'a été encaissé. */
export default function ExonerationModal({ facture, onFermer }: { facture: Facture; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: { montant: '', motif: '', type: 'REMISE' },
    validate: {
      montant: (v) =>
        Number(v) >= 0.01 && Number(v) <= facture.resteAPayer
          ? null
          : `Entre 0,01 et ${facture.resteAPayer}`,
      motif: (v) => (v.trim() === '' ? 'Motif requis' : null),
    },
  })

  const exoneration = useMutation({
    mutationFn: (v: Valeurs) =>
      exonererFacture(facture.id, { montant: Number(v.montant), motif: v.motif.trim(), typeExoneration: v.type }),
    onSuccess: () => {
      for (const cle of ['factures-etab', 'bilan-financier', 'factures-retard', 'paiements']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Exonération enregistrée', message: facture.numero })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={`Exonérer — ${facture.numero}`}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          exoneration.mutate(valeurs)
        })}
      >
        <Stack>
          <Text size="sm" c="dimmed">
            {facture.elevePrenom} {facture.eleveNom} · {facture.libelle} · reste{' '}
            {formaterMontant(facture.resteAPayer, facture.devise)}
          </Text>
          <Select label="Type" allowDeselect={false} data={TYPES} {...formulaire.getInputProps('type')} />
          <NumberInput
            label="Montant exonéré"
            min={0.01}
            max={facture.resteAPayer}
            decimalScale={2}
            {...formulaire.getInputProps('montant')}
          />
          <Textarea label="Motif" autosize minRows={2} maxLength={255} {...formulaire.getInputProps('motif')} />
          <Alert color="orange" variant="light">
            L'exonération est enregistrée dans le journal d'audit et ne peut pas être retirée depuis cet écran.
          </Alert>
          {erreur !== null && (
            <Alert color="red" variant="light">
              {erreur}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onFermer}>
              Annuler
            </Button>
            <Button type="submit" loading={exoneration.isPending}>
              Exonérer
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
