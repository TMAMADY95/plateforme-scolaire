import { api } from './client'

/**
 * Types de l'identité nationale (INE et état civil).
 * À replacer dans api/types.ts quand la branche sera validée : ils sont isolés ici
 * pour ne modifier aucun fichier existant tant que la compilation n'a pas tourné.
 */
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

/** Un INE est exploitable s'il commence par GN suivi de 12 chiffres. La clé est vérifiée par le serveur. */
export function inePlausible(saisie: string): boolean {
  return /^GN\d{12}$/.test(saisie.toUpperCase().replace(/[\s-]/g, ''))
}
