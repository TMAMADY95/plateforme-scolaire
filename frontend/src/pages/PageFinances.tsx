import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, SimpleGrid, Stack, Table, Tabs, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { annulerFacture, bilanFinancier, facturesEnRetard, listerFactures } from '../api/endpoints'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { useAuth } from '../auth/useAuth'
import { etablissementCourant, etablissementsGeres } from '../auth/roles'
import { formaterMontant } from '../utils/monnaie'
import { formaterDate } from '../utils/dates'
import type { Facture } from '../api/types'
import ConfirmationModal from '../components/ConfirmationModal'
import ExonerationModal from './finances/ExonerationModal'
import GenererFacturesModal from './finances/GenererFacturesModal'
import GestionFrais from './finances/GestionFrais'
import PaiementsModal from './finances/PaiementsModal'

/** Vue direction : synthèse financière, impayés et liste des factures. */
export default function PageFinances() {
  const { utilisateur } = useAuth()
  const annee = useAnneeScolaire()
  const queryClient = useQueryClient()
  const [pourPaiements, setPourPaiements] = useState<Facture | null>(null)
  const [pourExoneration, setPourExoneration] = useState<Facture | null>(null)
  const [aAnnuler, setAAnnuler] = useState<Facture | null>(null)
  const [generation, setGeneration] = useState(false)
  const [erreurAction, setErreurAction] = useState<string | null>(null)

  const etablissementId = utilisateur ? etablissementCourant(utilisateur) : undefined
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const pret = etablissementId !== undefined && anneeId !== null

  const bilan = useQuery({
    queryKey: ['bilan-financier', etablissementId, anneeId],
    queryFn: () => bilanFinancier(etablissementId as number, anneeId as number),
    enabled: pret,
  })

  const factures = useQuery({
    queryKey: ['factures-etab', etablissementId, anneeId],
    queryFn: () => listerFactures(etablissementId as number, anneeId as number),
    enabled: pret,
  })

  const retards = useQuery({
    queryKey: ['factures-retard', etablissementId],
    queryFn: () => facturesEnRetard(etablissementId as number),
    enabled: etablissementId !== undefined,
  })

  const annulation = useMutation({
    mutationFn: (f: Facture) => annulerFacture(f.id),
    onSuccess: (f) => {
      for (const cle of ['factures-etab', 'bilan-financier', 'factures-retard']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Facture annulée', message: f.numero })
      setAAnnuler(null)
    },
    onError: (e) => {
      setAAnnuler(null)
      setErreurAction(e.message)
    },
  })

  if (utilisateur === null || etablissementsGeres(utilisateur).length === 0) {
    return (
      <Alert color="blue" variant="light">
        Aucun établissement n'est rattaché à votre compte.
      </Alert>
    )
  }

  const libelleAnnee = annee.options.find((o) => o.value === annee.anneeId)?.label

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={2}>Finances</Title>
          <Text c="dimmed" size="sm">
            {libelleAnnee ? `Année scolaire ${libelleAnnee}` : 'Année scolaire en cours'}
          </Text>
        </div>
        <Button disabled={!pret} onClick={() => setGeneration(true)}>
          Générer les factures
        </Button>
      </Group>
      {erreurAction !== null && (
        <Alert color="red" variant="light" withCloseButton onClose={() => setErreurAction(null)}>
          {erreurAction}
        </Alert>
      )}
      <Tabs defaultValue="factures">
        <Tabs.List>
          <Tabs.Tab value="factures">Factures</Tabs.Tab>
          <Tabs.Tab value="bareme">Barème des frais</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="bareme" pt="md">
          {pret && <GestionFrais etablissementId={etablissementId} anneeScolaireId={anneeId} />}
        </Tabs.Panel>
        <Tabs.Panel value="factures" pt="md">
        <Stack>

      {(bilan.isPending || factures.isPending || retards.isPending) && pret && <Loader />}
      {(bilan.isError || factures.isError || retards.isError) && (
        <Alert color="red" variant="light">
          {(bilan.error ?? factures.error ?? retards.error)?.message}
        </Alert>
      )}

      {bilan.data && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
          <Card withBorder padding="md">
            <Text size="xs" c="dimmed">
              Total dû
            </Text>
            <Text fw={700} size="lg">
              {formaterMontant(bilan.data.totalDu, bilan.data.devise)}
            </Text>
          </Card>
          <Card withBorder padding="md">
            <Text size="xs" c="dimmed">
              Encaissé
            </Text>
            <Text fw={700} size="lg" c="green">
              {formaterMontant(bilan.data.totalPaye, bilan.data.devise)}
            </Text>
          </Card>
          <Card withBorder padding="md">
            <Text size="xs" c="dimmed">
              Restant dû
            </Text>
            <Text fw={700} size="lg" c="red">
              {formaterMontant(bilan.data.totalRestant, bilan.data.devise)}
            </Text>
          </Card>
          <Card withBorder padding="md">
            <Text size="xs" c="dimmed">
              Exonéré
            </Text>
            <Text fw={700} size="lg">
              {formaterMontant(bilan.data.totalExonere, bilan.data.devise)}
            </Text>
          </Card>
        </SimpleGrid>
      )}

      {retards.data && retards.data.length > 0 && (
        <>
          <Title order={3} mt="md">
            Factures en retard ({retards.data.length})
          </Title>
          <Table.ScrollContainer minWidth={600}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Numéro</Table.Th>
                  <Table.Th>Élève</Table.Th>
                  <Table.Th>Libellé</Table.Th>
                  <Table.Th>Échéance</Table.Th>
                  <Table.Th>Reste à payer</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {retards.data.map((f) => (
                  <Table.Tr key={f.id}>
                    <Table.Td>{f.numero}</Table.Td>
                    <Table.Td>
                      {f.elevePrenom} {f.eleveNom}
                    </Table.Td>
                    <Table.Td>{f.libelle}</Table.Td>
                    <Table.Td>{f.echeance ? formaterDate(f.echeance) : '—'}</Table.Td>
                    <Table.Td>{formaterMontant(f.resteAPayer, f.devise)}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </>
      )}

      <Title order={3} mt="md">
        Toutes les factures
      </Title>
      {factures.data && factures.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucune facture émise pour cette année. Utilisez la génération pour créer les factures
          à partir de la grille tarifaire.
        </Alert>
      )}
      {factures.data && factures.data.length > 0 && (
        <Table.ScrollContainer minWidth={720}>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Numéro</Table.Th>
                <Table.Th>Élève</Table.Th>
                <Table.Th>Libellé</Table.Th>
                <Table.Th>Montant dû</Table.Th>
                <Table.Th>Payé</Table.Th>
                <Table.Th>Reste</Table.Th>
                <Table.Th>Statut</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {factures.data.map((f) => (
                <Table.Tr key={f.id}>
                  <Table.Td>{f.numero}</Table.Td>
                  <Table.Td>
                    {f.elevePrenom} {f.eleveNom}
                  </Table.Td>
                  <Table.Td>{f.libelle}</Table.Td>
                  <Table.Td>{formaterMontant(f.montantDu, f.devise)}</Table.Td>
                  <Table.Td>{formaterMontant(f.montantPaye, f.devise)}</Table.Td>
                  <Table.Td>{formaterMontant(f.resteAPayer, f.devise)}</Table.Td>
                  <Table.Td>
                    <Group gap="xs">
                      <Badge variant="light">{f.statut}</Badge>
                      {f.enRetard && (
                        <Badge color="red" variant="light">
                          En retard
                        </Badge>
                      )}
                    </Group>
                  </Table.Td>
                  <Table.Td>
                    <Group gap="xs" wrap="nowrap">
                      <Button size="compact-xs" variant="light" onClick={() => setPourPaiements(f)}>
                        Paiements
                      </Button>
                      {f.statut === 'IMPAYEE' && f.montantPaye === 0 && f.resteAPayer > 0 && (
                        <Button size="compact-xs" variant="light" color="orange" onClick={() => setPourExoneration(f)}>
                          Exonérer
                        </Button>
                      )}
                      {f.statut !== 'ANNULEE' && f.montantPaye === 0 && (
                        <Button size="compact-xs" variant="subtle" color="red" onClick={() => setAAnnuler(f)}>
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
        </Stack>
        </Tabs.Panel>
      </Tabs>
      {pourPaiements && <PaiementsModal facture={pourPaiements} onFermer={() => setPourPaiements(null)} />}
      {pourExoneration && <ExonerationModal facture={pourExoneration} onFermer={() => setPourExoneration(null)} />}
      {generation && pret && (
        <GenererFacturesModal
          etablissementId={etablissementId}
          anneeScolaireId={anneeId}
          devise={bilan.data?.devise ?? null}
          onFermer={() => setGeneration(false)}
        />
      )}
      {aAnnuler && (
        <ConfirmationModal
          titre="Annuler la facture"
          message={`Annuler la facture ${aAnnuler.numero} (${aAnnuler.elevePrenom} ${aAnnuler.eleveNom}) ? Cette action est définitive.`}
          libelleConfirmer="Annuler la facture"
          enCours={annulation.isPending}
          onConfirmer={() => annulation.mutate(aAnnuler)}
          onFermer={() => setAAnnuler(null)}
        />
      )}
    </Stack>
  )
}
