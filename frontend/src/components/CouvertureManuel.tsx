import { Badge } from '@mantine/core'

const ETATS: Record<string, { libelle: string; couleur: string }> = {
  COMPLET: { libelle: 'Complet', couleur: 'green' },
  PARTIEL: { libelle: 'Partiel', couleur: 'yellow' },
  INSUFFISANT: { libelle: 'Insuffisant', couleur: 'red' },
  SANS_ELEVE: { libelle: 'Sans élève', couleur: 'gray' },
}

export const pourcent = (v: number | null) => (v === null ? '—' : `${String(v).replace('.', ',')} %`)

export default function CouvertureManuel({ etat, couverture }: { etat: string; couverture: number | null }) {
  const e = ETATS[etat] ?? ETATS.SANS_ELEVE
  return <Badge variant="light" color={e.couleur}>{e.libelle} · {pourcent(couverture)}</Badge>
}
