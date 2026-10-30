import { Stack, Tabs, Text, Title } from '@mantine/core'
import JournalAuditNational from './ministere/JournalAuditNational'
import ListeEtablissements from './ministere/ListeEtablissements'
import Referentiels from './ministere/Referentiels'
import {
  reussiteNationale,
  statistiquesNationales,
  statistiquesParEtablissement,
  statistiquesParZone,
  syntheseNationale,
} from '../api/endpoints'
import SyntheseNationale from './ministere/SyntheseNationale'

/** Administration nationale : chiffres clés, établissements et référentiels. */
export default function PageMinistere() {
  return (
    <Stack>
      <div>
        <Title order={2}>Administration nationale</Title>
        <Text c="dimmed">Pilotage du réseau : chiffres, établissements, référentiels et suivi des opérations.</Text>
      </div>
      <Tabs defaultValue="synthese" variant="pills" keepMounted={false}>
        <Tabs.List mb="xs">
          <Tabs.Tab value="synthese">Chiffres clés</Tabs.Tab>
          <Tabs.Tab value="etablissements">Établissements</Tabs.Tab>
          <Tabs.Tab value="referentiels">Référentiels</Tabs.Tab>
          <Tabs.Tab value="audit">Journal d'audit</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="synthese">
          <SyntheseNationale
            cle="synthese-nationale"
            charger={syntheseNationale}
            chargerStatistiques={statistiquesNationales}
            chargerParEtablissement={statistiquesParEtablissement}
            chargerParZone={statistiquesParZone}
            chargerReussite={reussiteNationale}
          />
        </Tabs.Panel>
        <Tabs.Panel value="etablissements">
          <ListeEtablissements />
        </Tabs.Panel>
        <Tabs.Panel value="referentiels">
          <Referentiels />
        </Tabs.Panel>
        <Tabs.Panel value="audit">
          <JournalAuditNational />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
