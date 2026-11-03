import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eleveParIne, elevesSansActe, identiteNationale, modifierEtatCivil } from './endpointsEcole'
import type { EtatCivilRequete, IdentiteNationale } from './typesEcole'

/** Retrouver un élève par son INE. */
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
  useQuery({
    queryKey: ['eleves-sans-acte', etablissementId],
    queryFn: () => elevesSansActe(etablissementId),
  })

/** Saisie de la pièce d'état civil ; l'identité nationale est rafraîchie après succès. */
export function useModifierEtatCivil(eleveId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (requete: EtatCivilRequete) => modifierEtatCivil(eleveId, requete),
    onSuccess: (identite: IdentiteNationale) => {
      queryClient.setQueryData(['identite-nationale', eleveId], identite)
    },
  })
}
