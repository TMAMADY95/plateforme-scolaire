import { useState } from 'react'
import { Alert, Button, Group, Loader, Select, Stack, Table, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { useQuery } from '@tanstack/react-query'
import { listerEvaluations, listerPeriodes } from '../../api/endpoints'
import type { Evaluation } from '../../api/types'
import { formaterDate } from '../../utils/dates'
import ModifierEvaluationModal from './ModifierEvaluationModal'
import NotesModal from './NotesModal'
import NouvelleEvaluationModal from './NouvelleEvaluationModal'

interface Props {
  classeId: number
  anneeScolaireId: number
  /** Matières que l'enseignant assure dans cette classe : les seules qu'il peut noter. */
  mesMatieres: { id: number; libelle: string }[]
}

export default function OngletEvaluations({ classeId, anneeScolaireId, mesMatieres }: Props) {
  const [periodeFiltre, setPeriodeFiltre] = useState<string | null>(null)
  const [nouvelleOuverte, { open, close }] = useDisclosure()
  const [ouverte, setOuverte] = useState<Evaluation | null>(null)
  const [aModifier, setAModifier] = useState<Evaluation | null>(null)

  const periodes = useQuery({ queryKey: ['periodes', anneeScolaireId], queryFn: () => listerPeriodes(anneeScolaireId) })
  const evaluations = useQuery({ queryKey: ['evaluations', classeId], queryFn: () => listerEvaluations(classeId) })

  const visibles = (evaluations.data ?? []).filter(
    (e) => periodeFiltre === null || String(e.periodeId) === periodeFiltre,
  )
  const optionsPeriodes = (periodes.data ?? []).map((p) => ({ value: String(p.id), label: p.libelle }))

  return (
    <Stack>
      <Group justify="space-between" align="flex-end">
        <Select
          label="Période"
          placeholder="Toutes"
          data={optionsPeriodes}
          value={periodeFiltre}
          onChange={setPeriodeFiltre}
          clearable
          w={220}
        />
        <Button onClick={open} disabled={periodes.isPending}>
          Nouvelle évaluation
        </Button>
      </Group>

      {(periodes.isError || evaluations.isError) && (
        <Alert color="red" variant="light">
          {(periodes.error ?? evaluations.error)?.message}
        </Alert>
      )}
      {evaluations.isPending && <Loader />}
      {periodes.data && periodes.data.length === 0 && (
        <Alert color="orange" variant="light">
          Aucune période n'est définie pour cette année scolaire : le ministère doit en créer avant de pouvoir
          saisir des évaluations.
        </Alert>
      )}
      {evaluations.data && visibles.length === 0 && <Text c="dimmed">Aucune évaluation pour ces critères.</Text>}

      {visibles.length > 0 && (
        <Table.ScrollContainer minWidth={720}>
          <Table striped highlightOnHover withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Évaluation</Table.Th>
                <Table.Th>Matière</Table.Th>
                <Table.Th>Période</Table.Th>
                <Table.Th>Date</Table.Th>
                <Table.Th>Coef.</Table.Th>
                <Table.Th>Barème</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {visibles.map((e) => {
                const mienne = mesMatieres.some((m) => m.id === e.matiereId)
                return (
                  <Table.Tr key={e.id}>
                    <Table.Td>{e.titre}</Table.Td>
                    <Table.Td>{e.matiere}</Table.Td>
                    <Table.Td>{e.periode}</Table.Td>
                    <Table.Td>{formaterDate(e.dateEvaluation)}</Table.Td>
                    <Table.Td>{e.coefficient}</Table.Td>
                    <Table.Td>{e.bareme}</Table.Td>
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        <Button size="compact-sm" variant={mienne ? 'light' : 'subtle'} onClick={() => setOuverte(e)}>
                          {mienne ? 'Saisir les notes' : 'Consulter'}
                        </Button>
                        {mienne && (
                          <Button size="compact-sm" variant="subtle" onClick={() => setAModifier(e)}>
                            Modifier
                          </Button>
                        )}
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                )
              })}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {nouvelleOuverte && periodes.data && (
        <NouvelleEvaluationModal
          classeId={classeId}
          periodes={periodes.data}
          mesMatieres={mesMatieres}
          onFermer={close}
        />
      )}
      {aModifier !== null && <ModifierEvaluationModal evaluation={aModifier} onFermer={() => setAModifier(null)} />}
      {ouverte !== null && (
        <NotesModal
          evaluation={ouverte}
          modifiable={mesMatieres.some((m) => m.id === ouverte.matiereId)}
          onFermer={() => setOuverte(null)}
        />
      )}
    </Stack>
  )
}
