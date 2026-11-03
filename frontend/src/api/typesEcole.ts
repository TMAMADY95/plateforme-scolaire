import { api } from './client'

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

/** Libellé lisible d'un type d'acte ; « Non renseigné » quand rien n'est saisi. */
export function libelleActe(type: string | null): string {
  return ACTES_NAISSANCE.find((a) => a.value === type)?.label ?? 'Non renseigné'
}

/**
 * Vérifie la forme d'un INE avant l'appel au serveur : GN suivi de 12 chiffres.
 * La clé de contrôle est vérifiée par le serveur, qui reste seul juge.
 */
export function inePlausible(saisie: string): boolean {
  return /^GN\d{12}$/.test(saisie.toUpperCase().replace(/[\s-]/g, ''))
}

// Réexportés pour éviter d'ajouter deux points d'entrée aux écrans existants.
export { api }
