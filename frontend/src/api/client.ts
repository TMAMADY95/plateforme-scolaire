// Client HTTP minimal : joint le cookie de session, convertit les erreurs de l'API en ApiError
// avec un message lisible, et prévient l'application quand la session n'est plus valide.

const BASE = import.meta.env.VITE_API_URL ?? ''

/** Exigé par le serveur sur les écritures authentifiées par cookie (protection contre les requêtes forgées). */
const ENTETE_APPLICATION = { 'X-Requested-With': 'EduManage' }

export class ApiError extends Error {
  readonly statut: number

  constructor(statut: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.statut = statut
  }
}

interface Rappels {
  /** Le serveur a refusé la session (expirée, révoquée, compte verrouillé). */
  nonAutorise?: () => void
  /** Le serveur exige le changement du mot de passe initial. */
  motDePasseRequis?: () => void
}

interface Options {
  /** Pour les routes publiques (connexion) : un refus n'y signifie pas que la session a expiré. */
  appelPublic?: boolean
}

interface CorpsErreur {
  detail?: string
  message?: string
  erreur?: string
}

const rappels: Rappels = {}

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
  const entetes: Record<string, string> = { Accept: 'application/json', ...ENTETE_APPLICATION }
  if (corps !== undefined) entetes['Content-Type'] = 'application/json'

  let reponse: Response
  try {
    reponse = await fetch(`${BASE}${chemin}`, {
      method: methode,
      credentials: 'include',
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
  if (reponse.status === 401 && !options.appelPublic) rappels.nonAutorise?.()
  if (reponse.status === 403 && erreur.erreur === 'CHANGEMENT_MOT_DE_PASSE_REQUIS') {
    rappels.motDePasseRequis?.()
  }
  throw new ApiError(reponse.status, erreur.detail ?? erreur.message ?? messageParDefaut(reponse.status))
}

/** Télécharge un fichier protégé et l'enregistre sous `nomFichier` (les erreurs restent ainsi affichées dans la page). */
export async function telecharger(chemin: string, nomFichier: string): Promise<void> {
  let reponse: Response
  try {
    reponse = await fetch(`${BASE}${chemin}`, { credentials: 'include', headers: ENTETE_APPLICATION })
  } catch {
    throw new ApiError(0, 'Le serveur est injoignable. Vérifiez votre connexion.')
  }
  if (!reponse.ok) {
    const erreur = enCorpsErreur(lireJson(await reponse.text()))
    if (reponse.status === 401) rappels.nonAutorise?.()
    throw new ApiError(reponse.status, erreur.detail ?? erreur.message ?? messageParDefaut(reponse.status))
  }
  const url = URL.createObjectURL(await reponse.blob())
  const lien = document.createElement('a')
  lien.href = url
  lien.download = nomFichier
  document.body.appendChild(lien)
  lien.click()
  lien.remove()
  URL.revokeObjectURL(url)
}

/** Envoie un fichier (multipart) ; le navigateur fixe lui-même l'en-tête Content-Type avec sa limite. */
export async function envoyerFichier<T>(chemin: string, fichier: File): Promise<T> {
  const entetes: Record<string, string> = { Accept: 'application/json', ...ENTETE_APPLICATION }
  const formulaire = new FormData()
  formulaire.append('fichier', fichier)
  let reponse: Response
  try {
    reponse = await fetch(`${BASE}${chemin}`, { method: 'POST', credentials: 'include', headers: entetes, body: formulaire })
  } catch {
    throw new ApiError(0, 'Le serveur est injoignable. Vérifiez votre connexion.')
  }
  const texte = await reponse.text()
  if (reponse.ok) return (texte ? lireJson(texte) : undefined) as T
  if (reponse.status === 413) throw new ApiError(413, 'Fichier trop volumineux (5 Mo au plus).')
  const erreur = enCorpsErreur(lireJson(texte))
  if (reponse.status === 401) rappels.nonAutorise?.()
  throw new ApiError(reponse.status, erreur.detail ?? erreur.message ?? messageParDefaut(reponse.status))
}

export const api = {
  get: <T>(chemin: string, options?: Options) => requete<T>('GET', chemin, undefined, options),
  post: <T>(chemin: string, corps?: unknown, options?: Options) => requete<T>('POST', chemin, corps, options),
  put: <T>(chemin: string, corps?: unknown) => requete<T>('PUT', chemin, corps),
  delete: <T>(chemin: string) => requete<T>('DELETE', chemin),
}
