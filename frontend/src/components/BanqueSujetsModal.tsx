import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Modal, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ajouterSujet, banqueSujets, compromettreSujet, supprimerSujet, tirerSujet, validerSujet } from '../api/endpoints'
import type { BanqueSujets, Examen } from '../api/types'

type Epreuve = BanqueSujets['epreuves'][number]

const LIBELLE = { ELABORATION: 'En élaboration', VALIDE: 'Validé (réserve)', TIRE: 'Tiré au sort', COMPROMIS: 'Compromis' }
const COULEUR = { ELABORATION: 'gray', VALIDE: 'blue', TIRE: 'green', COMPROMIS: 'red' }

/** Banque de sujets : trois variantes par épreuve, validation, tirage au sort et remplacement en cas de fuite. */
export default function BanqueSujetsModal({ examen, onFermer }: { examen: Examen; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const cle = ['banque-sujets', examen.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => banqueSujets(examen.id) })
  const appliquer = (d: BanqueSujets) => queryClient.setQueryData(cle, d)
  const d = donnees.data
  return (
    <Modal opened onClose={onFermer} size="xl" title={`Banque de sujets : ${examen.libelle}`}>
      <Stack gap="sm">
        <Text size="sm" c="dimmed">
          Chaque épreuve a trois variantes (A, B, C). Quand les trois sont validées, une variante est tirée au sort ; en cas de fuite, elle est
          remplacée par une variante de réserve. Seul le suivi des sujets est enregistré, jamais leur contenu.
        </Text>
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && d.epreuves.length === 0 && <Text size="sm" c="dimmed">Aucune épreuve au calendrier de cet examen.</Text>}
        {d?.epreuves.map((e) => <CarteEpreuve key={e.id} epreuve={e} examenId={examen.id} onChange={appliquer} />)}
      </Stack>
    </Modal>
  )
}

function CarteEpreuve({ epreuve: e, examenId, onChange }: { epreuve: Epreuve; examenId: number; onChange: (d: BanqueSujets) => void }) {
  const [variante, setVariante] = useState<string | null>(null)
  const [intitule, setIntitule] = useState('')
  const [auteurs, setAuteurs] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const options = { onSuccess: (d: BanqueSujets) => { onChange(d); setErreur(null) }, onError: (x: Error) => setErreur(x.message) }
  const ajout = useMutation({
    mutationFn: () => ajouterSujet(examenId, e.id, { variante: variante as string, intitule: intitule.trim(), auteurs: auteurs.trim() === '' ? null : auteurs.trim() }),
    onSuccess: (d) => { onChange(d); setErreur(null); setIntitule(''); setAuteurs(''); setVariante(null) },
    onError: (x) => setErreur(x.message),
  })
  const validation = useMutation({ mutationFn: (id: number) => validerSujet(examenId, id), ...options })
  const suppression = useMutation({ mutationFn: (id: number) => supprimerSujet(examenId, id), ...options })
  const compromission = useMutation({ mutationFn: (id: number) => compromettreSujet(examenId, id), ...options })
  const tirage = useMutation({ mutationFn: () => tirerSujet(examenId, e.id), ...options })
  const prises = e.variantes.map((v) => v.variante)
  const libres = ['A', 'B', 'C'].filter((v) => !prises.some((p) => p === v))

  return (
    <Card withBorder padding="sm">
      <Stack gap="xs">
        <Group justify="space-between">
          <Group gap="xs">
            <Text fw={600}>{e.nom}</Text>
            <Text size="xs" c="dimmed">{new Date(e.date).toLocaleDateString('fr-FR')}</Text>
            {e.varianteTiree ? <Badge color="green" variant="light">Variante {e.varianteTiree} utilisée</Badge>
              : <Badge color="gray" variant="light">{e.validees} / 3 validée(s)</Badge>}
          </Group>
          {!e.varianteTiree && (
            <Button size="xs" variant="light" disabled={!e.tirable} loading={tirage.isPending} onClick={() => tirage.mutate()}>
              Tirer au sort
            </Button>
          )}
        </Group>
        {e.variantes.length > 0 && (
          <Table verticalSpacing={4}>
            <Table.Tbody>
              {e.variantes.map((v) => (
                <Table.Tr key={v.id}>
                  <Table.Td w={40}><b>{v.variante}</b></Table.Td>
                  <Table.Td>{v.intitule}{v.auteurs ? <Text size="xs" c="dimmed">{v.auteurs}</Text> : null}</Table.Td>
                  <Table.Td w={150}><Badge color={COULEUR[v.statut]} variant="light">{LIBELLE[v.statut]}</Badge></Table.Td>
                  <Table.Td w={200}>
                    <Group gap={4} justify="flex-end">
                      {v.statut === 'ELABORATION' && <Button size="compact-xs" variant="light" onClick={() => validation.mutate(v.id)}>Valider</Button>}
                      {v.statut === 'TIRE' && <Button size="compact-xs" color="red" variant="light" onClick={() => compromission.mutate(v.id)}>Fuite : remplacer</Button>}
                      {(v.statut === 'ELABORATION' || v.statut === 'VALIDE') && (
                        <Button size="compact-xs" color="red" variant="subtle" onClick={() => suppression.mutate(v.id)}>Supprimer</Button>
                      )}
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
        {!e.varianteTiree && libres.length > 0 && (
          <Group align="flex-end" gap="xs">
            <Select size="xs" label="Variante" data={libres} value={variante} onChange={setVariante} w={90} />
            <TextInput size="xs" label="Intitulé du sujet" value={intitule} onChange={(x) => setIntitule(x.currentTarget.value)} w={260} />
            <TextInput size="xs" label="Auteurs" value={auteurs} onChange={(x) => setAuteurs(x.currentTarget.value)} w={200} />
            <Button size="xs" variant="light" disabled={variante === null || intitule.trim() === ''} loading={ajout.isPending} onClick={() => ajout.mutate()}>
              Ajouter
            </Button>
          </Group>
        )}
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
      </Stack>
    </Card>
  )
}
