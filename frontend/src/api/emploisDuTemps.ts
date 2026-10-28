import { api } from './client'
import type { Creneau, CreneauEleve, CreneauRequete, Salle, SalleRequete } from './types'

// ---------- Salles ----------
// Le client HTTP n'expose que get / post / put : la suppression passe par un POST.

export const listerSalles = (etablissementId: number) =>
  api.get<Salle[]>(`/api/etablissements/${etablissementId}/salles`)

export const creerSalle = (etablissementId: number, requete: SalleRequete) =>
  api.post<Salle>(`/api/etablissements/${etablissementId}/salles`, requete)

export const supprimerSalle = (etablissementId: number, salleId: number) =>
  api.post<void>(`/api/etablissements/${etablissementId}/salles/${salleId}/suppression`)

// ---------- Créneaux ----------

export const listerCreneauxClasse = (classeId: number) =>
  api.get<Creneau[]>(`/api/classes/${classeId}/creneaux`)

export const listerCreneauxEtablissement = (etablissementId: number, anneeScolaireId: number) =>
  api.get<Creneau[]>(`/api/etablissements/${etablissementId}/creneaux?anneeScolaireId=${anneeScolaireId}`)

export const creerCreneau = (classeId: number, requete: CreneauRequete) =>
  api.post<Creneau>(`/api/classes/${classeId}/creneaux`, requete)

export const modifierCreneau = (creneauId: number, requete: CreneauRequete) =>
  api.put<Creneau>(`/api/creneaux/${creneauId}`, requete)

export const supprimerCreneau = (creneauId: number) =>
  api.post<void>(`/api/creneaux/${creneauId}/suppression`)

// ---------- Consultation (élève / parent) ----------

/** Les créneaux d'une classe, vus par l'élève ou le parent (même endpoint, DTO allégé). */
export const emploiDuTempsClasse = (classeId: number) =>
  api.get<CreneauEleve[]>(`/api/classes/${classeId}/emploi-du-temps`)
