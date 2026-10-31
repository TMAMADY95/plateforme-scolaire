import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Select, Stack, Table, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listerEnseignants, listerEtablissements, muterEnseignant } from '../api/endpoints'
import type { Enseignant, LignePostes } from '../api/types'

/** Les enseignants d'un établissement, avec la possibilité de les muter vers un autre établissement du périmètre. */
export default function MutationEnseignantsModal({ etablissement, onFermer }: { etablissement: LignePostes; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const enseignants = useQuery({ queryKey: ['enseignants', etablissement.id], queryFn: () => listerEnseignants(etablissement.id) })
  const etablissements = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })
  const public_ = etablissement.secteur === 'PUBLIC'
  const [choisi, setChoisi] = useState<Enseignant | null>(null)
  const [destination, setDestination] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => muterEnseignant(choisi!.id, { depuisEtablissementId: etablissement.id, versEtablissementId: Number(destination) }),
    onSuccess: () => {
      notifications.show({ color: 'green', title: 'Enseignant muté', message: `${choisi?.prenom} ${choisi?.nom}` })
      void queryClient.invalidateQueries({ queryKey: ['enseignants'] })
      void queryClient.invalidateQueries({ queryKey: ['postes'] })
      void queryClient.invalidateQueries({ queryKey: ['synthese-nationale-statistiques'] })
      setChoisi(null)
      setDestination(null)
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} size="lg" title={`Enseignants : ${etablissement.nom}`}>
      <Stack>
        {enseignants.isPending && <Loader />}
        {enseignants.isError && (
          <Alert color="red" variant="light">
            {enseignants.error.message}
          </Alert>
        )}
        {!public_ && (
          <Alert color="blue" variant="light">
            Établissement privé : ses enseignants ne peuvent pas être mutés. Les mutations se font uniquement entre établissements publics.
          </Alert>
        )}
        {enseignants.data && enseignants.data.length === 0 && <Text c="dimmed">Aucun enseignant dans cet établissement.</Text>}
        {enseignants.data && enseignants.data.length > 0 && (
          <Table verticalSpacing="xs">
            <Table.Tbody>
              {enseignants.data.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td>
                    <Text fw={600}>
                      {e.prenom} {e.nom}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {e.matricule ?? e.email}
                    </Text>
                  </Table.Td>
                  <Table.Td ta="right">
                    {public_ && <Button size="compact-xs" variant={choisi?.id === e.id ? 'filled' : 'light'} onClick={() => setChoisi(choisi?.id === e.id ? null : e)}>
                      Muter
                    </Button>}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
        {choisi && (
          <Stack gap="xs">
            <Select
              label={`Muter ${choisi.prenom} ${choisi.nom} vers`}
              description="Établissements publics uniquement. Ses affectations aux classes de cet établissement seront retirées ; ses notes déjà saisies restent."
              searchable
              placeholder="Choisir l'établissement d'accueil"
              data={(etablissements.data ?? [])
                .filter((e) => e.id !== etablissement.id && e.statut === 'ACTIF' && e.secteur === 'PUBLIC')
                .map((e) => ({ value: String(e.id), label: `${e.nom} (${e.codeOfficiel})` }))}
              value={destination}
              onChange={setDestination}
            />
            {erreur && (
              <Alert color="red" variant="light">
                {erreur}
              </Alert>
            )}
            <Group justify="flex-end">
              <Button onClick={() => mutation.mutate()} loading={mutation.isPending} disabled={destination === null}>
                Confirmer la mutation
              </Button>
            </Group>
          </Stack>
        )}
      </Stack>
    </Modal>
  )
}
