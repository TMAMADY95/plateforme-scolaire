/** « NATIONAL » ou un type de zone, écrit pour être lu (National, Préfecture, Région…). */
export const libelleOrganisation = (niveau: string): string =>
  niveau === 'NATIONAL' ? 'National' : niveau.charAt(0).toUpperCase() + niveau.slice(1).toLowerCase()
