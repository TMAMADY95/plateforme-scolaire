import type { ReactNode } from 'react'

/** Petites icônes en traits (24 x 24), sans dépendance externe. */
const TRACES = {
  accueil: <path d="M3 11.5 12 4l9 7.5M5.5 10v9.5h13V10M10 19.5v-5h4v5" />,
  ecole: <path d="M3 9.5 12 4l9 5.5M5 10v9h14v-9M9 19v-5h6v5M3 20h18" />,
  classes: <path d="M4 5h16v11H4zM8 20h8M12 16v4" />,
  inscriptions: <path d="M6 3h9l4 4v14H6zM14 3v5h5M9 13h7M9 17h7" />,
  enseignants: <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 20c0-3.5 3.6-6 8-6s8 2.5 8 6" />,
  eleves: <path d="M9 11a3.2 3.2 0 1 0 0-6.4A3.2 3.2 0 0 0 9 11ZM2.5 19c0-2.8 2.9-4.8 6.5-4.8s6.5 2 6.5 4.8M16 5.5a3 3 0 0 1 0 5.8M18 14.6c2 .6 3.5 2.1 3.5 4.4" />,
  presences: <path d="M5 4h14v16H5zM9 9l2 2 4-4M9 15h6" />,
  resultats: <path d="M5 20V10M12 20V4M19 20v-7" />,
  horaires: <path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM12 7v5l3 2" />,
  finances: <path d="M3 7h18v12H3zM3 11h18M7 15h3" />,
  comptes: <path d="M12 3 4 6v5.5c0 4.6 3.2 8 8 9.5 4.8-1.5 8-4.9 8-9.5V6zM9 12l2 2 4-4" />,
  national: <path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM3 12h18M12 3c2.5 2.5 3.5 5.5 3.5 9s-1 6.5-3.5 9c-2.5-2.5-3.5-5.5-3.5-9s1-6.5 3.5-9Z" />,
  region: <path d="M12 21s7-6 7-11a7 7 0 1 0-14 0c0 5 7 11 7 11ZM12 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z" />,
  notes: <path d="M5 4h11l3 3v13H5zM8 10h8M8 14h8M8 18h5" />,
  enfants: <path d="M12 12a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM5 20c0-3.3 3.1-5.5 7-5.5s7 2.2 7 5.5" />,
  messages: <path d="M4 5h16v11H9l-5 4zM8 9.5h8M8 12.5h5" />,
  mot_de_passe: <path d="M6 11V8a6 6 0 0 1 12 0v3M5 11h14v10H5zM12 15v2.5" />,
  deconnexion: <path d="M10 4H5v16h5M15 8l4 4-4 4M19 12H9" />,
  argent: <path d="M12 3v18M16.5 7.5c-.8-1.2-2.4-2-4.5-2-2.6 0-4.5 1.2-4.5 3.2 0 4.3 9 2.2 9 6.6 0 2-2 3.2-4.5 3.2-2.2 0-3.9-.9-4.8-2.3" />,
  alerte: <path d="M12 4 2.5 20h19zM12 10v5M12 17.5v.5" />,
} satisfies Record<string, ReactNode>

export type NomIcone = keyof typeof TRACES

export default function Icone({ nom, taille = 20 }: { nom: NomIcone; taille?: number }) {
  return (
    <svg
      width={taille}
      height={taille}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {TRACES[nom]}
    </svg>
  )
}
