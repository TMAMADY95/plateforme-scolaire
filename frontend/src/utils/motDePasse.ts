const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'

/** Mot de passe aléatoire (sans caractères ambigus comme 0/O ou l/1), tiré avec le générateur sécurisé du navigateur. */
export function genererMotDePasse(longueur = 14): string {
  const tirages = new Uint32Array(longueur)
  crypto.getRandomValues(tirages)
  return Array.from(tirages, (n) => ALPHABET[n % ALPHABET.length]).join('') + '!'
}
