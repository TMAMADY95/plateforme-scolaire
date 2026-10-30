/**
 * Détails d'une entrée du journal d'audit, présentés pour une lecture humaine : libellés en français, valeurs
 * mises en forme, et tout ce qui est purement technique (identifiants internes, structures) est laissé de côté.
 */

export interface LigneDetail {
  libelle: string
  valeur: string
}

const LIBELLES: Record<string, string> = {
  nom: 'Nom',
  email: 'E-mail',
  montant: 'Montant',
  mode: 'Mode de paiement',
  facture: 'Facture',
  numero: 'Numéro',
  reference: 'Référence du paiement',
  motif: 'Motif',
  libelle: 'Libellé',
  code: 'Code',
  type: 'Type',
  date: 'Date',
  attendu: 'Montant attendu',
  recu: 'Montant reçu',
  montantDu: 'Montant dû',
  ancienMontant: 'Ancien montant',
  nouveauMontant: 'Nouveau montant',
  statutAvant: 'Statut avant',
  avant: 'Avant',
  apres: 'Après',
  presents: 'Présents',
  absents: 'Absents',
  retards: 'Retards',
  corrigees: 'Présences corrigées',
  creees: 'Enregistrées',
  ignorees: 'Ignorées',
}

/** Libellés propres à certaines actions (une même clé n'a pas partout le même sens). */
const LIBELLES_PAR_ACTION: Record<string, Record<string, string>> = {
  GENERATION_FACTURES: { creees: 'Factures créées', ignorees: 'Factures déjà existantes (ignorées)' },
  SAISIE_APPEL: { creees: 'Présences enregistrées' },
}

function formaterValeur(valeur: unknown): string | null {
  if (valeur === null || valeur === undefined || valeur === '') return null
  if (typeof valeur === 'boolean') return valeur ? 'Oui' : 'Non'
  if (typeof valeur === 'number' || (typeof valeur === 'string' && /^-?\d+(\.\d+)?$/.test(valeur))) {
    const n = Number(valeur)
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(n)
  }
  if (typeof valeur === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(valeur)) return valeur.split('-').reverse().join('/')
    if (/^[A-Z][A-Z_]+$/.test(valeur)) {
      const texte = valeur.replace(/_/g, ' ').toLowerCase()
      return texte.charAt(0).toUpperCase() + texte.slice(1)
    }
    return valeur
  }
  return null // objets et listes : trop techniques pour un affichage simple
}

/** Les lignes à montrer ; liste vide quand il n'y a rien d'utile au-delà du résumé de l'entrée. */
export function lignesDetails(action: string, details: Record<string, unknown> | null): LigneDetail[] {
  if (!details) return []
  const lignes: LigneDetail[] = []
  for (const [cle, brut] of Object.entries(details)) {
    const libelle = LIBELLES_PAR_ACTION[action]?.[cle] ?? LIBELLES[cle]
    if (!libelle) continue // clé technique (identifiants, structures internes) : non affichée
    const valeur = formaterValeur(brut)
    if (valeur !== null) lignes.push({ libelle, valeur })
  }
  return lignes
}
