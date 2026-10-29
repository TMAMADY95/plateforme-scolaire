import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, NumberInput, Stack, Table, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { enregistrerCoefficients, listerCoefficients } from '../../api/endpoints'
import type { Classe, CoefficientMatiere } from '../../api/types'

/** Coefficient de chaque matière de la classe : il pondère la moyenne générale (1 par défaut). */
export default function CoefficientsModal({ classe, onFermer }: { classe: Classe; onFermer: () => void }) {
  const coefficients = useQuery({
    queryKey: ['coefficients', classe.id],
    queryFn: () => listerCoefficients(classe.id),
  })

  return (
    <Modal opened onClose={onFermer} title={`Coefficients — classe ${classe.nom}`}>
      {coefficients.isPending && <Loader />}
      {coefficients.isError && (
        <Alert color="red" variant="light">
          {coefficients.error.message}
        </Alert>
      )}
      {coefficients.data && <Formulaire classe={classe} initiaux={coefficients.data} onFermer={onFermer} />}
    </Modal>
  )
}

function Formulaire({
  classe,
  initiaux,
  onFermer,
}: {
  classe: Classe
  initiaux: CoefficientMatiere[]
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const [valeurs, setValeurs] = useState<Record<number, number | string>>(
    Object.fromEntries(initiaux.map((c) => [c.matiereId, c.coefficient])),
  )
  const [erreur, setErreur] = useState<string | null>(null)

  const enregistrement = useMutation({
    mutationFn: (lignes: { matiereId: number; coefficient: number }[]) => enregistrerCoefficients(classe.id, lignes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['coefficients', classe.id] })
      // Les moyennes générales et les rangs dépendent des coefficients.
      void queryClient.invalidateQueries({ queryKey: ['resultats'] })
      void queryClient.invalidateQueries({ queryKey: ['bulletin'] })
      notifications.show({
        color: 'green',
        title: 'Coefficients enregistrés',
        message: `Les moyennes de la classe ${classe.nom} tiennent compte des nouveaux coefficients.`,
      })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  function enregistrer() {
    setErreur(null)
    const lignes: { matiereId: number; coefficient: number }[] = []
    for (const c of initiaux) {
      const v = Number(valeurs[c.matiereId])
      if (!Number.isFinite(v) || v < 0.01 || v > 99.99) {
        setErreur(`${c.libelle} : le coefficient doit être compris entre 0,01 et 99,99.`)
        return
      }
      if (v !== c.coefficient) lignes.push({ matiereId: c.matiereId, coefficient: v })
    }
    if (lignes.length === 0) {
      notifications.show({ color: 'orange', message: 'Aucune modification à enregistrer.' })
      return
    }
    enregistrement.mutate(lignes)
  }

  if (initiaux.length === 0) {
    return (
      <Stack>
        <Text c="dimmed">
          Aucune matière n'est encore enseignée dans cette classe. Affectez d'abord des enseignants.
        </Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>
            Fermer
          </Button>
        </Group>
      </Stack>
    )
  }

  return (
    <Stack>
      <Text size="sm" c="dimmed">
        Une matière sans coefficient compte pour 1. Changer un coefficient modifie la moyenne générale et le rang de
        tous les élèves de la classe, y compris pour les périodes passées.
      </Text>
      <Table>
        <Table.Tbody>
          {initiaux.map((c) => (
            <Table.Tr key={c.matiereId}>
              <Table.Td>{c.libelle}</Table.Td>
              <Table.Td w={140}>
                <NumberInput
                  aria-label={`Coefficient de ${c.libelle}`}
                  min={0.01}
                  max={99.99}
                  decimalScale={2}
                  decimalSeparator=","
                  value={valeurs[c.matiereId]}
                  onChange={(v) => setValeurs((anciennes) => ({ ...anciennes, [c.matiereId]: v }))}
                />
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      {erreur !== null && (
        <Alert color="red" variant="light">
          {erreur}
        </Alert>
      )}
      <Group justify="flex-end">
        <Button variant="default" onClick={onFermer}>
          Annuler
        </Button>
        <Button onClick={enregistrer} loading={enregistrement.isPending}>
          Enregistrer
        </Button>
      </Group>
    </Stack>
  )
}
