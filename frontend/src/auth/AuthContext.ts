import { createContext } from 'react'
import type { Moi } from '../api/types'

export interface AuthContextValeur {
  /** Null tant que personne n'est connecté. */
  utilisateur: Moi | null
  /** Vrai pendant la restauration de la session au chargement de la page. */
  chargement: boolean
  connexion: (email: string, motDePasse: string) => Promise<Moi>
  deconnexion: () => void
}

export const AuthContext = createContext<AuthContextValeur | null>(null)
