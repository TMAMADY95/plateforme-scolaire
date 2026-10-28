import { api } from './client'
import type {
  AffectationEnseignant,
  AffectationRequete,
  AnneeScolaire,
  Classe,
  Bulletin,
  ClasseRequete,
  Creneau,
  CreneauEleve,
  CreneauRequete,
  DecisionRequete,
  Enfant,
  Enseignant,
  EnseignantRequete,
  Etablissement,
  Evaluation,
  EvaluationRequete,
  ExonerationRequete,
  Facture,
  FeuilleAppel,
  Frais,
  FraisRequete,
  GenererFacturesRequete,
  GrilleNotes,
  Inscription,
  InscriptionRequete,
  LignePresenceRequete,
  LigneNoteRequete,
  Matiere,
  MonAffectation,
  MonProfilEleve,
  Niveau,
  Periode,
  PresenceEleve,
  Eleve,
  EleveRequete,
  LienParental,
  ParentRequete,
  ResultatGeneration,
  ResultatsClasse,
  Salle,
  SalleRequete,
  BilanFinancier,
} from './types'

export const obtenirEtablissement = (id: number) => api.get<Etablissement>(`/api/etablissements/${id}`)

export const listerAnnees = () => api.get<AnneeScolaire[]>('/api/annees-scolaires')

export const listerNiveaux = () => api.get<Niveau[]>('/api/niveaux')

export const listerClasses = (etablissementId: number, anneeScolaireId: number) =>
  api.get<Classe[]>(`/api/etablissements/${etablissementId}/classes?anneeScolaireId=${anneeScolaireId}`)

export const creerClasse = (etablissementId: number, requete: ClasseRequete) =>
  api.post<Classe>(`/api/etablissements/${etablissementId}/classes`, requete)

export const listerMatieres = () => api.get<Matiere[]>('/api/matieres')

// ---------- Inscriptions ----------

export function listerInscriptions(etablissementId: number, anneeScolaireId: number, statut?: string) {
  const parametres = new URLSearchParams({ anneeScolaireId: String(anneeScolaireId) })
  if (statut) parametres.set('statut', statut)
  return api.get<Inscription[]>(`/api/etablissements/${etablissementId}/inscriptions?${parametres.toString()}`)
}

export const demanderInscription = (etablissementId: number, requete: InscriptionRequete) =>
  api.post<Inscription>(`/api/etablissements/${etablissementId}/inscriptions`, requete)

export const deciderInscription = (id: number, decision: DecisionRequete) =>
  api.post<Inscription>(`/api/inscriptions/${id}/decision`, decision)

export const annulerInscription = (id: number) => api.post<Inscription>(`/api/inscriptions/${id}/annulation`)

// ---------- Enseignants ----------

export const listerEnseignants = (etablissementId: number) =>
  api.get<Enseignant[]>(`/api/etablissements/${etablissementId}/enseignants`)

export const creerEnseignant = (etablissementId: number, requete: EnseignantRequete) =>
  api.post<Enseignant>(`/api/etablissements/${etablissementId}/enseignants`, requete)

export const listerAffectationsClasse = (classeId: number) =>
  api.get<AffectationEnseignant[]>(`/api/classes/${classeId}/enseignants`)

export const affecterEnseignant = (classeId: number, requete: AffectationRequete) =>
  api.post<AffectationEnseignant>(`/api/classes/${classeId}/enseignants`, requete)

// ---------- Espace enseignant ----------

export const mesAffectations = () => api.get<MonAffectation[]>('/api/me/affectations')

export const listerPeriodes = (anneeScolaireId: number) =>
  api.get<Periode[]>(`/api/annees-scolaires/${anneeScolaireId}/periodes`)

export const obtenirFeuille = (classeId: number, date: string) =>
  api.get<FeuilleAppel>(`/api/classes/${classeId}/presences?date=${date}`)

export const enregistrerAppel = (classeId: number, date: string, lignes: LignePresenceRequete[]) =>
  api.put<FeuilleAppel>(`/api/classes/${classeId}/presences/${date}`, { lignes })

export const listerEvaluations = (classeId: number) => api.get<Evaluation[]>(`/api/classes/${classeId}/evaluations`)

export const creerEvaluation = (classeId: number, requete: EvaluationRequete) =>
  api.post<Evaluation>(`/api/classes/${classeId}/evaluations`, requete)

export const obtenirGrille = (evaluationId: number) => api.get<GrilleNotes>(`/api/evaluations/${evaluationId}`)

export const saisirNotes = (evaluationId: number, lignes: LigneNoteRequete[]) =>
  api.put<GrilleNotes>(`/api/evaluations/${evaluationId}/notes`, { lignes })

// ---------- Espace parent ----------

export const mesEnfants = () => api.get<Enfant[]>('/api/me/enfants')

export const obtenirBulletin = (eleveId: number, periodeId: number) =>
  api.get<Bulletin>(`/api/eleves/${eleveId}/bulletin?periodeId=${periodeId}`)

export const historiquePresences = (eleveId: number, du: string, au: string) =>
  api.get<PresenceEleve[]>(`/api/eleves/${eleveId}/presences?du=${du}&au=${au}`)

// ---------- Espace élève ----------

export const monProfilEleve = () => api.get<MonProfilEleve>('/api/me/eleve')

// ---------- Élèves, parents, résultats (direction) ----------

export const modifierEleve = (eleveId: number, requete: EleveRequete) =>
  api.put<Eleve>(`/api/eleves/${eleveId}`, requete)

export const listerParents = (eleveId: number) => api.get<LienParental[]>(`/api/eleves/${eleveId}/parents`)

export const lierParent = (eleveId: number, requete: ParentRequete) =>
  api.post<LienParental>(`/api/eleves/${eleveId}/parents`, requete)

export const obtenirResultats = (classeId: number, periodeId: number) =>
  api.get<ResultatsClasse>(`/api/classes/${classeId}/resultats?periodeId=${periodeId}`)

// ---------- Salles (Étape 2) ----------

export const listerSalles = (etablissementId: number) =>
  api.get<Salle[]>(`/api/etablissements/${etablissementId}/salles`)

export const creerSalle = (etablissementId: number, requete: SalleRequete) =>
  api.post<Salle>(`/api/etablissements/${etablissementId}/salles`, requete)

export const supprimerSalle = (etablissementId: number, salleId: number) =>
  api.post<void>(`/api/etablissements/${etablissementId}/salles/${salleId}/suppression`)

// ---------- Créneaux (Étape 2) ----------

export const listerCreneauxClasse = (classeId: number) =>
  api.get<Creneau[]>(`/api/classes/${classeId}/creneaux`)

export const listerCreneauxEtablissement = (etablissementId: number, anneeScolaireId: number) =>
  api.get<Creneau[]>(`/api/etablissements/${etablissementId}/creneaux?anneeScolaireId=${anneeScolaireId}`)

export const creerCreneau = (classeId: number, requete: CreneauRequete) =>
  api.post<Creneau>(`/api/classes/${classeId}/creneaux`, requete)

export const modifierCreneau = (creneauId: number, requete: CreneauRequete) =>
  api.put<Creneau>(`/api/creneaux/${creneauId}`, requete)

export const supprimerCreneau = (creneauId: number) =>
  api.post<void>(`/api/creneaux/${creneauId}/suppression`)

/** Les créneaux d'une classe, vus par l'élève ou le parent. */
export const emploiDuTempsClasse = (classeId: number) =>
  api.get<CreneauEleve[]>(`/api/classes/${classeId}/emploi-du-temps`)

// ---------- Frais et factures (Étape 3a) ----------

export const listerFrais = (etablissementId: number, anneeScolaireId?: number) =>
  api.get<Frais[]>(
    `/api/etablissements/${etablissementId}/frais${anneeScolaireId ? `?anneeScolaireId=${anneeScolaireId}` : ''}`,
  )

export const creerFrais = (etablissementId: number, requete: FraisRequete) =>
  api.post<Frais>(`/api/etablissements/${etablissementId}/frais`, requete)

export const modifierFrais = (fraisId: number, requete: FraisRequete) =>
  api.put<Frais>(`/api/frais/${fraisId}`, requete)

export const desactiverFrais = (fraisId: number) => api.post<void>(`/api/frais/${fraisId}/desactivation`)

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

export const mesFactures = () => api.get<Facture[]>('/api/me/factures')

export const facturesDeMonEnfant = (eleveId: number) =>
  api.get<Facture[]>(`/api/me/enfants/${eleveId}/factures`)
