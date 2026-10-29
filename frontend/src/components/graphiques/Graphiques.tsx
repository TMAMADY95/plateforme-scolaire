import { Group, Stack, Text } from '@mantine/core'

/** Graphiques en SVG, sans bibliothèque : barres, courbe et anneau. */

export interface Point {
  libelle: string
  valeur: number
}

export const PALETTE = ['#1f5bcc', '#12b886', '#f59f00', '#be4bdb', '#fa5252', '#15aabf', '#82c91e', '#868e96']

const COULEUR_PRINCIPALE = '#1f5bcc'
const COULEUR_TEXTE = '#667085'
const COULEUR_GRILLE = '#e4e9f2'

function nombre(valeur: number): string {
  return new Intl.NumberFormat('fr-FR').format(valeur)
}

/** Arrondit le maximum à une valeur « ronde » pour l'échelle verticale. */
export function maximumRond(max: number): number {
  if (max <= 5) return 5
  const puissance = 10 ** Math.floor(Math.log10(max))
  const unites = max / puissance
  const arrondi = unites <= 1 ? 1 : unites <= 2 ? 2 : unites <= 5 ? 5 : 10
  return arrondi * puissance
}

/** « 2026-09 » -> « sept. 26 ». */
export function libelleMois(mois: string): string {
  const [annee, m] = mois.split('-').map(Number)
  if (!annee || !m) return mois
  const court = new Date(annee, m - 1, 1).toLocaleDateString('fr-FR', { month: 'short' })
  return `${court} ${String(annee).slice(2)}`
}

function Vide({ texte }: { texte: string }) {
  return (
    <Text c="dimmed" size="sm" ta="center" py="xl">
      {texte}
    </Text>
  )
}

const L = 520
const H = 220
const MARGE = { haut: 16, droite: 12, bas: 34, gauche: 44 }

function Axes({ max }: { max: number }) {
  const hauteurUtile = H - MARGE.haut - MARGE.bas
  return (
    <>
      {[0, 0.25, 0.5, 0.75, 1].map((f) => {
        const y = MARGE.haut + hauteurUtile * (1 - f)
        return (
          <g key={f}>
            <line x1={MARGE.gauche} x2={L - MARGE.droite} y1={y} y2={y} stroke={COULEUR_GRILLE} />
            <text x={MARGE.gauche - 6} y={y + 4} textAnchor="end" fontSize="11" fill={COULEUR_TEXTE}>
              {nombre(Math.round(max * f))}
            </text>
          </g>
        )
      })}
    </>
  )
}

export function Barres({
  points,
  vide = 'Aucune donnée',
  suffixe = '',
  max: maxFixe,
}: {
  points: Point[]
  vide?: string
  /** Ajouté aux valeurs affichées au-dessus des barres (ex. « % »). */
  suffixe?: string
  /** Échelle fixe (ex. 100 pour des pourcentages) au lieu d'une échelle arrondie sur les données. */
  max?: number
}) {
  if (points.length === 0) return <Vide texte={vide} />
  const max = maxFixe ?? maximumRond(Math.max(...points.map((p) => p.valeur)))
  const largeurUtile = L - MARGE.gauche - MARGE.droite
  const hauteurUtile = H - MARGE.haut - MARGE.bas
  const pas = largeurUtile / points.length
  const largeurBarre = Math.min(48, pas * 0.6)
  return (
    <svg viewBox={`0 0 ${L} ${H}`} width="100%" role="img" aria-label="Diagramme en barres">
      <Axes max={max} />
      {points.map((p, i) => {
        const h = (p.valeur / max) * hauteurUtile
        const x = MARGE.gauche + pas * i + (pas - largeurBarre) / 2
        const y = MARGE.haut + hauteurUtile - h
        return (
          <g key={p.libelle}>
            <rect x={x} y={y} width={largeurBarre} height={h} rx={4} fill={COULEUR_PRINCIPALE}>
              <title>{`${p.libelle} : ${nombre(p.valeur)}`}</title>
            </rect>
            <text x={x + largeurBarre / 2} y={y - 4} textAnchor="middle" fontSize="11" fill={COULEUR_TEXTE}>
              {nombre(p.valeur)}
              {suffixe}
            </text>
            <text x={x + largeurBarre / 2} y={H - 14} textAnchor="middle" fontSize="11" fill={COULEUR_TEXTE}>
              {p.libelle.length > 12 ? `${p.libelle.slice(0, 11)}…` : p.libelle}
            </text>
          </g>
        )
      })}
    </svg>
  )
}

export function Courbe({ points, vide = 'Aucune donnée' }: { points: Point[]; vide?: string }) {
  if (points.length === 0) return <Vide texte={vide} />
  const max = maximumRond(Math.max(...points.map((p) => p.valeur)))
  const largeurUtile = L - MARGE.gauche - MARGE.droite
  const hauteurUtile = H - MARGE.haut - MARGE.bas
  const x = (i: number) => MARGE.gauche + (points.length === 1 ? largeurUtile / 2 : (largeurUtile * i) / (points.length - 1))
  const y = (v: number) => MARGE.haut + hauteurUtile * (1 - v / max)
  const trace = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.valeur).toFixed(1)}`).join(' ')
  const base = MARGE.haut + hauteurUtile
  const aire = `${trace} L${x(points.length - 1).toFixed(1)},${base} L${x(0).toFixed(1)},${base} Z`
  return (
    <svg viewBox={`0 0 ${L} ${H}`} width="100%" role="img" aria-label="Courbe d'évolution">
      <Axes max={max} />
      <path d={aire} fill={COULEUR_PRINCIPALE} opacity={0.1} />
      <path d={trace} fill="none" stroke={COULEUR_PRINCIPALE} strokeWidth={2.5} strokeLinejoin="round" />
      {points.map((p, i) => (
        <g key={p.libelle}>
          <circle cx={x(i)} cy={y(p.valeur)} r={4} fill="#fff" stroke={COULEUR_PRINCIPALE} strokeWidth={2}>
            <title>{`${p.libelle} : ${nombre(p.valeur)}`}</title>
          </circle>
          {(points.length <= 8 || i % Math.ceil(points.length / 8) === 0) && (
            <text x={x(i)} y={H - 14} textAnchor="middle" fontSize="11" fill={COULEUR_TEXTE}>
              {p.libelle}
            </text>
          )}
        </g>
      ))}
    </svg>
  )
}

export function Anneau({ points, libelleTotal, vide = 'Aucune donnée' }: { points: Point[]; libelleTotal: string; vide?: string }) {
  const total = points.reduce((somme, p) => somme + p.valeur, 0)
  if (points.length === 0 || total === 0) return <Vide texte={vide} />
  const rayon = 70
  const circonference = 2 * Math.PI * rayon
  let decalage = 0
  return (
    <Group wrap="wrap" justify="center" gap="xl">
      <svg viewBox="0 0 200 200" width={180} height={180} role="img" aria-label="Répartition">
        <g transform="rotate(-90 100 100)">
          {points.map((p, i) => {
            const longueur = (p.valeur / total) * circonference
            const cercle = (
              <circle
                key={p.libelle}
                cx={100}
                cy={100}
                r={rayon}
                fill="none"
                stroke={PALETTE[i % PALETTE.length]}
                strokeWidth={26}
                strokeDasharray={`${longueur} ${circonference - longueur}`}
                strokeDashoffset={-decalage}
              >
                <title>{`${p.libelle} : ${nombre(p.valeur)}`}</title>
              </circle>
            )
            decalage += longueur
            return cercle
          })}
        </g>
        <text x={100} y={100} textAnchor="middle" fontSize="26" fontWeight="700" fill="#1a2b4b">
          {nombre(total)}
        </text>
        <text x={100} y={120} textAnchor="middle" fontSize="11" fill={COULEUR_TEXTE}>
          {libelleTotal}
        </text>
      </svg>
      <Stack gap={6}>
        {points.map((p, i) => (
          <Group key={p.libelle} gap="xs" wrap="nowrap">
            <span
              style={{ width: 10, height: 10, borderRadius: 3, background: PALETTE[i % PALETTE.length], flex: 'none' }}
            />
            <Text size="sm">
              {p.libelle} · {Math.round((p.valeur / total) * 100)} %
            </Text>
          </Group>
        ))}
      </Stack>
    </Group>
  )
}
