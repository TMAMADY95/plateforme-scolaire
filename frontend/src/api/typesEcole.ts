// Libellés et contrôles de forme de l'identité nationale (INE, état civil).
// Les types de données (IdentiteNationale, EtatCivilRequete) sont dans api/types.ts,
// avec les autres DTO de l'API.

export const ACTES_NAISSANCE = [
  { value: 'EXTRAIT', label: "Extrait d'acte de naissance" },
  { value: 'JUGEMENT_SUPPLETIF', label: 'Jugement supplétif' },
  { value: 'AUCUN', label: 'Aucun acte (à régulariser)' },
] as const

/** Libellé lisible d'un type d'acte ; « Non renseigné » quand rien n'est saisi. */
export function libelleActe(type: string | null): string {
  return ACTES_NAISSANCE.find((a) => a.value === type)?.label ?? 'Non renseigné'
}

/**
 * Vérifie la forme d'un INE avant l'appel au serveur : GN suivi de 12 chiffres.
 * La clé de contrôle est vérifiée par le serveur, qui reste seul juge.
 */
export function inePlausible(saisie: string): boolean {
  return /^GN\d{12}$/.test(saisie.toUpperCase().replace(/[\s-]/g, ''))
}
