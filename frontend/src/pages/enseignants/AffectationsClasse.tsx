import { useState } from 'react'
import { Alert, Button, Group, Loader, Select, Stack, Table, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { useQuery } from '@tanstack/react-query'
import { listerAffectationsClasse, listerClasses } from '../../api/endpoints'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'
import AffectationModal from './AffectationModal'

export default function AffectationsClasse({ etablissementId }: { etablissementId: number }) {
  const { anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const [classeChoisie, setClasseChoisie] = useState<string | null>(null)
  const [ouverte, { open, close }] = useDisclosure()

  const classes = useQuery({
    queryKey: ['classes', etablissementId, anneeId],
    queryFn: () => listerClasses(etablissementId, Number(anneeId)),
    enabled: anneeId !== null,
  })

  const classe = classes.data?.find((c) => String(c.id) === classeChoisie)

  const affectations = useQuery({
    queryKey: ['affectations', classe?.id],
    queryFn: () => listerAffectationsClasse((classe as { id: number }).id),
    enabled: classe !== undefined,
  })

  const optionsClasses = (classes.data ?? []).map((c) => ({ value: String(c.id), label: `${c.nom} — ${c.niveauLibelle}` }))

  return (
    <Stack mt="md">
      <Group align="flex-end">
        <Select
          label="Année scolaire"
          data={options}
          value={anneeId}
          onChange={(valeur) => {
            setAnneeChoisie(valeur)
            setClasseChoisie(null)
          }}
          allowDeselect={false}
          w={200}
        />
        <Select
          label="Classe"
          placeholder="Choisir une classe"
          data={optionsClasses}
          value={classeChoisie}
          onChange={setClasseChoisie}
          w={260}
        />
        <Button onClick={open} disabled={classe === undefined}>
          Affecter un enseignant
        </Button>
      </Group>

      {classes.isError && (
        <Alert color="red" variant="light">
          {classes.error.message}
        </Alert>
      )}
      {classes.data && classes.data.length === 0 && (
        <Text c="dimmed">Aucune classe pour cette année scolaire.</Text>
      )}
      {classe === undefined && classes.data && classes.data.length > 0 && (
        <Text c="dimmed">Choisissez une classe pour voir ses enseignants.</Text>
      )}

      {affectations.isPending && classe !== undefined && <Loader />}
      {affectations.isError && (
        <Alert color="red" variant="light">
          {affectations.error.message}
        </Alert>
      )}
      {affectations.data && affectations.data.length === 0 && (
        <Text c="dimmed">Aucun enseignant n'est encore affecté à cette classe.</Text>
      )}
      {affectations.data && affectations.data.length > 0 && (
        <Table striped withTableBorder maw={520}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Enseignant</Table.Th>
              <Table.Th>Matière</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {affectations.data.map((a) => (
              <Table.Tr key={a.id}>
                <Table.Td>{a.enseignantNom}</Table.Td>
                <Table.Td>{a.matiereLibelle}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}

      {ouverte && classe !== undefined && <AffectationModal classe={classe} onFermer={close} />}
    </Stack>
  )
}
