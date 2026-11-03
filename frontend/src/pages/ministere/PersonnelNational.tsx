import { ActionIcon, Alert, Badge, Button, Group, Loader, Modal, NumberInput, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ajouterPersonnelNational, definirBesoinPersonnel, personnelNational, retirerPersonnelNational } from '../../api/endpoints'
import { useDroitsExamens } from '../../auth/useDroitsExamens'

const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const SOURCES: Record<string, string> = { CENTRES: 'saisi dans les centres', COMMISSIONS: 'commissions de correction', NATIONAL: 'saisi ici' }

/** Personnel mobilisé pour l'examen choisi : requis, disponible et manque par fonction ; jurys, superviseurs, inspecteurs et agents informatiques saisis ici. */
export default function PersonnelNational({ examenId }: { examenId: number }) {
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const [fonction, setFonction] = useState<string | null>(null)
  const [nom, setNom] = useState('')
  const [telephone, setTelephone] = useState('')
  const [besoin, setBesoin] = useState<{ fonction: string; libelle: string } | null>(null)
  const [requis, setRequis] = useState<number | string>(0)

  const requete = useQuery({ queryKey: ['personnel-national', examenId], queryFn: () => personnelNational(examenId) })
  const maj = () => void queryClient.invalidateQueries({ queryKey: ['personnel-national'] })
  const erreur = (e: Error) => notifications.show({ color: 'red', message: e.message })
  const ajouter = useMutation({
    mutationFn: () => ajouterPersonnelNational(examenId, { fonction: fonction as string, nom, telephone: telephone.trim() || null }),
    onSuccess: () => { setFonction(null); setNom(''); setTelephone(''); maj() },
    onError: erreur,
  })
  const retirer = useMutation({ mutationFn: (id: number) => retirerPersonnelNational(examenId, id), onSuccess: maj, onError: erreur })
  const definir = useMutation({
    mutationFn: () => definirBesoinPersonnel(examenId, { fonction: (besoin as { fonction: string }).fonction, requis: Number(requis) || 0 }),
    onSuccess: () => { setBesoin(null); maj() },
    onError: erreur,
  })
  const d = requete.data
  const saisissables = d?.fonctions.filter((f) => f.personnesSaisies) ?? []

  return (
    <Stack mt="xl">
      <Group justify="space-between">
        <div>
          <Title order={3}>Personnel mobilisé</Title>
          <Text c="dimmed" size="sm">Requis, disponible et manque par fonction pour l'examen choisi.</Text>
        </div>
        {dgecs && <Button variant="light" onClick={() => setFonction(saisissables[0]?.fonction ?? 'JURY')}>Ajouter une personne</Button>}
      </Group>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">Personnel indisponible.</Alert>}
      {d && (
        <>
          <Table.ScrollContainer minWidth={640}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr><Table.Th>Fonction</Table.Th><Table.Th ta="right">Requis</Table.Th><Table.Th ta="right">Disponibles</Table.Th><Table.Th ta="right">Manque</Table.Th><Table.Th>Origine</Table.Th><Table.Th /></Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.fonctions.map((f) => (
                  <Table.Tr key={f.fonction}>
                    <Table.Td>{f.libelle}</Table.Td>
                    <Table.Td ta="right">{f.requis === null ? <Text span c="dimmed">non déclaré</Text> : nombre(f.requis)}</Table.Td>
                    <Table.Td ta="right">{nombre(f.disponibles)}</Table.Td>
                    <Table.Td ta="right">{f.manque === null ? '—' : <Badge color={f.manque > 0 ? 'red' : 'green'} variant="light">{f.manque}</Badge>}</Table.Td>
                    <Table.Td><Text size="xs" c="dimmed">{SOURCES[f.source]}</Text></Table.Td>
                    <Table.Td>{dgecs && f.besoinModifiable && (
                      <Button size="xs" variant="subtle" onClick={() => { setBesoin({ fonction: f.fonction, libelle: f.libelle }); setRequis(f.requis ?? 0) }}>Définir le besoin</Button>
                    )}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
          {d.personnes.length > 0 && (
            <Table withTableBorder>
              <Table.Thead><Table.Tr><Table.Th>Fonction</Table.Th><Table.Th>Nom</Table.Th><Table.Th>Téléphone</Table.Th><Table.Th /></Table.Tr></Table.Thead>
              <Table.Tbody>
                {d.personnes.map((p) => (
                  <Table.Tr key={p.id}>
                    <Table.Td>{d.fonctions.find((f) => f.fonction === p.fonction)?.libelle ?? p.fonction}</Table.Td><Table.Td>{p.nom}</Table.Td><Table.Td>{p.telephone ?? '—'}</Table.Td>
                    <Table.Td>{dgecs && <ActionIcon color="red" variant="subtle" aria-label="Retirer" onClick={() => retirer.mutate(p.id)}>×</ActionIcon>}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </>
      )}
      <Modal opened={fonction !== null} onClose={() => setFonction(null)} title="Ajouter une personne">
        <Stack>
          <Select label="Fonction" data={saisissables.map((f) => ({ value: f.fonction, label: f.libelle }))} value={fonction} onChange={setFonction} allowDeselect={false} />
          <TextInput label="Nom et prénom" value={nom} onChange={(e) => setNom(e.currentTarget.value)} maxLength={150} required />
          <TextInput label="Téléphone" value={telephone} onChange={(e) => setTelephone(e.currentTarget.value)} maxLength={30} />
          <Button loading={ajouter.isPending} disabled={!nom.trim()} onClick={() => ajouter.mutate()}>Enregistrer</Button>
        </Stack>
      </Modal>
      <Modal opened={besoin !== null} onClose={() => setBesoin(null)} title={besoin ? `Besoin : ${besoin.libelle}` : ''}>
        <Stack>
          <NumberInput label="Nombre de personnes requises" min={0} value={requis} onChange={setRequis} />
          <Button loading={definir.isPending} onClick={() => definir.mutate()}>Enregistrer</Button>
        </Stack>
      </Modal>
    </Stack>
  )
}
