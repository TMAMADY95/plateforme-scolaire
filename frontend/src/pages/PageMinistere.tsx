import { Loader, Stack, Tabs, Text, Title } from '@mantine/core'
import { Suspense, lazy } from 'react'
import { useSearchParams } from 'react-router'
import {
  conformiteNationale,
  evolutionNationale,
  plansActionNationaux,
  postesNationaux,
  rapportNationalPdf,
  rapportExamenNational,
  reussiteNationale,
  statistiquesNationales,
  statistiquesParEtablissement,
  statistiquesParZone,
  syntheseNationale,
} from '../api/endpoints'
import PanneauPlansAction from '../components/PanneauPlansAction'
import PanneauPostes from '../components/PanneauPostes'
import PanneauExamens from '../components/PanneauExamens'
import TableauDeBordConformite from '../components/TableauDeBordConformite'
import Icone from '../components/Icone'
import { estMinistere } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import { SECTIONS_MINISTERE } from './sections'

// Les écrans des onglets sont chargés à la demande.
const JournalAuditNational = lazy(() => import('./ministere/JournalAuditNational'))
const ElevesNational = lazy(() => import('./ministere/ElevesNational'))
const ListeEtablissements = lazy(() => import('./ministere/ListeEtablissements'))
const Referentiels = lazy(() => import('./ministere/Referentiels'))
const ComptesInstitutionnels = lazy(() => import('./ministere/ComptesInstitutionnels'))
const SyntheseNationale = lazy(() => import('./ministere/SyntheseNationale'))
const StatistiquesExamens = lazy(() => import('./ministere/StatistiquesExamens'))
const StatistiquesCandidats = lazy(() => import('./ministere/StatistiquesCandidats'))
const QualiteReclamations = lazy(() => import('./ministere/QualiteReclamations'))
const LogistiqueCentres = lazy(() => import('./ministere/LogistiqueCentres'))
const IndicateursBudget = lazy(() => import('./ministere/IndicateursBudget'))
const SuiviTraitement = lazy(() => import('./ministere/SuiviTraitement'))
const SituationsStatistiques = lazy(() => import('./ministere/SituationsStatistiques'))
const ParticipationEpreuves = lazy(() => import('./ministere/ParticipationEpreuves'))
const JournalAlertes = lazy(() => import('./ministere/JournalAlertes'))

const ONGLETS = [
  { valeur: 'synthese', libelle: 'Chiffres clés', icone: 'resultats' },
  { valeur: 'stats-examens', libelle: 'Statistiques des examens', icone: 'resultats' },
  { valeur: 'stats-candidats', libelle: 'Candidats et résultats', icone: 'eleves' },
  { valeur: 'qualite', libelle: 'Qualité et réclamations', icone: 'alerte' },
  { valeur: 'logistique', libelle: 'Logistique des centres', icone: 'classes' },
  { valeur: 'indicateurs', libelle: 'Indicateurs et budget', icone: 'resultats' },
  { valeur: 'traitement', libelle: 'Copies et fiabilité', icone: 'notes' },
  { valeur: 'situations', libelle: 'Situation statistique', icone: 'resultats' },
  { valeur: 'participation', libelle: 'Participation', icone: 'presences' },
  { valeur: 'conformite', libelle: 'Conformité', icone: 'comptes' },
  { valeur: 'examens', libelle: 'Examens', icone: 'notes' },
  { valeur: 'plans', libelle: "Plans d'action", icone: 'presences' },
  { valeur: 'alertes', libelle: 'Alertes', icone: 'alerte' },
] as const

/**
 * Administration nationale. Les sections de gestion (établissements, élèves, enseignants, référentiels, comptes,
 * journal) sont dans le menu de gauche ; le suivi du réseau reste en onglets horizontaux.
 */
export default function PageMinistere() {
  const [parametres, setParametres] = useSearchParams()
  const demande = parametres.get('onglet')
  const utilisateur = useUtilisateur()
  const ministere = estMinistere(utilisateur)
  // La DGECS n'a pas les sections d'administration (comptes, référentiels, journal) : elle pilote les examens.
  const section = ministere ? SECTIONS_MINISTERE.find((s) => s.valeur === demande) : undefined
  // La DGECS ne voit que les statistiques des examens (pas les chiffres clés du réseau scolaire).
  const onglets = ministere ? ONGLETS : ONGLETS.filter((o) => o.valeur !== 'synthese')
  const onglet = onglets.find((o) => o.valeur === demande)?.valeur ?? (ministere ? 'synthese' : 'stats-examens')

  return (
    <Stack>
      <div>
        <Title order={2}>{section ? section.libelle : ministere ? 'Administration nationale' : 'DGECS : pilotage des examens'}</Title>
        <Text c="dimmed">
          {section
            ? 'Administration nationale'
            : ministere ? 'Pilotage du réseau : chiffres, conformité, examens et suivi des opérations.' : 'Statistiques des examens, conformité, examens et suivi des opérations.'}
        </Text>
      </div>

      <Suspense fallback={<Loader />}>
      {section ? (
        <>
          {section.valeur === 'etablissements' && <ListeEtablissements />}
          {section.valeur === 'eleves' && <ElevesNational />}
          {section.valeur === 'postes' && <PanneauPostes cle="nationale" charger={postesNationaux} ministere />}
          {section.valeur === 'referentiels' && <Referentiels />}
          {section.valeur === 'comptes' && <ComptesInstitutionnels />}
          {section.valeur === 'audit' && <JournalAuditNational />}
        </>
      ) : (
        <Tabs
          value={onglet}
          onChange={(v) => v && setParametres({ onglet: v }, { replace: true })}
          variant="pills"
          keepMounted={false}
        >
          <Tabs.List mb="xs">
            {onglets.map((o) => (
              <Tabs.Tab key={o.valeur} value={o.valeur} leftSection={<Icone nom={o.icone} taille={16} />}>
                {o.libelle}
              </Tabs.Tab>
            ))}
          </Tabs.List>
          <Tabs.Panel value="synthese">
            <SyntheseNationale
              cle="synthese-nationale"
              charger={syntheseNationale}
              chargerStatistiques={statistiquesNationales}
              chargerParEtablissement={statistiquesParEtablissement}
              chargerParZone={statistiquesParZone}
              chargerReussite={reussiteNationale}
              chargerEvolution={evolutionNationale}
              telechargerRapport={rapportNationalPdf}
            />
          </Tabs.Panel>
          <Tabs.Panel value="stats-examens">
            <StatistiquesExamens />
          </Tabs.Panel>
          <Tabs.Panel value="stats-candidats">
            <StatistiquesCandidats />
          </Tabs.Panel>
          <Tabs.Panel value="qualite">
            <QualiteReclamations />
          </Tabs.Panel>
          <Tabs.Panel value="logistique">
            <LogistiqueCentres />
          </Tabs.Panel>
          <Tabs.Panel value="indicateurs">
            <IndicateursBudget />
          </Tabs.Panel>
          <Tabs.Panel value="traitement">
            <SuiviTraitement />
          </Tabs.Panel>
          <Tabs.Panel value="situations">
            <SituationsStatistiques />
          </Tabs.Panel>
          <Tabs.Panel value="participation">
            <ParticipationEpreuves />
          </Tabs.Panel>
          <Tabs.Panel value="conformite">
            <TableauDeBordConformite cle="nationale" charger={conformiteNationale} ministere />
          </Tabs.Panel>
          <Tabs.Panel value="examens">
            <PanneauExamens cle="nationale" charger={rapportExamenNational} ministere />
          </Tabs.Panel>
          <Tabs.Panel value="plans">
            <PanneauPlansAction cle="nationale" charger={plansActionNationaux} />
          </Tabs.Panel>
          <Tabs.Panel value="alertes">
            <JournalAlertes />
          </Tabs.Panel>
        </Tabs>
      )}
      </Suspense>
    </Stack>
  )
}
