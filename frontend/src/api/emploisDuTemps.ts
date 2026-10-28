import { api } from './client'
import type { Creneau, CreneauEleve, CreneauRequete, Salle, SalleRequete } from './types'

// ---------- Salles ----------

export const listerSalles = (etablissementId: number) =>
  api.get<Salle[]>(`/api/etablissements/${etablissementId}/salles`)

export const creerSalle = (etablissementId: number, requete: SalleRequete) =>
  api.post<Salle>(`/api/etablissements/${etablissementId}/salles`, requete)

export const supprimerSalle = (etablissementId: number, salleId: number) =>
  api.delete<void>(`/api/etablissements/${etablissementId}/salles/${salleId}`)

// ---------- Créneaux ----------

export const listerCreneauxClasse = (classeId: number) =>
  api.get<Creneau[]>(`/api/classes/${classeId}/creneaux`)

export const listerCreneauxEtablissement = (etablissementId: number, anneeScolaireId: number) =>
  api.get<Creneau[]>(`/api/etablissements/${etablissementId}/creneaux?anneeScolaireId=${anneeScolaireId}`)

export const creerCreneau = (classeId: number, requete: CreneauRequete) =>
  api.post<Creneau>(`/api/classes/${classeId}/creneaux`, requete)

export const modifierCreneau = (creneauId: number, requete: CreneauRequete) =>
  api.put<Creneau>(`/api/creneaux/${creneauId}`, requete)

export const supprimerCreneau = (creneauId: number) => api.delete<void>(`/api/creneaux/${creneauId}`)

// ---------- Consultation (élève / parent) ----------

export const emploiDuTempsClasse = (classeId: number) =>
  api.get<CreneauEleve[]>(`/api/classes/${classeId}/creneaux`)
