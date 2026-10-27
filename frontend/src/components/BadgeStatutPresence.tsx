import { Badge } from '@mantine/core'
import type { StatutPresence } from '../api/types'

const PRESENTATION: Record<StatutPresence, { libelle: string; couleur: string }> = {
  PRESENT: { libelle: 'Présent', couleur: 'green' },
  ABSENT: { libelle: 'Absent', couleur: 'red' },
  RETARD: { libelle: 'Retard', couleur: 'orange' },
}

export default function BadgeStatutPresence({ statut }: { statut: StatutPresence }) {
  const { libelle, couleur } = PRESENTATION[statut]
  return <Badge color={couleur}>{libelle}</Badge>
}
