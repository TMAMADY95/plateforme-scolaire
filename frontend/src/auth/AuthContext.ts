import { createContext } from 'react'
import type { Moi } from '../api/types'

export interface AuthContextValeur {
  /** Null tant que personne n'est connecté. */
  utilisateur: Moi | null
  /** Vrai pendant la restauration de la session au chargement de la page. */
  chargement: boolean
  /** Renvoie null quand un code reçu par e-mail doit encore être saisi (rappeler avec [code]). */
  connexion: (email: string, motDePasse: string, code?: string) => Promise<Moi | null>
  deconnexion: () => void
}

export const AuthContext = createContext<AuthContextValeur | null>(null)
