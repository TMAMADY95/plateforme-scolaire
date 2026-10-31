import { useState } from 'react'
import { Alert, Button, Group, Modal, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { creerTypeExamen, listerNiveaux, listerTypesExamen, niveauxOrganisationExamen, supprimerTypeExamen } from '../api/endpoints'
import { libelleOrganisation } from '../utils/organisationExamen'

/** Modèles d'examens (CEE, BEPC, BAC…) : qui les organise et pour quelle classe. Modifiables, rien n'est figé. */
export default function ModelesExamenModal({ onFermer }: { onFermer: () => void }) {
  const queryClient = useQueryClient()
  const modeles = useQuery({ queryKey: ['types-examen'], queryFn: listerTypesExamen })
  const organisations = useQuery({ queryKey: ['niveaux-organisation'], queryFn: niveauxOrganisationExamen })
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })
  const [code, setCode] = useState('')
  const [libelle, setLibelle] = useState('')
  const [organisation, setOrganisation] = useState<string | null>('NATIONAL')
  const [niveauId, setNiveauId] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['types-examen'] })

  const creation = useMutation({
    mutationFn: () => creerTypeExamen({ code: code.trim(), libelle: libelle.trim(), niveauOrganisation: organisation as string, niveauId: niveauId === null ? null : Number(niveauId), cycleId: null }),
    onSuccess: () => {
      rafraichir()
      setCode('')
      setLibelle('')
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })
  const suppression = useMutation({ mutationFn: supprimerTypeExamen, onSuccess: rafraichir, onError: (e) => setErreur(e.message) })

  return (
    <Modal opened onClose={onFermer} size="xl" title="Modèles d'examens">
      <Stack>
        <Text size="sm" c="dimmed">
          Le niveau d'organisation dit qui organise l'examen : par exemple le CEE par préfecture ou par commune, le BEPC par région, le
          baccalauréat au niveau national. Les niveaux proposés sont ceux du référentiel des zones.
        </Text>
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
        <Table verticalSpacing="xs">
          <Table.Tbody>
            {modeles.data?.map((m) => (
              <Table.Tr key={m.id}>
                <Table.Td fw={600}>{m.code}</Table.Td>
                <Table.Td>{m.libelle}</Table.Td>
                <Table.Td>{libelleOrganisation(m.niveauOrganisation)}</Table.Td>
                <Table.Td>{m.niveau ?? '—'}</Table.Td>
                <Table.Td><Button size="compact-xs" variant="subtle" color="red" onClick={() => suppression.mutate(m.id)}>Supprimer</Button></Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
        <Group align="flex-end">
          <TextInput label="Code" placeholder="CEE" maxLength={30} value={code} onChange={(e) => setCode(e.currentTarget.value)} w={110} />
          <TextInput label="Libellé" maxLength={150} value={libelle} onChange={(e) => setLibelle(e.currentTarget.value)} w={260} />
          <Select label="Organisé par" data={(organisations.data ?? ['NATIONAL']).map((o) => ({ value: o, label: libelleOrganisation(o) }))} value={organisation} onChange={setOrganisation} allowDeselect={false} w={170} />
          <Select label="Classe d'examen" placeholder="Facultatif" clearable data={(niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))} value={niveauId} onChange={setNiveauId} w={170} />
          <Button disabled={code.trim() === '' || libelle.trim() === '' || organisation === null} loading={creation.isPending} onClick={() => creation.mutate()}>Ajouter</Button>
        </Group>
      </Stack>
    </Modal>
  )
}
