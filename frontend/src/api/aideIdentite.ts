// Libelles et controles de forme de l'identite nationale (INE, etat civil).
// Les types de donnees (IdentiteNationale, EtatCivilRequete) sont dans api/types.ts.

export const ACTES_NAISSANCE = [
  { value: 'EXTRAIT', label: "Extrait d'acte de naissance" },
  { value: 'JUGEMENT_SUPPLETIF', label: 'Jugement suppletif' },
  { value: 'AUCUN', label: 'Aucun acte (a regulariser)' },
] as const

/** Libelle lisible d'un type d'acte ; « Non renseigne » quand rien n'est saisi. */
export function libelleActe(type: string | null | undefined): string {
  return ACTES_NAISSANCE.find((a) => a.value === type)?.label ?? 'Non renseigne'
}

/**
 * Verifie la forme d'un INE avant l'appel au serveur : GN suivi de 12 chiffres.
 * La cle de controle est verifiee par le serveur, qui reste seul juge.
 */
export function inePlausible(saisie: string): boolean {
  return /^GN\d{12}$/.test(saisie.toUpperCase().replace(/[\s-]/g, ''))
}
