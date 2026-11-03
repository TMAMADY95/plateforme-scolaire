import { useState } from 'react'
import { Alert, Button, Card, Group, Stack, Text, TextInput, Title } from '@mantine/core'
import { eleveParIne } from '../../api/endpointsEcole'
import { inePlausible, libelleActe } from '../../api/typesEcole'
import { formaterDate } from '../../utils/dates'
import EtatCivilModal from './EtatCivilModal'

/**
 * Retrouver un élève par son identifiant national (INE), où qu'il soit scolarisé.
 * Sert au transfert d'un élève, ou à la vérification d'un dossier avant les examens.
 * L'INE n'est pas un secret : le serveur renvoie le même message pour un INE inconnu
 * et pour un élève hors du périmètre de l'utilisateur.
 */
export default function RechercheParIne() {
  const [saisie, setSaisie] = useState('')
  const [recherche, setRecherche] = useState<string | null>(null)
  const [resultat, setResultat] = useState<Awaited<ReturnType<typeof eleveParIne>> | null>(null)
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
        <Title order={4}>Retrouver un élève par son INE</Title>
        <Text size="sm" c="dimmed">
          Identifiant national attribué une seule fois, conservé d'une année à l'autre et d'un établissement à
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
                Vérifiez le numéro saisi : une clé de contrôle permet de détecter une faute de frappe avant même
                d'interroger le serveur.
              </Text>
            )}
          </Alert>
        )}
        {resultat !== null && (
          <Stack gap={4}>
            <Text fw={500}>
              {resultat.prenom} {resultat.nom} — matricule {resultat.matricule}
            </Text>
            <Text size="sm">INE : {resultat.ine}</Text>
            <Text size="sm">Né(e) le {formaterDate(resultat.dateNaissance)}</Text>
            <Text size="sm">État civil : {libelleActe(resultat.typeActeNaissance)}</Text>
            {resultat.sansActe && (
              <Alert color="orange" variant="light">
                Aucun acte de naissance enregistré : à régulariser avant une inscription aux examens nationaux.
              </Alert>
            )}
            <Group>
              <Button size="compact-sm" variant="light" onClick={() => setCorriger(true)}>
                Corriger l'état civil
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
