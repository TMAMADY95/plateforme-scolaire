import { Alert, Badge, Checkbox, Group, Modal, Stack, Table, Text, Tooltip } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { dossiersCandidatures, marquerPiece } from '../api/endpoints'
import type { DossiersCandidatures } from '../api/types'

/** Dossiers de candidature : pièces reçues par candidat, dossier complet ou non, alerte sur les évaluations manquées. */
export default function DossiersCandidaturesModal({ etablissementId, examenId, titre, peutModifier, onFermer }: {
  etablissementId: number
  examenId: number
  titre: string
  peutModifier: boolean
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const cle = ['dossiers-candidatures', etablissementId, examenId]
  const donnees = useQuery({ queryKey: cle, queryFn: () => dossiersCandidatures(etablissementId, examenId) })
  const marquage = useMutation({
    mutationFn: (v: { candidatId: number; type: string; fournie: boolean }) =>
      marquerPiece(etablissementId, examenId, v.candidatId, v.type, { fournie: v.fournie, reference: null }),
    onSuccess: (d: DossiersCandidatures) => queryClient.setQueryData(cle, d),
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })
  const d = donnees.data

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Dossiers de candidature : ${titre}`}>
      <Stack gap="sm">
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Text size="sm" c="dimmed">
              {d.complets} dossier(s) complet(s) sur {d.candidats.length}. Cochez chaque pièce reçue ; un dossier est complet quand toutes les pièces requises sont là.
            </Text>
            {d.candidats.length === 0 ? (
              <Text size="sm" c="dimmed">Aucun candidat inscrit.</Text>
            ) : (
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Candidat</Table.Th>
                    {d.pieces.map((p) => (
                      <Table.Th key={p.type}>
                        <Tooltip label={p.libelle} multiline w={240}>
                          <span>{p.libelle.split(' ').slice(0, 2).join(' ')}{p.requise ? '' : ' (si exigé)'}</span>
                        </Tooltip>
                      </Table.Th>
                    ))}
                    <Table.Th>Dossier</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.candidats.map((c) => (
                    <Table.Tr key={c.id}>
                      <Table.Td>
                        <Text size="sm">{c.nom}</Text>
                        <Text size="xs" c="dimmed">{c.numero}{c.libre ? ' · candidat libre' : c.classe ? ` · ${c.classe}` : ''}</Text>
                      </Table.Td>
                      {d.pieces.map((p) => (
                        <Table.Td key={p.type}>
                          {c.libre && p.type === 'ATTESTATION' ? <Text size="xs" c="dimmed">n/a</Text> : <Checkbox
                            checked={c.pieces[p.type] !== undefined}
                            disabled={!peutModifier || marquage.isPending}
                            onChange={(e) => marquage.mutate({ candidatId: c.id, type: p.type, fournie: e.currentTarget.checked })}
                          />}
                        </Table.Td>
                      ))}
                      <Table.Td>
                        <Group gap={4}>
                          {c.complet ? <Badge color="green" variant="light">Complet</Badge>
                            : <Tooltip label={`Manque : ${c.manques.join(' ; ')}`} multiline w={260}><Badge color="orange" variant="light">Incomplet</Badge></Tooltip>}
                          {c.evaluationsManquees > 0 && (
                            <Tooltip label="Le règlement demande d'avoir participé à toutes les évaluations de l'année" multiline w={260}>
                              <Badge color="red" variant="light">Absent à {c.evaluationsManquees} éval.</Badge>
                            </Tooltip>
                          )}
                        </Group>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            )}
          </>
        )}
      </Stack>
    </Modal>
  )
}
