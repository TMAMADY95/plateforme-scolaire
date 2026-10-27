import { Alert, Badge, Button, Card, Group, Loader, Select, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { useMesAffectations } from '../hooks/useMesAffectations'

interface ResumeClasse {
  classeId: number
  nom: string
  niveau: string
  matieres: string[]
}

export default function PageMesClasses() {
  const { anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const affectations = useMesAffectations()

  // Une carte par classe, avec les matières que l'enseignant y assure
  const classes: ResumeClasse[] = []
  for (const a of affectations.data ?? []) {
    if (anneeId === null || a.anneeScolaireId !== Number(anneeId)) continue
    let resume = classes.find((c) => c.classeId === a.classeId)
    if (resume === undefined) {
      resume = { classeId: a.classeId, nom: a.classeNom, niveau: a.niveauLibelle, matieres: [] }
      classes.push(resume)
    }
    resume.matieres.push(a.matiereLibelle)
  }

  return (
    <Stack>
      <Title order={2}>Mes classes</Title>
      <Select
        label="Année scolaire"
        data={options}
        value={anneeId}
        onChange={setAnneeChoisie}
        allowDeselect={false}
        maw={260}
      />

      {affectations.isPending && <Loader />}
      {affectations.isError && (
        <Alert color="red" variant="light">
          {affectations.error.message}
        </Alert>
      )}
      {affectations.data && classes.length === 0 && (
        <Alert color="blue" variant="light">
          Aucune classe ne vous est affectée pour cette année scolaire. La direction de votre établissement peut vous
          affecter à une classe.
        </Alert>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
        {classes.map((classe) => (
          <Card key={classe.classeId} withBorder padding="lg">
            <Text fw={600} size="lg">
              Classe {classe.nom}
            </Text>
            <Text c="dimmed" size="sm" mb="sm">
              {classe.niveau}
            </Text>
            <Group gap="xs" mb="md">
              {classe.matieres.map((matiere) => (
                <Badge key={matiere} variant="light">
                  {matiere}
                </Badge>
              ))}
            </Group>
            <Button component={Link} to={`/mes-classes/${classe.classeId}`} fullWidth variant="light">
              Ouvrir
            </Button>
          </Card>
        ))}
      </SimpleGrid>
    </Stack>
  )
}
