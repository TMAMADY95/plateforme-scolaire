import { useState } from 'react'
import { Alert, Badge, Button, Group, Modal, Stack, Table, Text, TextInput } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { delivrerDiplomes, diplomesExamen, revoquerDiplome } from '../api/endpoints'
import type { DiplomesExamen, Examen } from '../api/types'

export const LIBELLE_MENTION = { PASSABLE: 'Passable', ASSEZ_BIEN: 'Assez bien', BIEN: 'Bien', TRES_BIEN: 'Très bien' }

/** Diplômes d'un examen : délivrance aux admis après la proclamation, révocation motivée. */
export default function DiplomesExamenModal({ examen, onFermer }: { examen: Examen; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const cle = ['diplomes-examen', examen.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => diplomesExamen(examen.id) })
  const [erreur, setErreur] = useState<string | null>(null)
  const appliquer = (d: DiplomesExamen) => {
    queryClient.setQueryData(cle, d)
    setErreur(null)
  }
  const delivrance = useMutation({ mutationFn: () => delivrerDiplomes(examen.id), onSuccess: appliquer, onError: (e) => setErreur(e.message) })
  const d = donnees.data

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Diplômes : ${examen.libelle}`}>
      <Stack gap="sm">
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Group gap="xs">
              <Badge variant="light">{d.admis} admis</Badge>
              <Badge color="green" variant="light">{d.delivres} diplôme(s) délivré(s)</Badge>
              {d.revoques > 0 && <Badge color="red" variant="light">{d.revoques} révoqué(s)</Badge>}
            </Group>
            <Text size="sm" c="dimmed">
              Chaque diplôme porte un numéro et un code de vérification : toute personne peut en contrôler l'authenticité sur la page publique
              « Vérifier un diplôme ». Les établissements impriment les diplômes de leurs admis depuis leur écran des examens.
            </Text>
            {d.statutResultats !== 'PROCLAME' ? (
              <Alert color="gray" variant="light">Les diplômes sont délivrés après la proclamation des résultats.</Alert>
            ) : (
              <Group>
                <Button size="xs" loading={delivrance.isPending} disabled={d.delivres >= d.admis} onClick={() => delivrance.mutate()}>
                  Délivrer les diplômes des admis
                </Button>
              </Group>
            )}
            {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
            {d.diplomes.length > 0 && (
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Titulaire</Table.Th>
                    <Table.Th>Établissement</Table.Th>
                    <Table.Th>Mention</Table.Th>
                    <Table.Th>N°</Table.Th>
                    <Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.diplomes.map((x) => <LigneDiplome key={`${x.id}-${x.revoque}`} diplome={x} examenId={examen.id} onChange={appliquer} />)}
                </Table.Tbody>
              </Table>
            )}
          </>
        )}
      </Stack>
    </Modal>
  )
}

function LigneDiplome({ diplome: x, examenId, onChange }: { diplome: DiplomesExamen['diplomes'][number]; examenId: number; onChange: (d: DiplomesExamen) => void }) {
  const [motif, setMotif] = useState('')
  const [saisie, setSaisie] = useState(false)
  const revocation = useMutation({ mutationFn: () => revoquerDiplome(examenId, x.id, motif.trim()), onSuccess: onChange })
  return (
    <Table.Tr>
      <Table.Td>{x.titulaire}</Table.Td>
      <Table.Td>{x.etablissement}</Table.Td>
      <Table.Td>{LIBELLE_MENTION[x.mention]}</Table.Td>
      <Table.Td><Text size="xs">{x.numero}</Text></Table.Td>
      <Table.Td>
        {x.revoque ? (
          <Badge color="red" variant="light" title={x.motifRevocation ?? ''}>Révoqué</Badge>
        ) : saisie ? (
          <Group gap={4} wrap="nowrap">
            <TextInput size="xs" placeholder="Motif" value={motif} onChange={(e) => setMotif(e.currentTarget.value)} />
            <Button size="compact-xs" color="red" variant="light" disabled={motif.trim() === ''} loading={revocation.isPending} onClick={() => revocation.mutate()}>
              Confirmer
            </Button>
          </Group>
        ) : (
          <Button size="compact-xs" color="red" variant="subtle" onClick={() => setSaisie(true)}>Révoquer</Button>
        )}
      </Table.Td>
    </Table.Tr>
  )
}
