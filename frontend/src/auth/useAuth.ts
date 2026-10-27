import { useContext } from 'react'
import type { Moi } from '../api/types'
import { AuthContext, type AuthContextValeur } from './AuthContext'

export function useAuth(): AuthContextValeur {
  const contexte = useContext(AuthContext)
  if (contexte === null) throw new Error('useAuth doit être utilisé dans un AuthProvider')
  return contexte
}

/** Pour les pages protégées : l'utilisateur est forcément connecté. */
export function useUtilisateur(): Moi {
  const { utilisateur } = useAuth()
  if (utilisateur === null) throw new Error("Cette page nécessite d'être connecté")
  return utilisateur
}
