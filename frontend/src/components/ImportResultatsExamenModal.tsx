import { useQuery, useQueryClient } from '@tanstack/react-query'
import { listerEtablissements, saisirResultatExamen } from '../api/endpoints'
import ImportCsvModal from './ImportCsvModal'
import { champ } from '../utils/lireCsv'

interface Resultat {
  etablissementId: number
  code: string
  presentes: number
  admis: number
}

/** Saisit en masse les résultats d'un examen : code de l'établissement, présentés, admis. */
export default function ImportResultatsExamenModal({
  examenId,
  libelleExamen,
  onFermer,
}: {
  examenId: number
  libelleExamen: string
  onFermer: () => void
}) {
  const queryClient = useQueryClient()
  const etablissements = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })
  const parCode = new Map((etablissements.data ?? []).map((e) => [e.codeOfficiel.toLowerCase(), e.id]))

  const analyser = (l: Record<string, string>): Resultat | string => {
    const code = champ(l, 'code', 'codeofficiel')
    if (code === '') return 'code manquant'
    const id = parCode.get(code.toLowerCase())
    if (id === undefined) return `établissement inconnu ou hors de votre périmètre : « ${code} »`
    const presentes = Number(champ(l, 'presentes', 'present', 'candidats').replace(/\s/g, ''))
    const admis = Number(champ(l, 'admis', 'recus').replace(/\s/g, ''))
    if (!Number.isInteger(presentes) || presentes < 0 || champ(l, 'presentes', 'present', 'candidats') === '') return 'présentés invalide'
    if (!Number.isInteger(admis) || admis < 0 || champ(l, 'admis', 'recus') === '') return 'admis invalide'
    if (admis > presentes) return 'les admis dépassent les présentés'
    return { etablissementId: id, code, presentes, admis }
  }

  return (
    <ImportCsvModal<Resultat>
      titre={`Importer les résultats : ${libelleExamen}`}
      description="Une ligne par établissement. Un résultat déjà saisi est remplacé."
      colonnes={['Code', 'Présentés', 'Admis']}
      exemple={['GN-KIN-0001', '120', '96']}
      nomModele="modele-import-resultats-examen"
      analyser={etablissements.isSuccess ? analyser : () => 'établissements pas encore chargés, réessayez'}
      importer={(r) => saisirResultatExamen(examenId, r.etablissementId, { presentes: r.presentes, admis: r.admis })}
      libelle={(r) => r.code}
      onTermine={() => void queryClient.invalidateQueries({ queryKey: ['rapport-examen'] })}
      onFermer={onFermer}
    />
  )
}
