import { api } from './client'
import type {
  BilanFinancier,
  Facture,
  Frais,
  FraisRequete,
  GenererFacturesRequete,
  ExonerationRequete,
  ResultatGeneration,
} from './types'

// ---------- Grille tarifaire ----------

export const listerFrais = (etablissementId: number, anneeScolaireId?: number) =>
  api.get<Frais[]>(
    `/api/etablissements/${etablissementId}/frais${anneeScolaireId ? `?anneeScolaireId=${anneeScolaireId}` : ''}`,
  )

export const creerFrais = (etablissementId: number, requete: FraisRequete) =>
  api.post<Frais>(`/api/etablissements/${etablissementId}/frais`, requete)

export const modifierFrais = (fraisId: number, requete: FraisRequete) =>
  api.put<Frais>(`/api/frais/${fraisId}`, requete)

export const desactiverFrais = (fraisId: number) => api.post<void>(`/api/frais/${fraisId}/desactivation`)

// ---------- Factures ----------

export const listerFactures = (etablissementId: number, anneeScolaireId: number, statut?: string) => {
  const parametres = new URLSearchParams({ anneeScolaireId: String(anneeScolaireId) })
  if (statut) parametres.set('statut', statut)
  return api.get<Facture[]>(`/api/etablissements/${etablissementId}/factures?${parametres.toString()}`)
}

export const facturesEnRetard = (etablissementId: number) =>
  api.get<Facture[]>(`/api/etablissements/${etablissementId}/factures/en-retard`)

export const bilanFinancier = (etablissementId: number, anneeScolaireId: number) =>
  api.get<BilanFinancier>(`/api/etablissements/${etablissementId}/factures/bilan?anneeScolaireId=${anneeScolaireId}`)

export const genererFactures = (etablissementId: number, requete: GenererFacturesRequete) =>
  api.post<ResultatGeneration>(`/api/etablissements/${etablissementId}/factures/generation`, requete)

export const exonererFacture = (factureId: number, requete: ExonerationRequete) =>
  api.post<Facture>(`/api/factures/${factureId}/exonerations`, requete)

export const annulerFacture = (factureId: number) => api.post<Facture>(`/api/factures/${factureId}/annulation`)

// ---------- Espace élève et parent ----------

export const mesFactures = () => api.get<Facture[]>('/api/me/factures')

export const facturesDeMonEnfant = (eleveId: number) =>
  api.get<Facture[]>(`/api/me/enfants/${eleveId}/factures`)
