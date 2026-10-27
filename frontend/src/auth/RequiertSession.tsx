import type { ReactNode } from 'react'
import { Center, Loader } from '@mantine/core'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './useAuth'

interface Props {
  children: ReactNode
  /** Si vrai, un compte qui doit encore changer son mot de passe initial est redirigé vers cet écran. */
  motDePasseAJour?: boolean
}

/** Barrière des pages protégées : session obligatoire, et changement de mot de passe si exigé. */
export default function RequiertSession({ children, motDePasseAJour = false }: Props) {
  const { utilisateur, chargement } = useAuth()
  const lieu = useLocation()

  if (chargement) {
    return (
      <Center h="100vh">
        <Loader />
      </Center>
    )
  }
  if (utilisateur === null) {
    return <Navigate to="/connexion" replace state={{ depuis: lieu.pathname }} />
  }
  if (motDePasseAJour && utilisateur.changementMotDePasseRequis) {
    return <Navigate to="/changer-mot-de-passe" replace />
  }
  return <>{children}</>
}
