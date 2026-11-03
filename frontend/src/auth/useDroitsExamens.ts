import type { DroitsExamens } from './roles'
import { droitsExamens } from './roles'
import { useUtilisateur } from './useAuth'

/** Droits de l'utilisateur connecté sur le circuit des examens. */
export function useDroitsExamens(): DroitsExamens {
  return droitsExamens(useUtilisateur())
}
