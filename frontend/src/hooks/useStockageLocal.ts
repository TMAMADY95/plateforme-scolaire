import { useCallback, useState } from 'react'

/** Valeur mémorisée dans le navigateur (par appareil) ; sans stockage disponible, elle vit le temps de la page. */
export function useStockageLocal<T>(cle: string, defaut: T): [T, (valeur: T) => void] {
  const [valeur, setValeur] = useState<T>(() => {
    try {
      const brut = localStorage.getItem(cle)
      return brut === null ? defaut : (JSON.parse(brut) as T)
    } catch {
      return defaut
    }
  })
  const definir = useCallback(
    (nouvelle: T) => {
      setValeur(nouvelle)
      try {
        localStorage.setItem(cle, JSON.stringify(nouvelle))
      } catch {
        // stockage indisponible ou plein : tant pis
      }
    },
    [cle],
  )
  return [valeur, definir]
}
