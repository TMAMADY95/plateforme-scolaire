import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { notifications } from '@mantine/notifications'
import { useQueryClient } from '@tanstack/react-query'
import { api, enregistrerRappels } from '../api/client'
import type { Moi, VerificationRequise } from '../api/types'
import { AuthContext } from './AuthContext'

// Le jeton vit dans un cookie HttpOnly, invisible ici : le serveur est seul à savoir si une session est ouverte.
const ANCIENNE_CLE_JETON = 'gestion-scolaire.jeton'

/**
 * Le cookie est commun à tous les onglets. Quand la session change dans l'un d'eux, les autres se rechargent :
 * sinon, ils continueraient d'afficher le compte précédent tout en agissant avec le nouveau.
 * Un seul objet par onglet, pour émettre et écouter : un canal ne reçoit jamais ses propres messages.
 */
const canalSession = 'BroadcastChannel' in window ? new BroadcastChannel('gestion-scolaire.session') : null

function annoncerChangementDeSession() {
  canalSession?.postMessage('changement')
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [utilisateur, setUtilisateur] = useState<Moi | null>(null)
  const [chargement, setChargement] = useState(true)
  const sessionOuverte = useRef(false)

  const deconnexion = useCallback(() => {
    sessionOuverte.current = false
    setUtilisateur(null)
    queryClient.clear()
    // Seul le serveur peut effacer le cookie. Les autres onglets sont prévenus après, pour ne pas retrouver l'ancienne session.
    api
      .post<void>('/api/auth/deconnexion', undefined, { appelPublic: true })
      .then(annoncerChangementDeSession)
      .catch(() =>
        notifications.show({
          color: 'red',
          title: 'Déconnexion non confirmée',
          message: 'Le serveur est injoignable : fermez le navigateur pour terminer la session.',
        }),
      )
  }, [queryClient])

  // Restauration de la session quand la page est rechargée
  useEffect(() => {
    sessionStorage.removeItem(ANCIENNE_CLE_JETON)
    api
      .get<Moi>('/api/me')
      .then((moi) => {
        sessionOuverte.current = true
        setUtilisateur(moi)
      })
      .catch(() => undefined) // personne n'est connecté
      .finally(() => setChargement(false))
  }, [])

  useEffect(() => {
    if (canalSession === null) return
    canalSession.onmessage = () => window.location.reload()
    return () => {
      canalSession.onmessage = null
    }
  }, [])

  useEffect(() => {
    enregistrerRappels({
      nonAutorise: () => {
        if (!sessionOuverte.current) return // déjà déconnecté, ou page ouverte sans session
        deconnexion()
        notifications.show({ color: 'orange', title: 'Session expirée', message: 'Veuillez vous reconnecter.' })
      },
      motDePasseRequis: () =>
        setUtilisateur((actuel) => (actuel ? { ...actuel, changementMotDePasseRequis: true } : actuel)),
    })
  }, [deconnexion])

  const connexion = useCallback(async (email: string, motDePasse: string, code?: string) => {
    const reponse = await api.post<VerificationRequise | undefined>(
      '/api/auth/connexion-web',
      { email, motDePasse, code: code || undefined },
      { appelPublic: true },
    )
    if (reponse !== undefined) return null // un code reçu par e-mail doit encore être saisi
    const moi = await api.get<Moi>('/api/me')
    sessionOuverte.current = true
    setUtilisateur(moi)
    annoncerChangementDeSession()
    return moi
  }, [])

  const valeur = useMemo(
    () => ({ utilisateur, chargement, connexion, deconnexion }),
    [utilisateur, chargement, connexion, deconnexion],
  )

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>
}
