import { useState } from 'react'
import { Alert, Button, Card, Group, Stack, Text, TextInput, Title } from '@mantine/core'
import { eleveParIne } from '../../api/endpoints'
import { inePlausible, libelleActe } from '../../api/aideIdentite'
import type { IdentiteNationale } from '../../api/types'
import { formaterDate } from '../../utils/dates'
import EtatCivilModal from './EtatCivilModal'

/**
 * Retrouver un eleve par son identifiant national (INE), ou qu'il soit scolarise.
 * L'INE n'est pas un secret : le serveur renvoie le meme message pour un INE inconnu
 * et pour un eleve hors du perimetre de l'utilisateur.
 */
export default function RechercheParIne() {
  const [saisie, setSaisie] = useState('')
  const [recherche, setRecherche] = useState<string | null>(null)
  const [resultat, setResultat] = useState<IdentiteNationale | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [enCours, setEnCours] = useState(false)
  const [corriger, setCorriger] = useState(false)

  async function chercher() {
    const ine = saisie.trim().toUpperCase()
    setRecherche(ine)
    setErreur(null)
    setResultat(null)
    if (!inePlausible(ine)) {
      setErreur("L'INE doit commencer par GN suivi de 12 chiffres (ex. GN260000000101).")
      return
    }
    setEnCours(true)
    try {
      setResultat(await eleveParIne(ine))
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Recherche impossible')
    } finally {
      setEnCours(false)
    }
  }

  return (
    <Card withBorder padding="md">
      <Stack>
        <Title order={4}>Retrouver un eleve par son INE</Title>
        <Text size="sm" c="dimmed">
          Identifiant national attribue une seule fois, conserve d'une annee a l'autre et d'un etablissement a
          l'autre.
        </Text>
        <Group align="flex-end">
          <TextInput
            label="INE"
            placeholder="GN260000000101"
            value={saisie}
            onChange={(e) => setSaisie(e.currentTarget.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') void chercher()
            }}
            w={240}
          />
          <Button loading={enCours} onClick={() => void chercher()}>
            Rechercher
          </Button>
        </Group>
        {erreur !== null && (
          <Alert color="red" variant="light">
            {erreur}
            {recherche !== null && (
              <Text size="sm" mt={4}>
                Verifiez le numero saisi : une cle de controle permet de detecter une faute de frappe avant meme
                d'interroger le serveur.
              </Text>
            )}
          </Alert>
        )}
        {resultat !== null && (
          <Stack gap={4}>
            <Text fw={500}>
              {resultat.prenom} {resultat.nom} - matricule {resultat.matricule}
            </Text>
            <Text size="sm">INE : {resultat.ine}</Text>
            <Text size="sm">Ne(e) le {formaterDate(resultat.dateNaissance)}</Text>
            <Text size="sm">Etat civil : {libelleActe(resultat.typeActeNaissance)}</Text>
            {resultat.sansActe && (
              <Alert color="orange" variant="light">
                Aucun acte de naissance enregistre : a regulariser avant une inscription aux examens nationaux.
              </Alert>
            )}
            <Group>
              <Button size="compact-sm" variant="light" onClick={() => setCorriger(true)}>
                Corriger l'etat civil
              </Button>
            </Group>
          </Stack>
        )}
      </Stack>
      {corriger && resultat !== null && (
        <EtatCivilModal eleveId={resultat.id} ine={resultat.ine} onFermer={() => setCorriger(false)} />
      )}
    </Card>
  )
}
