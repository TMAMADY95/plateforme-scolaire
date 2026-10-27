function versIso(date: Date): string {
  const mois = String(date.getMonth() + 1).padStart(2, '0')
  const jour = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${mois}-${jour}`
}

/** "2026-10-26" -> "26/10/2026" (sans passer par Date : pas de décalage de fuseau horaire). */
export function formaterDate(iso: string | null): string {
  if (iso === null) return '—'
  return iso.split('-').reverse().join('/')
}

/** Date du jour au format "AAAA-MM-JJ", dans le fuseau local. */
export function aujourdhui(): string {
  return versIso(new Date())
}

/** Date d'il y a n jours au format "AAAA-MM-JJ", dans le fuseau local. */
export function ilYaJours(n: number): string {
  const date = new Date()
  date.setDate(date.getDate() - n)
  return versIso(date)
}
