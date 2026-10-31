import { Alert, Loader, Stack, Tabs } from '@mantine/core'
import EnteteEleve from '../components/EnteteEleve'
import { useMonProfilEleve } from '../hooks/useMonProfilEleve'
import PanneauAnnonces from '../components/PanneauAnnonces'
import OngletLiaison from './enfant/OngletLiaison'
import OngletCahierTexte from './enfant/OngletCahierTexte'
import OngletBulletin from './enfant/OngletBulletin'
import OngletDevoirs from './enfant/OngletDevoirs'
import OngletEmploiDuTemps from './enfant/OngletEmploiDuTemps'
import OngletFactures from './enfant/OngletFactures'
import OngletPresences from './enfant/OngletPresences'
import type { Enfant } from '../api/types'

export default function PageMonEspace() {
  const profil = useMonProfilEleve()

  if (profil.isPending) return <Loader />
  if (profil.isError) {
    return (
      <Alert color="red" variant="light">
        {profil.error.message}
      </Alert>
    )
  }

  const p = profil.data
  // Les onglets Bulletin et Présences attendent un objet de type Enfant ;
  // un élève n'ayant pas de lien parental, on complète les champs absents.
  const enfant: Enfant = {
    eleveId: p.eleveId,
    matricule: p.matricule,
    nom: p.nom,
    prenom: p.prenom,
    dateNaissance: p.dateNaissance,
    sexe: p.sexe,
    relation: '',
    responsableLegal: false,
    classeId: p.classeId,
    classeNom: p.classeNom,
    etablissementId: p.etablissementId,
    etablissementNom: p.etablissementNom,
    anneeScolaireId: p.anneeScolaireId,
    anneeLibelle: p.anneeLibelle,
  }

  return (
    <Stack>
      <EnteteEleve
        prenom={p.prenom}
        nom={p.nom}
        matricule={p.matricule}
        sousTitre={
          p.classeNom !== null
            ? `Classe ${p.classeNom} — ${p.etablissementNom} (${p.anneeLibelle})`
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
          <OngletDevoirs classeId={p.classeId} eleveId={p.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="cahier" pt="md">
          <OngletCahierTexte eleveId={p.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="liaison" pt="md">
          <OngletLiaison eleveId={p.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="annonces" pt="md">
          <PanneauAnnonces classeId={p.classeId} peutPublier={false} />
        </Tabs.Panel>

        <Tabs.Panel value="presences" pt="md">
          <OngletPresences eleveId={p.eleveId} />
        </Tabs.Panel>

        <Tabs.Panel value="emploi-du-temps" pt="md">
          <OngletEmploiDuTemps classeId={p.classeId} />
        </Tabs.Panel>

        <Tabs.Panel value="frais" pt="md">
          {/* eleveId à null : l'onglet affiche les factures de l'élève connecté. */}
          <OngletFactures eleveId={null} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
