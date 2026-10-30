import type { ReactNode } from 'react'
import { Group, Text, Title } from '@mantine/core'

/** Titre d'une section d'une page, avec une phrase d'explication et une action à droite. */
export default function EnteteSection({
  titre,
  description,
  action,
}: {
  titre: string
  description?: string
  action?: ReactNode
}) {
  return (
    <Group justify="space-between" align="flex-end" wrap="nowrap">
      <div>
        <Title order={4}>{titre}</Title>
        {description && (
          <Text size="sm" c="dimmed">
            {description}
          </Text>
        )}
      </div>
      {action}
    </Group>
  )
}
