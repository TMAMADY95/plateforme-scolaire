// Types à AJOUTER à frontend/src/api/types.ts (Étape 3a — frais et factures).

export interface Frais {
  id: number
  etablissementId: number
  anneeScolaireId: number
  niveauId: number | null
  niveauLibelle: string | null
  libelle: string
  montant: number
  devise: string
  echeance: string | null
  obligatoire: boolean
  actif: boolean
}

export interface FraisRequete {
  anneeScolaireId: number
  niveauId?: number | null
  libelle: string
  montant: number
  devise?: string
  echeance?: string | null
  obligatoire?: boolean
  actif?: boolean
}

export type StatutFacture = 'IMPAYEE' | 'PARTIELLE' | 'PAYEE' | 'EXONEREE' | 'ANNULEE'

export interface Facture {
  id: number
  fraisId: number
  numero: string
  libelle: string
  eleveId: number
  eleveNom: string
  elevePrenom: string
  matricule: string
  etablissementId: number
  anneeScolaireId: number
  montantDu: number
  montantExonere: number
  montantPaye: number
  resteAPayer: number
  devise: string
  echeance: string | null
  statut: StatutFacture
  dateEmission: string
  enRetard: boolean
}

export interface GenererFacturesRequete {
  anneeScolaireId: number
  classeId?: number | null
  niveauId?: number | null
}

export interface ResultatGeneration {
  facturesCreees: number
  facturesIgnorees: number
  elevesTraites: number
  montantTotalEmis: number
}

export interface ExonerationRequete {
  montant: number
  motif: string
  typeExoneration?: string
}

export interface BilanFinancier {
  etablissementId: number
  anneeScolaireId: number
  devise: string
  totalDu: number
  totalExonere: number
  totalPaye: number
  totalRestant: number
  nombreFactures: number
  nombreImpayees: number
  nombrePayees: number
  nombreEnRetard: number
}
