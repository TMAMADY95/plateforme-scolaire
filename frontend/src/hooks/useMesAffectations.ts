import { useQuery } from '@tanstack/react-query'
import { mesAffectations } from '../api/endpoints'

/** Classes et matières de l'enseignant connecté. */
export function useMesAffectations() {
  return useQuery({ queryKey: ['mes-affectations'], queryFn: mesAffectations })
}
