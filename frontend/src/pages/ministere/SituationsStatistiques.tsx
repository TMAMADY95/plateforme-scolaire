import { Alert, Badge, Button, Card, Group, Loader, Modal, Select, SimpleGrid, Stack, Table, Text, Textarea, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { etablirSituation, listerSituations } from '../../api/endpoints'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'
import { useDroitsExamens } from '../../auth/useDroitsExamens'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)
const dateHeure = (iso: string) => new Date(iso).toLocaleString('fr-FR', { dateStyle: 'long', timeStyle: 'short' })

/** Situations statistiques datées : chiffres figés par la DGECS, provisoires puis définitifs (une seule situation définitive par année). */
export default function SituationsStatistiques() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const [ouvert, setOuvert] = useState(false)
  const [statut, setStatut] = useState<string | null>('PROVISOIRE')
  const [commentaire, setCommentaire] = useState('')

  const requete = useQuery({ queryKey: ['situations', anneeId], queryFn: () => listerSituations(anneeId as number), enabled: anneeId !== null })
  const etablir = useMutation({
    mutationFn: () => etablirSituation(anneeId as number, { statut: statut ?? 'PROVISOIRE', commentaire: commentaire.trim() || null }),
    onSuccess: () => {
      setOuvert(false)
      setCommentaire('')
      void queryClient.invalidateQueries({ queryKey: ['situations'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })

  return (
    <Stack>
      <Group justify="space-between" align="end">
        <div>
          <Title order={3}>Situation statistique des examens</Title>
          <Text c="dimmed" size="sm">Chiffres figés à une date donnée. Seule la dernière situation fait foi ; la situation définitive est unique.</Text>
        </div>
        <Group align="end">
          <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={180} />
          {dgecs && <Button onClick={() => setOuvert(true)}>Établir une situation</Button>}
        </Group>
      </Group>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">Situations indisponibles.</Alert>}
      {requete.data && requete.data.length === 0 && <Text c="dimmed">Aucune situation établie pour cette année.</Text>}
      {requete.data?.map((s, i) => (
        <Card key={s.id} withBorder>
          <Stack gap="xs">
            <Group justify="space-between">
              <Group>
                <Badge color={s.statut === 'DEFINITIVE' ? 'green' : 'orange'}>{s.statut === 'DEFINITIVE' ? 'Définitive' : 'Provisoire'}</Badge>
                {i === 0 && <Badge variant="light">Dernière situation</Badge>}
                <Text fw={600}>{dateHeure(s.etablieLe)}</Text>
              </Group>
              <Text size="sm" c="dimmed">{s.etabliePar ? `Établie par ${s.etabliePar}` : ''}</Text>
            </Group>
            {s.commentaire && <Text size="sm">{s.commentaire}</Text>}
            <SimpleGrid cols={{ base: 2, sm: 4, lg: 8 }}>
              <div><Text size="xs" c="dimmed">Inscrits</Text><Text fw={700}>{nombre(s.inscrits)}</Text></div>
              <div><Text size="xs" c="dimmed">Filles</Text><Text fw={700}>{nombre(s.filles)}</Text></div>
              <div><Text size="xs" c="dimmed">Libres</Text><Text fw={700}>{nombre(s.libres)}</Text></div>
              <div><Text size="xs" c="dimmed">Présents</Text><Text fw={700}>{nombre(s.presents)}</Text></div>
              <div><Text size="xs" c="dimmed">Admis</Text><Text fw={700}>{nombre(s.admis)}</Text></div>
              <div><Text size="xs" c="dimmed">Réussite</Text><Text fw={700}>{pourcent(s.tauxReussite)}</Text></div>
              <div><Text size="xs" c="dimmed">Centres</Text><Text fw={700}>{nombre(s.centres)}</Text></div>
              <div><Text size="xs" c="dimmed">Salles</Text><Text fw={700}>{nombre(s.salles)}</Text></div>
            </SimpleGrid>
            {s.ecartInscritsActuel !== 0 && (
              <Text size="sm" c="orange">Depuis cette situation, le nombre d'inscrits a évolué de {s.ecartInscritsActuel > 0 ? '+' : ''}{nombre(s.ecartInscritsActuel)}.</Text>
            )}
            <Table.ScrollContainer minWidth={420}>
              <Table withTableBorder>
                <Table.Thead>
                  <Table.Tr><Table.Th>Niveau d'examen</Table.Th><Table.Th ta="right">Inscrits</Table.Th><Table.Th ta="right">Filles</Table.Th><Table.Th ta="right">Présents</Table.Th><Table.Th ta="right">Admis</Table.Th><Table.Th ta="right">Réussite</Table.Th></Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {s.niveaux.map((n) => (
                    <Table.Tr key={n.niveau}>
                      <Table.Td>{n.niveau}</Table.Td><Table.Td ta="right">{nombre(n.inscrits)}</Table.Td><Table.Td ta="right">{nombre(n.filles)}</Table.Td>
                      <Table.Td ta="right">{nombre(n.presents)}</Table.Td><Table.Td ta="right">{nombre(n.admis)}</Table.Td><Table.Td ta="right">{pourcent(n.tauxReussite)}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Stack>
        </Card>
      ))}
      <Modal opened={ouvert} onClose={() => setOuvert(false)} title="Établir une situation statistique">
        <Stack>
          <Select label="Statut" data={[{ value: 'PROVISOIRE', label: 'Provisoire' }, { value: 'DEFINITIVE', label: 'Définitive (résultats de tous les examens proclamés)' }]}
            value={statut} onChange={setStatut} allowDeselect={false} />
          {statut === 'DEFINITIVE' && <Alert color="orange">Une situation définitive est unique et ne peut plus être remplacée.</Alert>}
          <Textarea label="Commentaire" value={commentaire} onChange={(e) => setCommentaire(e.currentTarget.value)} maxLength={500} minRows={2} />
          <Button loading={etablir.isPending} onClick={() => etablir.mutate()}>Figer les chiffres actuels</Button>
        </Stack>
      </Modal>
    </Stack>
  )
}
