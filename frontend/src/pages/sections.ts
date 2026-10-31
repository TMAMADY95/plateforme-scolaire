import type { NomIcone } from '../components/Icone'

export interface Section {
  valeur: string
  libelle: string
  icone: NomIcone
}

/**
 * Sections de l'administration affichées dans le menu de gauche (sous « Administration nationale » ou « Ma région »).
 * Les autres vues restent en onglets horizontaux dans la page. La section ouverte est portée par l'adresse
 * (?onglet=…) : on peut recharger la page ou partager un lien.
 */
export const SECTIONS_MINISTERE: Section[] = [
  { valeur: 'etablissements', libelle: 'Établissements', icone: 'ecole' },
  { valeur: 'eleves', libelle: 'Élèves', icone: 'eleves' },
  { valeur: 'postes', libelle: 'Enseignants', icone: 'enseignants' },
  { valeur: 'referentiels', libelle: 'Référentiels', icone: 'classes' },
  { valeur: 'comptes', libelle: 'Comptes', icone: 'enfants' },
  { valeur: 'audit', libelle: "Journal d'audit", icone: 'horaires' },
]

export const SECTIONS_REGION: Section[] = [
  { valeur: 'etablissements', libelle: 'Établissements de la zone', icone: 'ecole' },
  { valeur: 'postes', libelle: 'Enseignants', icone: 'enseignants' },
]
