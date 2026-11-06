import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, Select, Stack, Table, Text, TextInput, Textarea, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { annulerTransfert, deciderTransfert, demanderTransfert, listerTransferts } from '../api/endpoints'
import type { TransfertEleve } from '../api/types'
import { formaterDate } from '../utils/dates'

const MOTIFS = [
  { value: 'DEMENAGEMENT', label: 'Déménagement' },
  { value: 'RAPPROCHEMENT_FAMILIAL', label: 'Rapprochement familial' },
  { value: 'ORIENTATION', label: 'Orientation / filière' },
  { value: 'DISCIPLINE', label: 'Raison disciplinaire' },
  { value: 'AUTRE', label: 'Autre' },
]
const libelleMotif = (m: string) => MOTIFS.find((x) => x.value === m)?.label ?? m
const STATUT: Record<string, { libelle: string; couleur: string }> = {
  DEMANDE: { libelle: 'En attente', couleur: 'yellow' },
  APPROUVE: { libelle: 'Approuvé', couleur: 'green' },
  REFUSE: { libelle: 'Refusé', couleur: 'red' },
  ANNULE: { libelle: 'Annulé', couleur: 'gray' },
}

/** Transferts d'élèves en cours d'année : l'école d'accueil demande, l'école d'origine décide. */
export default function TransfertsEleves({ etablissementId, anneeId }: { etablissementId: number; anneeId: number }) {
  const queryClient = useQueryClient()
  const transferts = useQuery({ queryKey: ['transferts', etablissementId, anneeId], queryFn: () => listerTransferts(etablissementId, anneeId) })
  const [demande, setDemande] = useState(false)
  const [matricule, setMatricule] = useState('')
  const [motif, setMotif] = useState<string | null>('DEMENAGEMENT')
  const [commentaire, setCommentaire] = useState('')
  const [aRefuser, setARefuser] = useState<TransfertEleve | null>(null)
  const [motifRefus, setMotifRefus] = useState('')

  const rafraichir = () => {
    void queryClient.invalidateQueries({ queryKey: ['transferts'] })
    void queryClient.invalidateQueries({ queryKey: ['inscriptions'] })
  }
  const erreur = (e: Error) => notifications.show({ color: 'red', title: 'Action impossible', message: e.message })

  const creer = useMutation({
    mutationFn: () => demanderTransfert(etablissementId, { matriculeEleve: matricule.trim(), anneeId, motif: motif ?? 'AUTRE', commentaire: commentaire.trim() || null }),
    onSuccess: () => {
      notifications.show({ color: 'green', message: 'Demande de transfert envoyée à l\'établissement d\'origine.' })
      setDemande(false)
      setMatricule('')
      setCommentaire('')
      rafraichir()
    },
    onError: erreur,
  })
  const decider = useMutation({
    mutationFn: (v: { t: TransfertEleve; decision: 'APPROUVE' | 'REFUSE'; motifRefus: string | null }) => deciderTransfert(v.t.id, { decision: v.decision, motifRefus: v.motifRefus }),
    onSuccess: (_r, v) => {
      notifications.show({ color: 'green', message: v.decision === 'APPROUVE' ? 'Transfert approuvé : l\'établissement d\'accueil peut accepter l\'élève.' : 'Transfert refusé.' })
      setARefuser(null)
      setMotifRefus('')
      rafraichir()
    },
    onError: erreur,
  })
  const annuler = useMutation({ mutationFn: (t: TransfertEleve) => annulerTransfert(t.id), onSuccess: rafraichir, onError: erreur })

  const d = transferts.data
  return (
    <Stack mt="xl">
      <Group justify="space-between">
        <div>
          <Title order={3}>Transferts d'élèves</Title>
          <Text c="dimmed" size="sm">Un élève qui change d'établissement en cours d'année : vous demandez le transfert avec son matricule, l'école d'origine l'approuve, puis vous acceptez son inscription avec une classe.</Text>
        </div>
        <Button variant="light" onClick={() => setDemande(true)}>Demander un transfert</Button>
      </Group>
      {transferts.isLoading && <Loader size="sm" />}
      {transferts.isError && <Alert color="red">Transferts indisponibles.</Alert>}
      {d && (
        <>
          <Text fw={600}>Demandes reçues (élèves qui quittent votre établissement)</Text>
          {d.sortants.length === 0 ? <Text c="dimmed" size="sm">Aucune demande reçue.</Text> : (
            <Table striped withTableBorder>
              <Table.Thead><Table.Tr><Table.Th>Élève</Table.Th><Table.Th>Vers</Table.Th><Table.Th>Motif</Table.Th><Table.Th>Demandé le</Table.Th><Table.Th>Statut</Table.Th><Table.Th>Actions</Table.Th></Table.Tr></Table.Thead>
              <Table.Tbody>
                {d.sortants.map((t) => (
                  <Table.Tr key={t.id}>
                    <Table.Td>{t.eleve}<Text size="xs" c="dimmed">{t.matricule}</Text></Table.Td>
                    <Table.Td>{t.destination}</Table.Td>
                    <Table.Td>{libelleMotif(t.motif)}{t.commentaire && <Text size="xs" c="dimmed">{t.commentaire}</Text>}</Table.Td>
                    <Table.Td>{formaterDate(t.demandeLe)}</Table.Td>
                    <Table.Td><Badge variant="light" color={STATUT[t.statut].couleur}>{STATUT[t.statut].libelle}</Badge>{t.motifRefus && <Text size="xs" c="dimmed">{t.motifRefus}</Text>}</Table.Td>
                    <Table.Td>
                      {t.statut === 'DEMANDE' && (
                        <Group gap="xs" wrap="nowrap">
                          <Button size="compact-sm" color="green" variant="light" loading={decider.isPending} onClick={() => decider.mutate({ t, decision: 'APPROUVE', motifRefus: null })}>Approuver</Button>
                          <Button size="compact-sm" color="red" variant="light" onClick={() => setARefuser(t)}>Refuser</Button>
                        </Group>
                      )}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
          <Text fw={600}>Demandes émises (élèves qui arrivent)</Text>
          {d.entrants.length === 0 ? <Text c="dimmed" size="sm">Aucune demande émise.</Text> : (
            <Table striped withTableBorder>
              <Table.Thead><Table.Tr><Table.Th>Élève</Table.Th><Table.Th>Depuis</Table.Th><Table.Th>Motif</Table.Th><Table.Th>Demandé le</Table.Th><Table.Th>Statut</Table.Th><Table.Th>Actions</Table.Th></Table.Tr></Table.Thead>
              <Table.Tbody>
                {d.entrants.map((t) => (
                  <Table.Tr key={t.id}>
                    <Table.Td>{t.eleve}<Text size="xs" c="dimmed">{t.matricule}</Text></Table.Td>
                    <Table.Td>{t.origine}</Table.Td>
                    <Table.Td>{libelleMotif(t.motif)}</Table.Td>
                    <Table.Td>{formaterDate(t.demandeLe)}</Table.Td>
                    <Table.Td>
                      <Badge variant="light" color={STATUT[t.statut].couleur}>{STATUT[t.statut].libelle}</Badge>
                      {t.motifRefus && <Text size="xs" c="dimmed">{t.motifRefus}</Text>}
                      {t.statut === 'APPROUVE' && <Text size="xs" c="dimmed">À accepter dans les inscriptions (en attente)</Text>}
                    </Table.Td>
                    <Table.Td>{t.statut === 'DEMANDE' && <Button size="compact-sm" color="gray" variant="subtle" loading={annuler.isPending} onClick={() => annuler.mutate(t)}>Annuler la demande</Button>}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </>
      )}
      <Modal opened={demande} onClose={() => setDemande(false)} title="Demander un transfert">
        <Stack>
          <TextInput label="Matricule de l'élève" value={matricule} onChange={(e) => setMatricule(e.currentTarget.value)} />
          <Select label="Motif" data={MOTIFS} value={motif} onChange={setMotif} allowDeselect={false} />
          <Textarea label="Commentaire" maxLength={500} value={commentaire} onChange={(e) => setCommentaire(e.currentTarget.value)} />
          <Button disabled={matricule.trim() === ''} loading={creer.isPending} onClick={() => creer.mutate()}>Envoyer la demande</Button>
        </Stack>
      </Modal>
      <Modal opened={aRefuser !== null} onClose={() => setARefuser(null)} title="Refuser le transfert">
        <Stack>
          <Text size="sm">{aRefuser?.eleve} vers {aRefuser?.destination}</Text>
          <Textarea label="Motif du refus (obligatoire)" maxLength={500} value={motifRefus} onChange={(e) => setMotifRefus(e.currentTarget.value)} />
          <Button color="red" disabled={motifRefus.trim() === ''} loading={decider.isPending}
            onClick={() => aRefuser && decider.mutate({ t: aRefuser, decision: 'REFUSE', motifRefus: motifRefus.trim() })}>Refuser</Button>
        </Stack>
      </Modal>
    </Stack>
  )
}
