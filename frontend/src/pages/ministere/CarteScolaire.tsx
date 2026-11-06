import { useState } from 'react'
import { Alert, Badge, Group, Loader, NumberInput, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { carteScolaire } from '../../api/endpoints'

/** Carte scolaire : salles et enseignants manquants par zone et par établissement, selon des normes saisies ici. */
export default function CarteScolaire({ anneeId }: { anneeId: number }) {
  const [normeSalle, setNormeSalle] = useState<number | string>(50)
  const [normeEnseignant, setNormeEnseignant] = useState<number | string>(40)
  const salle = Number(normeSalle)
  const enseignant = Number(normeEnseignant)
  const valide = salle >= 10 && salle <= 200 && enseignant >= 10 && enseignant <= 200
  const carte = useQuery({ queryKey: ['carte-scolaire', anneeId, salle, enseignant], queryFn: () => carteScolaire(anneeId, salle, enseignant), enabled: valide })
  const d = carte.data
  return (
    <Stack>
      <div>
        <Title order={4}>Carte scolaire : besoins en salles et en enseignants</Title>
        <Text c="dimmed" size="sm">
          Salles nécessaires = élèves ÷ norme, arrondi au supérieur. Les valeurs proposées (50 élèves par salle, 40 par enseignant) sont indicatives :
          remplacez-les par les normes officielles. Les établissements sans fiche d'infrastructure n'ont pas de manque de salles calculé.
        </Text>
      </div>
      <Group>
        <NumberInput label="Élèves par salle" min={10} max={200} allowDecimal={false} value={normeSalle} onChange={setNormeSalle} w={150} />
        <NumberInput label="Élèves par enseignant" min={10} max={200} allowDecimal={false} value={normeEnseignant} onChange={setNormeEnseignant} w={170} />
      </Group>
      {!valide && <Alert color="orange" variant="light">Les normes doivent être comprises entre 10 et 200.</Alert>}
      {carte.isLoading && <Loader />}
      {carte.isError && <Alert color="red">Carte indisponible.</Alert>}
      {d && (
        <>
          <Group gap="xs">
            <Badge variant="light" size="lg" color={d.total.manqueSalles ? 'orange' : 'green'}>{d.total.manqueSalles} salles manquantes</Badge>
            <Badge variant="light" size="lg" color={d.total.manqueEnseignants ? 'orange' : 'green'}>{d.total.manqueEnseignants} enseignants manquants</Badge>
            <Badge variant="light" size="lg" color={d.total.fichesManquantes ? 'yellow' : 'gray'}>{d.total.fichesManquantes} établissements sans fiche d'infrastructure</Badge>
          </Group>
          <Table.ScrollContainer minWidth={640}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Zone</Table.Th>
                  <Table.Th>Établissements</Table.Th>
                  <Table.Th>Élèves</Table.Th>
                  <Table.Th>Salles manquantes</Table.Th>
                  <Table.Th>Enseignants manquants</Table.Th>
                  <Table.Th>Sans fiche</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.zones.map((z) => (
                  <Table.Tr key={z.zone}>
                    <Table.Td>{z.zone}</Table.Td>
                    <Table.Td>{z.etablissements}</Table.Td>
                    <Table.Td>{z.eleves}</Table.Td>
                    <Table.Td>{z.manqueSalles}</Table.Td>
                    <Table.Td>{z.manqueEnseignants}</Table.Td>
                    <Table.Td>{z.fichesManquantes}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
          <Text fw={600} size="sm">Établissements les plus en manque</Text>
          <Table.ScrollContainer minWidth={760}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Établissement</Table.Th>
                  <Table.Th>Zone</Table.Th>
                  <Table.Th>Élèves</Table.Th>
                  <Table.Th>Salles</Table.Th>
                  <Table.Th>Nécessaires</Table.Th>
                  <Table.Th>Manque</Table.Th>
                  <Table.Th>Enseignants</Table.Th>
                  <Table.Th>Nécessaires</Table.Th>
                  <Table.Th>Manque</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.etablissements.slice(0, 30).map((l) => (
                  <Table.Tr key={l.etablissementId}>
                    <Table.Td>{l.etablissement}</Table.Td>
                    <Table.Td>{l.zone}</Table.Td>
                    <Table.Td>{l.eleves}</Table.Td>
                    <Table.Td>{l.salles ?? '—'}</Table.Td>
                    <Table.Td>{l.sallesNecessaires}</Table.Td>
                    <Table.Td>{l.manqueSalles ?? '—'}</Table.Td>
                    <Table.Td>{l.enseignants}</Table.Td>
                    <Table.Td>{l.enseignantsNecessaires}</Table.Td>
                    <Table.Td>{l.manqueEnseignants}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </>
      )}
    </Stack>
  )
}
