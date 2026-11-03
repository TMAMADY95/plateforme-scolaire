import { api } from './client'
import type { Eleve, EleveRequete, IdentiteNationale } from './types'

export const modifierEleve = (eleveId: number, requete: EleveRequete) =>
  api.put<Eleve>(`/api/eleves/${eleveId}`, requete)

// ---------- Identifiant national de l'élève (INE) et état civil ----------

/** Retrouver un élève par son INE (transfert, inscription dans un nouvel établissement). */
export const eleveParIne = (ine: string) =>
  api.get<IdentiteNationale>(`/api/eleves/ine/${encodeURIComponent(ine)}`)

export const identiteNationale = (eleveId: number) =>
  api.get<IdentiteNationale>(`/api/eleves/${eleveId}/identite-nationale`)

export const modifierEtatCivil = (eleveId: number, requete: EtatCivilRequete) =>
  api.put<IdentiteNationale>(`/api/eleves/${eleveId}/etat-civil`, requete)

/** Élèves de l'établissement sans acte de naissance : à régulariser avant les examens (CEE, BEPC, BAC). */
export const elevesSansActe = (etablissementId: number) =>
  api.get<IdentiteNationale[]>(`/api/eleves/sans-acte?etablissementId=${etablissementId}`)
