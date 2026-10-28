import { Alert, Group, Loader, Stack, Tabs, Text, Title } from '@mantine/core'
import { useMonProfilEleve } from '../hooks/useMonProfilEleve'
import OngletBulletin from './enfant/OngletBulletin'
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
      <Title order={2}>Mon espace</Title>

      <Group justify="space-between" align="flex-start">
        <div>
          <Text fw={600} size="lg">
            {p.prenom} {p.nom}
          </Text>
          <Text c="dimmed" size="sm">
            {p.classeNom !== null
              ? `Classe ${p.classeNom} — ${p.etablissementNom} (${p.anneeLibelle})`
              : "Pas d'inscription active"}
          </Text>
        </div>
      </Group>

      <Tabs defaultValue="bulletin" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="bulletin">Bulletin</Tabs.Tab>
          <Tabs.Tab value="presences">Présences</Tabs.Tab>
          <Tabs.Tab value="emploi-du-temps">Emploi du temps</Tabs.Tab>
          <Tabs.Tab value="frais">Frais</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="bulletin" pt="md">
          <OngletBulletin enfant={enfant} />
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
