import { useState } from 'react'
import { Alert, Badge, Button, Checkbox, Group, Modal, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  delibererExamen,
  initialiserJury,
  juryExamen,
  modifierMembreJury,
  proclamerExamen,
  rouvrirDeliberation,
} from '../api/endpoints'
import type { Examen, Jury, MembreJury } from '../api/types'
import { formaterDate } from '../utils/dates'

export const LIBELLE_STATUT: Record<Examen['statutResultats'], string> = {
  EN_COURS: 'Saisie en cours',
  DELIBERE: 'Délibéré',
  PROCLAME: 'Proclamé',
}
export const COULEUR_STATUT: Record<Examen['statutResultats'], string> = { EN_COURS: 'gray', DELIBERE: 'blue', PROCLAME: 'green' }

/** Jury de délibération (composition du règlement général), quorum des deux tiers, délibération puis proclamation. */
export default function DeliberationExamenModal({ examen, onFermer }: { examen: Examen; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const jury = useQuery({ queryKey: ['jury-examen', examen.id], queryFn: () => juryExamen(examen.id) })

  const appliquer = (j: Jury) => {
    queryClient.setQueryData(['jury-examen', examen.id], j)
    void queryClient.invalidateQueries({ queryKey: ['examens'] })
    void queryClient.invalidateQueries({ queryKey: ['rapport-examen'] })
  }
  const erreur = (titre: string) => (e: Error) => notifications.show({ color: 'red', title: titre, message: e.message })
  const initialisation = useMutation({ mutationFn: () => initialiserJury(examen.id), onSuccess: appliquer, onError: erreur('Jury impossible') })
  const deliberation = useMutation({ mutationFn: () => delibererExamen(examen.id), onSuccess: appliquer, onError: erreur('Délibération impossible') })
  const proclamation = useMutation({ mutationFn: () => proclamerExamen(examen.id), onSuccess: appliquer, onError: erreur('Proclamation impossible') })
  const reouverture = useMutation({ mutationFn: () => rouvrirDeliberation(examen.id), onSuccess: appliquer, onError: erreur('Action impossible') })

  const j = jury.data
  const modifiable = j?.statut === 'EN_COURS'

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Jury et proclamation : ${examen.libelle}`}>
      <Stack gap="sm">
        {jury.isError && <Alert color="red" variant="light">{jury.error.message}</Alert>}
        {j && (
          <>
            <Group gap="xs">
              <Badge color={COULEUR_STATUT[j.statut]} variant="light">{LIBELLE_STATUT[j.statut]}</Badge>
              {j.dateDeliberation && <Badge variant="light" color="gray">Délibéré le {formaterDate(j.dateDeliberation)}</Badge>}
              {j.dateProclamation && <Badge variant="light" color="green">Proclamé le {formaterDate(j.dateProclamation)}</Badge>}
            </Group>
            <Text size="sm" c="dimmed">
              Le jury délibère si au moins les deux tiers de ses membres sont présents, puis le ministère proclame les résultats.
              Après la proclamation, les résultats ne peuvent plus être modifiés.
            </Text>
            {j.membres.length === 0 ? (
              <Button loading={initialisation.isPending} onClick={() => initialisation.mutate()}>
                Constituer le jury ({examen.niveauOrganisation === 'NATIONAL' ? 'jury national' : 'composition prévue pour ce niveau'})
              </Button>
            ) : (
              <>
                <Table verticalSpacing="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Fonction</Table.Th>
                      <Table.Th>Nom du membre</Table.Th>
                      <Table.Th>Présent</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {j.membres.map((m) => (
                      <LigneMembre key={`${m.id}-${m.nom}-${m.present}`} examenId={examen.id} membre={m} modifiable={modifiable} onChange={appliquer} />
                    ))}
                  </Table.Tbody>
                </Table>
                <Group gap="xs">
                  <Badge color={j.quorumAtteint ? 'green' : 'orange'} variant="light">
                    {j.presents} présent(s) sur {j.membres.length} : quorum {j.quorumRequis}
                  </Badge>
                </Group>
              </>
            )}
            <Group justify="flex-end">
              {j.statut === 'EN_COURS' && j.membres.length > 0 && (
                <Button loading={deliberation.isPending} disabled={!j.quorumAtteint} onClick={() => deliberation.mutate()}>
                  Enregistrer la délibération
                </Button>
              )}
              {j.statut === 'DELIBERE' && (
                <>
                  <Button variant="default" loading={reouverture.isPending} onClick={() => reouverture.mutate()}>
                    Rouvrir la délibération
                  </Button>
                  {j.peutProclamer && (
                    <Button color="green" loading={proclamation.isPending} onClick={() => proclamation.mutate()}>
                      Proclamer les résultats
                    </Button>
                  )}
                </>
              )}
            </Group>
          </>
        )}
      </Stack>
    </Modal>
  )
}

function LigneMembre({ examenId, membre, modifiable, onChange }: { examenId: number; membre: MembreJury; modifiable: boolean; onChange: (j: Jury) => void }) {
  const [nom, setNom] = useState(membre.nom ?? '')
  const enregistrement = useMutation({
    mutationFn: (v: { nom: string; present: boolean }) => modifierMembreJury(examenId, membre.id, { nom: v.nom.trim() === '' ? null : v.nom, present: v.present }),
    onSuccess: onChange,
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })
  return (
    <Table.Tr>
      <Table.Td>{membre.fonction}</Table.Td>
      <Table.Td>
        <TextInput
          size="xs"
          value={nom}
          disabled={!modifiable}
          onChange={(e) => setNom(e.currentTarget.value)}
          onBlur={() => nom !== (membre.nom ?? '') && enregistrement.mutate({ nom, present: nom.trim() === '' ? false : membre.present })}
        />
      </Table.Td>
      <Table.Td>
        <Checkbox
          aria-label="Présent"
          checked={membre.present}
          disabled={!modifiable || (membre.nom ?? '') === ''}
          onChange={(e) => enregistrement.mutate({ nom: membre.nom ?? '', present: e.currentTarget.checked })}
        />
      </Table.Td>
    </Table.Tr>
  )
}
