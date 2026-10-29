import { Stack, Tabs, Title } from '@mantine/core'
import JournalAuditNational from './ministere/JournalAuditNational'
import ListeEtablissements from './ministere/ListeEtablissements'
import Referentiels from './ministere/Referentiels'
import { reussiteNationale, statistiquesNationales, syntheseNationale } from '../api/endpoints'
import SyntheseNationale from './ministere/SyntheseNationale'

/** Administration nationale : chiffres clés, établissements et référentiels. */
export default function PageMinistere() {
  return (
    <Stack>
      <Title order={2}>Administration nationale</Title>
      <Tabs defaultValue="synthese">
        <Tabs.List>
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
