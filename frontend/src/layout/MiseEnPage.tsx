import { AppShell, Badge, Burger, Button, Group, NavLink, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { Link, Outlet, useLocation } from 'react-router'
import { estEleve, estEnseignant, estGestionnaire, estParent, LIBELLES_ROLES } from '../auth/roles'
import { useAuth, useUtilisateur } from '../auth/useAuth'

interface Lien {
  chemin: string
  libelle: string
}

export default function MiseEnPage() {
  const utilisateur = useUtilisateur()
  const { deconnexion } = useAuth()
  const { pathname } = useLocation()
  const [menuOuvert, { toggle, close }] = useDisclosure()

  const liens: Lien[] = [{ chemin: '/', libelle: 'Accueil' }]

  if (estGestionnaire(utilisateur)) {
    liens.push({ chemin: '/classes', libelle: 'Classes' })
    liens.push({ chemin: '/inscriptions', libelle: 'Inscriptions' })
    liens.push({ chemin: '/enseignants', libelle: 'Enseignants' })
    liens.push({ chemin: '/eleves', libelle: 'Élèves' })
    liens.push({ chemin: '/presences', libelle: 'Présences' })
    liens.push({ chemin: '/resultats', libelle: 'Résultats' })
    liens.push({ chemin: '/emplois-du-temps', libelle: 'Emplois du temps' })
    liens.push({ chemin: '/finances', libelle: 'Finances' })
  }

  if (estEnseignant(utilisateur)) {
    liens.push({ chemin: '/mes-classes', libelle: 'Mes classes' })
  }

  if (estParent(utilisateur)) {
    liens.push({ chemin: '/mes-enfants', libelle: 'Mes enfants' })
  }

  if (estEleve(utilisateur)) {
    liens.push({ chemin: '/mon-espace', libelle: 'Mon espace' })
  }

  const roles = [...new Set(utilisateur.affectations.map((a) => LIBELLES_ROLES[a.role]))]

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 240, breakpoint: 'sm', collapsed: { mobile: !menuOuvert } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group wrap="nowrap">
            <Burger opened={menuOuvert} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Menu" />
            <Title order={4}>Gestion scolaire</Title>
          </Group>
          <Group wrap="nowrap" gap="sm">
            <Text size="sm" c="dimmed" visibleFrom="sm">
              {utilisateur.email}
            </Text>
            {roles.map((role) => (
              <Badge key={role} variant="light" visibleFrom="sm">
                {role}
              </Badge>
            ))}
            <Button component={Link} to="/changer-mot-de-passe" variant="subtle" size="xs" visibleFrom="sm">
              Mot de passe
            </Button>
            <Button variant="light" size="xs" onClick={deconnexion}>
              Déconnexion
            </Button>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        {liens.map((lien) => (
          <NavLink
            key={lien.chemin}
            component={Link}
            to={lien.chemin}
            label={lien.libelle}
            active={lien.chemin === '/' ? pathname === '/' : pathname.startsWith(lien.chemin)}
            onClick={close}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
