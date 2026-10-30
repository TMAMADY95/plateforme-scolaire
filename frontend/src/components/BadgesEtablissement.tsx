import { Badge, Group } from '@mantine/core'
import type { Cycle, Secteur } from '../api/types'

export const LIBELLES_SECTEUR: Record<Secteur, string> = { PUBLIC: 'Public', PRIVE: 'Privé' }
const COULEURS_SECTEUR: Record<Secteur, string> = { PUBLIC: 'blue', PRIVE: 'grape' }

/** Pastille « Public » ou « Privé ». */
export function BadgeSecteur({ secteur }: { secteur: Secteur }) {
  return (
    <Badge variant="light" color={COULEURS_SECTEUR[secteur] ?? 'gray'}>
      {LIBELLES_SECTEUR[secteur] ?? secteur}
    </Badge>
  )
}

/** Les cycles d'un établissement (primaire, collège, lycée…), un badge chacun. */
export function BadgesCycles({ cycles }: { cycles: Cycle[] }) {
  if (cycles.length === 0) return <>—</>
  return (
    <Group gap={4}>
      {cycles.map((c) => (
        <Badge key={c.id} variant="outline" color="gray" size="sm">
          {c.libelle}
        </Badge>
      ))}
    </Group>
  )
}
