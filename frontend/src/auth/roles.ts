import type { Moi, Role } from '../api/types'

export const LIBELLES_ROLES: Record<Role, string> = {
  ADMIN_TECHNIQUE: 'Administrateur technique',
  MINISTERE: 'Ministère',
  DIRECTION_REGIONALE: 'Direction régionale',
  DIRECTION_ETABLISSEMENT: "Direction d'établissement",
  PERSONNEL_ADMIN: 'Personnel administratif',
  ENSEIGNANT: 'Enseignant',
  PARENT: 'Parent',
  ELEVE: 'Élève',
}

const ROLES_DE_GESTION: Role[] = ['DIRECTION_ETABLISSEMENT', 'PERSONNEL_ADMIN']

/** Établissements que l'utilisateur gère (direction ou personnel administratif). */
export function etablissementsGeres(utilisateur: Moi): number[] {
  const ids = utilisateur.affectations
    .filter((a) => ROLES_DE_GESTION.includes(a.role) && a.perimetre === 'ETABLISSEMENT' && a.etablissementId !== null)
    .map((a) => a.etablissementId as number)
  return [...new Set(ids)]
}

/** Établissement sur lequel travaille l'utilisateur (le premier, tant qu'il n'y a pas de sélecteur). */
export function etablissementCourant(utilisateur: Moi): number | undefined {
  return etablissementsGeres(utilisateur)[0]
}

export function estGestionnaire(utilisateur: Moi): boolean {
  return etablissementsGeres(utilisateur).length > 0
}

export function estEnseignant(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'ENSEIGNANT')
}

export function estParent(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'PARENT')
}

export function estEleve(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'ELEVE')
}
