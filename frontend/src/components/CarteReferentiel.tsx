import type { ReactNode } from 'react'
import { Badge, Card, Group, Text, Title } from '@mantine/core'

/** Bloc d'un référentiel : titre, phrase d'explication, nombre d'éléments, puis le contenu. */
export default function CarteReferentiel({
  titre,
  description,
  nombre,
  children,
}: {
  titre: string
  description: string
  nombre?: number
  children: ReactNode
}) {
  return (
    <Card withBorder padding="lg">
      <Group justify="space-between" align="flex-start" mb="md" wrap="nowrap">
        <div>
          <Title order={4}>{titre}</Title>
          <Text size="sm" c="dimmed">
            {description}
          </Text>
        </div>
        {nombre !== undefined && (
          <Badge variant="light" size="lg" circle>
            {nombre}
          </Badge>
        )}
      </Group>
      {children}
    </Card>
  )
}
