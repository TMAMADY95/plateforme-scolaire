import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eleveParIne, elevesSansActe, identiteNationale, modifierEtatCivil } from '../api/endpoints'
import type { EtatCivilRequete, IdentiteNationale } from '../api/types'

/** Retrouver un eleve par son INE ; la requete ne part qu'avec une saisie non vide. */
export const useEleveParIne = (ine: string | null) =>
  useQuery({
    queryKey: ['eleve-ine', ine],
    queryFn: () => eleveParIne(ine as string),
    enabled: ine !== null && ine.trim() !== '',
    retry: false,
  })

export const useIdentiteNationale = (eleveId: number) =>
  useQuery({ queryKey: ['identite-nationale', eleveId], queryFn: () => identiteNationale(eleveId) })

export const useElevesSansActe = (etablissementId: number) =>
  useQuery({ queryKey: ['eleves-sans-acte', etablissementId], queryFn: () => elevesSansActe(etablissementId) })

/** Saisie de la piece d'etat civil ; le cache de la fiche est mis a jour apres succes. */
export function useModifierEtatCivil(eleveId: number) {
  const queryClient = useQueryClient()
  return useMutation<IdentiteNationale, Error, EtatCivilRequete>({
    mutationFn: (requete) => modifierEtatCivil(eleveId, requete),
    onSuccess: (identite) => {
      queryClient.setQueryData(['identite-nationale', eleveId], identite)
    },
  })
}
