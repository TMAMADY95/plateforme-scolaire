import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, NumberInput, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { SERIES_LYCEE, libelleSerie } from '../../api/types'
import { ajouterManuel, catalogueManuels, listerMatieres, listerNiveaux, supprimerManuel, syntheseManuels } from '../../api/endpoints'
import { estMinistere } from '../../auth/roles'
import { useUtilisateur } from '../../auth/useAuth'
import { pourcent } from '../../components/CouvertureManuel'

/** Manuels scolaires : couverture des établissements du périmètre ; le ministère tient aussi le catalogue. */
export default function ManuelsNational({ anneeId }: { anneeId: number }) {
  const utilisateur = useUtilisateur()
  const ministere = estMinistere(utilisateur)
  const queryClient = useQueryClient()
  const synthese = useQuery({ queryKey: ['manuels-synthese', anneeId], queryFn: () => syntheseManuels(anneeId) })
  const catalogue = useQuery({ queryKey: ['manuels-catalogue'], queryFn: catalogueManuels, enabled: ministere })
  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres, enabled: ministere })
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux, enabled: ministere })
  const [matiereId, setMatiereId] = useState<string | null>(null)
  const [niveauId, setNiveauId] = useState<string | null>(null)
  const [serie, setSerie] = useState<string | null>(null)
  const [titre, setTitre] = useState('')
  const [editeur, setEditeur] = useState('')
  const [edition, setEdition] = useState<number | string>('')
  const rafraichir = () => {
    void queryClient.invalidateQueries({ queryKey: ['manuels-catalogue'] })
    void queryClient.invalidateQueries({ queryKey: ['manuels-synthese'] })
  }
  const erreur = (e: Error) => notifications.show({ color: 'red', message: e.message })
  const ajouter = useMutation({
    mutationFn: () => ajouterManuel({ matiereId: Number(matiereId), niveauId: Number(niveauId), titre: titre.trim(), editeur: editeur.trim() || null, anneeEdition: edition === '' ? null : Number(edition), serie }),
    onSuccess: () => {
      setTitre('')
      setEditeur('')
      rafraichir()
    },
    onError: erreur,
  })
  const supprimer = useMutation({ mutationFn: (id: number) => supprimerManuel(id), onSuccess: rafraichir, onError: erreur })
  const d = synthese.data

  return (
    <Stack>
      <div>
        <Title order={4}>Manuels scolaires</Title>
        <Text c="dimmed" size="sm">Exemplaires utilisables déclarés par les établissements, comparés aux élèves de chaque niveau (un manuel par élève et par matière). Établissements classés du moins bien au mieux couverts.</Text>
      </div>
      {synthese.isLoading && <Loader />}
      {synthese.isError && <Alert color="red">Synthèse indisponible.</Alert>}
      {d && (
        <>
          <Group gap="xs">
            <Badge size="lg" variant="light">Couverture {pourcent(d.couverture)}</Badge>
            <Badge size="lg" variant="light">{d.exemplaires} exemplaires pour {d.besoin} besoins</Badge>
            <Badge size="lg" variant="light" color={d.etablissementsInsuffisants ? 'red' : 'gray'}>{d.etablissementsInsuffisants} établissements avec au moins un manuel insuffisant</Badge>
          </Group>
          {d.etablissements.length === 0 ? <Text c="dimmed">Aucun établissement concerné (catalogue vide ou aucun élève).</Text> : (
            <Table striped withTableBorder>
              <Table.Thead><Table.Tr><Table.Th>Établissement</Table.Th><Table.Th>Zone</Table.Th><Table.Th>Besoin</Table.Th><Table.Th>Exemplaires</Table.Th><Table.Th>Couverture</Table.Th><Table.Th>Manuels insuffisants</Table.Th></Table.Tr></Table.Thead>
              <Table.Tbody>
                {d.etablissements.map((l) => (
                  <Table.Tr key={l.etablissementId}>
                    <Table.Td>{l.etablissement}</Table.Td>
                    <Table.Td>{l.zone ?? '—'}</Table.Td>
                    <Table.Td>{l.besoin}</Table.Td>
                    <Table.Td>{l.exemplaires}</Table.Td>
                    <Table.Td>{pourcent(l.couverture)}</Table.Td>
                    <Table.Td>{l.manuelsInsuffisants} / {l.manuelsConcernes}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </>
      )}
      {ministere && (
        <Stack>
          <Title order={5}>Catalogue des manuels</Title>
          <Group align="flex-end">
            <Select label="Matière" searchable data={(matieres.data ?? []).map((m) => ({ value: String(m.id), label: m.libelle }))} value={matiereId} onChange={setMatiereId} w={200} />
            <Select label="Niveau" data={(niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))} value={niveauId} onChange={setNiveauId} w={180} />
            <Select label="Série" placeholder="Toutes" clearable data={SERIES_LYCEE} value={serie} onChange={setSerie} w={200} />
            <TextInput label="Titre" value={titre} onChange={(e) => setTitre(e.currentTarget.value)} w={260} />
            <TextInput label="Éditeur" value={editeur} onChange={(e) => setEditeur(e.currentTarget.value)} w={160} />
            <NumberInput label="Édition" min={1900} max={2100} value={edition} onChange={setEdition} w={100} />
            <Button disabled={matiereId === null || niveauId === null || titre.trim() === ''} loading={ajouter.isPending} onClick={() => ajouter.mutate()}>Ajouter</Button>
          </Group>
          {catalogue.data && (
            <Table striped withTableBorder>
              <Table.Thead><Table.Tr><Table.Th>Niveau</Table.Th><Table.Th>Matière</Table.Th><Table.Th>Titre</Table.Th><Table.Th>Éditeur</Table.Th><Table.Th /></Table.Tr></Table.Thead>
              <Table.Tbody>
                {catalogue.data.map((m) => (
                  <Table.Tr key={m.id}>
                    <Table.Td>{m.niveau}{m.serie ? ` · ${libelleSerie(m.serie)}` : ''}</Table.Td>
                    <Table.Td>{m.matiere}</Table.Td>
                    <Table.Td>{m.titre}{m.anneeEdition ? ` (${m.anneeEdition})` : ''}</Table.Td>
                    <Table.Td>{m.editeur ?? '—'}</Table.Td>
                    <Table.Td><Button size="compact-xs" color="red" variant="subtle" loading={supprimer.isPending && supprimer.variables === m.id} onClick={() => supprimer.mutate(m.id)}>Supprimer</Button></Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </Stack>
      )}
    </Stack>
  )
}
