import type { ReactNode } from 'react'
import { Card, Group, Text, ThemeIcon } from '@mantine/core'
import Icone from './Icone'
import type { NomIcone } from './Icone'

interface Props {
  libelle: string
  valeur: ReactNode
  icone: NomIcone
  couleur?: string
  /** Précision sous le chiffre (ex. « sur 12 factures »). */
  detail?: string
}

/** Carte de chiffre clé : pastille d'icône, grand nombre, légende. */
export default function CarteChiffre({ libelle, valeur, icone, couleur = 'marine', detail }: Props) {
  return (
    <Card withBorder padding="md">
      <Group wrap="nowrap" align="flex-start">
        <ThemeIcon size={44} radius="md" variant="light" color={couleur}>
          <Icone nom={icone} taille={24} />
        </ThemeIcon>
        <div>
          <Text size="sm" c="dimmed">
            {libelle}
          </Text>
          <Text fw={700} fz={26} lh={1.2}>
            {valeur}
          </Text>
          {detail && (
            <Text size="xs" c="dimmed">
              {detail}
            </Text>
          )}
        </div>
      </Group>
    </Card>
  )
}
