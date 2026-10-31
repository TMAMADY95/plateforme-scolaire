import { useState } from 'react'
import { Alert, Badge, Button, Group, Stack, Text, TextInput } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listeCandidats, rejeterListe, transmettreListe, validerListe, verifierListe } from '../api/endpoints'
import type { ListeCandidats } from '../api/types'
import { estMinistere } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'

const LIBELLE = { BROUILLON: 'En préparation', TRANSMISE: 'Transmise', VALIDEE_REGION: 'Vérifiée par la région', VALIDEE: 'Validée' }
const COULEUR = { BROUILLON: 'gray', TRANSMISE: 'blue', VALIDEE_REGION: 'cyan', VALIDEE: 'green' }

/** Circuit de validation de la liste des candidats : l'établissement transmet, la région vérifie, le ministère valide. */
export default function ListeCandidatsPanneau({ etablissementId, examenId, supervision }: { etablissementId: number; examenId: number; supervision: boolean }) {
  const queryClient = useQueryClient()
  const utilisateur = useUtilisateur()
  const cle = ['liste-candidats', etablissementId, examenId]
  const donnees = useQuery({ queryKey: cle, queryFn: () => listeCandidats(etablissementId, examenId) })
  const [motif, setMotif] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const appliquer = (l: ListeCandidats) => {
    queryClient.setQueryData(cle, l)
    setErreur(null)
    setMotif('')
    void queryClient.invalidateQueries({ queryKey: ['preparation-examen', etablissementId, examenId] })
  }
  const options = { onSuccess: appliquer, onError: (e: Error) => setErreur(e.message) }
  const transmission = useMutation({ mutationFn: () => transmettreListe(etablissementId, examenId), ...options })
  const verification = useMutation({ mutationFn: () => verifierListe(etablissementId, examenId), ...options })
  const validation = useMutation({ mutationFn: () => validerListe(etablissementId, examenId), ...options })
  const rejet = useMutation({ mutationFn: () => rejeterListe(etablissementId, examenId, motif.trim()), ...options })
  const l = donnees.data
  if (!l) {
    return null
  }
  const ministere = estMinistere(utilisateur)
  const peutRejeter = supervision && (l.statut === 'TRANSMISE' || (l.statut === 'VALIDEE_REGION' && ministere))

  return (
    <Stack gap="xs">
      <Group gap="xs">
        <Text fw={600}>Liste des candidats</Text>
        <Badge color={COULEUR[l.statut]} variant="light">{LIBELLE[l.statut]}</Badge>
        <Text size="sm" c="dimmed">{l.candidats} candidat(s), {l.dossiersComplets} dossier(s) complet(s)</Text>
      </Group>
      {l.motifRejet && l.statut === 'BROUILLON' && <Alert color="orange" variant="light">Liste renvoyée : {l.motifRejet}</Alert>}
      {l.statut !== 'BROUILLON' && <Text size="xs" c="dimmed">La liste n'est plus modifiable tant qu'elle n'est pas rejetée.</Text>}
      <Group align="flex-end">
        {!supervision && l.statut === 'BROUILLON' && (
          <Button size="xs" loading={transmission.isPending} disabled={l.candidats === 0} onClick={() => transmission.mutate()}>
            Transmettre la liste
          </Button>
        )}
        {supervision && l.statut === 'TRANSMISE' && (
          <Button size="xs" loading={verification.isPending} onClick={() => verification.mutate()}>Marquer vérifiée</Button>
        )}
        {supervision && ministere && l.statut === 'VALIDEE_REGION' && (
          <Button size="xs" color="green" loading={validation.isPending} onClick={() => validation.mutate()}>Valider la liste définitive</Button>
        )}
        {peutRejeter && (
          <>
            <TextInput size="xs" placeholder="Motif du rejet" value={motif} onChange={(e) => setMotif(e.currentTarget.value)} w={260} />
            <Button size="xs" color="red" variant="light" disabled={motif.trim() === ''} loading={rejet.isPending} onClick={() => rejet.mutate()}>
              Renvoyer à l'établissement
            </Button>
          </>
        )}
      </Group>
      {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
    </Stack>
  )
}
