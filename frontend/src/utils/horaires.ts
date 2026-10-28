/** Libellés et helpers d'affichage des créneaux (jours, heures). */

export const JOURS: { value: number; label: string; court: string }[] = [
  { value: 1, label: 'Lundi', court: 'Lun' },
  { value: 2, label: 'Mardi', court: 'Mar' },
  { value: 3, label: 'Mercredi', court: 'Mer' },
  { value: 4, label: 'Jeudi', court: 'Jeu' },
  { value: 5, label: 'Vendredi', court: 'Ven' },
  { value: 6, label: 'Samedi', court: 'Sam' },
  { value: 7, label: 'Dimanche', court: 'Dim' },
]

export function libelleJour(jourSemaine: number): string {
  return JOURS.find((j) => j.value === jourSemaine)?.label ?? `Jour ${jourSemaine}`
}

/** "08:00:00" -> "08:00" */
export function formaterHeure(heure: string | null | undefined): string {
  if (!heure) return ''
  return heure.slice(0, 5)
}

export function formaterPlage(debut: string, fin: string): string {
  return `${formaterHeure(debut)} – ${formaterHeure(fin)}`
}
