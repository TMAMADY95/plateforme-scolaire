/** Rend un nom de colonne comparable : sans accents, ni majuscules, ni espaces ou ponctuation. */
export const cleColonne = (texte: string) =>
  texte
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '')

export interface FichierCsv {
  /** Les lignes, indexées par nom de colonne normalisé (voir cleColonne). */
  lignes: Record<string, string>[]
  entetes: string[]
}

/**
 * Lit un fichier CSV exporté d'Excel : séparateur « ; », « , » ou tabulation détecté d'après la première ligne,
 * guillemets, BOM et lignes vides gérés. La première ligne contient les noms de colonnes.
 */
export function lireCsv(texteBrut: string): FichierCsv {
  const texte = texteBrut.replace(/^﻿/, '')
  const premiere = texte.split(/\r?\n/, 1)[0] ?? ''
  const separateur = [';', '\t', ','].reduce((meilleur, s) =>
    premiere.split(s).length > premiere.split(meilleur).length ? s : meilleur,
  )

  const lignes: string[][] = []
  let ligne: string[] = []
  let champ = ''
  let entreGuillemets = false
  for (let i = 0; i < texte.length; i++) {
    const c = texte[i]
    if (entreGuillemets) {
      if (c === '"' && texte[i + 1] === '"') {
        champ += '"'
        i++
      } else if (c === '"') {
        entreGuillemets = false
      } else {
        champ += c
      }
    } else if (c === '"') {
      entreGuillemets = true
    } else if (c === separateur) {
      ligne.push(champ)
      champ = ''
    } else if (c === '\n' || c === '\r') {
      if (c === '\r' && texte[i + 1] === '\n') i++
      ligne.push(champ)
      champ = ''
      lignes.push(ligne)
      ligne = []
    } else {
      champ += c
    }
  }
  if (champ !== '' || ligne.length > 0) {
    ligne.push(champ)
    lignes.push(ligne)
  }

  const nonVides = lignes.filter((l) => l.some((v) => v.trim() !== ''))
  if (nonVides.length === 0) return { lignes: [], entetes: [] }
  const entetes = nonVides[0].map((e) => e.trim())
  const cles = entetes.map(cleColonne)
  return {
    entetes,
    lignes: nonVides.slice(1).map((valeurs) => {
      const objet: Record<string, string> = {}
      cles.forEach((cle, i) => {
        if (cle !== '') objet[cle] = (valeurs[i] ?? '').trim()
      })
      return objet
    }),
  }
}

/** La première valeur non vide parmi les colonnes possibles (noms déjà normalisés). */
export const champ = (ligne: Record<string, string>, ...noms: string[]) => {
  for (const nom of noms) {
    const v = ligne[nom]
    if (v !== undefined && v !== '') return v
  }
  return ''
}
