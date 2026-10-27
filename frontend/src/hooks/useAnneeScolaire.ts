import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listerAnnees } from '../api/endpoints'

/** Liste des années scolaires et sélection courante (par défaut : la plus récente). */
export function useAnneeScolaire() {
  const annees = useQuery({ queryKey: ['annees'], queryFn: listerAnnees })
  const [choisie, setAnneeChoisie] = useState<string | null>(null)

  const anneeId = choisie ?? (annees.data && annees.data.length > 0 ? String(annees.data[0].id) : null)
  const options = useMemo(
    () => (annees.data ?? []).map((a) => ({ value: String(a.id), label: a.libelle })),
    [annees.data],
  )

  return { annees, anneeId, setAnneeChoisie, options }
}
