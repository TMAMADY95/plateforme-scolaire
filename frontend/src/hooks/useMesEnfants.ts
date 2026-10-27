import { useQuery } from '@tanstack/react-query'
import { mesEnfants } from '../api/endpoints'

/** Enfants rattachés au parent connecté. */
export function useMesEnfants() {
  return useQuery({ queryKey: ['mes-enfants'], queryFn: mesEnfants })
}
