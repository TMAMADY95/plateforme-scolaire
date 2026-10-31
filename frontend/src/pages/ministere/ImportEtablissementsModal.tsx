import { useQuery, useQueryClient } from '@tanstack/react-query'
import { creerEtablissement, listerCycles, listerEtablissements, listerZones } from '../../api/endpoints'
import type { EtablissementRequete } from '../../api/types'
import ImportCsvModal from '../../components/ImportCsvModal'
import { champ, cleColonne } from '../../utils/lireCsv'

/** Crée des établissements en masse depuis un fichier CSV : code, nom, secteur, cycles, zone, adresse, capacité. */
export default function ImportEtablissementsModal({ onFermer }: { onFermer: () => void }) {
  const queryClient = useQueryClient()
  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones })
  const cycles = useQuery({ queryKey: ['cycles'], queryFn: listerCycles })
  const existants = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })
  const analyser = (l: Record<string, string>, rang: number, toutes: Record<string, string>[]): EtablissementRequete | string => {
    const code = champ(l, 'code', 'codeofficiel')
    const nom = champ(l, 'nom', 'etablissement')
    if (code === '') return 'code manquant'
    if (nom === '') return 'nom manquant'
    const cleCode = code.toLowerCase()
    if ((existants.data ?? []).some((e) => e.codeOfficiel.toLowerCase() === cleCode)) return `le code « ${code} » existe déjà`
    if (toutes.slice(0, rang).some((x) => champ(x, 'code', 'codeofficiel').toLowerCase() === cleCode))
      return `le code « ${code} » apparaît deux fois dans le fichier`

    const brutSecteur = cleColonne(champ(l, 'secteur'))
    const secteur = brutSecteur.startsWith('pub') ? 'PUBLIC' : brutSecteur.startsWith('pri') ? 'PRIVE' : null
    if (secteur === null) return 'secteur à indiquer : Public ou Privé'

    const noms = champ(l, 'cycles', 'cycle').split(/[|,/+]+/).map((c) => c.trim()).filter((c) => c !== '')
    if (noms.length === 0) return 'au moins un cycle (Primaire, Collège, Lycée)'
    const cycleIds: number[] = []
    for (const n of noms) {
      const trouve = (cycles.data ?? []).find((c) => cleColonne(c.libelle) === cleColonne(n) || cleColonne(c.code) === cleColonne(n))
      if (!trouve) return `cycle inconnu : « ${n} »`
      if (!cycleIds.includes(trouve.id)) cycleIds.push(trouve.id)
    }

    let zoneId: number | null = null
    const zone = champ(l, 'zone', 'region', 'prefecture', 'commune')
    if (zone !== '') {
      const trouvees = (zones.data ?? []).filter((z) => cleColonne(z.nom) === cleColonne(zone))
      if (trouvees.length === 0) return `zone inconnue : « ${zone} »`
      if (trouvees.length > 1) return `zone ambiguë : « ${zone} » existe plusieurs fois`
      zoneId = trouvees[0].id
    }

    const brutCapacite = champ(l, 'capacite')
    const capacite = brutCapacite === '' ? undefined : Number(brutCapacite.replace(/\s/g, ''))
    if (capacite !== undefined && (!Number.isInteger(capacite) || capacite <= 0)) return 'capacité invalide'

    const adresse = champ(l, 'adresse')
    return { codeOfficiel: code, nom, zoneId, secteur, cycleIds, adresse: adresse === '' ? undefined : adresse, capacite }
  }

  return (
    <ImportCsvModal<EtablissementRequete>
      titre="Importer des établissements"
      description="Un établissement par ligne. Secteur : Public ou Privé. Cycles séparés par « | » (ex. Primaire|Collège). La zone doit porter le nom d'une zone existante ; elle peut rester vide."
      colonnes={['Code', 'Nom', 'Secteur', 'Cycles', 'Zone', 'Adresse', 'Capacité']}
      exemple={['GN-KIN-0001', 'École primaire de Kindia centre', 'Public', 'Primaire', 'Kindia', 'Quartier Centre', '600']}
      nomModele="modele-import-etablissements"
      analyser={zones.isSuccess && cycles.isSuccess && existants.isSuccess ? analyser : () => 'référentiels pas encore chargés, réessayez'}
      importer={creerEtablissement}
      libelle={(e) => e.codeOfficiel}
      onTermine={() => {
        void queryClient.invalidateQueries({ queryKey: ['etablissements'] })
        void queryClient.invalidateQueries({ queryKey: ['synthese-nationale'] })
      }}
      onFermer={onFermer}
    />
  )
}
