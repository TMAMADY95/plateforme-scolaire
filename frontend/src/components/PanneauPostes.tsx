import { useState } from 'react'
import { Alert, Button, Card, Group, Loader, NumberInput, Select, SimpleGrid, Stack, Table, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { definirPostes, listerZones } from '../api/endpoints'
import type { LignePostes, RapportPostes } from '../api/types'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { exporterCsv } from '../utils/exportCsv'
import { BadgeSecteur } from './BadgesEtablissement'
import CarteChiffre from './CarteChiffre'
import EnteteSection from './EnteteSection'
import MutationEnseignantsModal from './MutationEnseignantsModal'

const nombre = (n: number) => new Intl.NumberFormat('fr-FR').format(n)

interface Props {
  cle: string
  charger: (anneeScolaireId: number, zoneId?: number) => Promise<RapportPostes>
  /** Ministère : filtre par région, préfecture ou commune. */
  ministere?: boolean
}

/** Besoins en enseignants : postes prévus, postes vacants, besoin estimé selon la norme, et mutations. */
export default function PanneauPostes({ cle, charger, ministere = false }: Props) {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const [zoneId, setZoneId] = useState<string | null>(null)
  const [pour, setPour] = useState<LignePostes | null>(null)
  const filtreZone = ministere && zoneId ? Number(zoneId) : undefined

  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones, enabled: ministere })
  const rapport = useQuery({
    queryKey: ['postes', cle, anneeId, filtreZone ?? null],
    queryFn: () => charger(anneeId as number, filtreZone),
    enabled: anneeId !== null,
  })

  return (
    <Stack mt="md" gap="lg">
      <EnteteSection
        titre="Postes d'enseignants"
        description="Postes prévus, postes vacants et besoin estimé par établissement ; mutation des enseignants d'un établissement à un autre."
        action={<Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />}
      />
      {ministere && (
        <Select
          label="Région, préfecture ou commune"
          placeholder="Tout le pays"
          clearable
          searchable
          data={(zones.data ?? []).map((z) => ({ value: String(z.id), label: `${z.nom} (${z.typeZone})` }))}
          value={zoneId}
          onChange={setZoneId}
          w={280}
        />
      )}
      {rapport.isPending && anneeId !== null && <Loader />}
      {rapport.isError && (
        <Alert color="red" variant="light">
          {rapport.error.message}
        </Alert>
      )}
      {rapport.data && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Enseignants en poste" valeur={nombre(rapport.data.totaux.enseignants)} icone="enseignants" couleur="violet" detail={`${nombre(rapport.data.totaux.eleves)} élèves`} />
            <CarteChiffre libelle="Postes prévus" valeur={nombre(rapport.data.totaux.postes)} icone="inscriptions" detail="là où ils sont renseignés" />
            <CarteChiffre libelle="Postes vacants" valeur={nombre(rapport.data.totaux.vacants)} icone="alerte" couleur="orange" />
            <CarteChiffre
              libelle="Besoin estimé"
              valeur={nombre(rapport.data.totaux.besoinEstime)}
              icone="alerte"
              couleur="red"
              detail={`norme : ${rapport.data.normeElevesParEnseignant} élèves par enseignant`}
            />
          </SimpleGrid>
          <Card withBorder padding="lg">
            <EnteteSection
              titre="Par établissement"
              description="Triés par besoin estimé décroissant."
              action={
                <Button
                  size="xs"
                  variant="light"
                  disabled={rapport.data.lignes.length === 0}
                  onClick={() =>
                    exporterCsv(
                      'postes-enseignants',
                      [
                        { titre: 'Code', valeur: (l: LignePostes) => l.codeOfficiel },
                        { titre: 'Établissement', valeur: (l) => l.nom },
                        { titre: 'Zone', valeur: (l) => l.zone },
                        { titre: 'Élèves', valeur: (l) => l.eleves },
                        { titre: 'Enseignants', valeur: (l) => l.enseignants },
                        { titre: 'Postes prévus', valeur: (l) => l.postes },
                        { titre: 'Postes vacants', valeur: (l) => l.vacants },
                        { titre: 'En surnombre', valeur: (l) => l.enSurnombre },
                        { titre: 'Besoin estimé', valeur: (l) => l.besoinEstime },
                      ],
                      rapport.data.lignes,
                    )
                  }
                >
                  Exporter (Excel)
                </Button>
              }
            />
            {rapport.data.lignes.length === 0 ? (
              <Text c="dimmed" mt="md">
                Aucun établissement dans ce périmètre.
              </Text>
            ) : (
              <Table.ScrollContainer minWidth={900} mt="md">
                <Table highlightOnHover verticalSpacing="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Établissement</Table.Th>
                      <Table.Th>Secteur</Table.Th>
                      <Table.Th ta="right">Élèves</Table.Th>
                      <Table.Th ta="right">Enseignants</Table.Th>
                      <Table.Th ta="right">Élèves / ens.</Table.Th>
                      <Table.Th>Postes prévus</Table.Th>
                      <Table.Th ta="right">Vacants</Table.Th>
                      <Table.Th ta="right">Besoin estimé</Table.Th>
                      <Table.Th />
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {rapport.data.lignes.map((l) => (
                      <LignePoste key={`${l.id}-${l.postes}`} ligne={l} onEnseignants={() => setPour(l)} />
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            )}
          </Card>
        </>
      )}
      {pour && <MutationEnseignantsModal etablissement={pour} onFermer={() => setPour(null)} />}
    </Stack>
  )
}

function LignePoste({ ligne, onEnseignants }: { ligne: LignePostes; onEnseignants: () => void }) {
  const queryClient = useQueryClient()
  const [postes, setPostes] = useState<number | string>(ligne.postes ?? '')
  const [erreur, setErreur] = useState<string | null>(null)
  const modifie = (postes === '' ? null : Number(postes)) !== ligne.postes

  const enregistrement = useMutation({
    mutationFn: () => definirPostes(ligne.id, postes === '' ? null : Number(postes)),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['postes'] })
      notifications.show({ color: 'green', title: 'Postes enregistrés', message: ligne.nom })
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Table.Tr>
      <Table.Td>
        <Text fw={600}>{ligne.nom}</Text>
        <Text size="xs" c="dimmed">
          {ligne.codeOfficiel}
          {ligne.zone ? ` · ${ligne.zone}` : ''}
        </Text>
        {erreur && (
          <Text size="xs" c="red">
            {erreur}
          </Text>
        )}
      </Table.Td>
      <Table.Td>
        <BadgeSecteur secteur={ligne.secteur} />
      </Table.Td>
      <Table.Td ta="right">{nombre(ligne.eleves)}</Table.Td>
      <Table.Td ta="right">{nombre(ligne.enseignants)}</Table.Td>
      <Table.Td ta="right">{ligne.elevesParEnseignant === null ? '—' : String(ligne.elevesParEnseignant).replace('.', ',')}</Table.Td>
      <Table.Td>
        <Group gap={6} wrap="nowrap">
          <NumberInput aria-label="Postes prévus" min={0} allowDecimal={false} size="xs" w={80} value={postes} onChange={setPostes} />
          <Button size="compact-xs" variant="light" disabled={!modifie} loading={enregistrement.isPending} onClick={() => enregistrement.mutate()}>
            OK
          </Button>
        </Group>
      </Table.Td>
      <Table.Td ta="right">
        {ligne.vacants === null ? '—' : ligne.vacants}
        {ligne.enSurnombre > 0 ? ` (+${ligne.enSurnombre} en surnombre)` : ''}
      </Table.Td>
      <Table.Td ta="right" fw={ligne.besoinEstime > 0 ? 700 : 400} c={ligne.besoinEstime > 0 ? 'red' : undefined}>
        {ligne.besoinEstime}
      </Table.Td>
      <Table.Td>
        <Button size="compact-xs" variant="light" onClick={onEnseignants}>
          Enseignants
        </Button>
      </Table.Td>
    </Table.Tr>
  )
}
