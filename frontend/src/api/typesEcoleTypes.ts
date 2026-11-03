import type { Eleve } from './types'

/**
 * Champ `ine` ajouté au type Eleve par le lot INE.
 * À replacer dans api/types.ts quand la branche sera fusionnée : il est isolé ici
 * pour ne pas modifier un fichier central tant que la compilation n'a pas tourné.
 */
export interface EleveAvecIne extends Eleve {
  ine?: string | null
}
