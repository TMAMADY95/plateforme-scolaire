import { Alert, Anchor, Group, Loader, Stack, Tabs, Text, Title } from '@mantine/core'
import { Link, useParams } from 'react-router'
import { useMesEnfants } from '../hooks/useMesEnfants'
import OngletBulletin from './enfant/OngletBulletin'
import OngletPresences from './enfant/OngletPresences'

export default function PageEnfant() {
  const { eleveId } = useParams()
  const id = Number(eleveId)
  const enfants = useMesEnfants()

  if (enfants.isPending) return <Loader />
  if (enfants.isError) {
    return (
      <Alert color="red" variant="light">
        {enfants.error.message}
      </Alert>
    )
  }

  const enfant = enfants.data.find((e) => e.eleveId === id)
  if (enfant === undefined) {
    return (
      <Stack>
        <Alert color="orange" variant="light">
          Cet élève n'est pas rattaché à votre compte.
        </Alert>
        <Anchor component={Link} to="/mes-enfants">
          Retour à mes enfants
        </Anchor>
      </Stack>
    )
  }

  return (
    <Stack>
      <Anchor component={Link} to="/mes-enfants" size="sm">
        ← Mes enfants
      </Anchor>
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={2}>
            {enfant.prenom} {enfant.nom}
          </Title>
          <Text c="dimmed" size="sm">
            {enfant.classeNom !== null
              ? `Classe ${enfant.classeNom} — ${enfant.etablissementNom} (${enfant.anneeLibelle})`
              : "Pas d'inscription active"}
          </Text>
        </div>
      </Group>

      <Tabs defaultValue="bulletin" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="bulletin">Bulletin</Tabs.Tab>
          <Tabs.Tab value="presences">Présences</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="bulletin" pt="md">
          <OngletBulletin enfant={enfant} />
        </Tabs.Panel>
        <Tabs.Panel value="presences" pt="md">
          <OngletPresences eleveId={enfant.eleveId} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
