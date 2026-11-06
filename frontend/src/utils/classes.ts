import { libelleSerie } from '../api/types'
import type { Classe } from '../api/types'

/** Libellé d'une classe dans les listes : « 11A — 11e année — Sciences mathématiques » (la série n'apparaît qu'au lycée). */
export function libelleClasse(c: Pick<Classe, 'nom' | 'niveauLibelle' | 'serie'>): string {
  const serie = c.serie ? ` — ${libelleSerie(c.serie)}` : ''
  return `${c.nom} — ${c.niveauLibelle}${serie}`
}
