import { Group, Text } from '@mantine/core'
import { NOM_APPLICATION } from '../theme'

/** Mortier stylisé + nom de l'application. */
export default function Logo({ couleur = '#fff', taille = 22 }: { couleur?: string; taille?: number }) {
  return (
    <Group gap={8} wrap="nowrap">
      <svg width={taille + 6} height={taille + 6} viewBox="0 0 32 32" fill="none" aria-hidden="true">
        <path d="M16 5 2 12l14 7 14-7z" fill={couleur} />
        <path d="M8 16v5c0 2 3.6 4 8 4s8-2 8-4v-5l-8 4z" fill={couleur} opacity="0.75" />
        <path d="M28 12v8" stroke={couleur} strokeWidth="2" strokeLinecap="round" />
      </svg>
      <Text fw={700} fz={taille} c={couleur} lh={1}>
        {NOM_APPLICATION}
      </Text>
    </Group>
  )
}
