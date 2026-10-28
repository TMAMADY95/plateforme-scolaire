import { useQuery } from '@tanstack/react-query'
import { monProfilEleve } from '../api/endpoints'

/** Dossier élève du compte connecté. */
export function useMonProfilEleve() {
  return useQuery({ queryKey: ['mon-profil-eleve'], queryFn: monProfilEleve })
}
