/** Formatage des montants. Le montant arrive de l'API sous forme de nombre. */

const DEVISES_SANS_DECIMALES = ['XOF', 'XAF', 'GNF', 'CDF']

export function formaterMontant(montant: number | null | undefined, devise = 'XOF'): string {
  if (montant === null || montant === undefined) return '—'
  const sansDecimales = DEVISES_SANS_DECIMALES.includes(devise)
  const valeur = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: sansDecimales ? 0 : 2,
    maximumFractionDigits: sansDecimales ? 0 : 2,
  }).format(montant)
  return `${valeur} ${devise}`
}
