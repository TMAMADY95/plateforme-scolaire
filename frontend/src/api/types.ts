// Types miroirs des objets renvoyés par l'API Spring (paquet web.dto du backend).
// Fichier complet : Étape 1 (espace élève), Étape 2 (emplois du temps), Étape 3a (frais et factures).

export type Role =
  | 'ADMIN_TECHNIQUE'
  | 'MINISTERE'
  | 'DIRECTION_REGIONALE'
  | 'DIRECTION_ETABLISSEMENT'
  | 'PERSONNEL_ADMIN'
  | 'ENSEIGNANT'
  | 'PARENT'
  | 'ELEVE'

export type Perimetre = 'NATIONAL' | 'ZONE' | 'ETABLISSEMENT' | 'PERSONNEL'

export interface Affectation {
  role: Role
  perimetre: Perimetre
  zoneId: number | null
  etablissementId: number | null
}

export interface Moi {
  id: number
  email: string
  changementMotDePasseRequis: boolean
  affectations: Affectation[]
}

export interface PieceJointe {
  id: number
  nom: string
  typeMime: string
  taille: number
  ajouteeLe: string
}

export interface VerificationRequise {
  verificationRequise: true
}

export interface LoginResponse {
  token: string
  type: string
  utilisateurId: number
  email: string
  changementMotDePasseRequis: boolean
}

export interface Cycle {
  id: number
  code: string
  libelle: string
  ordre: number
}

export interface CycleRequete {
  code: string
  libelle: string
  ordre: number
}

export type Secteur = 'PUBLIC' | 'PRIVE'

export interface Etablissement {
  id: number
  codeOfficiel: string
  nom: string
  secteur: Secteur
  /** Un ou plusieurs cycles, dans l'ordre (primaire, collège, lycée…). */
  cycles: Cycle[]
  statut: string
  zoneId: number | null
  adresse: string | null
  capacite: number | null
  typeEtablissementId: number | null
  typeEtablissement: string | null
}

export interface TypeEtablissement {
  id: number
  code: string
  libelle: string
}

export interface TypeEtablissementRequete {
  code: string
  libelle: string
}

export interface AnneeScolaire {
  id: number
  libelle: string
  dateDebut: string
  dateFin: string
}

export interface Niveau {
  id: number
  code: string
  libelle: string
  ordre: number
  /** Cycle du niveau ; null tant qu'il n'est pas renseigné. */
  cycleId: number | null
  cycle: string | null
}

export interface Classe {
  id: number
  etablissementId: number
  anneeScolaireId: number
  niveauId: number
  niveauLibelle: string
  nom: string
  capacite: number | null
}

export interface ClasseRequete {
  anneeScolaireId: number
  niveauId: number
  nom: string
  capacite: number | null
}

/** L'année scolaire d'une classe ne se modifie pas. */
export interface ClasseModificationRequete {
  niveauId: number
  nom: string
  capacite: number | null
}

export interface Eleve {
  id: number
  matricule: string
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
}

export type StatutInscription = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE' | 'ANNULEE' | 'TRANSFEREE'

export interface Inscription {
  id: number
  etablissementId: number
  anneeScolaireId: number
  classeId: number | null
  classeNom: string | null
  statut: StatutInscription
  dateDemande: string
  dateDecision: string | null
  eleve: Eleve
}

export interface NouvelEleve {
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
}

/** Soit matriculeEleve (élève déjà connu), soit nouvelEleve : jamais les deux. */
export interface InscriptionRequete {
  anneeScolaireId: number
  matriculeEleve?: string
  nouvelEleve?: NouvelEleve
  confirmerHomonyme?: boolean
}

export interface DecisionRequete {
  decision: 'ACCEPTEE' | 'REFUSEE'
  classeId?: number
}

export interface Matiere {
  id: number
  code: string
  libelle: string
}

export interface Enseignant {
  id: number
  utilisateurId: number
  email: string
  nom: string
  prenom: string
  matricule: string | null
}

export interface EnseignantRequete {
  email: string
  nom: string
  prenom: string
  telephone?: string
  matricule?: string
  motDePasseInitial: string
}

export interface AffectationEnseignant {
  id: number
  classeId: number
  enseignantId: number
  enseignantNom: string
  matiereId: number
  matiereLibelle: string
}

export interface AffectationRequete {
  enseignantId: number
  matiereId: number
}

export interface MonAffectation {
  classeId: number
  classeNom: string
  niveauLibelle: string
  anneeScolaireId: number
  anneeLibelle: string
  etablissementId: number
  matiereId: number
  matiereLibelle: string
}

export type StatutPresence = 'PRESENT' | 'ABSENT' | 'RETARD'

export interface LigneFeuille {
  inscriptionId: number
  eleve: Eleve
  statut: StatutPresence | null
  motif: string | null
  justifie: boolean
}

export interface FeuilleAppel {
  classeId: number
  date: string
  lignes: LigneFeuille[]
}

export interface LignePresenceRequete {
  inscriptionId: number
  statut: StatutPresence
  motif?: string
  justifie?: boolean
}

export interface Periode {
  id: number
  anneeScolaireId: number
  libelle: string
  ordre: number
  dateDebut: string
  dateFin: string
  /** Null : période nationale ; sinon propre à un établissement. */
  etablissementId: number | null
  typePeriode: 'TRIMESTRE' | 'SEMESTRE' | 'AUTRE'
}

export interface Evaluation {
  id: number
  classeId: number
  matiereId: number
  matiere: string
  periodeId: number
  periode: string
  enseignantId: number
  titre: string
  dateEvaluation: string
  coefficient: number
  bareme: number
}

export interface EvaluationRequete {
  matiereId: number
  periodeId: number
  titre: string
  dateEvaluation: string
  coefficient?: number
  bareme?: number
}

/** La matière et la période d'une évaluation ne se modifient pas. */
export interface EvaluationModificationRequete {
  titre: string
  dateEvaluation: string
  coefficient?: number
  bareme?: number
}

export interface LigneNote {
  inscriptionId: number
  eleve: Eleve
  valeur: number | null
  absent: boolean
}

export interface GrilleNotes {
  evaluation: Evaluation
  lignes: LigneNote[]
}

export interface LigneNoteRequete {
  inscriptionId: number
  valeur?: number
  absent?: boolean
}

export interface Enfant {
  eleveId: number
  matricule: string
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
  relation: string
  responsableLegal: boolean
  classeId: number | null
  classeNom: string | null
  etablissementId: number | null
  etablissementNom: string | null
  anneeScolaireId: number | null
  anneeLibelle: string | null
}

/** Profil de l'élève connecté (Étape 1). */
export interface MonProfilEleve {
  eleveId: number
  matricule: string
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
  classeId: number | null
  classeNom: string | null
  etablissementId: number | null
  etablissementNom: string | null
  anneeScolaireId: number | null
  anneeLibelle: string | null
}

export interface LigneBulletin {
  matiereId: number
  matiere: string
  moyenneEleve: number | null
  moyenneClasse: number | null
  /** Poids de la matière dans la moyenne générale (1 par défaut). */
  coefficient: number
}

export interface Bulletin {
  eleve: Eleve
  classeId: number
  classeNom: string
  periodeId: number
  periode: string
  matieres: LigneBulletin[]
  moyenneGenerale: number | null
  moyenneGeneraleClasse: number | null
  rang: number | null
  effectif: number
}

export interface PresenceEleve {
  id: number
  inscriptionId: number
  classeId: number | null
  date: string
  statut: StatutPresence
  motif: string | null
  justifie: boolean
}

export interface LigneResultat {
  inscriptionId: number
  eleve: Eleve
  /** Clés : identifiants de matières (les clés d'un objet JSON sont toujours du texte). */
  moyennesParMatiere: Record<string, number>
  moyenneGenerale: number | null
  rang: number | null
}

export interface ResultatsClasse {
  classeId: number
  periodeId: number
  matieres: Matiere[]
  eleves: LigneResultat[]
  moyennesClasseParMatiere: Record<string, number>
  moyenneGeneraleClasse: number | null
}

export interface LienParental {
  id: number
  eleveId: number
  utilisateurId: number
  email: string
  nom: string
  prenom: string
  relation: string
  responsableLegal: boolean
}

export interface ParentRequete {
  email: string
  nom: string
  prenom: string
  telephone?: string
  relation: string
  responsableLegal?: boolean
  motDePasseInitial?: string
}

export interface EleveRequete {
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
}

// ===================== Étape 2 — emplois du temps =====================

export interface Salle {
  id: number
  etablissementId: number
  nom: string
  capacite: number | null
  typeSalle: string | null
}

export interface SalleRequete {
  nom: string
  capacite?: number
  typeSalle?: string
}

export interface Creneau {
  id: number
  classeId: number
  classeNom: string
  matiereId: number
  matiereLibelle: string
  enseignantId: number | null
  enseignantNom: string | null
  salleId: number | null
  salleNom: string | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
}

export interface CreneauRequete {
  matiereId: number
  enseignantId?: number | null
  salleId?: number | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
}

export interface CreneauEleve {
  matiereLibelle: string
  enseignantNom: string | null
  salleNom: string | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
}

// ===================== Étape 3a — frais et factures =====================

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
  devise: string | null
  totalDu: number
  totalExonere: number
  totalPaye: number
  totalRestant: number
  nombreFactures: number
  nombreImpayees: number
  nombrePayees: number
  nombreEnRetard: number
}

// ===================== Lot 6 — administration nationale =====================

export type StatutEtablissement = 'ACTIF' | 'SUSPENDU' | 'FERME'

export interface Zone {
  id: number
  parentId: number | null
  nom: string
  typeZone: string
}

export interface ZoneRequete {
  parentId: number | null
  nom: string
  typeZone: string
}

export interface EtablissementRequete {
  codeOfficiel: string
  nom: string
  zoneId: number | null
  secteur: Secteur
  cycleIds: number[]
  adresse?: string
  capacite?: number
  typeEtablissementId?: number | null
}

export interface DirectionRequete {
  email: string
  nom: string
  prenom: string
  telephone?: string
  motDePasseInitial: string
}

export interface ComptePersonnel {
  id: number
  email: string
  nom: string
  prenom: string
  role: string
}

/** Un compte vu par son administrateur (jamais le mot de passe). */
export interface Compte {
  id: number
  email: string
  nom: string
  prenom: string
  role: Role
  actif: boolean
  doitChangerMotDePasse: boolean
}

export interface ElementContexte {
  libelle: string
  valeur: string
}

export interface JournalAudit {
  id: number
  date: string
  utilisateurEmail: string | null
  utilisateurNom: string | null
  action: string
  /** Phrase lisible ("Note modifiée : 14,00 → 16,00"). */
  resume: string
  entite: string
  entiteId: number | null
  /** Qui ou quoi est concerné : élève, établissement, classe, facture, compte… */
  contexte: ElementContexte[]
  details: Record<string, unknown> | null
  adresseIp: string | null
}

export interface PageAudit {
  contenu: JournalAudit[]
  total: number
  page: number
  taille: number
}

export interface SyntheseNationale {
  anneeScolaireId: number
  etablissements: number
  actifs: number
  suspendus: number
  fermes: number
  classes: number
  elevesInscrits: number
}

export interface AnneeScolaireRequete {
  libelle: string
  dateDebut: string
  dateFin: string
}

export interface PeriodeRequete {
  libelle: string
  ordre: number
  dateDebut: string
  dateFin: string
}

export interface NiveauRequete {
  code: string
  libelle: string
  ordre: number
  cycleId: number | null
}

export interface MatiereRequete {
  code: string
  libelle: string
}

// ===================== Étape 3b — paiements =====================

export type ModePaiement = 'ESPECES' | 'VIREMENT' | 'CHEQUE' | 'MOBILE_MONEY'
export type StatutPaiement = 'EN_ATTENTE' | 'VALIDE' | 'ECHEC' | 'ANNULE'

export interface Paiement {
  id: number
  factureId: number
  montant: number
  devise: string
  mode: ModePaiement
  statut: StatutPaiement
  referenceInterne: string
  referenceExterne: string | null
  createdAt: string
  valideLe: string | null
  annuleLe: string | null
  motifAnnulation: string | null
}

export interface PaiementRequete {
  montant: number
  mode: 'ESPECES' | 'VIREMENT' | 'CHEQUE'
  reference?: string
}

export interface PaiementInitie {
  reference: string
  urlPaiement: string
  montant: number
  devise: string
}

/** Coefficient d'une matière dans une classe ; `defini` est faux quand c'est la valeur par défaut (1). */
export interface CoefficientMatiere {
  matiereId: number
  code: string
  libelle: string
  coefficient: number
  defini: boolean
}

// ===================== Statistiques des tableaux de bord =====================

export interface Repartition {
  libelle: string
  valeur: number
}

export interface PointMois {
  /** AAAA-MM */
  mois: string
  nombre: number
}

export interface Statistiques {
  anneeScolaireId: number
  etablissements: number
  enseignants: number
  /** Direction et personnel administratif. */
  personnelDirection: number
  classes: number
  elevesInscrits: number
  /** Pourcentage (présents et retards), null quand aucune présence n'a été saisie. */
  tauxPresence: number | null
  joursPresence: number
  repartitionParType: Repartition[]
  repartitionParSecteur: Repartition[]
  effectifsParCycle: Repartition[]
  effectifsParNiveau: Repartition[]
  effectifsParSexe: Repartition[]
  effectifsParZone: Repartition[]
  inscriptionsParMois: PointMois[]
  repartitionParTypeEtablissement: Repartition[]
  effectifsParTypeEtablissement: Repartition[]
}

export interface ReussiteClasse {
  classeId: number
  classe: string
  niveau: string
  evalues: number
  admis: number
  /** Pourcentage, null sans aucun élève noté. */
  taux: number | null
  moyenneClasse: number | null
}

export interface Reussite {
  etablissementId: number
  anneeScolaireId: number
  periodeId: number
  seuil: number
  evalues: number
  admis: number
  taux: number | null
  classes: ReussiteClasse[]
}

export interface GroupeReussite {
  libelle: string
  evalues: number
  admis: number
  /** Pourcentage, null sans aucun élève noté. */
  taux: number | null
}

export interface ReussiteReseau {
  anneeScolaireId: number
  periodeId: number
  seuil: number
  evalues: number
  admis: number
  taux: number | null
  parNiveau: GroupeReussite[]
  parZone: GroupeReussite[]
  /** Triés du meilleur taux au moins bon. */
  etablissements: GroupeReussite[]
}

export interface Contact {
  utilisateurId: number
  nom: string
  role: string
}

export interface MessageConversation {
  id: number
  auteurId: number
  auteur: string
  moi: boolean
  contenu: string
  envoyeLe: string
}

/** Dans les listes, `messages` est vide et `active` vaut false : seul le détail les renseigne. */
export interface Conversation {
  id: number
  eleveId: number
  eleve: string
  sujet: string
  interlocuteur: string
  roleInterlocuteur: string
  dernierMessageLe: string
  apercu: string
  nonLu: boolean
  active: boolean
  messages: MessageConversation[]
}

export interface NouvelleConversationRequete {
  eleveId: number
  destinataireId: number
  sujet: string
  contenu: string
}

export interface ResumeEnfant {
  eleveId: number
  prenom: string
  nom: string
  relation: string
  classeId: number | null
  classeNom: string | null
  periode: string | null
  moyenneGenerale: number | null
  moyenneClasse: number | null
  rang: number | null
  effectif: number
  absencesSemaine: number
  absencesNonJustifiees: number
  retardsSemaine: number
  prochainsDevoirs: { id: number; matiere: string; titre: string; dateLimite: string }[]
  facturesImpayees: number
  facturesEnRetard: number
  /** null quand les factures sont dans des devises différentes (pas de total) */
  resteAPayer: number | null
  devise: string | null
}

export interface Annonce {
  id: number
  classeId: number
  auteur: string
  titre: string
  contenu: string
  publieLe: string
  /** L'utilisateur courant peut la supprimer (auteur ou direction). */
  modifiable: boolean
}

export interface Devoir {
  id: number
  classeId: number
  matiereId: number
  matiere: string
  enseignant: string
  titre: string
  consigne: string | null
  dateLimite: string
  creeLe: string
  /** Renseigné dans la liste d'un élève. */
  fait: boolean | null
  /** Renseignés dans la liste destinée à l'équipe : élèves ayant terminé / élèves de la classe. */
  faits: number | null
  effectif: number | null
}

/** matiereId est requis à la création ; il est ignoré à la modification. */
export interface DevoirRequete {
  matiereId?: number
  titre: string
  consigne?: string
  dateLimite: string
}

/** Les effectifs d'un établissement pour une année (tableau « par établissement »). */
export interface LigneEtablissementStat {
  id: number
  codeOfficiel: string
  nom: string
  secteur: Secteur
  cycles: Cycle[]
  statut: string
  zoneId: number | null
  zone: string | null
  eleves: number
  enseignants: number
  personnelDirection: number
  classes: number
}

/** Les effectifs d'une zone (région, département…), sous-zones comprises. */
export interface LigneZoneStat {
  zoneId: number
  nom: string
  typeZone: string
  parentId: number | null
  etablissements: number
  eleves: number
  enseignants: number
  personnelDirection: number
  classes: number
}

// ---------- Conformité, inspections et sécurité ----------

export type StatutAutorisation = 'EN_ATTENTE' | 'AUTORISE' | 'NON_AUTORISE' | 'RETIRE'

export interface LigneConformite {
  id: number
  codeOfficiel: string
  nom: string
  secteur: Secteur
  cycles: Cycle[]
  statut: string
  zoneId: number | null
  zone: string | null
  autorisation: StatutAutorisation
  reference: string | null
  motifStatut: string | null
  derniereInspection: string | null
  resultatInspection: 'CONFORME' | 'NON_CONFORME' | null
  inspecte: boolean
  nonConforme: boolean
  signalementsOuverts: number
  signalementsGraves: number
  derniereDonnee: string | null
  sansDonnees: boolean
  urgent: boolean
  score: number
  motifsUrgence: string[]
}

export interface IndicateursConformite {
  total: number
  publics: number
  prives: number
  autorises: number
  nonAutorises: number
  autorisationEnAttente: number
  nonConformes: number
  inspectes: number
  avecProblemeSecurite: number
  sansDonnees: number
  fermes: number
  urgents: number
}

export interface Conformite {
  indicateurs: IndicateursConformite
  lignes: LigneConformite[]
}

/** Filtres du rapport ; les valeurs vides signifient « tous ». */
export interface FiltresConformite {
  zoneId: string
  statut: string
  secteur: string
  cycleId: string
}

export interface Inspection {
  id: number
  dateInspection: string
  resultat: 'CONFORME' | 'NON_CONFORME'
  observations: string | null
  inspecteur: string
}

export interface InspectionRequete {
  dateInspection: string
  resultat: 'CONFORME' | 'NON_CONFORME'
  observations: string | null
}

export type CategorieSecurite = 'INCENDIE' | 'BATIMENT' | 'EAU_HYGIENE' | 'CLOTURE' | 'AUTRE'
export type GraviteSecurite = 'FAIBLE' | 'MOYENNE' | 'ELEVEE'

export interface Signalement {
  id: number
  categorie: CategorieSecurite
  gravite: GraviteSecurite
  description: string
  statut: 'OUVERT' | 'RESOLU'
  signalePar: string
  createdAt: string
  resoluAt: string | null
}

export interface SignalementRequete {
  categorie: CategorieSecurite
  gravite: GraviteSecurite
  description: string
}

export interface AutorisationRequete {
  statut: StatutAutorisation
  reference: string | null
  dateAutorisation: string | null
  motif: string | null
}

// ---------- Examens nationaux ----------

export interface Examen {
  id: number
  libelle: string
  anneeScolaireId: number
  cycleId: number | null
  cycle: string | null
  niveauId: number | null
  niveau: string | null
  /** NATIONAL ou un type de zone (COMMUNE, PREFECTURE, REGION…) : qui organise l'examen. */
  niveauOrganisation: string
  /** Période officielle des épreuves ; null tant qu'elle n'est pas publiée. */
  dateDebut: string | null
  dateFin: string | null
  /** Types d'établissement participants ; vide : tous les types. */
  typesEtablissementIds: number[]
  typesEtablissement: string[]
  statutResultats: 'EN_COURS' | 'DELIBERE' | 'PROCLAME'
  dateDeliberation: string | null
  dateProclamation: string | null
}

export interface MembreJury {
  id: number
  ordre: number
  fonction: string
  nom: string | null
  present: boolean
}

export interface Jury {
  examenId: number
  statut: 'EN_COURS' | 'DELIBERE' | 'PROCLAME'
  dateDeliberation: string | null
  dateProclamation: string | null
  membres: MembreJury[]
  presents: number
  quorumRequis: number
  quorumAtteint: boolean
  peutProclamer: boolean
}

export interface ExamenRequete {
  libelle: string
  anneeScolaireId: number
  cycleId: number | null
  niveauId?: number | null
  niveauOrganisation?: string | null
  dateDebut?: string | null
  dateFin?: string | null
  typesEtablissementIds?: number[]
}

export interface TypeExamen {
  id: number
  code: string
  libelle: string
  niveauOrganisation: string
  niveauId: number | null
  niveau: string | null
  cycleId: number | null
  cycle: string | null
}

export interface TypeExamenRequete {
  code: string
  libelle: string
  niveauOrganisation: string
  niveauId: number | null
  cycleId: number | null
}

export type SerieBac = 'SCIENCES_MATHEMATIQUES' | 'SCIENCES_SOCIALES' | 'SCIENCES_EXPERIMENTALES'

export interface ResultatSerie {
  serie: SerieBac
  presentes: number
  admis: number
}

export interface ResultatExamenRequete {
  presentes: number
  admis: number
  absents?: number | null
  elimines?: number | null
  presentesFilles?: number | null
  admisFilles?: number | null
  series?: ResultatSerie[] | null
}

export interface SyntheseExamen {
  libelle: string
  etablissements: number
  transmis: number
  presentes: number
  admis: number
  taux: number | null
}

export interface LigneExamen {
  id: number
  codeOfficiel: string
  nom: string
  secteur: Secteur
  zoneId: number | null
  zone: string | null
  presentes: number | null
  admis: number | null
  taux: number | null
  /** Zone qui organise l'examen pour cet établissement (null si examen national). */
  organisateur: string | null
  absents: number | null
  elimines: number | null
  presentesFilles: number | null
  admisFilles: number | null
  series: ResultatSerie[]
}

export interface RepartitionExamen {
  libelle: string
  presentes: number
  admis: number
  taux: number | null
}

export interface StatistiquesExamen {
  absents: number
  elimines: number
  parSexe: RepartitionExamen[]
  parRegion: SyntheseExamen[]
  parPrefecture: SyntheseExamen[]
  parSerie: RepartitionExamen[]
}

export interface RapportExamen {
  examen: Examen
  synthese: SyntheseExamen
  parSecteur: SyntheseExamen[]
  lignes: LigneExamen[]
  statistiques: StatistiquesExamen | null
}

export interface PointAnnee {
  anneeScolaireId: number
  libelle: string
  eleves: number
  classes: number
  presentesExamens: number
  admisExamens: number
  tauxExamens: number | null
}

export interface Evolution {
  annees: PointAnnee[]
}

// ---------- Postes d'enseignants et mutations ----------

export interface LignePostes {
  id: number
  codeOfficiel: string
  nom: string
  secteur: Secteur
  zoneId: number | null
  zone: string | null
  eleves: number
  enseignants: number
  postes: number | null
  vacants: number | null
  enSurnombre: number
  besoinEstime: number
  elevesParEnseignant: number | null
}

export interface RapportPostes {
  normeElevesParEnseignant: number
  totaux: {
    etablissements: number
    eleves: number
    enseignants: number
    postes: number
    vacants: number
    besoinEstime: number
    enSurnombre: number
  }
  lignes: LignePostes[]
}

export interface MutationRequete {
  depuisEtablissementId: number
  versEtablissementId: number
}

export interface CompteInstitutionnel {
  id: number
  email: string
  nom: string
  prenom: string
  telephone: string | null
  role: 'MINISTERE' | 'DIRECTION_REGIONALE'
  zoneId: number | null
  zone: string | null
  actif: boolean
}

export type StatutPlanAction = 'A_FAIRE' | 'EN_COURS' | 'TERMINE' | 'ANNULE'

export interface PlanAction {
  id: number
  etablissementId: number
  etablissement: string
  zone: string | null
  description: string
  echeance: string
  statut: StatutPlanAction
  enRetard: boolean
  note: string | null
  creePar: string
  createdAt: string
  termineLe: string | null
}

export interface PlanActionRequete {
  description: string
  echeance: string
  inspectionId?: number | null
}

export interface AlerteEnvoyee {
  id: number
  envoyeeLe: string
  type: 'URGENCE' | 'PLAN_ACTION' | 'RESULTATS_EXAMEN' | 'ABSENCE'
  canal: 'EMAIL' | 'SMS'
  destinataire: string
  resume: string
  reussie: boolean
}

export interface EntreeCahierTexte {
  id: number
  classeId: number
  matiereId: number
  matiere: string
  enseignant: string
  dateSeance: string
  contenu: string
}

export type StatutJustification = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE'

export interface Justification {
  id: number
  presenceId: number
  eleveId: number
  eleve: string
  classe: string | null
  dateAbsence: string
  typePresence: string
  motif: string
  statut: StatutJustification
  reponse: string | null
  soumiseLe: string
}

export interface EnseignantDeLEleve {
  enseignantId: number
  nom: string
  matieres: string[]
}

export type StatutRendezVous = 'DEMANDE' | 'CONFIRME' | 'REFUSE' | 'ANNULE'

export interface RendezVous {
  id: number
  eleveId: number
  eleve: string
  enseignantId: number
  enseignant: string
  dateHeure: string
  motif: string
  statut: StatutRendezVous
  reponse: string | null
}

export type StatutSortie = 'DEMANDEE' | 'ACCEPTEE' | 'REFUSEE'

export interface Sortie {
  id: number
  eleveId: number
  eleve: string
  dateSortie: string
  heureSortie: string
  motif: string
  accompagnateur: string | null
  statut: StatutSortie
  reponse: string | null
}

export interface ExamenEtablissement {
  examen: Examen
  candidats: number
}

export interface Candidat {
  id: number
  eleveId: number
  nom: string
  matricule: string
  classe: string | null
  numero: string | null
  centreId: number | null
  centre: string | null
  numeroPv: string | null
  salle: number | null
  table: number | null
  libre: boolean
}

export interface RepartitionCentre {
  centreId: number
  centre: string
  candidats: number
  salles: number
  surveillants: number
  capacite: number | null
  repartie: boolean
  plan: { numero: number; places: { table: number; numeroPv: string; nom: string; matricule: string; etablissement: string }[] }[]
}

export interface ElevePossible {
  eleveId: number
  nom: string
  matricule: string
  classe: string
  tauxAbsences: number | null
  eligible: boolean
  motif: string | null
}

export interface PreparationExamen {
  examen: Examen
  candidats: Candidat[]
  possibles: ElevePossible[]
}

export interface CentreExamen {
  id: number
  nom: string
  adresse: string | null
  zoneId: number | null
  zone: string | null
  dateDebut: string | null
  etablissementId: number | null
}

export interface Epreuve {
  id: number
  examenId: number
  matiereId: number | null
  matiere: string
  date: string
  heureDebut: string
  heureFin: string
  coefficient: number
}

export interface EpreuveRequete {
  matiereId: number | null
  libelle: string | null
  date: string
  heureDebut: string
  heureFin: string
  coefficient?: number
}

export type StatutCandidat = 'INSCRIT' | 'PRESENT' | 'ABSENT' | 'ELIMINE'

export type TypeIncident = 'FRAUDE' | 'RETARD' | 'MALAISE' | 'PERTURBATION' | 'FUITE_SUJET' | 'AUTRE'

export interface IncidentsCentre {
  centreId: number
  centre: string
  candidats: { id: number; nom: string; numeroPv: string | null }[]
  epreuves: { id: number; nom: string }[]
  incidents: {
    id: number
    type: TypeIncident
    description: string
    survenuLe: string
    signalePar: string | null
    statut: 'OUVERT' | 'TRAITE'
    decision: string | null
    elimine: boolean
    candidatId: number | null
    candidat: string | null
    numeroPv: string | null
    epreuveId: number | null
    epreuve: string | null
  }[]
  parType: Record<string, number>
}

export interface CorrectionExamen {
  examenId: number
  statutResultats: string
  niveau: string
  epreuves: { id: number; nom: string; sousCommission: boolean }[]
  commissions: {
    id: number
    epreuveId: number | null
    nom: string
    lieu: string | null
    nbCopies: number
    nbCopiesCorrigees: number
    cloturee: boolean
    complete: boolean
    manques: string[]
    membres: { id: number; role: 'PRESIDENT' | 'SECRETAIRE' | 'CORRECTEUR'; nom: string; telephone: string | null }[]
  }[]
  nbCopies: number
  nbCopiesCorrigees: number
}

export interface DossiersCandidatures {
  examenId: number
  examen: string
  pieces: { type: string; libelle: string; requise: boolean }[]
  candidats: {
    id: number
    nom: string
    numero: string | null
    classe: string | null
    pieces: Record<string, { reference: string | null; fournieLe: string }>
    complet: boolean
    manques: string[]
    evaluationsManquees: number
    libre: boolean
  }[]
  complets: number
}

export interface ListeCandidats {
  statut: 'BROUILLON' | 'TRANSMISE' | 'VALIDEE_REGION' | 'VALIDEE'
  candidats: number
  dossiersComplets: number
  transmiseLe: string | null
  valideeRegionLe: string | null
  valideeLe: string | null
  motifRejet: string | null
}

export interface BanqueSujets {
  examenId: number
  epreuves: {
    id: number
    nom: string
    date: string
    variantes: {
      id: number
      variante: 'A' | 'B' | 'C'
      intitule: string
      auteurs: string | null
      statut: 'ELABORATION' | 'VALIDE' | 'TIRE' | 'COMPROMIS'
      valideLe: string | null
      tireLe: string | null
    }[]
    validees: number
    tirable: boolean
    varianteTiree: string | null
  }[]
}

export interface DiplomesExamen {
  examenId: number
  statutResultats: string
  admis: number
  delivres: number
  revoques: number
  diplomes: {
    id: number
    numero: string
    titulaire: string
    etablissement: string
    mention: 'PASSABLE' | 'ASSEZ_BIEN' | 'BIEN' | 'TRES_BIEN'
    delivreLe: string
    revoque: boolean
    motifRevocation: string | null
  }[]
}

export interface VerificationDiplome {
  statut: 'AUTHENTIQUE' | 'REVOQUE' | 'INCONNU'
  titulaire: string | null
  examen: string | null
  anneeScolaire: string | null
  mention: string | null
  numero: string | null
  delivreLe: string | null
  motifRevocation: string | null
}

export type EtatPli = 'A_CONDITIONNER' | 'CONDITIONNE' | 'EXPEDIE_IRE' | 'RECU_DPE' | 'RECU_PRESIDENT' | 'OUVERT'

export interface DeroulementCentre {
  centreId: number
  centre: string
  candidats: number
  epreuves: {
    epreuveId: number
    nom: string
    date: string
    heureDebut: string
    heureFin: string
    nbExemplaires: number
    statutPli: EtatPli
    etapeSuivante: EtatPli | null
    conditionneLe: string | null
    expedieLe: string | null
    recuDpeLe: string | null
    recuPresidentLe: string | null
    ouvertLe: string | null
    ouvertPar: string | null
    temoin1: string | null
    temoin2: string | null
    compositionDebut: string | null
    compositionFin: string | null
    presents: number | null
    absents: number | null
    elimines: number | null
    copiesRamassees: number | null
    observations: string | null
    rapportClos: boolean
    ecartCopies: number | null
  }[]
}

export type FonctionCentre = 'PRESIDENT' | 'CHEF_CENTRE' | 'SECRETAIRE' | 'SURVEILLANT' | 'CHEF_SALLE' | 'SECURITE' | 'SANTE'

export interface PersonnelCentre {
  centreId: number
  centre: string
  salles: number
  personnel: { id: number; role: FonctionCentre; nom: string; telephone: string | null; salle: number | null }[]
  besoins: { libelle: string; requis: number; affectes: number }[]
  complet: boolean
}

export interface NotesCandidat {
  id: number
  nom: string
  sexe: string | null
  numeroPv: string | null
  statut: StatutCandidat
  notes: Record<string, number>
  moyenne: number | null
  decision: 'ADMIS' | 'NON_ADMIS' | null
}

export interface NotesCandidats {
  examenId: number
  statutResultats: 'EN_COURS' | 'DELIBERE' | 'PROCLAME'
  epreuves: { id: number; nom: string; coefficient: number }[]
  candidats: NotesCandidat[]
}

export interface AffectationAutomatique {
  candidatsAffectes: number
  etablissementsAffectes: number
  sansCentre: string[]
}

export interface CentreExamenRequete {
  /** Facultatif quand [etablissementId] est donné : le centre prend le nom de l'établissement. */
  nom: string | null
  adresse: string | null
  zoneId: number | null
  dateDebut: string | null
  etablissementId?: number | null
}

export interface EleveListe {
  inscriptionId: number
  eleveId: number
  matricule: string
  nom: string
  prenom: string
  sexe: string | null
  dateNaissance: string | null
  statut: string
  etablissementId: number
  etablissement: string
  secteur: string
  typeEtablissement: string | null
  zone: string | null
  classe: string | null
  niveau: string | null
  cycle: string | null
}

export interface ElevesPage {
  elements: EleveListe[]
  total: number
  page: number
  taille: number
  totalPages: number
}

export interface FiltresEleves {
  anneeScolaireId: number
  q?: string
  zoneId?: string | null
  etablissementId?: string | null
  secteur?: string | null
  typeEtablissementId?: string | null
  cycleId?: string | null
  niveauId?: string | null
  sexe?: string | null
  statut?: string | null
}

export interface CriteresCentre {
  /** TOUS ou PUBLIC. */
  secteurAdmis: string
  autorisationRequise: boolean
  sallesMinimum: number
  capaciteMinimum: number
}

export interface EtablissementCentre {
  id: number
  codeOfficiel: string
  nom: string
  zoneId: number | null
  zone: string | null
  secteur: string
  typeEtablissement: string | null
  salles: number
  capacite: number
  eligible: boolean
  motifs: string[]
}
