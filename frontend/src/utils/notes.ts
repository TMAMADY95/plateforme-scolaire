/** 15.333 -> "15,33" ; null -> "—". */
export function formaterNote(valeur: number | null): string {
  return valeur === null ? '—' : valeur.toFixed(2).replace('.', ',')
}

/** Écart signé entre deux moyennes : "+1,20" ou "-0,50" ; "—" si l'une manque. */
export function formaterEcart(eleve: number | null, classe: number | null): string {
  if (eleve === null || classe === null) return '—'
  const ecart = eleve - classe
  return `${ecart > 0 ? '+' : ''}${ecart.toFixed(2).replace('.', ',')}`
}

/** 1 -> "1er", 2 -> "2e". */
export function formaterRang(rang: number): string {
  return rang === 1 ? '1er' : `${rang}e`
}
