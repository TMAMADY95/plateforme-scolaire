import { Avatar, Badge, Group, Stack, Text, Title } from '@mantine/core'

interface Props {
  prenom: string
  nom: string
  /** Ex. « Classe 6e A — Collège du Centre (2026-2027) ». */
  sousTitre: string
  matricule?: string
  badge?: string
}

/** En-tête d'un élève : avatar à initiales, nom, classe, matricule. */
export default function EnteteEleve({ prenom, nom, sousTitre, matricule, badge }: Props) {
  return (
    <Group wrap="nowrap" gap="md">
      <Avatar size={64} radius="xl" color="marine">
        {(prenom.charAt(0) + nom.charAt(0)).toUpperCase()}
      </Avatar>
      <Stack gap={2}>
        <Group gap="xs">
          <Title order={2}>
            {prenom} {nom}
          </Title>
          {badge && <Badge variant="light">{badge}</Badge>}
        </Group>
        <Text c="dimmed" size="sm">
          {sousTitre}
        </Text>
        {matricule && (
          <Text c="dimmed" size="xs">
            Matricule {matricule}
          </Text>
        )}
      </Stack>
    </Group>
  )
}
