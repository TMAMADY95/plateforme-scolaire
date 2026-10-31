import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { notifications } from '@mantine/notifications'
import { useQueryClient } from '@tanstack/react-query'
import { api, definirJeton, enregistrerRappels } from '../api/client'
import type { LoginResponse, Moi, VerificationRequise } from '../api/types'
import { AuthContext } from './AuthContext'

// sessionStorage : le jeton disparaît à la fermeture de l'onglet.
const CLE_JETON = 'gestion-scolaire.jeton'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [utilisateur, setUtilisateur] = useState<Moi | null>(null)
  const [chargement, setChargement] = useState<boolean>(() => sessionStorage.getItem(CLE_JETON) !== null)

  const deconnexion = useCallback(() => {
    sessionStorage.removeItem(CLE_JETON)
    definirJeton(null)
    setUtilisateur(null)
    queryClient.clear()
  }, [queryClient])

  // Restauration de la session quand la page est rechargée
  useEffect(() => {
    const jeton = sessionStorage.getItem(CLE_JETON)
    if (jeton === null) return
    definirJeton(jeton)
    api
      .get<Moi>('/api/me')
      .then(setUtilisateur)
      .catch(() => deconnexion())
      .finally(() => setChargement(false))
  }, [deconnexion])

  useEffect(() => {
    enregistrerRappels({
      nonAutorise: () => {
        if (sessionStorage.getItem(CLE_JETON) === null) return // déjà déconnecté
        deconnexion()
        notifications.show({ color: 'orange', title: 'Session expirée', message: 'Veuillez vous reconnecter.' })
      },
      motDePasseRequis: () =>
        setUtilisateur((actuel) => (actuel ? { ...actuel, changementMotDePasseRequis: true } : actuel)),
    })
  }, [deconnexion])

  const connexion = useCallback(async (email: string, motDePasse: string, code?: string) => {
    const reponse = await api.post<LoginResponse | VerificationRequise>(
      '/api/auth/login',
      { email, motDePasse, code: code || undefined },
      { sansJeton: true },
    )
    if ('verificationRequise' in reponse) return null
    definirJeton(reponse.token)
    sessionStorage.setItem(CLE_JETON, reponse.token)
    const moi = await api.get<Moi>('/api/me')
    setUtilisateur(moi)
    return moi
  }, [])

  const valeur = useMemo(
    () => ({ utilisateur, chargement, connexion, deconnexion }),
    [utilisateur, chargement, connexion, deconnexion],
  )

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>
}
