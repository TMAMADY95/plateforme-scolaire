import { Badge } from '@mantine/core'
import type { StatutInscription } from '../api/types'

const PRESENTATION: Record<StatutInscription, { libelle: string; couleur: string }> = {
  EN_ATTENTE: { libelle: 'En attente', couleur: 'yellow' },
  ACCEPTEE: { libelle: 'Acceptée', couleur: 'green' },
  REFUSEE: { libelle: 'Refusée', couleur: 'red' },
  ANNULEE: { libelle: 'Annulée', couleur: 'gray' },
  TRANSFEREE: { libelle: 'Transférée', couleur: 'blue' },
}

export default function BadgeStatutInscription({ statut }: { statut: StatutInscription }) {
  const { libelle, couleur } = PRESENTATION[statut]
  return <Badge color={couleur}>{libelle}</Badge>
}
