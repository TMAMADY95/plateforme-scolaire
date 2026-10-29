import { createTheme } from '@mantine/core'
import type { MantineColorsTuple } from '@mantine/core'

/** Nom de l'application, affiché dans le menu, la connexion et l'onglet du navigateur. */
export const NOM_APPLICATION = 'EduManage'
export const SOUS_TITRE_APPLICATION = 'Plateforme nationale de gestion scolaire'

/** Bleu marine de la charte (le 8e ton est la couleur principale des boutons). */
const marine: MantineColorsTuple = [
  '#eaf1fd',
  '#d3e1fa',
  '#a6c2f4',
  '#76a1ee',
  '#4f85e9',
  '#3a74e6',
  '#2f6be6',
  '#1f5bcc',
  '#0f3c8a',
  '#0a2a63',
]

export const theme = createTheme({
  primaryColor: 'marine',
  primaryShade: 7,
  colors: { marine },
  defaultRadius: 'md',
  fontFamily: 'Inter, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  headings: { fontWeight: '700' },
  components: {
    Card: { defaultProps: { radius: 'lg', shadow: 'xs' } },
    Paper: { defaultProps: { radius: 'lg' } },
  },
})
