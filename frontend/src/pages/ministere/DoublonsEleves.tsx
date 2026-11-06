import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, Stack, Table, Text, Textarea, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { doublonsEleves, fusionnerEleves } from '../../api/endpoints'
import { formaterDate } from '../../utils/dates'

type Dossier = { id: number; matricule: string; identifiantNational: string; lieuNaissance: string | null; nomMere: string | null; inscriptions: number; dernierEtablissement: string | null }

/** Registre national : dossiers élèves qui semblent être le même élève (même nom, prénom et date de naissance) ; fusion en gardant un dossier. */
export default function DoublonsEleves() {
  const queryClient = useQueryClient()
  const doublons = useQuery({ queryKey: ['doublons-eleves'], queryFn: doublonsEleves })
  const [choix, setChoix] = useState<{ garder: Dossier; absorber: Dossier } | null>(null)
  const [motif, setMotif] = useState('')

  const fusion = useMutation({
    mutationFn: () => fusionnerEleves(choix!.garder.id, choix!.absorber.id, motif.trim()),
    onSuccess: (r) => {
      notifications.show({ color: 'green', message: `Dossiers fusionnés : ${r.conserveIdentifiant} conserve l'historique (${r.lignesReprises} lignes reprises). L'ancien identifiant ${r.absorbeIdentifiant} reste valide.` })
      setChoix(null)
      setMotif('')
      void queryClient.invalidateQueries({ queryKey: ['doublons-eleves'] })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Fusion impossible', message: e.message, autoClose: false }),
  })

  return (
    <Stack>
      <div>
        <Title order={4}>Doublons possibles</Title>
        <Text c="dimmed" size="sm">Dossiers de même nom, prénom et date de naissance. Des jumeaux ou des homonymes peuvent exister : vérifiez le lieu de naissance, le nom de la mère et les inscriptions avant de fusionner. La fusion est définitive (les anciens identifiants restent valides).</Text>
      </div>
      {doublons.isLoading && <Loader />}
      {doublons.isError && <Alert color="red">Doublons indisponibles.</Alert>}
      {doublons.data && doublons.data.groupes.length === 0 && <Text c="dimmed">Aucun doublon possible détecté.</Text>}
      {doublons.data?.groupes.map((g) => (
        <Stack key={`${g.nom}-${g.prenom}-${g.dateNaissance}`} gap="xs">
          <Group gap="xs">
            <Text fw={600}>{g.prenom} {g.nom}</Text>
            <Badge variant="light">{formaterDate(g.dateNaissance)}</Badge>
          </Group>
          <Table withTableBorder striped>
            <Table.Thead><Table.Tr><Table.Th>Identifiant national</Table.Th><Table.Th>Matricule</Table.Th><Table.Th>Lieu de naissance</Table.Th><Table.Th>Mère</Table.Th><Table.Th>Inscriptions</Table.Th><Table.Th>Dernier établissement</Table.Th><Table.Th>Fusion</Table.Th></Table.Tr></Table.Thead>
            <Table.Tbody>
              {g.dossiers.map((d) => (
                <Table.Tr key={d.id}>
                  <Table.Td>{d.identifiantNational}</Table.Td>
                  <Table.Td>{d.matricule}</Table.Td>
                  <Table.Td>{d.lieuNaissance ?? '—'}</Table.Td>
                  <Table.Td>{d.nomMere ?? '—'}</Table.Td>
                  <Table.Td>{d.inscriptions}</Table.Td>
                  <Table.Td>{d.dernierEtablissement ?? '—'}</Table.Td>
                  <Table.Td>
                    {g.dossiers.length === 2 && (
                      <Button size="compact-sm" variant="light" onClick={() => setChoix({ garder: d, absorber: g.dossiers.find((x) => x.id !== d.id)! })}>
                        Garder celui-ci
                      </Button>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
          {g.dossiers.length > 2 && <Text size="xs" c="dimmed">Plus de deux dossiers : fusionnez d'abord deux d'entre eux en les traitant un par un (relancez la détection).</Text>}
        </Stack>
      ))}
      <Modal opened={choix !== null} onClose={() => setChoix(null)} title="Fusionner deux dossiers">
        {choix && (
          <Stack>
            <Text size="sm">Conserver <b>{choix.garder.identifiantNational}</b> et y reprendre tout l'historique de <b>{choix.absorber.identifiantNational}</b>, qui sera supprimé.</Text>
            <Textarea label="Motif (obligatoire)" maxLength={500} value={motif} onChange={(e) => setMotif(e.currentTarget.value)} />
            <Button color="red" disabled={motif.trim() === ''} loading={fusion.isPending} onClick={() => fusion.mutate()}>Fusionner définitivement</Button>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
}
