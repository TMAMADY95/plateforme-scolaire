import { Alert, Badge, Button, Card, Group, Loader, Progress, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { Link, Navigate } from 'react-router'
import { bilanFinancier, obtenirEtablissement, statistiquesEtablissement } from '../api/endpoints'
import { estDirectionRegionale, estEleve, estEnseignant, estMinistere, estParent, etablissementCourant, LIBELLES_ROLES } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import { BadgeSecteur, BadgesCycles } from '../components/BadgesEtablissement'
import CarteChiffre from '../components/CarteChiffre'
import CarteReussite from '../components/CarteReussite'
import { Barres, Courbe, libelleMois } from '../components/graphiques/Graphiques'
import Icone from '../components/Icone'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { formaterMontant } from '../utils/monnaie'

export default function PageAccueil() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)

  const etablissement = useQuery({
    queryKey: ['etablissement', etablissementId],
    queryFn: () => obtenirEtablissement(etablissementId as number),
    enabled: etablissementId !== undefined,
  })

  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? undefined : Number(annee.anneeId)
  const statistiques = useQuery({
    queryKey: ['statistiques-etablissement', etablissementId, anneeId],
    queryFn: () => statistiquesEtablissement(etablissementId as number, anneeId as number),
    enabled: etablissementId !== undefined && anneeId !== undefined,
  })
  const bilan = useQuery({
    queryKey: ['bilan-financier', etablissementId, anneeId],
    queryFn: () => bilanFinancier(etablissementId as number, anneeId as number),
    enabled: etablissementId !== undefined && anneeId !== undefined,
  })

  if (etablissementId === undefined && estMinistere(utilisateur)) {
    return <Navigate to="/ministere" replace />
  }
  if (etablissementId === undefined && estDirectionRegionale(utilisateur)) {
    return <Navigate to="/region" replace />
  }
  if (etablissementId === undefined && estEnseignant(utilisateur)) {
    return <Navigate to="/mes-classes" replace />
  }
  if (etablissementId === undefined && estParent(utilisateur)) {
    return <Navigate to="/mes-enfants" replace />
  }
  if (etablissementId === undefined && estEleve(utilisateur)) {
    return <Navigate to="/mon-espace" replace />
  }

  if (etablissementId === undefined) {
    const roles = [...new Set(utilisateur.affectations.map((a) => LIBELLES_ROLES[a.role]))].join(', ')
    return (
      <Stack>
        <Title order={2}>Bienvenue</Title>
        <Alert color="blue" variant="light" title="Aucun espace pour ce rôle">
          Votre compte ({roles}) n'a pas d'espace dédié sur le site. Rapprochez-vous de l'administration si vous
          pensez qu'il manque un rôle ou un établissement.
        </Alert>
      </Stack>
    )
  }

  return (
    <Stack>
      <div>
        <Title order={2}>Tableau de bord</Title>
        {etablissement.data && (
          <Text c="dimmed">
            {etablissement.data.nom} · Année {annee.options.find((o) => o.value === annee.anneeId)?.label ?? '—'}
          </Text>
        )}
      </div>
      {etablissement.isPending && <Loader />}
      {etablissement.isError && (
        <Alert color="red" variant="light">
          {etablissement.error.message}
        </Alert>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
        <CarteChiffre
          libelle="Élèves inscrits"
          valeur={statistiques.data ? statistiques.data.elevesInscrits : '—'}
          icone="eleves"
          couleur="cyan"
        />
        <CarteChiffre
          libelle="Enseignants"
          valeur={statistiques.data ? statistiques.data.enseignants : '—'}
          icone="enseignants"
          couleur="violet"
        />
        <CarteChiffre
          libelle="Factures impayées"
          valeur={bilan.data ? bilan.data.nombreImpayees : '—'}
          detail={bilan.data ? `sur ${bilan.data.nombreFactures} factures` : undefined}
          icone="finances"
          couleur="orange"
        />
        <CarteChiffre
          libelle="Factures en retard"
          valeur={bilan.data ? bilan.data.nombreEnRetard : '—'}
          icone="alerte"
          couleur="red"
        />
      </SimpleGrid>

      {statistiques.isError && (
        <Alert color="red" variant="light">
          {statistiques.error.message}
        </Alert>
      )}
      {statistiques.data && (
        <SimpleGrid cols={{ base: 1, md: 2 }}>
          <Card withBorder padding="lg">
            <Text fw={600} mb="sm">
              Évolution des inscriptions
            </Text>
            <Courbe
              points={statistiques.data.inscriptionsParMois.map((p) => ({ libelle: libelleMois(p.mois), valeur: p.nombre }))}
              vide="Aucune demande d'inscription pour cette année."
            />
          </Card>
          <Card withBorder padding="lg">
            <Text fw={600} mb="sm">
              Effectifs par niveau
            </Text>
            <Barres points={statistiques.data.effectifsParNiveau} vide="Aucun élève placé en classe." />
            {statistiques.data.tauxPresence !== null && (
              <Text size="sm" c="dimmed" mt="sm">
                Taux de présence ({statistiques.data.joursPresence} derniers jours) :{' '}
                {String(statistiques.data.tauxPresence).replace('.', ',')} %
              </Text>
            )}
          </Card>
        </SimpleGrid>
      )}

      {anneeId !== undefined && <CarteReussite etablissementId={etablissementId} anneeScolaireId={anneeId} />}

      {bilan.data && bilan.data.nombreFactures > 0 && (
        <Card withBorder padding="lg">
          <Group justify="space-between" mb="xs">
            <Text fw={600}>Recouvrement des frais scolaires</Text>
            <Text size="sm" c="dimmed">
              {formaterMontant(bilan.data.totalPaye, bilan.data.devise)} encaissés sur{' '}
              {formaterMontant(bilan.data.totalDu - bilan.data.totalExonere, bilan.data.devise)}
            </Text>
          </Group>
          <Progress
            value={tauxRecouvrement(bilan.data.totalPaye, bilan.data.totalDu - bilan.data.totalExonere)}
            size="lg"
            radius="xl"
            color="green"
          />
          <Text size="xs" c="dimmed" mt={6}>
            Reste à payer : {formaterMontant(bilan.data.totalRestant, bilan.data.devise)}
          </Text>
        </Card>
      )}

      <Card withBorder padding="lg">
        <Text fw={600} mb="sm">
          Accès rapides
        </Text>
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
          <Button component={Link} to="/inscriptions" variant="light" leftSection={<Icone nom="inscriptions" taille={18} />}>
            Inscriptions
          </Button>
          <Button component={Link} to="/enseignants" variant="light" leftSection={<Icone nom="enseignants" taille={18} />}>
            Enseignants
          </Button>
          <Button component={Link} to="/classes" variant="light" leftSection={<Icone nom="classes" taille={18} />}>
            Classes
          </Button>
          <Button component={Link} to="/emplois-du-temps" variant="light" leftSection={<Icone nom="horaires" taille={18} />}>
            Emplois du temps
          </Button>
        </SimpleGrid>
      </Card>

      {etablissement.data && (
        <Card withBorder padding="lg">
          <Group justify="space-between" mb="xs">
            <Text fw={600} size="lg">
              {etablissement.data.nom}
            </Text>
            <Badge color={etablissement.data.statut === 'ACTIF' ? 'green' : 'gray'}>{etablissement.data.statut}</Badge>
          </Group>
          <Text c="dimmed" size="sm">
            Code officiel : {etablissement.data.codeOfficiel}
          </Text>
          <Group gap="xs" mt="xs">
            <BadgeSecteur secteur={etablissement.data.secteur} />
            <BadgesCycles cycles={etablissement.data.cycles} />
          </Group>
        </Card>
      )}
    </Stack>
  )
}

/** Part encaissée, en pourcentage borné entre 0 et 100. */
function tauxRecouvrement(paye: number, du: number): number {
  return du <= 0 ? 100 : Math.max(0, Math.min(100, (paye / du) * 100))
}
