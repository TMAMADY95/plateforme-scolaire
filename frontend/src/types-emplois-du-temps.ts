// Types à AJOUTER à frontend/src/api/types.ts (Étape 2 — emplois du temps).

export interface Salle {
  id: number
  etablissementId: number
  nom: string
  capacite: number | null
  typeSalle: string | null
}

export interface SalleRequete {
  nom: string
  capacite?: number
  typeSalle?: string
}

export interface Creneau {
  id: number
  classeId: number
  classeNom: string
  matiereId: number
  matiereLibelle: string
  enseignantId: number | null
  enseignantNom: string | null
  salleId: number | null
  salleNom: string | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
}

export interface CreneauRequete {
  matiereId: number
  enseignantId?: number | null
  salleId?: number | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
}

export interface CreneauEleve {
  matiereLibelle: string
  enseignantNom: string | null
  salleNom: string | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
}
