import { Alert, Badge, Group, Loader, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { annuaireStatistique, telechargerAnnuaire } from '../../api/endpoints'
import BoutonTelechargement from '../../components/BoutonTelechargement'

const nombre = (v: number | null) => (v === null ? '—' : String(v).replace('.', ','))

/** Annuaire statistique de l'année, calculé en direct sur le périmètre de l'utilisateur (mêmes rubriques que l'annuaire officiel). */
export default function AnnuaireStatistique({ anneeId }: { anneeId: number }) {
  const annuaire = useQuery({ queryKey: ['annuaire', anneeId], queryFn: () => annuaireStatistique(anneeId) })
  const d = annuaire.data
  return (
    <Stack>
      <Group justify="space-between" align="flex-end">
        <div>
          <Title order={4}>Annuaire statistique</Title>
          <Text c="dimmed" size="sm">
            Calculé en direct à partir des inscriptions et des fiches enseignants. Âge = année de début de l'année scolaire moins année de naissance ;
            redoublant = élève déjà inscrit au même niveau l'année précédente (définitions à confirmer).
          </Text>
        </div>
        <BoutonTelechargement telecharger={() => telechargerAnnuaire(anneeId)}>Exporter (CSV)</BoutonTelechargement>
      </Group>
      {annuaire.isLoading && <Loader />}
      {annuaire.isError && <Alert color="red">Annuaire indisponible.</Alert>}
      {d && (
        <>
          <Group gap="xs">
            <Badge variant="light" size="lg">{d.generalites.etablissements} établissements</Badge>
            <Badge variant="light" size="lg">{d.generalites.classes} classes</Badge>
            <Badge variant="light" size="lg">{d.generalites.eleves} élèves dont {d.generalites.filles} filles</Badge>
            <Badge variant="light" size="lg">{d.generalites.redoublants} redoublants</Badge>
            <Badge variant="light" size="lg">{nombre(d.generalites.elevesParClasse)} élèves par classe</Badge>
            <Badge variant="light" size="lg">{d.enseignants.total} enseignants dont {d.enseignants.femmes} femmes</Badge>
            <Badge variant="light" size="lg">{nombre(d.generalites.elevesParEnseignant)} élèves par enseignant</Badge>
          </Group>
          <Table.ScrollContainer minWidth={600}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Niveau</Table.Th>
                  <Table.Th>Classes</Table.Th>
                  <Table.Th>Élèves</Table.Th>
                  <Table.Th>Filles</Table.Th>
                  <Table.Th>Redoublants</Table.Th>
                  <Table.Th>Élèves / classe</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.niveaux.map((n) => (
                  <Table.Tr key={n.niveauId}>
                    <Table.Td>{n.niveau}</Table.Td>
                    <Table.Td>{n.classes}</Table.Td>
                    <Table.Td>{n.eleves}</Table.Td>
                    <Table.Td>{n.filles}</Table.Td>
                    <Table.Td>{n.redoublants}</Table.Td>
                    <Table.Td>{nombre(n.elevesParClasse)}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
          <Group align="flex-start" grow>
            <Table withTableBorder>
              <Table.Thead>
                <Table.Tr><Table.Th>Âge</Table.Th><Table.Th>Garçons</Table.Th><Table.Th>Filles</Table.Th></Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.ages.map((a) => (
                  <Table.Tr key={a.age}><Table.Td>{a.age} ans</Table.Td><Table.Td>{a.garcons}</Table.Td><Table.Td>{a.filles}</Table.Td></Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            <Table withTableBorder>
              <Table.Thead>
                <Table.Tr><Table.Th>Type d'établissement</Table.Th><Table.Th>Établissements</Table.Th><Table.Th>Élèves</Table.Th></Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.types.map((t) => (
                  <Table.Tr key={t.type}><Table.Td>{t.type}</Table.Td><Table.Td>{t.etablissements}</Table.Td><Table.Td>{t.eleves}</Table.Td></Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Group>
        </>
      )}
    </Stack>
  )
}
