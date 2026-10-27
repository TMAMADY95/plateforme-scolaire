// Client HTTP minimal : ajoute le jeton, convertit les erreurs de l'API en ApiError
// avec un message lisible, et prévient l'application quand la session n'est plus valide.

const BASE = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  readonly statut: number

  constructor(statut: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.statut = statut
  }
}

interface Rappels {
  /** Le serveur a refusé le jeton (expiré, révoqué, compte verrouillé). */
  nonAutorise?: () => void
  /** Le serveur exige le changement du mot de passe initial. */
  motDePasseRequis?: () => void
}

interface Options {
  /** Pour les appels sans jeton (connexion). */
  sansJeton?: boolean
}

interface CorpsErreur {
  detail?: string
  message?: string
  erreur?: string
}

let jetonCourant: string | null = null
const rappels: Rappels = {}

export function definirJeton(jeton: string | null): void {
  jetonCourant = jeton
}

export function enregistrerRappels(nouveaux: Rappels): void {
  Object.assign(rappels, nouveaux)
}

function lireJson(texte: string): unknown {
  try {
    return JSON.parse(texte)
  } catch {
    return null
  }
}

function enCorpsErreur(valeur: unknown): CorpsErreur {
  return typeof valeur === 'object' && valeur !== null ? (valeur as CorpsErreur) : {}
}

function messageParDefaut(statut: number): string {
  switch (statut) {
    case 401:
      return 'Session expirée, veuillez vous reconnecter.'
    case 403:
      return "Vous n'avez pas le droit d'effectuer cette action."
    case 404:
      return 'Élément introuvable.'
    default:
      return `Une erreur est survenue (code ${statut}).`
  }
}

async function requete<T>(
  methode: string,
  chemin: string,
  corps?: unknown,
  options: Options = {},
): Promise<T> {
  const entetes: Record<string, string> = { Accept: 'application/json' }
  if (corps !== undefined) entetes['Content-Type'] = 'application/json'
  const avecJeton = jetonCourant !== null && !options.sansJeton
  if (avecJeton) entetes.Authorization = `Bearer ${jetonCourant}`

  let reponse: Response
  try {
    reponse = await fetch(`${BASE}${chemin}`, {
      method: methode,
      headers: entetes,
      body: corps !== undefined ? JSON.stringify(corps) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Le serveur est injoignable. Vérifiez votre connexion.')
  }

  if (reponse.ok) {
    if (reponse.status === 204) return undefined as T
    const texte = await reponse.text()
    return (texte ? lireJson(texte) : undefined) as T
  }

  const erreur = enCorpsErreur(lireJson(await reponse.text()))
  if (reponse.status === 401 && avecJeton) rappels.nonAutorise?.()
  if (reponse.status === 403 && erreur.erreur === 'CHANGEMENT_MOT_DE_PASSE_REQUIS') {
    rappels.motDePasseRequis?.()
  }
  throw new ApiError(reponse.status, erreur.detail ?? erreur.message ?? messageParDefaut(reponse.status))
}

export const api = {
  get: <T>(chemin: string) => requete<T>('GET', chemin),
  post: <T>(chemin: string, corps?: unknown, options?: Options) => requete<T>('POST', chemin, corps, options),
  put: <T>(chemin: string, corps?: unknown) => requete<T>('PUT', chemin, corps),
}
