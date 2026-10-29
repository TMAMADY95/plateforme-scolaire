import { useQuery } from '@tanstack/react-query'
import { messagesNonLus } from '../api/endpoints'

/** Nombre de conversations non lues, relu toutes les minutes. */
export function useMessagesNonLus(actif: boolean) {
  return useQuery({
    queryKey: ['messagerie', 'non-lus'],
    queryFn: messagesNonLus,
    enabled: actif,
    refetchInterval: 60_000,
    select: (r) => r.nonLus,
  })
}
