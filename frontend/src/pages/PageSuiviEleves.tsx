import { useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { annulerAbandon, declarerAbandon, enregistrerVulnerabilite, suiviEleves } from '../api/endpoints'
import { MOTIFS_ABANDON, TYPES_HANDICAP, TYPES_ORPHELIN, libelleDans } from '../api/types'
import type { LigneSuiviEleve } from '../api/types'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { formaterDate } from '../utils/dates'

/** Abandon scolaire et situation des élèves (orphelins, handicap). Visible de la direction de l'établissement uniquement. */
export default function PageSuiviEleves() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const queryClient = useQueryClient()
  const { anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const [recherche, setRecherche] = useState('')
  const [abandonDe, setAbandonDe] = useState<LigneSuiviEleve | null>(null)
  const [situationDe, setSituationDe] = useState<LigneSuiviEleve | null>(null)

  const liste = useQuery({
    queryKey: ['suivi-eleves', etablissementId, anneeId],
    queryFn: () => suiviEleves(etablissementId as number, Number(anneeId)),
    enabled: etablissementId !== undefined && anneeId !== null,
  })
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['suivi-eleves'] })
  const erreur = (e: Error) => notifications.show({ color: 'red', title: 'Action impossible', message: e.message })

  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [motif, setMotif] = useState<string | null>(null)
  const [observation, setObservation] = useState('')
  const declarer = useMutation({
    mutationFn: () => declarerAbandon(etablissementId as number, { inscriptionId: abandonDe!.inscriptionId, dateAbandon: date, motif: motif ?? '', observation: observation.trim() || null }),
    onSuccess: () => {
      notifications.show({ color: 'green', message: 'Abandon enregistré.' })
      setAbandonDe(null)
      setMotif(null)
      setObservation('')
      rafraichir()
    },
    onError: erreur,
  })
  const annuler = useMutation({ mutationFn: (id: number) => annulerAbandon(id), onSuccess: rafraichir, onError: erreur })

  const [orphelin, setOrphelin] = useState<string | null>('AUCUN')
  const [handicap, setHandicap] = useState<string | null>(null)
  const situation = useMutation({
    mutationFn: () => enregistrerVulnerabilite(etablissementId as number, situationDe!.eleveId, { orphelin: orphelin ?? 'AUCUN', handicap }),
    onSuccess: () => {
      setSituationDe(null)
      rafraichir()
    },
    onError: erreur,
  })

  if (etablissementId === undefined) {
    return <Alert color="orange" variant="light">Aucun établissement n'est associé à votre compte.</Alert>
  }
  const eleves = (liste.data?.eleves ?? []).filter((l) => `${l.nom} ${l.matricule}`.toLowerCase().includes(recherche.trim().toLowerCase()))
  const abandons = (liste.data?.eleves ?? []).filter((l) => l.abandon !== null).length

  return (
    <Stack>
      <Group justify="space-between" align="flex-end">
        <div>
          <Title order={2}>Suivi des élèves</Title>
          <Text c="dimmed" size="sm">
            Abandons en cours d'année, élèves orphelins ou en situation de handicap. Ces informations sont confidentielles : le ministère ne reçoit que des totaux.
            Un élève qui abandonne reste compté dans les effectifs de l'année.
          </Text>
        </div>
        <Select aria-label="Année scolaire" data={options} value={anneeId} onChange={setAnneeChoisie} allowDeselect={false} w={170} />
      </Group>
      <Group>
        <TextInput placeholder="Rechercher un élève" value={recherche} onChange={(e) => setRecherche(e.currentTarget.value)} w={260} />
        {liste.data && <Badge variant="light" size="lg" color={abandons ? 'orange' : 'gray'}>{abandons} abandon(s) sur {liste.data.eleves.length} élèves</Badge>}
      </Group>
      {liste.isLoading && <Loader />}
      {liste.isError && <Alert color="red">{liste.error.message}</Alert>}
      {liste.data && (
        <Table.ScrollContainer minWidth={760}>
          <Table striped withTableBorder verticalSpacing="xs">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Élève</Table.Th>
                <Table.Th>Classe</Table.Th>
                <Table.Th>Situation</Table.Th>
                <Table.Th>Abandon</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {eleves.map((l) => (
                <Table.Tr key={l.inscriptionId}>
                  <Table.Td>{l.nom}<Text size="xs" c="dimmed">{l.matricule}</Text></Table.Td>
                  <Table.Td>{l.classe ?? '—'}</Table.Td>
                  <Table.Td>
                    {l.orphelin !== 'AUCUN' && <Badge variant="light" mr={4}>{libelleDans(TYPES_ORPHELIN, l.orphelin)}</Badge>}
                    {l.handicap && <Badge variant="light" color="grape">Handicap {libelleDans(TYPES_HANDICAP, l.handicap).toLowerCase()}</Badge>}
                    {l.orphelin === 'AUCUN' && !l.handicap && '—'}
                  </Table.Td>
                  <Table.Td>
                    {l.abandon ? (
                      <>
                        <Badge color="orange" variant="light">{libelleDans(MOTIFS_ABANDON, l.abandon.motif)}</Badge>
                        <Text size="xs" c="dimmed">le {formaterDate(l.abandon.dateAbandon)}</Text>
                      </>
                    ) : '—'}
                  </Table.Td>
                  <Table.Td>
                    <Group gap="xs" wrap="nowrap">
                      <Button size="compact-xs" variant="light" onClick={() => { setSituationDe(l); setOrphelin(l.orphelin); setHandicap(l.handicap) }}>Situation</Button>
                      {l.abandon ? (
                        <Button size="compact-xs" variant="subtle" color="red" loading={annuler.isPending} onClick={() => annuler.mutate(l.abandon!.id)}>Annuler l'abandon</Button>
                      ) : (
                        <Button size="compact-xs" variant="light" color="orange" onClick={() => setAbandonDe(l)}>Déclarer un abandon</Button>
                      )}
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
      {abandonDe && (
        <Modal opened onClose={() => setAbandonDe(null)} title={`Abandon de ${abandonDe.nom}`}>
          <Stack>
            <TextInput label="Date" type="date" value={date} onChange={(e) => setDate(e.currentTarget.value)} />
            <Select label="Motif" data={MOTIFS_ABANDON} value={motif} onChange={setMotif} />
            <TextInput label="Observation (facultatif)" value={observation} onChange={(e) => setObservation(e.currentTarget.value)} />
            <Group justify="flex-end">
              <Button variant="default" onClick={() => setAbandonDe(null)}>Annuler</Button>
              <Button color="orange" disabled={motif === null} loading={declarer.isPending} onClick={() => declarer.mutate()}>Enregistrer</Button>
            </Group>
          </Stack>
        </Modal>
      )}
      {situationDe && (
        <Modal opened onClose={() => setSituationDe(null)} title={`Situation de ${situationDe.nom}`}>
          <Stack>
            <Select label="Orphelin" data={TYPES_ORPHELIN} value={orphelin} onChange={setOrphelin} allowDeselect={false} />
            <Select label="Handicap" placeholder="Aucun" clearable data={TYPES_HANDICAP} value={handicap} onChange={setHandicap} />
            <Group justify="flex-end">
              <Button variant="default" onClick={() => setSituationDe(null)}>Annuler</Button>
              <Button loading={situation.isPending} onClick={() => situation.mutate()}>Enregistrer</Button>
            </Group>
          </Stack>
        </Modal>
      )}
    </Stack>
  )
}
