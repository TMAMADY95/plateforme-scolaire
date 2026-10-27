import { useState } from 'react'
import { Alert, Button, Group, Loader, Select, Stack, Table, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { annulerInscription, deciderInscription, listerInscriptions } from '../api/endpoints'
import type { Inscription } from '../api/types'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import BadgeStatutInscription from '../components/BadgeStatutInscription'
import ConfirmationModal from '../components/ConfirmationModal'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { formaterDate } from '../utils/dates'
import AcceptationModal from './inscriptions/AcceptationModal'
import NouvelleInscriptionModal from './inscriptions/NouvelleInscriptionModal'

const STATUTS = [
  { value: 'EN_ATTENTE', label: 'En attente' },
  { value: 'ACCEPTEE', label: 'Acceptées' },
  { value: 'REFUSEE', label: 'Refusées' },
  { value: 'ANNULEE', label: 'Annulées' },
]

interface Confirmation {
  inscription: Inscription
  action: 'refuser' | 'annuler'
}

export default function PageInscriptions() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const queryClient = useQueryClient()
  const { annees, anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const [statut, setStatut] = useState<string | null>(null)
  const [nouvelleOuverte, { open, close }] = useDisclosure()
  const [aAccepter, setAAccepter] = useState<Inscription | null>(null)
  const [aConfirmer, setAConfirmer] = useState<Confirmation | null>(null)

  const inscriptions = useQuery({
    queryKey: ['inscriptions', etablissementId, anneeId, statut],
    queryFn: () => listerInscriptions(etablissementId as number, Number(anneeId), statut ?? undefined),
    enabled: etablissementId !== undefined && anneeId !== null,
  })

  const traitement = useMutation({
    mutationFn: ({ inscription, action }: Confirmation) =>
      action === 'refuser'
        ? deciderInscription(inscription.id, { decision: 'REFUSEE' })
        : annulerInscription(inscription.id),
    onSuccess: (_resultat, { action }) => {
      void queryClient.invalidateQueries({ queryKey: ['inscriptions', etablissementId] })
      notifications.show({
        color: 'green',
        message: action === 'refuser' ? 'Inscription refusée.' : 'Inscription annulée.',
      })
      setAConfirmer(null)
    },
    onError: (erreur) => {
      notifications.show({ color: 'red', title: 'Action impossible', message: erreur.message })
      setAConfirmer(null)
    },
  })

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
        <Title order={2}>Inscriptions</Title>
        <Button onClick={open} disabled={annees.isPending}>
          Nouvelle inscription
        </Button>
      </Group>

      <Group>
        <Select
          label="Année scolaire"
          data={options}
          value={anneeId}
          onChange={setAnneeChoisie}
          allowDeselect={false}
          w={200}
        />
        <Select label="Statut" placeholder="Tous" data={STATUTS} value={statut} onChange={setStatut} clearable w={200} />
      </Group>

      {(annees.isError || inscriptions.isError) && (
        <Alert color="red" variant="light">
          {(annees.error ?? inscriptions.error)?.message}
        </Alert>
      )}
      {inscriptions.isPending && anneeId !== null && <Loader />}

      {inscriptions.data && inscriptions.data.length === 0 && (
        <Text c="dimmed">Aucune inscription pour ces critères.</Text>
      )}
      {inscriptions.data && inscriptions.data.length > 0 && (
        <Table.ScrollContainer minWidth={760}>
          <Table striped highlightOnHover withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Élève</Table.Th>
                <Table.Th>Matricule</Table.Th>
                <Table.Th>Naissance</Table.Th>
                <Table.Th>Demande</Table.Th>
                <Table.Th>Statut</Table.Th>
                <Table.Th>Classe</Table.Th>
                <Table.Th>Actions</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {inscriptions.data.map((i) => (
                <Table.Tr key={i.id}>
                  <Table.Td>
                    {i.eleve.prenom} {i.eleve.nom}
                  </Table.Td>
                  <Table.Td>{i.eleve.matricule}</Table.Td>
                  <Table.Td>{formaterDate(i.eleve.dateNaissance)}</Table.Td>
                  <Table.Td>{formaterDate(i.dateDemande)}</Table.Td>
                  <Table.Td>
                    <BadgeStatutInscription statut={i.statut} />
                  </Table.Td>
                  <Table.Td>{i.classeNom ?? '—'}</Table.Td>
                  <Table.Td>
                    <Group gap="xs" wrap="nowrap">
                      {i.statut === 'EN_ATTENTE' && (
                        <>
                          <Button size="compact-sm" color="green" variant="light" onClick={() => setAAccepter(i)}>
                            Accepter
                          </Button>
                          <Button
                            size="compact-sm"
                            color="red"
                            variant="light"
                            onClick={() => setAConfirmer({ inscription: i, action: 'refuser' })}
                          >
                            Refuser
                          </Button>
                        </>
                      )}
                      {(i.statut === 'EN_ATTENTE' || i.statut === 'ACCEPTEE') && (
                        <Button
                          size="compact-sm"
                          color="gray"
                          variant="subtle"
                          onClick={() => setAConfirmer({ inscription: i, action: 'annuler' })}
                        >
                          Annuler
                        </Button>
                      )}
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {nouvelleOuverte && (
        <NouvelleInscriptionModal
          etablissementId={etablissementId}
          anneeIdInitiale={anneeId}
          optionsAnnees={options}
          onFermer={close}
        />
      )}
      {aAccepter !== null && <AcceptationModal inscription={aAccepter} onFermer={() => setAAccepter(null)} />}
      {aConfirmer !== null && (
        <ConfirmationModal
          titre={aConfirmer.action === 'refuser' ? "Refuser l'inscription" : "Annuler l'inscription"}
          message={`${
            aConfirmer.action === 'refuser' ? 'Refuser' : 'Annuler'
          } l'inscription de ${aConfirmer.inscription.eleve.prenom} ${aConfirmer.inscription.eleve.nom} ?`}
          libelleConfirmer={aConfirmer.action === 'refuser' ? 'Refuser' : "Annuler l'inscription"}
          enCours={traitement.isPending}
          onConfirmer={() => traitement.mutate(aConfirmer)}
          onFermer={() => setAConfirmer(null)}
        />
      )}
    </Stack>
  )
}
