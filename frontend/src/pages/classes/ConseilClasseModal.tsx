import { useEffect, useState } from 'react'
import { Alert, Badge, Button, Group, Loader, Modal, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { appliquerSuggestionsConseil, conseilClasse, deciderConseilClasse } from '../../api/endpoints'
import type { Classe, ConseilClasse } from '../../api/types'

const DECISIONS = [
  { value: 'PASSAGE', label: 'Passage' },
  { value: 'REDOUBLEMENT', label: 'Redoublement' },
  { value: 'EXCLUSION', label: 'Exclusion' },
]
const SUGGESTION: Record<string, { libelle: string; couleur: string }> = {
  ADMIS: { libelle: 'Admis', couleur: 'green' },
  A_DELIBERER: { libelle: 'À délibérer', couleur: 'yellow' },
  REDOUBLE: { libelle: 'Redouble', couleur: 'red' },
}
const MENTION: Record<string, string> = { FELICITATIONS: 'Félicitations', ENCOURAGEMENTS: 'Encouragements', TABLEAU_HONNEUR: "Tableau d'honneur" }
const note = (v: number | null | undefined) => (v === null || v === undefined ? '—' : v.toFixed(2).replace('.', ','))

interface Saisie {
  decision: string | null
  observation: string
}

/** Conseil de classe de fin d'année : moyenne annuelle, suggestion et décision (passage, redoublement, exclusion) de chaque élève. */
export default function ConseilClasseModal({ classe, onFermer }: { classe: Classe; onFermer: () => void }) {
  const conseil = useQuery({ queryKey: ['conseil-classe', classe.id], queryFn: () => conseilClasse(classe.id) })
  return (
    <Modal opened onClose={onFermer} size="95%" title={`Conseil de classe — ${classe.nom}`}>
      {conseil.isPending && <Loader />}
      {conseil.isError && <Alert color="red" variant="light">{conseil.error.message}</Alert>}
      {conseil.data && <Tableau classeId={classe.id} donnees={conseil.data} />}
    </Modal>
  )
}

function Tableau({ classeId, donnees }: { classeId: number; donnees: ConseilClasse }) {
  const queryClient = useQueryClient()
  const [saisies, setSaisies] = useState<Record<number, Saisie>>({})
  useEffect(() => {
    const init: Record<number, Saisie> = {}
    donnees.eleves.forEach((l) => {
      init[l.inscriptionId] = { decision: l.decision, observation: l.observation ?? '' }
    })
    setSaisies(init)
  }, [donnees])

  const apres = (d: ConseilClasse) => {
    queryClient.setQueryData(['conseil-classe', classeId], d)
  }
  const enregistrer = useMutation({
    mutationFn: () => {
      const modifiees = donnees.eleves.filter((l) => {
        const s = saisies[l.inscriptionId]
        return s !== undefined && ((s.decision ?? null) !== l.decision || (s.decision !== null && s.observation.trim() !== (l.observation ?? '')))
      })
      return deciderConseilClasse(classeId, modifiees.map((l) => ({
        inscriptionId: l.inscriptionId,
        decision: saisies[l.inscriptionId].decision,
        observation: saisies[l.inscriptionId].observation.trim() || null,
      })))
    },
    onSuccess: (d) => {
      notifications.show({ color: 'green', message: 'Décisions enregistrées.' })
      apres(d)
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })
  const suggestions = useMutation({
    mutationFn: () => appliquerSuggestionsConseil(classeId),
    onSuccess: (d) => {
      notifications.show({ color: 'green', message: 'Décisions évidentes appliquées : les cas à délibérer restent à trancher.' })
      apres(d)
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const r = donnees.resume
  const maj = (id: number, changement: Partial<Saisie>) => setSaisies((s) => ({ ...s, [id]: { ...(s[id] ?? { decision: null, observation: '' }), ...changement } }))

  return (
    <Stack>
      <Text size="sm" c="dimmed">
        Moyenne annuelle = moyenne des moyennes des périodes. Suggestion : 10 et plus admis, de 8 à moins de 10 à délibérer, moins de 8 redouble. Une décision contraire à la suggestion, une exclusion ou une décision sans moyenne doit être motivée.
      </Text>
      <Group gap="xs">
        <Badge variant="light" size="lg">{r.effectif} élèves</Badge>
        <Badge variant="light" size="lg" color="green">{r.admis} admis</Badge>
        <Badge variant="light" size="lg" color="yellow">{r.aDeliberer} à délibérer</Badge>
        <Badge variant="light" size="lg" color="red">{r.redouble} redoublent</Badge>
        <Badge variant="light" size="lg">{r.decides} décidés : {r.passages} passages, {r.redoublements} redoublements, {r.exclusions} exclusions</Badge>
        {r.tauxPassage !== null && <Badge variant="light" size="lg" color="blue">Taux de passage {String(r.tauxPassage).replace('.', ',')} %</Badge>}
      </Group>
      <Table.ScrollContainer minWidth={900}>
        <Table striped withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Rang</Table.Th>
              <Table.Th>Élève</Table.Th>
              {donnees.periodes.map((p) => <Table.Th key={p.id}>{p.libelle}</Table.Th>)}
              <Table.Th>Moyenne annuelle</Table.Th>
              <Table.Th>Suggestion</Table.Th>
              <Table.Th>Décision</Table.Th>
              <Table.Th>Observation</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {donnees.eleves.map((l) => {
              const s = saisies[l.inscriptionId] ?? { decision: l.decision, observation: l.observation ?? '' }
              return (
                <Table.Tr key={l.inscriptionId}>
                  <Table.Td>{l.rang ?? '—'}</Table.Td>
                  <Table.Td>{l.eleve.prenom} {l.eleve.nom}<Text size="xs" c="dimmed">{l.eleve.matricule}</Text></Table.Td>
                  {donnees.periodes.map((p) => <Table.Td key={p.id}>{note(l.moyennesPeriodes[String(p.id)])}</Table.Td>)}
                  <Table.Td><b>{note(l.moyenneAnnuelle)}</b>{l.mention && <Text size="xs" c="dimmed">{MENTION[l.mention]}</Text>}</Table.Td>
                  <Table.Td>{l.suggestion ? <Badge variant="light" color={SUGGESTION[l.suggestion].couleur}>{SUGGESTION[l.suggestion].libelle}</Badge> : '—'}</Table.Td>
                  <Table.Td>
                    <Select aria-label="Décision" placeholder="—" data={DECISIONS} value={s.decision} onChange={(v) => maj(l.inscriptionId, { decision: v })} clearable w={150} />
                  </Table.Td>
                  <Table.Td>
                    <TextInput aria-label="Observation" maxLength={500} value={s.observation} disabled={s.decision === null} onChange={(e) => maj(l.inscriptionId, { observation: e.currentTarget.value })} />
                  </Table.Td>
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
      <Group justify="flex-end">
        <Button variant="light" loading={suggestions.isPending} onClick={() => suggestions.mutate()}>Appliquer les décisions évidentes</Button>
        <Button loading={enregistrer.isPending} onClick={() => enregistrer.mutate()}>Enregistrer</Button>
      </Group>
    </Stack>
  )
}
