import type { Moi, Role } from '../api/types'

export const LIBELLES_ROLES: Record<Role, string> = {
  ADMIN_TECHNIQUE: 'Administrateur technique',
  MINISTERE: 'Ministère',
  DGECS: 'DGECS (examens et concours)',
  DIRECTION_REGIONALE: 'Inspection régionale (IRE)',
  DIRECTION_LOCALE: 'Direction préfectorale / communale',
  CHEF_CENTRE: "Chef de centre d'examen",
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

/** Direction d'un établissement (le personnel administratif n'administre pas les comptes). */
export function estDirectionEtablissement(utilisateur: Moi): boolean {
  return utilisateur.affectations.some(
    (a) => a.role === 'DIRECTION_ETABLISSEMENT' && a.perimetre === 'ETABLISSEMENT' && a.etablissementId !== null,
  )
}

export function estMinistere(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'MINISTERE')
}

export function estDirectionRegionale(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'DIRECTION_REGIONALE')
}

export function estDgecs(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'DGECS')
}

/** DPE ou DCE : un seul rôle, la zone dit s'il s'agit d'une préfecture ou d'une commune. */
export function estDirectionLocale(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'DIRECTION_LOCALE')
}

export function estChefCentre(utilisateur: Moi): boolean {
  return utilisateur.affectations.some((a) => a.role === 'CHEF_CENTRE')
}

/** Ministère ou DGECS : vue nationale du réseau. */
export function estNational(utilisateur: Moi): boolean {
  return estMinistere(utilisateur) || estDgecs(utilisateur)
}

/** IRE ou DPE/DCE : direction rattachée à une zone. */
export function estDirectionDeZone(utilisateur: Moi): boolean {
  return estDirectionRegionale(utilisateur) || estDirectionLocale(utilisateur)
}

export interface DroitsExamens {
  /** Le ministère consulte tout le circuit d'examen. */
  ministere: boolean
  /** DGECS : pilotage technique national. */
  dgecs: boolean
  /** Fixer le calendrier : le ministère, ou l'organisateur d'un examen non national (IRE, DPE/DCE). */
  calendrier: boolean
  /** Actions techniques : DGECS, IRE, DPE/DCE. */
  gestion: boolean
  /** Actions de terrain : gestion plus chef de centre. */
  terrain: boolean
}

/** Qui peut quoi sur le circuit des examens : la DGECS organise tout ; le ministère suit, consulte et valide (proclamation), sans rien saisir. */
export function droitsExamens(utilisateur: Moi): DroitsExamens {
  const dgecs = estDgecs(utilisateur)
  const ministere = estMinistere(utilisateur)
  const zone = estDirectionDeZone(utilisateur)
  return {
    ministere,
    dgecs,
    calendrier: dgecs || zone,
    gestion: dgecs || zone,
    terrain: dgecs || zone || estChefCentre(utilisateur),
  }
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
