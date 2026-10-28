// Types miroirs des objets renvoyés par l'API Spring (paquet web.dto du backend).

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

export interface Etablissement {
  id: number
  codeOfficiel: string
  nom: string
  typeEtablissement: string | null
  statut: string
  zoneId: number | null
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
