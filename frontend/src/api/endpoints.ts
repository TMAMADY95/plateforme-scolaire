import { api, envoyerFichier, telecharger } from './client'
import type {
  SuiviListes,
  ConcordanceRapports,
  CalendrierPrevisionnel,
  ResultatExamenEleve,
  ControleCoherence,
  PreparationCentre,
  ResumePreparation,
  ChefCentre,
  MonCentre,
  AffectationAutomatique,
  CriteresCentre,
  EtablissementCentre,
  Epreuve,
  EpreuveRequete,
  ElevesPage,
  FiltresEleves,
  TypeEtablissement,
  TypeEtablissementRequete,
  ExamenEtablissement,
  PreparationExamen,
  CentreExamen,
  Jury,
  NotesCandidat,
  PersonnelCentre,
  DeroulementCentre,
  DiplomesExamen,
  VerificationDiplome,
  BanqueSujets,
  Candidat,
  ListeCandidats,
  DossiersCandidatures,
  CorrectionExamen,
  IncidentsCentre,
  TypeIncident,
  FonctionCentre,
  NotesCandidats,
  StatutCandidat,
  RepartitionCentre,
  CentreExamenRequete,
  TypeExamen,
  TypeExamenRequete,
  EnseignantDeLEleve,
  RendezVous,
  Sortie,
  EntreeCahierTexte,
  Justification,
  PieceJointe,
  AlerteEnvoyee,
  PlanAction,
  PlanActionRequete,
  StatutPlanAction,
  AffectationEnseignant,
  AffectationRequete,
  AnneeScolaire,
  AnneeScolaireRequete,
  Cycle,
  CycleRequete,
  AutorisationRequete,
  CompteInstitutionnel,
  MutationRequete,
  RapportPostes,
  Evolution,
  Examen,
  ExamenRequete,
  RapportExamen,
  ResultatExamenRequete,
  Conformite,
  FiltresConformite,
  Inspection,
  InspectionRequete,
  LigneEtablissementStat,
  Signalement,
  SignalementRequete,
  LigneZoneStat,
  PeriodeRequete,
  Compte,
  ComptePersonnel,
  CoefficientMatiere,
  Reussite,
  ReussiteReseau,
  Statistiques,
  PageAudit,
  DirectionRequete,
  EtablissementRequete,
  MatiereRequete,
  NiveauRequete,
  StatutEtablissement,
  SyntheseNationale,
  Zone,
  ZoneRequete,
  Classe,
  Bulletin,
  ClasseModificationRequete,
  ClasseRequete,
  Creneau,
  CreneauEleve,
  CreneauRequete,
  DecisionRequete,
  Contact,
  Conversation,
  Annonce,
  Devoir,
  ResumeEnfant,
  DevoirRequete,
  Enfant,
  NouvelleConversationRequete,
  Enseignant,
  EnseignantRequete,
  Etablissement,
  Evaluation,
  EvaluationModificationRequete,
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
  Paiement,
  PaiementInitie,
  PaiementRequete,
  Periode,
  PresenceEleve,
  Eleve,
  EleveRequete,
  IdentiteNationale,
  EtatCivilRequete,
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
export const modifierClasse = (classeId: number, requete: ClasseModificationRequete) =>
  api.put<Classe>(`/api/classes/${classeId}`, requete)
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

/** Les périodes qui s'appliquent à une classe : celles de son établissement, sinon celles du ministère. */
export const periodesDeLaClasse = (classeId: number) => api.get<Periode[]>(`/api/classes/${classeId}/periodes`)
export const periodesEtablissement = (etablissementId: number, anneeScolaireId: number) =>
  api.get<Periode[]>(`/api/etablissements/${etablissementId}/annees-scolaires/${anneeScolaireId}/periodes`)
export const genererPeriodes = (etablissementId: number, anneeScolaireId: number, type: 'TRIMESTRE' | 'SEMESTRE') =>
  api.post<Periode[]>(`/api/etablissements/${etablissementId}/annees-scolaires/${anneeScolaireId}/periodes/generation`, { type })

export interface PeriodeEtablissementRequete {
  libelle: string
  ordre: number
  dateDebut: string
  dateFin: string
  typePeriode: string
}

export const modifierPeriodeEtablissement = (periodeId: number, requete: PeriodeEtablissementRequete) =>
  api.put<Periode>(`/api/etablissements/periodes/${periodeId}`, requete)
export const supprimerPeriodeEtablissement = (periodeId: number) =>
  api.delete<void>(`/api/etablissements/periodes/${periodeId}`)
export const retablirPeriodes = (etablissementId: number, anneeScolaireId: number) =>
  api.delete<void>(`/api/etablissements/${etablissementId}/annees-scolaires/${anneeScolaireId}/periodes`)
export const obtenirFeuille = (classeId: number, date: string) =>
  api.get<FeuilleAppel>(`/api/classes/${classeId}/presences?date=${date}`)
export const enregistrerAppel = (classeId: number, date: string, lignes: LignePresenceRequete[]) =>
  api.put<FeuilleAppel>(`/api/classes/${classeId}/presences/${date}`, { lignes })
export const listerEvaluations = (classeId: number) => api.get<Evaluation[]>(`/api/classes/${classeId}/evaluations`)
export const modifierEvaluation = (evaluationId: number, requete: EvaluationModificationRequete) =>
  api.put<Evaluation>(`/api/evaluations/${evaluationId}`, requete)
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

/** Retrouver un élève par son INE, où qu'il soit scolarisé (transfert, dossier d'examen). */
export const eleveParIne = (ine: string) =>
  api.get<IdentiteNationale>(`/api/eleves/ine/${encodeURIComponent(ine)}`)

export const identiteNationale = (eleveId: number) =>
  api.get<IdentiteNationale>(`/api/eleves/${eleveId}/identite-nationale`)

/** Saisie de la pièce d'état civil (extrait, jugement supplétif ou aucun). */
export const modifierEtatCivil = (eleveId: number, requete: EtatCivilRequete) =>
  api.put<IdentiteNationale>(`/api/eleves/${eleveId}/etat-civil`, requete)

/** Élèves de l'établissement sans acte : à régulariser avant les examens (CEE, BEPC, BAC). */
export const elevesSansActe = (etablissementId: number) =>
  api.get<IdentiteNationale[]>(`/api/eleves/sans-acte?etablissementId=${etablissementId}`)

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
  api.delete<void>(`/api/etablissements/${etablissementId}/salles/${salie_id}`)

// ---------- Créneaux (Étape 2) ----------

export const listerCreneauxClasse = (classeId: number) =>
  api.get<Creneau[]>(`/api/classes/${classeId}/creneaux`)
export const listerCreneauxEtablissement = (etablissementId: number, anneeScolaireId: number) =>
  api.get<Creneau[]>(`/api/etablissements/${etablissementId}/creneaux?anneeScolaireId=${anneeScolaireId}`)
export const creerCreneau = (classeId: number, requete: CreneauRequete) =>
  api.post<Creneau>(`/api/classes/${classeId}/creneaux`, requete)
export const modifierCreneau = (creneauId: number, requete: CreneauRequete) =>
  api.put<Creneau>(`/api/creneaux/${creneauId}`, requete)
export const supprimerCreneau = (creneauId: number) => api.delete<void>(`/api/creneaux/${creneauId}`)

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

/** Sans jeton : l'utilisateur a oublié son mot de passe. Répond toujours pareil, que l'adresse existe ou non. */
export const demanderReinitialisation = (email: string) =>
  api.post<void>('/api/auth/mot-de-passe-oublie', { email }, { sansJeton: true })
export const reinitialiserAvecJeton = (jeton: string, nouveauMotDePasse: string) =>
  api.post<void>('/api/auth/reinitialisation', { jeton, nouveauMotDePasse }, { sansJeton: true })

export const telechargerBulletin = (eleveId: number, periodeId: number) =>
  telecharger(`/api/eleves/${eleveId}/bulletin.pdf?periodeId=${periodeId}`, `bulletin-${eleveId}-${periodeId}.pdf`)
export const telechargerRecu = (factureId: number) =>
  telecharger(`/api/factures/${factureId}/recu.pdf`, `recu-facture-${factureId}.pdf`)
export const mesFactures = () => api.get<Facture[]>('/api/me/factures')
export const facturesDeMonEnfant = (eleveId: number) =>
  api.get<Facture[]>(`/api/me/enfants/${eleveId}/factures`)

// ---------- Administration nationale (Lot 6, ministère) ----------

export const listerCycles = () => api.get<Cycle[]>('/api/cycles')
export const creerCycle = (requete: CycleRequete) => api.post<Cycle>('/api/cycles', requete)
export const modifierCycle = (id: number, requete: CycleRequete) => api.put<Cycle>(`/api/cycles/${id}`, requete)
export const listerEtablissements = () => api.get<Etablissement[]>('/api/etablissements')
export const creerEtablissement = (requete: EtablissementRequete) =>
  api.post<Etablissement>('/api/etablissements', requete)
export const modifierEtablissement = (id: number, requete: EtablissementRequete) =>
  api.put<Etablissement>(`/api/etablissements/${id}`, requete)
export const changerStatutEtablissement = (id: number, statut: StatutEtablissement) =>
  api.post<Etablissement>(`/api/etablissements/${id}/statut`, { statut })
export const creerDirection = (etablissementId: number, requete: DirectionRequete) =>
  api.post<ComptePersonnel>(`/api/etablissements/${etablissementId}/direction`, requete)
export const listerZones = () => api.get<Zone[]>('/api/zones')
export const modifierZone = (id: number, requete: ZoneRequete) => api.put<Zone>(`/api/zones/${id}`, requete)
export const supprimerZone = (id: number) => api.delete<void>(`/api/zones/${id}`)
export const creerZone = (requete: ZoneRequete) => api.post<Zone>('/api/zones', requete)
export const syntheseNationale = (anneeScolaireId: number) =>
  api.get<SyntheseNationale>(`/api/ministere/synthese?anneeScolaireId=${anneeScolaireId}`)
export const creerDirectionRegionale = (zoneId: number, requete: DirectionRequete) =>
  api.post<ComptePersonnel>(`/api/zones/${zoneId}/direction-regionale`, requete)
export const creerDirectionLocale = (zoneId: number, requete: DirectionRequete) =>
  api.post<ComptePersonnel>(`/api/zones/${zoneId}/direction-locale`, requete)
export const syntheseRegionale = (anneeScolaireId: number) =>
  api.get<SyntheseNationale>(`/api/region/synthese?anneeScolaireId=${anneeScolaireId}`)
export const creerPeriode = (anneeScolaireId: number, requete: PeriodeRequete) =>
  api.post<Periode>(`/api/annees-scolaires/${anneeScolaireId}/periodes`, requete)
export const modifierPeriode = (id: number, requete: PeriodeRequete) => api.put<Periode>(`/api/periodes/${id}`, requete)
export const modifierAnnee = (id: number, requete: AnneeScolaireRequete) =>
  api.put<AnneeScolaire>(`/api/annees-scolaires/${id}`, requete)
export const modifierNiveau = (id: number, requete: NiveauRequete) => api.put<Niveau>(`/api/niveaux/${id}`, requete)
export const modifierMatiere = (id: number, requete: MatiereRequete) =>
  api.put<Matiere>(`/api/matieres/${id}`, requete)
