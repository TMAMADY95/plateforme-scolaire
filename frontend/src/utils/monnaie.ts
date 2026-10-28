/**
 * Formatage des montants. Le montant arrive de l'API sous forme de nombre et la devise
 * sous forme de code ISO 4217 (EUR, XOF, MAD...). Le nombre de décimales est celui de la devise.
 */
export function formaterMontant(montant: number | null | undefined, devise?: string | null): string {
  if (montant === null || montant === undefined) return '—'
  if (!devise) {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(montant)
  }
  try {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: devise }).format(montant)
  } catch {
    // Code de devise inconnu du navigateur : on affiche le code tel quel.
    return `${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(montant)} ${devise}`
  }
}
