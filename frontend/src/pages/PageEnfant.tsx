import { Alert, Anchor, Loader, Stack, Tabs } from '@mantine/core'
import { Link, useParams } from 'react-router'
import EnteteEleve from '../components/EnteteEleve'
import { useMesEnfants } from '../hooks/useMesEnfants'
import PanneauAnnonces from '../components/PanneauAnnonces'
import OngletLiaison from './enfant/OngletLiaison'
import OngletCahierTexte from './enfant/OngletCahierTexte'
import OngletBulletin from './enfant/OngletBulletin'
import OngletDevoirs from './enfant/OngletDevoirs'
import OngletEmploiDuTemps from './enfant/OngletEmploiDuTemps'
import OngletFactures from './enfant/OngletFactures'
import OngletPresences from './enfant/OngletPresences'

export default function PageEnfant() {
  const { eleveId } = useParams()
  const id = Number(eleveId)
  const enfants = useMesEnfants()

  if (enfants.isPending) return <Loader />
  if (enfants.isError) {
    return (
      <Alert color="red" variant="light">
        {enfants.error.message}
      </Alert>
    )
  }

  const enfant = enfants.data.find((e) => e.eleveId === id)
  if (enfant === undefined) {
    return (
      <Stack>
        <Alert color="orange" variant="light">
          Cet élève n'est pas rattaché à votre compte.
        </Alert>
        <Anchor component={Link} to="/mes-enfants">
          Retour à mes enfants
        </Anchor>
      </Stack>
    )
  }

  return (
    <Stack>
      <Anchor component={Link} to="/mes-enfants" size="sm">
        ← Mes enfants
      </Anchor>
      <EnteteEleve
        prenom={enfant.prenom}
        nom={enfant.nom}
        matricule={enfant.matricule}
        badge={enfant.relation}
        sousTitre={
          enfant.classeNom !== null
            ? `Classe ${enfant.classeNom} — ${enfant.etablissementNom} (${enfant.anneeLibelle})`
            : "Pas d'inscription active"
        }
      />

      <Tabs defaultValue="bulletin" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="bulletin">Bulletin</Tabs.Tab>
          <Tabs.Tab value="devoirs">Devoirs</Tabs.Tab>
          <Tabs.Tab value="cahier">Cahier de texte</Tabs.Tab>
          <Tabs.Tab value="liaison">École-famille</Tabs.Tab>
          <Tabs.Tab value="annonces">Annonces</Tabs.Tab>
          <Tabs.Tab value="presences">Présences</Tabs.Tab>
          <Tabs.Tab value="emploi-du-temps">Emploi du temps</Tabs.Tab>
          <Tabs.Tab value="frais">Frais</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="bulletin" pt="md">
          <OngletBulletin enfant={enfant} />
        </Tabs.Panel>

        <Tabs.Panel value="devoirs" pt="md">
          <OngletDevoirs classeId={enfant.classeId} eleveId={enfant.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="cahier" pt="md">
          <OngletCahierTexte eleveId={enfant.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="liaison" pt="md">
          <OngletLiaison eleveId={enfant.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="annonces" pt="md">
          <PanneauAnnonces classeId={enfant.classeId} peutPublier={false} />
        </Tabs.Panel>

        <Tabs.Panel value="presences" pt="md">
          <OngletPresences eleveId={enfant.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="emploi-du-temps" pt="md">
          <OngletEmploiDuTemps classeId={enfant.classeId} />
        </Tabs.Panel>

        <Tabs.Panel value="frais" pt="md">
          {/* eleveId de l'enfant : le parent voit les factures de son enfant. */}
          <OngletFactures eleveId={enfant.eleveId} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
