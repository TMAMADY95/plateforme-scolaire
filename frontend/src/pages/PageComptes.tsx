import { useState } from 'react'
import { Alert, Button, Group, Stack, Text, Title } from '@mantine/core'
import { creerPersonnel } from '../api/endpoints'
import { useUtilisateur } from '../auth/useAuth'
import { etablissementCourant } from '../auth/roles'
import DirectionModal from './ministere/DirectionModal'
import ListeComptes from './comptes/ListeComptes'
import { useQueryClient } from '@tanstack/react-query'

/** Comptes de l'établissement (direction) : personnel, enseignants, parents et élèves. */
export default function PageComptes() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const queryClient = useQueryClient()
  const [creation, setCreation] = useState(false)

  if (etablissementId === undefined) {
    return (
      <Alert color="orange" variant="light">
        Aucun établissement n'est associé à votre compte.
      </Alert>
    )
  }

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Comptes</Title>
        <Button onClick={() => setCreation(true)}>Nouveau personnel administratif</Button>
      </Group>
      <Text c="dimmed" size="sm">
        Réinitialisez le mot de passe d'une personne qui l'a oublié : vous lui communiquez un mot de passe
        temporaire qu'elle devra remplacer dès sa connexion.
      </Text>
      <ListeComptes etablissementId={etablissementId} avecRecherche />
      {creation && (
        <DirectionModal
          titre="Nouveau personnel administratif"
          messageSucces="Compte de personnel créé"
          creer={async (requete) => {
            const compte = await creerPersonnel(etablissementId, requete)
            void queryClient.invalidateQueries({ queryKey: ['comptes'] })
            return compte
          }}
          onFermer={() => setCreation(false)}
        />
      )}
    </Stack>
  )
}
