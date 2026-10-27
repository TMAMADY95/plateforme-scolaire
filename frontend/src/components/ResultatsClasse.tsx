import { useState } from 'react'
import { Alert, Loader, Select, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerPeriodes, obtenirResultats } from '../api/endpoints'
import type { Periode } from '../api/types'
import { aujourdhui } from '../utils/dates'
import { formaterNote } from '../utils/notes'

function periodeParDefaut(periodes: Periode[]): string | null {
  const jour = aujourdhui()
  const choisie = periodes.find((p) => p.dateDebut <= jour && jour <= p.dateFin) ?? periodes[periodes.length - 1]
  return choisie ? String(choisie.id) : null
}

interface Props {
  classeId: number
  anneeScolaireId: number
}

/** Moyennes par matière, moyenne générale et rang de chaque élève d'une classe, pour une période. */
export default function ResultatsClasse({ classeId, anneeScolaireId }: Props) {
  const [choix, setChoix] = useState<string | null>(null)

  const periodes = useQuery({ queryKey: ['periodes', anneeScolaireId], queryFn: () => listerPeriodes(anneeScolaireId) })
  const periodeId = choix ?? (periodes.data ? periodeParDefaut(periodes.data) : null)

  const resultats = useQuery({
    queryKey: ['resultats', classeId, periodeId],
    queryFn: () => obtenirResultats(classeId, Number(periodeId)),
    enabled: periodeId !== null,
  })

  const optionsPeriodes = (periodes.data ?? []).map((p) => ({ value: String(p.id), label: p.libelle }))

  return (
    <Stack>
      <Select
        label="Période"
        data={optionsPeriodes}
        value={periodeId}
        onChange={setChoix}
        allowDeselect={false}
        w={240}
      />

      {(periodes.isPending || (resultats.isPending && periodeId !== null)) && <Loader />}
      {(periodes.isError || resultats.isError) && (
        <Alert color="red" variant="light">
          {(periodes.error ?? resultats.error)?.message}
        </Alert>
      )}
      {periodes.data && periodes.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucune période n'est définie pour cette année scolaire.
        </Alert>
      )}

      {resultats.data && resultats.data.matieres.length === 0 && (
        <Text c="dimmed">Aucune note n'a encore été saisie pour cette période.</Text>
      )}
      {resultats.data && resultats.data.matieres.length > 0 && (
        <Table.ScrollContainer minWidth={360 + resultats.data.matieres.length * 110}>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Rang</Table.Th>
                <Table.Th>Élève</Table.Th>
                {resultats.data.matieres.map((m) => (
                  <Table.Th key={m.id}>{m.libelle}</Table.Th>
                ))}
                <Table.Th>Moyenne générale</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {resultats.data.eleves.map((ligne) => (
                <Table.Tr key={ligne.inscriptionId}>
                  <Table.Td>{ligne.rang ?? '—'}</Table.Td>
                  <Table.Td>
                    {ligne.eleve.nom} {ligne.eleve.prenom}
                  </Table.Td>
                  {resultats.data.matieres.map((m) => (
                    <Table.Td key={m.id}>{formaterNote(ligne.moyennesParMatiere[String(m.id)] ?? null)}</Table.Td>
                  ))}
                  <Table.Td fw={600}>{formaterNote(ligne.moyenneGenerale)}</Table.Td>
                </Table.Tr>
              ))}
              <Table.Tr>
                <Table.Td />
                <Table.Td fw={600}>Moyenne de la classe</Table.Td>
                {resultats.data.matieres.map((m) => (
                  <Table.Td key={m.id} fw={600}>
                    {formaterNote(resultats.data.moyennesClasseParMatiere[String(m.id)] ?? null)}
                  </Table.Td>
                ))}
                <Table.Td fw={600}>{formaterNote(resultats.data.moyenneGeneraleClasse)}</Table.Td>
              </Table.Tr>
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
    </Stack>
  )
}
