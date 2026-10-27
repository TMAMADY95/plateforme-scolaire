import { Alert, Anchor, Badge, Group, Loader, Stack, Tabs, Text, Title } from '@mantine/core'
import { Link, useParams } from 'react-router'
import { useMesAffectations } from '../hooks/useMesAffectations'
import ResultatsClasse from '../components/ResultatsClasse'
import OngletAppel from './classe/OngletAppel'
import OngletEleves from './classe/OngletEleves'
import OngletEvaluations from './classe/OngletEvaluations'

export default function PageClasseEnseignant() {
  const { classeId } = useParams()
  const id = Number(classeId)
  const affectations = useMesAffectations()

  if (affectations.isPending) return <Loader />
  if (affectations.isError) {
    return (
      <Alert color="red" variant="light">
        {affectations.error.message}
      </Alert>
    )
  }

  const deLaClasse = affectations.data.filter((a) => a.classeId === id)
  if (deLaClasse.length === 0) {
    return (
      <Stack>
        <Alert color="orange" variant="light">
          Cette classe n'existe pas ou ne fait pas partie de vos classes.
        </Alert>
        <Anchor component={Link} to="/mes-classes">
          Retour à mes classes
        </Anchor>
      </Stack>
    )
  }

  const { classeNom, niveauLibelle, anneeLibelle, anneeScolaireId } = deLaClasse[0]
  const mesMatieres = deLaClasse.map((a) => ({ id: a.matiereId, libelle: a.matiereLibelle }))

  return (
    <Stack>
      <Anchor component={Link} to="/mes-classes" size="sm">
        ← Mes classes
      </Anchor>
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={2}>Classe {classeNom}</Title>
          <Text c="dimmed" size="sm">
            {niveauLibelle} — {anneeLibelle}
          </Text>
        </div>
        <Group gap="xs">
          {mesMatieres.map((m) => (
            <Badge key={m.id} variant="light">
              {m.libelle}
            </Badge>
          ))}
        </Group>
      </Group>

      <Tabs defaultValue="appel" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="appel">Appel</Tabs.Tab>
          <Tabs.Tab value="evaluations">Évaluations et notes</Tabs.Tab>
          <Tabs.Tab value="resultats">Résultats</Tabs.Tab>
          <Tabs.Tab value="eleves">Élèves</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="appel" pt="md">
          <OngletAppel classeId={id} />
        </Tabs.Panel>
        <Tabs.Panel value="evaluations" pt="md">
          <OngletEvaluations classeId={id} anneeScolaireId={anneeScolaireId} mesMatieres={mesMatieres} />
        </Tabs.Panel>
        <Tabs.Panel value="resultats" pt="md">
          <ResultatsClasse classeId={id} anneeScolaireId={anneeScolaireId} />
        </Tabs.Panel>
        <Tabs.Panel value="eleves" pt="md">
          <OngletEleves classeId={id} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
