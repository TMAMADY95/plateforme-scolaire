/** Les mêmes candidats composent-ils les deux épreuves ? Oui si l'une est commune à toutes les séries ou si les séries sont égales. */
export function memesCandidats(serieA: string | null, serieB: string | null): boolean {
  return serieA === null || serieB === null || serieA === serieB
}

interface Creneau {
  date: string
  heureDebut: string
  heureFin: string
  serie: string | null
}

/** Épreuve déjà prévue qui se déroule en même temps pour les mêmes candidats ; undefined sinon. Des créneaux qui se touchent ne se chevauchent pas. */
export function epreuveEnConflit<T extends Creneau>(existantes: T[], nouvelle: Creneau): T | undefined {
  const debut = nouvelle.heureDebut.slice(0, 5)
  const fin = nouvelle.heureFin.slice(0, 5)
  return existantes.find(
    (e) => e.date === nouvelle.date && memesCandidats(e.serie, nouvelle.serie) && debut < e.heureFin.slice(0, 5) && e.heureDebut.slice(0, 5) < fin,
  )
}
