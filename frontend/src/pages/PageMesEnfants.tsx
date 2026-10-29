import { Alert, Avatar, Badge, Button, Card, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'
import { useMesEnfants } from '../hooks/useMesEnfants'
import { formaterDate } from '../utils/dates'

export default function PageMesEnfants() {
  const enfants = useMesEnfants()

  return (
    <Stack>
      <div>
        <Title order={2}>Mes enfants</Title>
        {enfants.data && enfants.data.length > 0 && (
          <Text c="dimmed">
            {enfants.data.length} {enfants.data.length > 1 ? 'enfants rattachés' : 'enfant rattaché'} à votre compte
          </Text>
        )}
      </div>

      {enfants.isPending && <Loader />}
      {enfants.isError && (
        <Alert color="red" variant="light">
          {enfants.error.message}
        </Alert>
      )}
      {enfants.data && enfants.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucun enfant n'est encore rattaché à votre compte. Rapprochez-vous de l'établissement pour qu'il vous
          rattache à votre enfant.
        </Alert>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
        {enfants.data?.map((enfant) => (
          <Card key={enfant.eleveId} withBorder padding="lg">
            <Group wrap="nowrap" align="flex-start" mb="xs">
              <Avatar size={52} radius="xl" color="marine">
                {(enfant.prenom.charAt(0) + enfant.nom.charAt(0)).toUpperCase()}
              </Avatar>
              <div style={{ flex: 1 }}>
                <Group justify="space-between" wrap="nowrap" gap="xs">
                  <Text fw={600} size="lg">
                    {enfant.prenom} {enfant.nom}
                  </Text>
                  <Badge variant="light">{enfant.relation}</Badge>
                </Group>
                <Text c="dimmed" size="sm">
                  Né(e) le {formaterDate(enfant.dateNaissance)} · {enfant.matricule}
                </Text>
              </div>
            </Group>
            <Text size="sm" mt="sm" mb="md">
              {enfant.classeNom !== null
                ? `Classe ${enfant.classeNom} — ${enfant.etablissementNom} (${enfant.anneeLibelle})`
                : "Pas d'inscription active pour le moment"}
            </Text>
            <Button component={Link} to={`/mes-enfants/${enfant.eleveId}`} fullWidth>
              Voir le détail
            </Button>
          </Card>
        ))}
      </SimpleGrid>
    </Stack>
  )
}
