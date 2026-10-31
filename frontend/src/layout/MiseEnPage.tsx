import { AppShell, Avatar, Badge, Burger, Group, Menu, NavLink, Stack, Text, UnstyledButton } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { Link, Outlet, useLocation, useSearchParams } from 'react-router'
import {
  estDirectionRegionale,
  estEleve,
  estEnseignant,
  estGestionnaire,
  estMinistere,
  estDirectionEtablissement,
  estParent,
  LIBELLES_ROLES,
} from '../auth/roles'
import { useAuth, useUtilisateur } from '../auth/useAuth'
import Icone from '../components/Icone'
import type { NomIcone } from '../components/Icone'
import Logo from '../components/Logo'
import { useMessagesNonLus } from '../hooks/useMessagesNonLus'
import { SECTIONS_MINISTERE, SECTIONS_REGION } from '../pages/sections'
import type { Section } from '../pages/sections'

interface Lien {
  chemin: string
  libelle: string
  icone: NomIcone
  /** Nombre affiché à droite du libellé (0 = rien). */
  pastille?: number
  /** Sections affichées sous le lien (menu déroulant) : adresse « chemin?onglet=valeur ». */
  sections?: Section[]
}

function initiales(email: string): string {
  return email.slice(0, 2).toUpperCase()
}

export default function MiseEnPage() {
  const utilisateur = useUtilisateur()
  const { deconnexion } = useAuth()
  const { pathname } = useLocation()
  const [parametres] = useSearchParams()
  const ongletCourant = parametres.get('onglet')
  const [menuOuvert, { toggle, close }] = useDisclosure()

  const peutMessagerie = estParent(utilisateur) || estEnseignant(utilisateur) || estGestionnaire(utilisateur)
  const nonLus = useMessagesNonLus(peutMessagerie).data ?? 0

  const liens: Lien[] = [{ chemin: '/', libelle: 'Tableau de bord', icone: 'accueil' }]

  if (estGestionnaire(utilisateur)) {
    liens.push({ chemin: '/classes', libelle: 'Classes', icone: 'classes' })
    liens.push({ chemin: '/inscriptions', libelle: 'Inscriptions', icone: 'inscriptions' })
    liens.push({ chemin: '/enseignants', libelle: 'Enseignants', icone: 'enseignants' })
    liens.push({ chemin: '/eleves', libelle: 'Élèves', icone: 'eleves' })
    liens.push({ chemin: '/presences', libelle: 'Présences', icone: 'presences' })
    liens.push({ chemin: '/resultats', libelle: 'Résultats', icone: 'resultats' })
    liens.push({ chemin: '/emplois-du-temps', libelle: 'Emplois du temps', icone: 'horaires' })
    liens.push({ chemin: '/finances', libelle: 'Finances', icone: 'finances' })
  }

  if (estDirectionEtablissement(utilisateur)) {
    liens.push({ chemin: '/comptes', libelle: 'Comptes', icone: 'comptes' })
    liens.push({ chemin: '/alertes-parents', libelle: 'Alertes parents', icone: 'alerte' })
  }

  if (estMinistere(utilisateur)) {
    liens.push({ chemin: '/ministere', libelle: 'Administration nationale', icone: 'national', sections: SECTIONS_MINISTERE })
  }

  if (estDirectionRegionale(utilisateur)) {
    liens.push({ chemin: '/region', libelle: 'Ma région', icone: 'region', sections: SECTIONS_REGION })
  }

  if (estEnseignant(utilisateur)) {
    liens.push({ chemin: '/mes-classes', libelle: 'Mes classes', icone: 'classes' })
  }

  if (estParent(utilisateur)) {
    liens.push({ chemin: '/mes-enfants', libelle: 'Mes enfants', icone: 'enfants' })
  }

  if (estEleve(utilisateur)) {
    liens.push({ chemin: '/mon-espace', libelle: 'Mon espace', icone: 'notes' })
  }

  if (peutMessagerie) {
    liens.push({ chemin: '/messages', libelle: 'Messages', icone: 'messages', pastille: nonLus })
  }

  const roles = [...new Set(utilisateur.affectations.map((a) => LIBELLES_ROLES[a.role]))]

  return (
    <AppShell
      header={{ height: 64 }}
      navbar={{ width: 260, breakpoint: 'sm', collapsed: { mobile: !menuOuvert } }}
      padding="lg"
    >
      <AppShell.Header withBorder={false} style={{ boxShadow: '0 1px 0 rgba(10, 42, 99, 0.08)' }}>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group wrap="nowrap">
            <Burger opened={menuOuvert} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Menu" />
            <Text fw={600} c="marine.8" visibleFrom="sm">
              {roles.join(' · ')}
            </Text>
          </Group>

          <Menu position="bottom-end" width={230} withArrow>
            <Menu.Target>
              <UnstyledButton aria-label="Mon compte">
                <Group gap="sm" wrap="nowrap">
                  <Stack gap={0} align="flex-end" visibleFrom="sm">
                    <Text size="sm" fw={600}>
                      {utilisateur.email}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {roles[0]}
                    </Text>
                  </Stack>
                  <Avatar color="marine" radius="xl">
                    {initiales(utilisateur.email)}
                  </Avatar>
                </Group>
              </UnstyledButton>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Label>{utilisateur.email}</Menu.Label>
              {roles.map((role) => (
                <Badge key={role} variant="light" mx="sm" mb={4}>
                  {role}
                </Badge>
              ))}
              <Menu.Divider />
              <Menu.Item component={Link} to="/changer-mot-de-passe" leftSection={<Icone nom="mot_de_passe" taille={16} />}>
                Changer mon mot de passe
              </Menu.Item>
              <Menu.Item color="red" onClick={deconnexion} leftSection={<Icone nom="deconnexion" taille={16} />}>
                Déconnexion
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md" className="menu-lateral">
        <Stack gap="lg" h="100%">
          <Logo />
          <div>
            {liens.map((lien) => {
              const actif = lien.chemin === '/' ? pathname === '/' : pathname.startsWith(lien.chemin)
              const rendu = (
                <NavLink
                  key={lien.chemin}
                  component={Link}
                  to={lien.chemin}
                  label={lien.libelle}
                  rightSection={lien.pastille ? <Badge size="sm" circle color="red">{lien.pastille}</Badge> : undefined}
                  leftSection={<Icone nom={lien.icone} />}
                  active={actif && !(lien.sections?.some((s) => s.valeur === ongletCourant) ?? false)}
                  defaultOpened={actif}
                  childrenOffset={28}
                  onClick={close}
                >
                  {lien.sections?.map((s) => (
                    <NavLink
                      key={s.valeur}
                      component={Link}
                      to={`${lien.chemin}?onglet=${s.valeur}`}
                      label={s.libelle}
                      leftSection={<Icone nom={s.icone} taille={16} />}
                      active={actif && ongletCourant === s.valeur}
                      onClick={close}
                    />
                  ))}
                </NavLink>
              )
              return rendu
            })}
          </div>
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
