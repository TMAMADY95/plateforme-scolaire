import type { ReactNode } from 'react'
import { Box, Text, UnstyledButton } from '@mantine/core'
import { useSearchParams } from 'react-router'
import Icone from './Icone'
import type { NomIcone } from './Icone'

export interface OngletLateral {
  valeur: string
  libelle: string
  icone: NomIcone
  contenu: ReactNode
}

export interface GroupeOnglets {
  titre: string
  onglets: OngletLateral[]
}

/**
 * Navigation par sections : un menu vertical à gauche, regroupé par thème, avec le contenu de la section choisie à
 * droite. Sur petit écran le menu passe au-dessus, en bandeau défilant. La section ouverte est gardée dans l'adresse
 * (?onglet=…), ce qui permet de recharger la page ou de partager un lien sans perdre sa place.
 */
export default function OngletsLateraux({ groupes }: { groupes: GroupeOnglets[] }) {
  const [parametres, setParametres] = useSearchParams()
  const tous = groupes.flatMap((g) => g.onglets)
  const demande = parametres.get('onglet')
  const actif = tous.find((o) => o.valeur === demande) ?? tous[0]

  const choisir = (valeur: string) => {
    const suivants = new URLSearchParams(parametres)
    suivants.set('onglet', valeur)
    setParametres(suivants, { replace: true })
  }

  return (
    <div className="onglets-lateraux">
      <nav className="onglets-lateraux__menu" aria-label="Sections">
        {groupes.map((g) => (
          <div key={g.titre} className="onglets-lateraux__groupe">
            <Text className="onglets-lateraux__titre" size="xs" fw={700} c="dimmed" tt="uppercase">
              {g.titre}
            </Text>
            {g.onglets.map((o) => (
              <UnstyledButton
                key={o.valeur}
                className="onglets-lateraux__onglet"
                data-actif={o.valeur === actif?.valeur ? 'true' : undefined}
                aria-current={o.valeur === actif?.valeur ? 'page' : undefined}
                onClick={() => choisir(o.valeur)}
              >
                <span className="onglets-lateraux__icone">
                  <Icone nom={o.icone} taille={18} />
                </span>
                <span>{o.libelle}</span>
              </UnstyledButton>
            ))}
          </div>
        ))}
      </nav>
      <Box className="onglets-lateraux__contenu">{actif?.contenu}</Box>
    </div>
  )
}
