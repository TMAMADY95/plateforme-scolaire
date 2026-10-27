import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listerClasses } from '../api/endpoints'
import { useAnneeScolaire } from './useAnneeScolaire'

/** Sélection « année scolaire puis classe » d'un établissement. */
export function useSelectionClasse(etablissementId: number | undefined) {
  const { anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const [classeChoisie, setClasseChoisie] = useState<string | null>(null)

  const classes = useQuery({
    queryKey: ['classes', etablissementId, anneeId],
    queryFn: () => listerClasses(etablissementId as number, Number(anneeId)),
    enabled: etablissementId !== undefined && anneeId !== null,
  })

  const classe = classes.data?.find((c) => String(c.id) === classeChoisie)
  const optionsClasses = (classes.data ?? []).map((c) => ({ value: String(c.id), label: `${c.nom} — ${c.niveauLibelle}` }))

  function changerAnnee(valeur: string | null) {
    setAnneeChoisie(valeur)
    setClasseChoisie(null)
  }

  return { optionsAnnees: options, anneeId, changerAnnee, classes, classe, classeChoisie, setClasseChoisie, optionsClasses }
}

export type SelectionClasse = ReturnType<typeof useSelectionClasse>
