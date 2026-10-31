import { Stack, Tabs, Text, Title } from '@mantine/core'
import { useSearchParams } from 'react-router'
import JournalAuditNational from './ministere/JournalAuditNational'
import ElevesNational from './ministere/ElevesNational'
import ListeEtablissements from './ministere/ListeEtablissements'
import Referentiels from './ministere/Referentiels'
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
import JournalAlertes from './ministere/JournalAlertes'
import PanneauPostes from '../components/PanneauPostes'
import PanneauExamens from '../components/PanneauExamens'
import TableauDeBordConformite from '../components/TableauDeBordConformite'
import ComptesInstitutionnels from './ministere/ComptesInstitutionnels'
import Icone from '../components/Icone'
import { SECTIONS_MINISTERE } from './sections'
import SyntheseNationale from './ministere/SyntheseNationale'

const ONGLETS = [
  { valeur: 'synthese', libelle: 'Chiffres clés', icone: 'resultats' },
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
  const section = SECTIONS_MINISTERE.find((s) => s.valeur === demande)
  const onglet = ONGLETS.find((o) => o.valeur === demande)?.valeur ?? 'synthese'

  return (
    <Stack>
      <div>
        <Title order={2}>{section ? section.libelle : 'Administration nationale'}</Title>
        <Text c="dimmed">
          {section
            ? 'Administration nationale'
            : 'Pilotage du réseau : chiffres, conformité, examens et suivi des opérations.'}
        </Text>
      </div>

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
            {ONGLETS.map((o) => (
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
    </Stack>
  )
}
