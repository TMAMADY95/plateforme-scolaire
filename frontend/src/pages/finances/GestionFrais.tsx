import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Stack, Table, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { desactiverFrais, listerFrais } from '../../api/endpoints'
import type { Frais } from '../../api/types'
import ConfirmationModal from '../../components/ConfirmationModal'
import { formaterDate } from '../../utils/dates'
import { formaterMontant } from '../../utils/monnaie'
import FraisModal from './FraisModal'

/** Barème des frais d'un établissement pour une année scolaire. */
export default function GestionFrais({
  etablissementId,
  anneeScolaireId,
}: {
  etablissementId: number
  anneeScolaireId: number
}) {
  const queryClient = useQueryClient()
  const [edition, setEdition] = useState<{ frais?: Frais } | null>(null)
  const [aDesactiver, setADesactiver] = useState<Frais | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const frais = useQuery({
    queryKey: ['frais', etablissementId, anneeScolaireId],
    queryFn: () => listerFrais(etablissementId, anneeScolaireId),
  })

  const desactivation = useMutation({
    mutationFn: (f: Frais) => desactiverFrais(f.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['frais'] })
      notifications.show({ color: 'green', title: 'Frais désactivé', message: '' })
      setADesactiver(null)
    },
    onError: (e) => {
      setADesactiver(null)
      setErreur(e.message)
    },
  })

  const deviseExistante = frais.data && frais.data.length > 0 ? frais.data[0].devise : null

  return (
    <Stack>
      <Group justify="space-between">
        <Text c="dimmed" size="sm">
          Le barème sert de base à la génération des factures. Un frais désactivé n'est plus facturé.
        </Text>
        <Button onClick={() => setEdition({})}>Nouveau frais</Button>
      </Group>

      {frais.isPending && <Loader />}
      {frais.isError && (
        <Alert color="red" variant="light">
          {frais.error.message}
        </Alert>
      )}
      {erreur !== null && (
        <Alert color="red" variant="light" withCloseButton onClose={() => setErreur(null)}>
          {erreur}
        </Alert>
      )}
      {frais.data && frais.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucun frais défini pour cette année. Ajoutez au moins un frais avant de générer les factures.
        </Alert>
      )}
      {frais.data && frais.data.length > 0 && (
        <Table.ScrollContainer minWidth={640}>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Libellé</Table.Th>
                <Table.Th>Niveau</Table.Th>
                <Table.Th>Montant</Table.Th>
                <Table.Th>Échéance</Table.Th>
                <Table.Th>État</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {frais.data.map((f) => (
                <Table.Tr key={f.id}>
                  <Table.Td>{f.libelle}</Table.Td>
                  <Table.Td>{f.niveauLibelle ?? 'Tous'}</Table.Td>
                  <Table.Td>{formaterMontant(f.montant, f.devise)}</Table.Td>
                  <Table.Td>{f.echeance ? formaterDate(f.echeance) : '—'}</Table.Td>
                  <Table.Td>
                    <Group gap="xs">
                      <Badge color={f.actif ? 'green' : 'gray'} variant="light">
                        {f.actif ? 'Actif' : 'Désactivé'}
                      </Badge>
                      {!f.obligatoire && <Badge variant="light">Facultatif</Badge>}
                    </Group>
                  </Table.Td>
                  <Table.Td>
                    <Group gap="xs" wrap="nowrap">
                      <Button size="compact-xs" variant="light" onClick={() => setEdition({ frais: f })}>
                        Modifier
                      </Button>
                      {f.actif && (
                        <Button size="compact-xs" variant="subtle" color="red" onClick={() => setADesactiver(f)}>
                          Désactiver
                        </Button>
                      )}
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {edition && (
        <FraisModal
          etablissementId={etablissementId}
          anneeScolaireId={anneeScolaireId}
          deviseExistante={deviseExistante}
          frais={edition.frais}
          onFermer={() => setEdition(null)}
        />
      )}
      {aDesactiver && (
        <ConfirmationModal
          titre="Désactiver le frais"
          message={`Désactiver « ${aDesactiver.libelle} » ? Les factures déjà émises restent inchangées.`}
          libelleConfirmer="Désactiver"
          enCours={desactivation.isPending}
          onConfirmer={() => desactivation.mutate(aDesactiver)}
          onFermer={() => setADesactiver(null)}
        />
      )}
    </Stack>
  )
}
