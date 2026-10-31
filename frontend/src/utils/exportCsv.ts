export interface ColonneCsv<T> {
  titre: string
  valeur: (ligne: T) => string | number | null | undefined
}

const echapper = (v: string | number | null | undefined) => {
  const texte = v === null || v === undefined ? '' : String(v)
  // Neutralise les formules (=, +, -, @) pour qu'un tableur ne les exécute pas.
  const sur = /^[=+\-@]/.test(texte) && Number.isNaN(Number(texte)) ? `'${texte}` : texte
  return /[;"\n\r]/.test(sur) ? `"${sur.replace(/"/g, '""')}"` : sur
}

/** Télécharge un tableau en CSV (séparateur « ; », UTF-8 avec BOM) : s'ouvre directement dans Excel en français. */
export function exporterCsv<T>(nomFichier: string, colonnes: ColonneCsv<T>[], lignes: T[]) {
  const corps = [colonnes.map((c) => echapper(c.titre)).join(';'), ...lignes.map((l) => colonnes.map((c) => echapper(c.valeur(l))).join(';'))]
  const blob = new Blob(['﻿', corps.join('\r\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${nomFichier}-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
