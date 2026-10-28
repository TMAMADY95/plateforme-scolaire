/** Rend lisible l'adresse locale du poste de développement ; les autres adresses restent telles quelles. */
export function formaterAdresse(adresse: string | null): string {
  if (adresse === null) return '—'
  if (adresse === '0:0:0:0:0:0:0:1' || adresse === '::1' || adresse === '127.0.0.1') return 'Ce poste (local)'
  return adresse
}
