import { api } from './client'

/** Écrit par le service Élèves ; dupliqué ici car le lot 6 tient ses routes dans un fichier séparé. */
export interface IdentiteNationale {
  id: number
  ine: string
  matricule: string
  nom: string
  prenom: string
  dateNaissance: string
  lieuNaissance: string | null
  typeActeNaissance: string | null
  numeroActeNaissance: string | null
  /** Vrai quand aucune pièce d'état civil n'est renseignée : à régulariser avant les examens. */
  sansActe: boolean
}

export interface EtatCivilRequete {
  typeActeNaissance: 'EXTRAIT' | 'JUGEMENT_SUPPLETIF' | 'AUCUN'
  numeroActeNaissance?: string | null
}

export const ACTES_NAISSANCE = [
  { value: 'EXTRAIT', label: "Extrait d'acte de naissance" },
  { value: 'JUGEMENT_SUPPLETIF', label: 'Jugement supplétif' },
  { value: 'AUCUN', label: 'Aucun acte (à régulariser)' },
]

/** Libellé lisible d'un type d'acte ; null quand rien n'est renseigné. */
export function libelleActe(type: string | null): string {
  return ACTES_NAISSANCE.find((a) => a.value === type)?.label ?? 'Non renseigné'
}
