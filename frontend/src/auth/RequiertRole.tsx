import type { ReactNode } from 'react'
import { Alert } from '@mantine/core'
import type { Moi } from '../api/types'
import { useUtilisateur } from './useAuth'

interface Props {
  autorise: (utilisateur: Moi) => boolean
  children: ReactNode
}

/** Affiche la page seulement si le rôle de l'utilisateur le permet (le serveur reste l'autorité). */
export default function RequiertRole({ autorise, children }: Props) {
  const utilisateur = useUtilisateur()
  if (!autorise(utilisateur)) {
    return (
      <Alert color="orange" variant="light" title="Accès refusé">
        Cette page n'est pas accessible avec votre rôle.
      </Alert>
    )
  }
  return <>{children}</>
}
