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
