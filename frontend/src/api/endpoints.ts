import { api, envoyerFichier, telecharger } from './client'
import type {
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
  api.delete<void>(`/api/etablissements/${etablissementId}/salles/${salleId}`)

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
  api.delete<void>(`/api/creneaux/${creneauId}`)

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

export const syntheseRegionale = (anneeScolaireId: number) =>
  api.get<SyntheseNationale>(`/api/region/synthese?anneeScolaireId=${anneeScolaireId}`)

export const creerPeriode = (anneeScolaireId: number, requete: PeriodeRequete) =>
  api.post<Periode>(`/api/annees-scolaires/${anneeScolaireId}/periodes`, requete)

export const modifierPeriode = (id: number, requete: PeriodeRequete) =>
  api.put<Periode>(`/api/periodes/${id}`, requete)

export const modifierAnnee = (id: number, requete: AnneeScolaireRequete) =>
  api.put<AnneeScolaire>(`/api/annees-scolaires/${id}`, requete)

export const modifierNiveau = (id: number, requete: NiveauRequete) => api.put<Niveau>(`/api/niveaux/${id}`, requete)

export const modifierMatiere = (id: number, requete: MatiereRequete) =>
  api.put<Matiere>(`/api/matieres/${id}`, requete)

export const creerAnnee = (requete: AnneeScolaireRequete) => api.post<AnneeScolaire>('/api/annees-scolaires', requete)

export const creerNiveau = (requete: NiveauRequete) => api.post<Niveau>('/api/niveaux', requete)

export const creerMatiere = (requete: MatiereRequete) => api.post<Matiere>('/api/matieres', requete)

// ---------- Paiements (Étape 3b) ----------

export const listerPaiements = (factureId: number) => api.get<Paiement[]>(`/api/factures/${factureId}/paiements`)

export const enregistrerPaiement = (factureId: number, requete: PaiementRequete) =>
  api.post<Paiement>(`/api/factures/${factureId}/paiements`, requete)

export const annulerPaiement = (factureId: number, paiementId: number, motif: string) =>
  api.post<Paiement>(`/api/factures/${factureId}/paiements/${paiementId}/annulation`, { motif })

/** Élève ou parent : démarre un paiement en ligne et renvoie l'adresse où finir le paiement. */
export const payerEnLigne = (factureId: number, montant?: number) =>
  api.post<PaiementInitie>(`/api/me/factures/${factureId}/paiements`, montant === undefined ? {} : { montant })

/** Profil dev uniquement : joue le rôle du prestataire de paiement simulé. */
export const simulerPaiement = (reference: string, succes: boolean) =>
  api.post<void>(`/api/dev/paiements/${encodeURIComponent(reference)}/simuler?succes=${succes}`)

// ---------- Comptes et audit (Lot 10) ----------

export const listerComptes = (etablissementId: number, recherche = '') =>
  api.get<Compte[]>(`/api/etablissements/${etablissementId}/comptes?q=${encodeURIComponent(recherche)}`)

export const creerPersonnel = (etablissementId: number, requete: DirectionRequete) =>
  api.post<ComptePersonnel>(`/api/etablissements/${etablissementId}/personnel`, requete)

export const reinitialiserMotDePasse = (utilisateurId: number, motDePasseTemporaire: string) =>
  api.post<void>(`/api/utilisateurs/${utilisateurId}/reinitialisation-mot-de-passe`, { motDePasseTemporaire })

export const rechercherAudit = (filtres: { action: string; entite: string; eleve: string; page: number; taille: number }) => {
  const parametres = new URLSearchParams({
    action: filtres.action,
    entite: filtres.entite,
    eleve: filtres.eleve,
    page: String(filtres.page),
    taille: String(filtres.taille),
  })
  return api.get<PageAudit>(`/api/audit?${parametres.toString()}`)
}

// ---------- Coefficients des matières ----------

export const listerCoefficients = (classeId: number) =>
  api.get<CoefficientMatiere[]>(`/api/classes/${classeId}/coefficients`)

export const enregistrerCoefficients = (classeId: number, lignes: { matiereId: number; coefficient: number }[]) =>
  api.put<CoefficientMatiere[]>(`/api/classes/${classeId}/coefficients`, { lignes })

// ---------- Statistiques des tableaux de bord ----------

export const statistiquesNationales = (anneeScolaireId: number, zoneId?: number) =>
  api.get<Statistiques>(
    `/api/ministere/statistiques?anneeScolaireId=${anneeScolaireId}${zoneId === undefined ? '' : `&zoneId=${zoneId}`}`,
  )

export const statistiquesParEtablissement = (anneeScolaireId: number, zoneId?: number) =>
  api.get<LigneEtablissementStat[]>(
    `/api/ministere/statistiques/etablissements?anneeScolaireId=${anneeScolaireId}${zoneId === undefined ? '' : `&zoneId=${zoneId}`}`,
  )

export const statistiquesParZone = (anneeScolaireId: number) =>
  api.get<LigneZoneStat[]>(`/api/ministere/statistiques/zones?anneeScolaireId=${anneeScolaireId}`)

export const statistiquesRegionalesParEtablissement = (anneeScolaireId: number) =>
  api.get<LigneEtablissementStat[]>(`/api/region/statistiques/etablissements?anneeScolaireId=${anneeScolaireId}`)

export const statistiquesRegionales = (anneeScolaireId: number) =>
  api.get<Statistiques>(`/api/region/statistiques?anneeScolaireId=${anneeScolaireId}`)

export const statistiquesEtablissement = (etablissementId: number, anneeScolaireId: number) =>
  api.get<Statistiques>(`/api/etablissements/${etablissementId}/statistiques?anneeScolaireId=${anneeScolaireId}`)

export const reussiteEtablissement = (etablissementId: number, anneeScolaireId: number, periodeId: number) =>
  api.get<Reussite>(
    `/api/etablissements/${etablissementId}/reussite?anneeScolaireId=${anneeScolaireId}&periodeId=${periodeId}`,
  )

export const reussiteNationale = (anneeScolaireId: number, periodeId: number) =>
  api.get<ReussiteReseau>(`/api/ministere/reussite?anneeScolaireId=${anneeScolaireId}&periodeId=${periodeId}`)

export const reussiteRegionale = (anneeScolaireId: number, periodeId: number) =>
  api.get<ReussiteReseau>(`/api/region/reussite?anneeScolaireId=${anneeScolaireId}&periodeId=${periodeId}`)

export const listerDevoirs = (classeId: number) => api.get<Devoir[]>(`/api/classes/${classeId}/devoirs`)

export const creerDevoir = (classeId: number, requete: DevoirRequete) =>
  api.post<Devoir>(`/api/classes/${classeId}/devoirs`, requete)

export const modifierDevoir = (devoirId: number, requete: DevoirRequete) =>
  api.put<Devoir>(`/api/devoirs/${devoirId}`, requete)

export const devoirsEleve = (eleveId: number) => api.get<Devoir[]>(`/api/eleves/${eleveId}/devoirs`)

export const marquerDevoirFait = (devoirId: number, eleveId: number, fait: boolean) =>
  api.put<Devoir>(`/api/devoirs/${devoirId}/fait`, { eleveId, fait })

export const supprimerDevoir = (devoirId: number) => api.delete<void>(`/api/devoirs/${devoirId}`)

export const contactsMessagerie = (eleveId: number) =>
  api.get<Contact[]>(`/api/messagerie/contacts?eleveId=${eleveId}`)

export const listerConversations = () => api.get<Conversation[]>('/api/messagerie/conversations')

export const demarrerConversation = (requete: NouvelleConversationRequete) =>
  api.post<Conversation>('/api/messagerie/conversations', requete)

export const ouvrirConversation = (id: number) => api.get<Conversation>(`/api/messagerie/conversations/${id}`)

export const repondreConversation = (id: number, contenu: string) =>
  api.post<Conversation>(`/api/messagerie/conversations/${id}/messages`, { contenu })

export const messagesNonLus = () => api.get<{ nonLus: number }>('/api/messagerie/non-lus')

export const resumeParent = () => api.get<ResumeEnfant[]>('/api/me/resume')

export const listerAnnonces = (classeId: number) => api.get<Annonce[]>(`/api/classes/${classeId}/annonces`)

export const publierAnnonce = (classeId: number, requete: { titre: string; contenu: string }) =>
  api.post<Annonce>(`/api/classes/${classeId}/annonces`, requete)

export const supprimerAnnonce = (annonceId: number) => api.delete<void>(`/api/annonces/${annonceId}`)

// ---------- Conformité, inspections et sécurité ----------

const requeteFiltres = (f: FiltresConformite, avecZone: boolean) => {
  const p = new URLSearchParams()
  if (avecZone && f.zoneId) p.set('zoneId', f.zoneId)
  if (f.statut) p.set('statut', f.statut)
  if (f.secteur) p.set('secteur', f.secteur)
  if (f.cycleId) p.set('cycleId', f.cycleId)
  const texte = p.toString()
  return texte === '' ? '' : `?${texte}`
}

export const conformiteNationale = (f: FiltresConformite) =>
  api.get<Conformite>(`/api/ministere/conformite${requeteFiltres(f, true)}`)

export const conformiteRegionale = (f: FiltresConformite) =>
  api.get<Conformite>(`/api/region/conformite${requeteFiltres(f, false)}`)

export const listerInspections = (etablissementId: number) =>
  api.get<Inspection[]>(`/api/etablissements/${etablissementId}/inspections`)

export const creerInspection = (etablissementId: number, requete: InspectionRequete) =>
  api.post<Inspection>(`/api/etablissements/${etablissementId}/inspections`, requete)

export const listerSignalements = (etablissementId: number) =>
  api.get<Signalement[]>(`/api/etablissements/${etablissementId}/signalements-securite`)

export const creerSignalement = (etablissementId: number, requete: SignalementRequete) =>
  api.post<Signalement>(`/api/etablissements/${etablissementId}/signalements-securite`, requete)

export const resoudreSignalement = (id: number) =>
  api.post<Signalement>(`/api/signalements-securite/${id}/resolution`)

export const modifierAutorisation = (etablissementId: number, requete: AutorisationRequete) =>
  api.put<void>(`/api/etablissements/${etablissementId}/autorisation`, requete)

// ---------- Examens nationaux ----------

export const listerExamens = (anneeScolaireId: number) =>
  api.get<Examen[]>(`/api/examens?anneeScolaireId=${anneeScolaireId}`)

export const creerExamen = (requete: ExamenRequete) => api.post<Examen>('/api/examens', requete)

export const rapportExamenNational = (examenId: number, zoneId?: number) =>
  api.get<RapportExamen>(`/api/ministere/examens/${examenId}${zoneId === undefined ? '' : `?zoneId=${zoneId}`}`)

export const rapportExamenRegional = (examenId: number) => api.get<RapportExamen>(`/api/region/examens/${examenId}`)

export const saisirResultatExamen = (examenId: number, etablissementId: number, requete: ResultatExamenRequete) =>
  api.put<void>(`/api/examens/${examenId}/resultats/${etablissementId}`, requete)

// ---------- Évolution et rapports PDF ----------

export const evolutionNationale = (zoneId?: number) =>
  api.get<Evolution>(`/api/ministere/evolution${zoneId === undefined ? '' : `?zoneId=${zoneId}`}`)

export const evolutionRegionale = () => api.get<Evolution>('/api/region/evolution')

export const rapportNationalPdf = (anneeScolaireId: number, zoneId?: number) =>
  telecharger(
    `/api/ministere/rapport.pdf?anneeScolaireId=${anneeScolaireId}${zoneId === undefined ? '' : `&zoneId=${zoneId}`}`,
    'rapport-national.pdf',
  )

export const rapportRegionalPdf = (anneeScolaireId: number) =>
  telecharger(`/api/region/rapport.pdf?anneeScolaireId=${anneeScolaireId}`, 'rapport-regional.pdf')

export const rapportEtablissementPdf = (etablissementId: number, anneeScolaireId: number) =>
  telecharger(
    `/api/etablissements/${etablissementId}/rapport.pdf?anneeScolaireId=${anneeScolaireId}`,
    `rapport-etablissement-${etablissementId}.pdf`,
  )

// ---------- Postes d'enseignants et mutations ----------

export const postesNationaux = (anneeScolaireId: number, zoneId?: number) =>
  api.get<RapportPostes>(
    `/api/ministere/postes?anneeScolaireId=${anneeScolaireId}${zoneId === undefined ? '' : `&zoneId=${zoneId}`}`,
  )

export const postesRegionaux = (anneeScolaireId: number) =>
  api.get<RapportPostes>(`/api/region/postes?anneeScolaireId=${anneeScolaireId}`)

export const definirPostes = (etablissementId: number, postes: number | null) =>
  api.put<void>(`/api/etablissements/${etablissementId}/postes`, { postes })

export const muterEnseignant = (enseignantId: number, requete: MutationRequete) =>
  api.post<void>(`/api/enseignants/${enseignantId}/mutation`, requete)

// ---------- Comptes du ministère et des directions régionales ----------

export const listerComptesInstitutionnels = () => api.get<CompteInstitutionnel[]>('/api/ministere/comptes')

export const creerCompteMinistere = (requete: DirectionRequete) =>
  api.post<CompteInstitutionnel>('/api/ministere/comptes', requete)

export const activerCompte = (id: number, actif: boolean) =>
  api.post<void>(`/api/ministere/comptes/${id}/activation`, { actif })

export const reinitialiserCompteInstitutionnel = (id: number, motDePasseTemporaire: string) =>
  api.post<void>(`/api/ministere/comptes/${id}/reinitialisation`, { motDePasseTemporaire })

// ---------- Plans d'action et journal des alertes ----------

export const plansActionEtablissement = (etablissementId: number) =>
  api.get<PlanAction[]>(`/api/etablissements/${etablissementId}/plans-action`)

export const creerPlanAction = (etablissementId: number, requete: PlanActionRequete) =>
  api.post<PlanAction>(`/api/etablissements/${etablissementId}/plans-action`, requete)

export const changerStatutPlanAction = (planId: number, statut: StatutPlanAction, note: string | null) =>
  api.post<PlanAction>(`/api/plans-action/${planId}/statut`, { statut, note })

export const plansActionNationaux = () => api.get<PlanAction[]>('/api/ministere/plans-action')
export const plansActionRegionaux = () => api.get<PlanAction[]>('/api/region/plans-action')
export const alertesParentsEtablissement = (etablissementId: number) =>
  api.get<AlerteEnvoyee[]>(`/api/etablissements/${etablissementId}/alertes-parents`)
export const journalAlertes = () => api.get<AlerteEnvoyee[]>('/api/ministere/alertes')

export const exporterAuditCsv = (filtres: { action: string; entite: string; eleve: string }) =>
  telecharger(
    `/api/audit/export.csv?${new URLSearchParams({ action: filtres.action, entite: filtres.entite, eleve: filtres.eleve }).toString()}`,
    'journal-audit.csv',
  )

// ---------- Pièces jointes des inspections ----------

export const listerPiecesJointes = (inspectionId: number) =>
  api.get<PieceJointe[]>(`/api/inspections/${inspectionId}/pieces`)

export const ajouterPieceJointe = (inspectionId: number, fichier: File) =>
  envoyerFichier<PieceJointe>(`/api/inspections/${inspectionId}/pieces`, fichier)

export const telechargerPieceJointe = (piece: PieceJointe) => telecharger(`/api/pieces/${piece.id}`, piece.nom)

export const supprimerPieceJointe = (pieceId: number) => api.delete<void>(`/api/pieces/${pieceId}`)

// ---------- Cahier de texte et justifications d'absence ----------

export const cahierTexteClasse = (classeId: number) => api.get<EntreeCahierTexte[]>(`/api/classes/${classeId}/cahier-texte`)
export const cahierTexteEleve = (eleveId: number) => api.get<EntreeCahierTexte[]>(`/api/eleves/${eleveId}/cahier-texte`)
export const ajouterCahierTexte = (classeId: number, requete: { matiereId: number; dateSeance: string; contenu: string }) =>
  api.post<EntreeCahierTexte>(`/api/classes/${classeId}/cahier-texte`, requete)
export const supprimerCahierTexte = (id: number) => api.delete<void>(`/api/cahier-texte/${id}`)

export const justificationsEleve = (eleveId: number) => api.get<Justification[]>(`/api/eleves/${eleveId}/justifications`)
export const demanderJustification = (presenceId: number, motif: string) =>
  api.post<Justification>(`/api/presences/${presenceId}/justification`, { motif })
export const justificationsEnAttente = (etablissementId: number) =>
  api.get<Justification[]>(`/api/etablissements/${etablissementId}/justifications`)
export const deciderJustification = (id: number, accepter: boolean, reponse: string | null) =>
  api.post<Justification>(`/api/justifications/${id}/decision`, { accepter, reponse })

// ---------- Liaison école-famille ----------

export const enseignantsDeLEleve = (eleveId: number) => api.get<EnseignantDeLEleve[]>(`/api/eleves/${eleveId}/enseignants`)
export const rendezVousEleve = (eleveId: number) => api.get<RendezVous[]>(`/api/eleves/${eleveId}/rendez-vous`)
export const demanderRendezVous = (eleveId: number, requete: { enseignantId: number; dateHeure: string; motif: string }) =>
  api.post<RendezVous>(`/api/eleves/${eleveId}/rendez-vous`, requete)
export const annulerRendezVous = (id: number) => api.post<RendezVous>(`/api/rendez-vous/${id}/annulation`)
export const mesRendezVous = () => api.get<RendezVous[]>('/api/me/rendez-vous')
export const repondreRendezVous = (id: number, accepter: boolean, reponse: string | null) =>
  api.post<RendezVous>(`/api/rendez-vous/${id}/reponse`, { accepter, reponse })

export const sortiesEleve = (eleveId: number) => api.get<Sortie[]>(`/api/eleves/${eleveId}/sorties`)
export const demanderSortie = (
  eleveId: number,
  requete: { dateSortie: string; heureSortie: string; motif: string; accompagnateur: string | null },
) => api.post<Sortie>(`/api/eleves/${eleveId}/sorties`, requete)
export const sortiesEtablissement = (etablissementId: number) =>
  api.get<Sortie[]>(`/api/etablissements/${etablissementId}/sorties`)
export const deciderSortie = (id: number, accepter: boolean, reponse: string | null) =>
  api.post<Sortie>(`/api/sorties/${id}/decision`, { accepter, reponse })

export const listerTypesExamen = () => api.get<TypeExamen[]>('/api/types-examen')
export const creerTypeExamen = (requete: TypeExamenRequete) => api.post<TypeExamen>('/api/types-examen', requete)
export const supprimerTypeExamen = (id: number) => api.delete<void>(`/api/types-examen/${id}`)
export const niveauxOrganisationExamen = () => api.get<string[]>('/api/examens/niveaux-organisation')

// ---------- Examens de l'établissement : candidats, centres, convocations ----------

export const examensEtablissement = (etablissementId: number, anneeScolaireId: number) =>
  api.get<ExamenEtablissement[]>(`/api/etablissements/${etablissementId}/examens?anneeScolaireId=${anneeScolaireId}`)

export const preparationExamen = (etablissementId: number, examenId: number) =>
  api.get<PreparationExamen>(`/api/etablissements/${etablissementId}/examens/${examenId}/candidats`)

export const inscrireCandidats = (etablissementId: number, examenId: number, eleveIds: number[]) =>
  api.post<PreparationExamen>(`/api/etablissements/${etablissementId}/examens/${examenId}/candidats`, { eleveIds })

export const retirerCandidat = (candidatId: number) => api.delete<void>(`/api/candidats-examen/${candidatId}`)

export const telechargerConvocations = (etablissementId: number, examenId: number) =>
  telecharger(`/api/etablissements/${etablissementId}/examens/${examenId}/convocations.pdf`, `convocations-${examenId}.pdf`)

export const telechargerListeAppel = (examenId: number, centreId: number) =>
  telecharger(`/api/examens/${examenId}/centres/${centreId}/liste-appel.pdf`, `liste-appel-${centreId}.pdf`)

export const incidentsCentre = (examenId: number, centreId: number) =>
  api.get<IncidentsCentre>(`/api/examens/${examenId}/centres/${centreId}/incidents`)
export const signalerIncident = (
  examenId: number,
  centreId: number,
  requete: { type: TypeIncident; candidatId: number | null; epreuveId: number | null; description: string; signalePar: string | null; eliminer: boolean },
) => api.post<IncidentsCentre>(`/api/examens/${examenId}/centres/${centreId}/incidents`, requete)
export const traiterIncident = (examenId: number, centreId: number, incidentId: number, decision: string) =>
  api.post<IncidentsCentre>(`/api/examens/${examenId}/centres/${centreId}/incidents/${incidentId}/traitement`, { decision })

export const correctionExamen = (examenId: number) => api.get<CorrectionExamen>(`/api/examens/${examenId}/correction`)
export const genererSousCommissions = (examenId: number) => api.post<CorrectionExamen>(`/api/examens/${examenId}/correction/sous-commissions`, {})
export const modifierCommission = (
  examenId: number,
  commissionId: number,
  requete: { epreuveId: number | null; lieu: string | null; nbCopies: number; nbCopiesCorrigees: number },
) => api.put<CorrectionExamen>(`/api/examens/${examenId}/correction/commissions/${commissionId}`, requete)
export const supprimerCommission = (examenId: number, commissionId: number) =>
  api.delete<CorrectionExamen>(`/api/examens/${examenId}/correction/commissions/${commissionId}`)
export const ajouterMembreCommission = (
  examenId: number,
  commissionId: number,
  requete: { role: string; nom: string; telephone: string | null },
) => api.post<CorrectionExamen>(`/api/examens/${examenId}/correction/commissions/${commissionId}/membres`, requete)
export const retirerMembreCommission = (examenId: number, commissionId: number, membreId: number) =>
  api.delete<CorrectionExamen>(`/api/examens/${examenId}/correction/commissions/${commissionId}/membres/${membreId}`)
export const cloturerCommission = (examenId: number, commissionId: number) =>
  api.post<CorrectionExamen>(`/api/examens/${examenId}/correction/commissions/${commissionId}/cloture`, {})

export const dossiersCandidatures = (etablissementId: number, examenId: number) =>
  api.get<DossiersCandidatures>(`/api/etablissements/${etablissementId}/examens/${examenId}/dossiers`)
export const marquerPiece = (etablissementId: number, examenId: number, candidatId: number, type: string, requete: { fournie: boolean; reference: string | null }) =>
  api.put<DossiersCandidatures>(`/api/etablissements/${etablissementId}/examens/${examenId}/candidats/${candidatId}/pieces/${type}`, requete)

const baseListe = (etablissementId: number, examenId: number) => `/api/etablissements/${etablissementId}/examens/${examenId}/liste`
export const listeCandidats = (etablissementId: number, examenId: number) => api.get<ListeCandidats>(baseListe(etablissementId, examenId))
export const transmettreListe = (etablissementId: number, examenId: number) => api.post<ListeCandidats>(`${baseListe(etablissementId, examenId)}/transmission`, {})
export const verifierListe = (etablissementId: number, examenId: number) => api.post<ListeCandidats>(`${baseListe(etablissementId, examenId)}/verification`, {})
export const validerListe = (etablissementId: number, examenId: number) => api.post<ListeCandidats>(`${baseListe(etablissementId, examenId)}/validation`, {})
export const rejeterListe = (etablissementId: number, examenId: number, motif: string) =>
  api.post<ListeCandidats>(`${baseListe(etablissementId, examenId)}/rejet`, { motif })

export const banqueSujets = (examenId: number) => api.get<BanqueSujets>(`/api/examens/${examenId}/sujets`)
export const ajouterSujet = (examenId: number, epreuveId: number, requete: { variante: string; intitule: string; auteurs: string | null }) =>
  api.post<BanqueSujets>(`/api/examens/${examenId}/epreuves/${epreuveId}/sujets`, requete)
export const supprimerSujet = (examenId: number, sujetId: number) => api.delete<BanqueSujets>(`/api/examens/${examenId}/sujets/${sujetId}`)
export const validerSujet = (examenId: number, sujetId: number) => api.post<BanqueSujets>(`/api/examens/${examenId}/sujets/${sujetId}/validation`, {})
export const tirerSujet = (examenId: number, epreuveId: number) => api.post<BanqueSujets>(`/api/examens/${examenId}/epreuves/${epreuveId}/tirage`, {})
export const compromettreSujet = (examenId: number, sujetId: number) => api.post<BanqueSujets>(`/api/examens/${examenId}/sujets/${sujetId}/compromission`, {})

export const diplomesExamen = (examenId: number) => api.get<DiplomesExamen>(`/api/examens/${examenId}/diplomes`)
export const delivrerDiplomes = (examenId: number) => api.post<DiplomesExamen>(`/api/examens/${examenId}/diplomes`, {})
export const revoquerDiplome = (examenId: number, diplomeId: number, motif: string) =>
  api.post<DiplomesExamen>(`/api/examens/${examenId}/diplomes/${diplomeId}/revocation`, { motif })
export const telechargerDiplomes = (etablissementId: number, examenId: number) =>
  telecharger(`/api/etablissements/${etablissementId}/examens/${examenId}/diplomes.pdf`, `diplomes-${examenId}.pdf`)
export const verifierDiplome = (code: string) =>
  api.get<VerificationDiplome>(`/api/verification-diplome/${encodeURIComponent(code)}`, { sansJeton: true })

export const inscrireCandidatLibre = (
  etablissementId: number,
  examenId: number,
  requete: { nom: string; prenom: string; dateNaissance: string; sexe: string | null },
) => api.post<Candidat>(`/api/etablissements/${etablissementId}/examens/${examenId}/candidats-libres`, requete)

const baseDeroulement = (examenId: number, centreId: number) => `/api/examens/${examenId}/centres/${centreId}`
export const deroulementCentre = (examenId: number, centreId: number) => api.get<DeroulementCentre>(`${baseDeroulement(examenId, centreId)}/deroulement`)
export const avancerPli = (examenId: number, centreId: number, epreuveId: number) =>
  api.post<DeroulementCentre>(`${baseDeroulement(examenId, centreId)}/epreuves/${epreuveId}/pli/avancement`, {})
export const ouvrirPli = (examenId: number, centreId: number, epreuveId: number, requete: { ouvertPar: string; temoin1: string; temoin2: string }) =>
  api.post<DeroulementCentre>(`${baseDeroulement(examenId, centreId)}/epreuves/${epreuveId}/pli/ouverture`, requete)
export const enregistrerRapportEpreuve = (
  examenId: number,
  centreId: number,
  epreuveId: number,
  requete: {
    compositionDebut: string | null
    compositionFin: string | null
    presents: number
    absents: number
    elimines: number
    copiesRamassees: number
    observations: string | null
    cloturer: boolean
  },
) => api.put<DeroulementCentre>(`${baseDeroulement(examenId, centreId)}/epreuves/${epreuveId}/rapport`, requete)

export const personnelCentre = (examenId: number, centreId: number) =>
  api.get<PersonnelCentre>(`/api/examens/${examenId}/centres/${centreId}/personnel`)
export const ajouterPersonnelCentre = (
  examenId: number,
  centreId: number,
  requete: { role: FonctionCentre; nom: string; telephone: string | null; salle: number | null },
) => api.post<PersonnelCentre>(`/api/examens/${examenId}/centres/${centreId}/personnel`, requete)
export const retirerPersonnelCentre = (examenId: number, centreId: number, personneId: number) =>
  api.delete<PersonnelCentre>(`/api/examens/${examenId}/centres/${centreId}/personnel/${personneId}`)

export const notesCandidats = (examenId: number, etablissementId: number) =>
  api.get<NotesCandidats>(`/api/examens/${examenId}/etablissements/${etablissementId}/notes`)
export const saisirNotesCandidat = (
  examenId: number,
  etablissementId: number,
  candidatId: number,
  requete: { statut: StatutCandidat; notes: Record<string, number> },
) => api.put<NotesCandidat>(`/api/examens/${examenId}/etablissements/${etablissementId}/candidats/${candidatId}/notes`, requete)
export const consoliderResultats = (examenId: number, etablissementId: number) =>
  api.post<void>(`/api/examens/${examenId}/etablissements/${etablissementId}/consolidation`, {})
export const telechargerReleves = (etablissementId: number, examenId: number) =>
  telecharger(`/api/etablissements/${etablissementId}/examens/${examenId}/releves.pdf`, `releves-${examenId}.pdf`)

export const juryExamen = (examenId: number) => api.get<Jury>(`/api/examens/${examenId}/jury`)
export const initialiserJury = (examenId: number) => api.post<Jury>(`/api/examens/${examenId}/jury`, {})
export const modifierMembreJury = (examenId: number, membreId: number, requete: { nom: string | null; present: boolean }) =>
  api.put<Jury>(`/api/examens/${examenId}/jury/membres/${membreId}`, requete)
export const delibererExamen = (examenId: number) => api.post<Jury>(`/api/examens/${examenId}/deliberation`, {})
export const rouvrirDeliberation = (examenId: number) => api.delete<Jury>(`/api/examens/${examenId}/deliberation`)
export const proclamerExamen = (examenId: number) => api.post<Jury>(`/api/examens/${examenId}/proclamation`, {})

export const centresExamen = (examenId: number) => api.get<CentreExamen[]>(`/api/examens/${examenId}/centres`)

export const creerCentreExamen = (examenId: number, requete: CentreExamenRequete) =>
  api.post<CentreExamen>(`/api/examens/${examenId}/centres`, requete)

export const repartitionCentre = (examenId: number, centreId: number) =>
  api.get<RepartitionCentre>(`/api/examens/${examenId}/centres/${centreId}/repartition`)
export const repartirCentre = (examenId: number, centreId: number) =>
  api.post<RepartitionCentre>(`/api/examens/${examenId}/centres/${centreId}/repartition`, {})
export const supprimerCentreExamen = (centreId: number) => api.delete<void>(`/api/centres-examen/${centreId}`)

export const affecterCentreExamen = (examenId: number, etablissementId: number, centreId: number) =>
  api.put<number>(`/api/examens/${examenId}/etablissements/${etablissementId}/centre`, { centreId })

// ---------- Types d'établissement ----------

export const listerTypesEtablissement = () => api.get<TypeEtablissement[]>('/api/types-etablissement')

export const creerTypeEtablissement = (requete: TypeEtablissementRequete) =>
  api.post<TypeEtablissement>('/api/types-etablissement', requete)

export const modifierTypeEtablissement = (id: number, requete: TypeEtablissementRequete) =>
  api.put<TypeEtablissement>(`/api/types-etablissement/${id}`, requete)

export const supprimerTypeEtablissement = (id: number) => api.delete<void>(`/api/types-etablissement/${id}`)

// ---------- Liste nationale des élèves (ministère) ----------

function requeteEleves(f: FiltresEleves): URLSearchParams {
  const p = new URLSearchParams({ anneeScolaireId: String(f.anneeScolaireId) })
  const champs: [string, string | null | undefined][] = [
    ['q', f.q?.trim()],
    ['zoneId', f.zoneId],
    ['etablissementId', f.etablissementId],
    ['secteur', f.secteur],
    ['typeEtablissementId', f.typeEtablissementId],
    ['cycleId', f.cycleId],
    ['niveauId', f.niveauId],
    ['sexe', f.sexe],
    ['statut', f.statut],
  ]
  champs.forEach(([cle, valeur]) => {
    if (valeur !== undefined && valeur !== null && valeur !== '') p.set(cle, valeur)
  })
  return p
}

export const elevesNationaux = (f: FiltresEleves, page: number, taille: number) => {
  const p = requeteEleves(f)
  p.set('page', String(page))
  p.set('taille', String(taille))
  return api.get<ElevesPage>(`/api/ministere/eleves?${p.toString()}`)
}

export const telechargerElevesPdf = (f: FiltresEleves) =>
  telecharger(`/api/ministere/eleves.pdf?${requeteEleves(f).toString()}`, 'liste-eleves.pdf')

// ---------- Calendrier des examens : période, épreuves, affectation des centres ----------

export const definirPeriodeExamen = (examenId: number, dateDebut: string | null, dateFin: string | null) =>
  api.put<Examen>(`/api/examens/${examenId}/periode`, { dateDebut, dateFin })

export const supprimerExamen = (examenId: number, forcer = false) =>
  api.delete<void>(`/api/examens/${examenId}${forcer ? '?forcer=true' : ''}`)

export const epreuvesExamen = (examenId: number) => api.get<Epreuve[]>(`/api/examens/${examenId}/epreuves`)

export const ajouterEpreuve = (examenId: number, requete: EpreuveRequete) =>
  api.post<Epreuve>(`/api/examens/${examenId}/epreuves`, requete)

export const supprimerEpreuve = (id: number) => api.delete<void>(`/api/epreuves-examen/${id}`)

export const affecterCentresAutomatiquement = (examenId: number) =>
  api.post<AffectationAutomatique>(`/api/examens/${examenId}/affectation-automatique`)

export const criteresCentres = () => api.get<CriteresCentre>('/api/centres-examen/criteres')

export const modifierCriteresCentres = (requete: CriteresCentre) =>
  api.put<CriteresCentre>('/api/centres-examen/criteres', requete)

export const etablissementsCentres = () => api.get<EtablissementCentre[]>('/api/centres-examen/etablissements')
