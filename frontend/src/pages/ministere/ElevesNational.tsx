import { useDeferredValue, useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, Pagination, Select, SimpleGrid, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  elevesNationaux,
  listerCycles,
  listerEtablissements,
  listerNiveaux,
  listerTypesEtablissement,
  listerZones,
  telechargerElevesPdf,
} from '../../api/endpoints'
import type { FiltresEleves } from '../../api/types'
import EnteteSection from '../../components/EnteteSection'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'
import { formaterDate } from '../../utils/dates'

const TAILLE = 25

const STATUTS = [
  { value: 'ACCEPTEE', label: 'Inscrits' },
  { value: 'EN_ATTENTE', label: 'En attente' },
  { value: 'REFUSEE', label: 'Refusées' },
  { value: 'ANNULEE', label: 'Annulées' },
  { value: 'TRANSFEREE', label: 'Transférés' },
  { value: 'TOUS', label: 'Tous les statuts' },
]

/** Liste nationale des élèves : filtres combinables, pagination et export PDF. */
export default function ElevesNational() {
  const annee = useAnneeScolaire()
  const [recherche, setRecherche] = useState('')
  const q = useDeferredValue(recherche)
  const [zone, setZone] = useState<string | null>(null)
  const [etablissement, setEtablissement] = useState<string | null>(null)
  const [secteur, setSecteur] = useState<string | null>(null)
  const [type, setType] = useState<string | null>(null)
  const [cycle, setCycle] = useState<string | null>(null)
  const [niveau, setNiveau] = useState<string | null>(null)
  const [sexe, setSexe] = useState<string | null>(null)
  const [statut, setStatut] = useState<string | null>('ACCEPTEE')
  const [page, setPage] = useState(1)

  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones })
  const etablissements = useQuery({ queryKey: ['etablissements'], queryFn: listerEtablissements })
  const types = useQuery({ queryKey: ['types-etablissement'], queryFn: listerTypesEtablissement })
  const cycles = useQuery({ queryKey: ['cycles'], queryFn: listerCycles })
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })

  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const filtres: FiltresEleves | null =
    anneeId === null
      ? null
      : { anneeScolaireId: anneeId, q, zoneId: zone, etablissementId: etablissement, secteur, typeEtablissementId: type, cycleId: cycle, niveauId: niveau, sexe, statut }

  const liste = useQuery({
    queryKey: ['eleves-nationaux', filtres, page],
    queryFn: () => elevesNationaux(filtres as FiltresEleves, page - 1, TAILLE),
    enabled: filtres !== null,
    placeholderData: (precedent) => precedent,
  })

  const pdf = useMutation({
    mutationFn: () => telechargerElevesPdf(filtres as FiltresEleves),
    onError: (e) => notifications.show({ color: 'red', title: 'Export impossible', message: e.message }),
  })

  /** Chaque changement de filtre ramène à la première page. */
  const changer = (fixer: (v: string | null) => void) => (v: string | null) => {
    fixer(v)
    setPage(1)
  }

  const niveauxVisibles = (niveaux.data ?? []).filter((n) => cycle === null || String(n.cycleId) === cycle)
  const reinitialiser = () => {
    setRecherche('')
    setZone(null)
    setEtablissement(null)
    setSecteur(null)
    setType(null)
    setCycle(null)
    setNiveau(null)
    setSexe(null)
    setStatut('ACCEPTEE')
    setPage(1)
  }

  return (
    <Stack mt="md" gap="lg">
      <EnteteSection
        titre="Élèves"
        description="Tous les élèves du réseau. Combinez les filtres puis téléchargez la liste en PDF."
        action={
          <Button variant="default" loading={pdf.isPending} disabled={filtres === null} onClick={() => pdf.mutate()}>
            Télécharger en PDF
          </Button>
        }
      />

      <Card withBorder padding="md">
        <Stack gap="sm">
          <Group align="flex-end" grow>
            <TextInput
              label="Rechercher"
              placeholder="Nom, prénom ou matricule"
              value={recherche}
              onChange={(e) => {
                setRecherche(e.currentTarget.value)
                setPage(1)
              }}
            />
            <Select label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={(v) => { annee.setAnneeChoisie(v); setPage(1) }} allowDeselect={false} />
            <Select label="Statut d'inscription" data={STATUTS} value={statut} onChange={changer(setStatut)} allowDeselect={false} />
          </Group>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <Select
              label="Zone"
              placeholder="Toutes (sous-zones comprises)"
              data={(zones.data ?? []).map((z) => ({ value: String(z.id), label: `${z.nom} (${z.typeZone})` }))}
              value={zone}
              onChange={changer(setZone)}
              searchable
              clearable
            />
            <Select
              label="Établissement"
              placeholder="Tous"
              data={(etablissements.data ?? []).map((e) => ({ value: String(e.id), label: e.nom }))}
              value={etablissement}
              onChange={changer(setEtablissement)}
              searchable
              clearable
            />
            <Select
              label="Secteur"
              placeholder="Tous"
              data={[{ value: 'PUBLIC', label: 'Public' }, { value: 'PRIVE', label: 'Privé' }]}
              value={secteur}
              onChange={changer(setSecteur)}
              clearable
            />
            <Select
              label="Type d'établissement"
              placeholder="Tous"
              data={(types.data ?? []).map((t) => ({ value: String(t.id), label: t.libelle }))}
              value={type}
              onChange={changer(setType)}
              clearable
            />
            <Select
              label="Cycle"
              placeholder="Tous"
              data={(cycles.data ?? []).map((c) => ({ value: String(c.id), label: c.libelle }))}
              value={cycle}
              onChange={(v) => {
                setCycle(v)
                setNiveau(null)
                setPage(1)
              }}
              clearable
            />
            <Select
              label="Niveau"
              placeholder="Tous"
              data={niveauxVisibles.map((n) => ({ value: String(n.id), label: n.libelle }))}
              value={niveau}
              onChange={changer(setNiveau)}
              searchable
              clearable
            />
            <Select
              label="Sexe"
              placeholder="Tous"
              data={[{ value: 'F', label: 'Filles' }, { value: 'M', label: 'Garçons' }, { value: 'N', label: 'Non précisé' }]}
              value={sexe}
              onChange={changer(setSexe)}
              clearable
            />
            <Group align="flex-end">
              <Button variant="subtle" onClick={reinitialiser}>
                Réinitialiser les filtres
              </Button>
            </Group>
          </SimpleGrid>
        </Stack>
      </Card>

      {liste.isError && (
        <Alert color="red" variant="light">
          {liste.error.message}
        </Alert>
      )}
      {liste.isPending && filtres !== null && <Loader />}

      {liste.data && (
        <Card withBorder padding="md">
          <Text size="sm" c="dimmed" mb="sm">
            {liste.data.total} élève{liste.data.total > 1 ? 's' : ''} correspondant aux filtres
          </Text>
          {liste.data.elements.length === 0 ? (
            <Text c="dimmed" ta="center" py="lg">
              Aucun élève ne correspond à ces critères.
            </Text>
          ) : (
            <Table.ScrollContainer minWidth={900}>
              <Table highlightOnHover verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Matricule</Table.Th>
                    <Table.Th>Élève</Table.Th>
                    <Table.Th>Sexe</Table.Th>
                    <Table.Th>Naissance</Table.Th>
                    <Table.Th>Établissement</Table.Th>
                    <Table.Th>Zone</Table.Th>
                    <Table.Th>Classe</Table.Th>
                    <Table.Th>Statut</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {liste.data.elements.map((e) => (
                    <Table.Tr key={e.inscriptionId}>
                      <Table.Td>{e.matricule}</Table.Td>
                      <Table.Td>
                        {e.nom} {e.prenom}
                      </Table.Td>
                      <Table.Td>{e.sexe ?? '—'}</Table.Td>
                      <Table.Td>{e.dateNaissance ? formaterDate(e.dateNaissance) : '—'}</Table.Td>
                      <Table.Td>
                        {e.etablissement}
                        <Text size="xs" c="dimmed">
                          {e.secteur === 'PRIVE' ? 'Privé' : 'Public'}
                          {e.typeEtablissement ? ` · ${e.typeEtablissement}` : ''}
                        </Text>
                      </Table.Td>
                      <Table.Td>{e.zone ?? '—'}</Table.Td>
                      <Table.Td>
                        {e.classe ?? '—'}
                        {e.cycle && (
                          <Text size="xs" c="dimmed">
                            {e.cycle}
                          </Text>
                        )}
                      </Table.Td>
                      <Table.Td>
                        <Badge variant="light" color={e.statut === 'ACCEPTEE' ? 'green' : 'gray'}>
                          {STATUTS.find((s) => s.value === e.statut)?.label ?? e.statut}
                        </Badge>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
          {liste.data.totalPages > 1 && (
            <Group justify="center" mt="md">
              <Pagination total={liste.data.totalPages} value={page} onChange={setPage} />
            </Group>
          )}
        </Card>
      )}
    </Stack>
  )
}
