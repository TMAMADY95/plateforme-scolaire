import { Alert, Group, Select, Text } from '@mantine/core'
import type { SelectionClasse } from '../hooks/useSelectionClasse'

/** Les deux listes « année » et « classe », pilotées par useSelectionClasse. */
export default function SelecteursClasse({ selection }: { selection: SelectionClasse }) {
  return (
    <>
      <Group>
        <Select
          label="Année scolaire"
          data={selection.optionsAnnees}
          value={selection.anneeId}
          onChange={selection.changerAnnee}
          allowDeselect={false}
          w={200}
        />
        <Select
          label="Classe"
          placeholder="Choisir une classe"
          data={selection.optionsClasses}
          value={selection.classeChoisie}
          onChange={selection.setClasseChoisie}
          w={280}
        />
      </Group>
      {selection.classes.isError && (
        <Alert color="red" variant="light">
          {selection.classes.error.message}
        </Alert>
      )}
      {selection.classes.data && selection.classes.data.length === 0 && (
        <Text c="dimmed">Aucune classe pour cette année scolaire.</Text>
      )}
    </>
  )
}
