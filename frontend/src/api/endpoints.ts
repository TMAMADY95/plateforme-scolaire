import { api, telecharger } from './client'
import type {
  AffectationEnseignant,
  AffectationRequete,
  AnneeScolaire,
  AnneeScolaireRequete,
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
  Devoir,
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

export const statistiquesNationales = (anneeScolaireId: number) =>
  api.get<Statistiques>(`/api/ministere/statistiques?anneeScolaireId=${anneeScolaireId}`)

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
