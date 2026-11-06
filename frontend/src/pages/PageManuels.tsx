import { useEffect, useState } from 'react'
import { Alert, Button, Group, Loader, NumberInput, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { declarerManuel, dotationManuels } from '../api/endpoints'
import type { LigneDotationManuel } from '../api/types'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import CouvertureManuel from '../components/CouvertureManuel'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'

interface Saisie {
  recus: number
  distribues: number
  perdus: number
  observation: string
}

/** Manuels scolaires de l'établissement : exemplaires reçus, distribués aux élèves et perdus, comparés aux effectifs de chaque niveau. */
export default function PageManuels() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const queryClient = useQueryClient()
  const { anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const dotation = useQuery({
    queryKey: ['manuels', etablissementId, anneeId],
    queryFn: () => dotationManuels(etablissementId as number, Number(anneeId)),
    enabled: etablissementId !== undefined && anneeId !== null,
  })
  const [saisies, setSaisies] = useState<Record<number, Saisie>>({})
  useEffect(() => {
    const init: Record<number, Saisie> = {}
    dotation.data?.lignes.forEach((l) => {
      init[l.manuelId] = { recus: l.recus, distribues: l.distribues, perdus: l.perdus, observation: l.observation ?? '' }
    })
    setSaisies(init)
  }, [dotation.data])

  const enregistrer = useMutation({
    mutationFn: (l: LigneDotationManuel) => {
      const s = saisies[l.manuelId]
      return declarerManuel(etablissementId as number, Number(anneeId), l.manuelId, { ...s, observation: s.observation.trim() || null })
    },
    onSuccess: (d) => {
      notifications.show({ color: 'green', message: 'Enregistré.' })
      queryClient.setQueryData(['manuels', etablissementId, anneeId], d)
      void queryClient.invalidateQueries({ queryKey: ['manuels-synthese'] })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })

  if (etablissementId === undefined) return <Alert color="orange" variant="light">Aucun établissement n'est associé à votre compte.</Alert>
  const maj = (id: number, c: Partial<Saisie>) => setSaisies((s) => ({ ...s, [id]: { ...s[id], ...c } }))
  const lignes = dotation.data?.lignes ?? []

  return (
    <Stack>
      <Title order={2}>Manuels scolaires</Title>
      <Text c="dimmed" size="sm">
        Pour chaque manuel du programme des niveaux de votre établissement : exemplaires reçus, distribués aux élèves et perdus ou hors d'usage. La couverture compare les exemplaires utilisables aux élèves du niveau (objectif : un manuel par élève).
      </Text>
      <Select label="Année scolaire" data={options} value={anneeId} onChange={setAnneeChoisie} allowDeselect={false} w={200} />
      {dotation.isPending && anneeId !== null && <Loader />}
      {dotation.isError && <Alert color="red" variant="light">{dotation.error.message}</Alert>}
      {dotation.data && lignes.length === 0 && <Text c="dimmed">Aucun manuel au catalogue pour les niveaux de votre établissement. Le ministère tient le catalogue des manuels.</Text>}
      {lignes.length > 0 && (
        <Table.ScrollContainer minWidth={1000}>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr><Table.Th>Manuel</Table.Th><Table.Th>Élèves</Table.Th><Table.Th>Reçus</Table.Th><Table.Th>Distribués</Table.Th><Table.Th>Perdus</Table.Th><Table.Th>En stock</Table.Th><Table.Th>Couverture</Table.Th><Table.Th>Observation</Table.Th><Table.Th /></Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {lignes.map((l) => {
                const s = saisies[l.manuelId]
                if (!s) return null
                return (
                  <Table.Tr key={l.manuelId}>
                    <Table.Td>{l.titre}<Text size="xs" c="dimmed">{l.matiere} · {l.niveau}</Text></Table.Td>
                    <Table.Td>{l.effectif}</Table.Td>
                    <Table.Td><NumberInput aria-label="Reçus" min={0} value={s.recus} onChange={(v) => maj(l.manuelId, { recus: Number(v) || 0 })} w={90} /></Table.Td>
                    <Table.Td><NumberInput aria-label="Distribués" min={0} value={s.distribues} onChange={(v) => maj(l.manuelId, { distribues: Number(v) || 0 })} w={90} /></Table.Td>
                    <Table.Td><NumberInput aria-label="Perdus" min={0} value={s.perdus} onChange={(v) => maj(l.manuelId, { perdus: Number(v) || 0 })} w={90} /></Table.Td>
                    <Table.Td>{Math.max(0, s.recus - s.distribues - s.perdus)}</Table.Td>
                    <Table.Td><CouvertureManuel etat={l.etat} couverture={l.couverture} /></Table.Td>
                    <Table.Td><TextInput aria-label="Observation" maxLength={500} value={s.observation} onChange={(e) => maj(l.manuelId, { observation: e.currentTarget.value })} /></Table.Td>
                    <Table.Td><Button size="compact-sm" loading={enregistrer.isPending && enregistrer.variables?.manuelId === l.manuelId} onClick={() => enregistrer.mutate(l)}>Enregistrer</Button></Table.Td>
                  </Table.Tr>
                )
              })}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
      {lignes.some((l) => saisies[l.manuelId] && saisies[l.manuelId].distribues + saisies[l.manuelId].perdus > saisies[l.manuelId].recus) && (
        <Group><Alert color="orange" variant="light">Une ligne a plus d'exemplaires distribués et perdus que de reçus : elle sera refusée à l'enregistrement.</Alert></Group>
      )}
    </Stack>
  )
}
