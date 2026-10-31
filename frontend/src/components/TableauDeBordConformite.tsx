import { useState } from 'react'
import type { ReactNode } from 'react'
import { ActionIcon, Alert, Badge, Button, Card, Checkbox, Group, Loader, Popover, Select, SimpleGrid, Stack, Table, Text, TextInput } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerCycles, listerZones } from '../api/endpoints'
import { useStockageLocal } from '../hooks/useStockageLocal'
import { exporterCsv } from '../utils/exportCsv'
import type { Conformite, FiltresConformite, LigneConformite } from '../api/types'
import { BadgeSecteur, BadgesCycles } from './BadgesEtablissement'
import CarteChiffre from './CarteChiffre'
import EnteteSection from './EnteteSection'
import type { NomIcone } from './Icone'
import SuiviEtablissementModal, { LIBELLES_AUTORISATION } from './SuiviEtablissementModal'

type Indicateur =
  | 'total'
  | 'publics'
  | 'prives'
  | 'autorises'
  | 'nonAutorises'
  | 'enAttente'
  | 'nonConformes'
  | 'inspectes'
  | 'securite'
  | 'sansDonnees'
  | 'fermes'
  | 'urgents'

const CARTES: { cle: Indicateur; libelle: string; icone: NomIcone; couleur: string; valeur: (c: Conformite) => number; garde: (l: LigneConformite) => boolean }[] = [
  { cle: 'total', libelle: 'Établissements', icone: 'ecole', couleur: 'marine', valeur: (c) => c.indicateurs.total, garde: () => true },
  { cle: 'publics', libelle: 'Écoles publiques', icone: 'ecole', couleur: 'blue', valeur: (c) => c.indicateurs.publics, garde: (l) => l.secteur === 'PUBLIC' },
  { cle: 'prives', libelle: 'Écoles privées', icone: 'ecole', couleur: 'grape', valeur: (c) => c.indicateurs.prives, garde: (l) => l.secteur === 'PRIVE' },
  { cle: 'autorises', libelle: 'Autorisés', icone: 'comptes', couleur: 'green', valeur: (c) => c.indicateurs.autorises, garde: (l) => l.autorisation === 'AUTORISE' },
  { cle: 'nonAutorises', libelle: 'Non autorisés', icone: 'alerte', couleur: 'red', valeur: (c) => c.indicateurs.nonAutorises, garde: (l) => l.autorisation === 'NON_AUTORISE' || l.autorisation === 'RETIRE' },
  { cle: 'enAttente', libelle: 'Autorisation en attente', icone: 'horaires', couleur: 'gray', valeur: (c) => c.indicateurs.autorisationEnAttente, garde: (l) => l.autorisation === 'EN_ATTENTE' },
  { cle: 'nonConformes', libelle: 'Non conformes', icone: 'alerte', couleur: 'orange', valeur: (c) => c.indicateurs.nonConformes, garde: (l) => l.nonConforme },
  { cle: 'inspectes', libelle: 'Inspectés (moins d\'un an)', icone: 'notes', couleur: 'teal', valeur: (c) => c.indicateurs.inspectes, garde: (l) => l.inspecte },
  { cle: 'securite', libelle: 'Problèmes de sécurité', icone: 'alerte', couleur: 'red', valeur: (c) => c.indicateurs.avecProblemeSecurite, garde: (l) => l.signalementsOuverts > 0 },
  { cle: 'sansDonnees', libelle: 'Données non transmises', icone: 'presences', couleur: 'yellow', valeur: (c) => c.indicateurs.sansDonnees, garde: (l) => l.sansDonnees },
  { cle: 'fermes', libelle: 'Suspendus ou fermés', icone: 'accueil', couleur: 'gray', valeur: (c) => c.indicateurs.fermes, garde: (l) => l.statut === 'SUSPENDU' || l.statut === 'FERME' },
  { cle: 'urgents', libelle: 'Inspection urgente', icone: 'alerte', couleur: 'red', valeur: (c) => c.indicateurs.urgents, garde: (l) => l.urgent },
]

const STATUTS = [
  { value: 'ACTIF', label: 'Actif' },
  { value: 'SUSPENDU', label: 'Suspendu' },
  { value: 'FERME', label: 'Fermé' },
]
const SECTEURS = [
  { value: 'PUBLIC', label: 'Public' },
  { value: 'PRIVE', label: 'Privé' },
]
const jour = (iso: string) => new Date(iso).toLocaleDateString('fr-FR')
const COULEUR_STATUT: Record<string, string> = { ACTIF: 'green', SUSPENDU: 'orange', FERME: 'gray' }

interface Props {
  /** Clé de cache : ministère ou région. */
  cle: string
  charger: (filtres: FiltresConformite) => Promise<Conformite>
  /** Ministère : filtre par zone et décision d'autorisation. */
  ministere?: boolean
}

/** Les indicateurs de conformité, d'inspection et de sécurité, avec le détail par établissement. */
export default function TableauDeBordConformite({ cle, charger, ministere = false }: Props) {
  const [filtres, setFiltres] = useState<FiltresConformite>({ zoneId: '', statut: '', secteur: '', cycleId: '' })
  const [actif, setActif] = useState<Indicateur>('total')
  const [choisi, setChoisi] = useState<number | null>(null)

  // Préférences propres à cet utilisateur sur cet appareil : filtres favoris et indicateurs affichés.
  const [favoris, setFavoris] = useStockageLocal<{ nom: string; filtres: FiltresConformite }[]>(`conformite.favoris.${cle}`, [])
  const [masquees, setMasquees] = useStockageLocal<Indicateur[]>(`conformite.masquees.${cle}`, [])
  const [nomFavori, setNomFavori] = useState('')

  const rapport = useQuery({ queryKey: ['conformite', cle, filtres], queryFn: () => charger(filtres) })
  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones, enabled: ministere })
  const cycles = useQuery({ queryKey: ['cycles'], queryFn: listerCycles })

  const carte = CARTES.find((c) => c.cle === actif) ?? CARTES[0]
  const lignes = (rapport.data?.lignes ?? []).filter(carte.garde)
  const ligneChoisie = rapport.data?.lignes.find((l) => l.id === choisi)
  const maj = (cleFiltre: keyof FiltresConformite) => (v: string | null) => setFiltres((f) => ({ ...f, [cleFiltre]: v ?? '' }))

  const exporter = (l: LigneConformite[], titre: string) =>
    exporterCsv(
      `conformite-${titre.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
      [
        { titre: 'Code', valeur: (x) => x.codeOfficiel },
        { titre: 'Établissement', valeur: (x) => x.nom },
        { titre: 'Zone', valeur: (x) => x.zone },
        { titre: 'Secteur', valeur: (x) => (x.secteur === 'PUBLIC' ? 'Public' : 'Privé') },
        { titre: 'Cycles', valeur: (x) => x.cycles.map((y) => y.libelle).join(', ') },
        { titre: 'Statut', valeur: (x) => STATUTS.find((s) => s.value === x.statut)?.label ?? x.statut },
        { titre: 'Autorisation', valeur: (x) => LIBELLES_AUTORISATION[x.autorisation] },
        { titre: 'Référence arrêté', valeur: (x) => x.reference },
        { titre: 'Dernière inspection', valeur: (x) => x.derniereInspection },
        { titre: 'Résultat', valeur: (x) => (x.resultatInspection === null ? '' : x.nonConforme ? 'Non conforme' : 'Conforme') },
        { titre: 'Signalements de sécurité ouverts', valeur: (x) => x.signalementsOuverts },
        { titre: 'Dernières données', valeur: (x) => x.derniereDonnee },
        { titre: 'Données non transmises', valeur: (x) => (x.sansDonnees ? 'Oui' : 'Non') },
        { titre: 'Inspection urgente', valeur: (x) => (x.urgent ? 'Oui' : 'Non') },
        { titre: 'Motifs', valeur: (x) => x.motifsUrgence.join(' ; ') },
      ],
      l,
    )

  return (
    <Stack mt="md" gap="lg">
      <EnteteSection
        titre="Conformité et inspections"
        description="Autorisation, inspections, sécurité et transmission des données. Cliquez sur un chiffre pour voir les établissements concernés."
      />
      <Group align="flex-end">
        {ministere && (
          <Select
            label="Région, préfecture ou commune"
            placeholder="Tout le pays"
            clearable
            searchable
            data={(zones.data ?? []).map((z) => ({ value: String(z.id), label: `${z.nom} (${z.typeZone})` }))}
            value={filtres.zoneId || null}
            onChange={maj('zoneId')}
            w={260}
          />
        )}
        <Select label="Statut" placeholder="Tous" clearable data={STATUTS} value={filtres.statut || null} onChange={maj('statut')} w={150} />
        <Select label="Secteur" placeholder="Tous" clearable data={SECTEURS} value={filtres.secteur || null} onChange={maj('secteur')} w={150} />
        <Select
          label="Niveau d'enseignement"
          placeholder="Tous"
          clearable
          data={(cycles.data ?? []).map((c) => ({ value: String(c.id), label: c.libelle }))}
          value={filtres.cycleId || null}
          onChange={maj('cycleId')}
          w={200}
        />
        <Popover width={300} position="bottom-end" shadow="md" withArrow>
          <Popover.Target>
            <Button variant="default">Favoris et affichage</Button>
          </Popover.Target>
          <Popover.Dropdown>
            <Stack gap="sm">
              <Text fw={600} size="sm">Filtres favoris</Text>
              {favoris.length === 0 && <Text size="xs" c="dimmed">Aucun favori. Réglez les filtres puis enregistrez-les.</Text>}
              {favoris.map((f) => (
                <Group key={f.nom} justify="space-between" wrap="nowrap">
                  <Button size="compact-sm" variant="light" onClick={() => setFiltres(f.filtres)}>{f.nom}</Button>
                  <ActionIcon variant="subtle" color="red" aria-label={`Supprimer le favori ${f.nom}`} onClick={() => setFavoris(favoris.filter((x) => x.nom !== f.nom))}>
                    ×
                  </ActionIcon>
                </Group>
              ))}
              <Group gap="xs" wrap="nowrap">
                <TextInput size="xs" placeholder="Nom du favori" maxLength={40} value={nomFavori} onChange={(e) => setNomFavori(e.currentTarget.value)} />
                <Button
                  size="xs"
                  disabled={nomFavori.trim() === ''}
                  onClick={() => {
                    const nom = nomFavori.trim()
                    setFavoris([...favoris.filter((x) => x.nom !== nom), { nom, filtres }].slice(-10))
                    setNomFavori('')
                  }}
                >
                  Enregistrer
                </Button>
              </Group>
              <Text fw={600} size="sm" mt="xs">Indicateurs affichés</Text>
              {CARTES.filter((c) => c.cle !== 'total').map((c) => (
                <Checkbox
                  key={c.cle}
                  size="xs"
                  label={c.libelle}
                  checked={!masquees.includes(c.cle)}
                  onChange={(e) => setMasquees(e.currentTarget.checked ? masquees.filter((x) => x !== c.cle) : [...masquees, c.cle])}
                />
              ))}
            </Stack>
          </Popover.Dropdown>
        </Popover>
      </Group>

      {rapport.isPending && <Loader />}
      {rapport.isError && (
        <Alert color="red" variant="light">
          {rapport.error.message}
        </Alert>
      )}
      {rapport.data && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, md: 3, lg: 4 }}>
            {CARTES.filter((c) => !masquees.includes(c.cle)).map((c) => (
              <Cliquable key={c.cle} actif={c.cle === actif} onClick={() => setActif(c.cle)}>
                <CarteChiffre
                  libelle={c.libelle}
                  valeur={new Intl.NumberFormat('fr-FR').format(c.valeur(rapport.data))}
                  icone={c.icone}
                  couleur={c.couleur}
                />
              </Cliquable>
            ))}
          </SimpleGrid>

          <Card withBorder padding="lg">
            <EnteteSection
              titre={carte.libelle}
              description={`${lignes.length} établissement${lignes.length > 1 ? 's' : ''}`}
              action={
                <Group gap="xs">
                  {actif !== 'total' && (
                    <Button size="xs" variant="default" onClick={() => setActif('total')}>
                      Tout afficher
                    </Button>
                  )}
                  <Button size="xs" variant="light" disabled={lignes.length === 0} onClick={() => exporter(lignes, carte.libelle)}>
                    Exporter (Excel)
                  </Button>
                </Group>
              }
            />
            {lignes.length === 0 ? (
              <Text c="dimmed" mt="md">
                Aucun établissement.
              </Text>
            ) : (
              <Table.ScrollContainer minWidth={900} mt="md">
                <Table highlightOnHover verticalSpacing="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Établissement</Table.Th>
                      <Table.Th>Secteur</Table.Th>
                      <Table.Th>Cycles</Table.Th>
                      <Table.Th>Statut</Table.Th>
                      <Table.Th>Autorisation</Table.Th>
                      <Table.Th>Dernière inspection</Table.Th>
                      <Table.Th ta="right">Sécurité</Table.Th>
                      <Table.Th>Données</Table.Th>
                      <Table.Th />
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {[...lignes].sort((a, b) => b.score - a.score || a.nom.localeCompare(b.nom, 'fr')).map((l) => (
                      <Table.Tr key={l.id}>
                        <Table.Td>
                          <Group gap={6} wrap="nowrap">
                            <Text fw={600}>{l.nom}</Text>
                            {l.urgent && (
                              <Badge color="red" size="xs">
                                Urgent
                              </Badge>
                            )}
                          </Group>
                          <Text size="xs" c="dimmed">
                            {l.codeOfficiel}
                            {l.zone ? ` · ${l.zone}` : ''}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <BadgeSecteur secteur={l.secteur} />
                        </Table.Td>
                        <Table.Td>
                          <BadgesCycles cycles={l.cycles} />
                        </Table.Td>
                        <Table.Td>
                          <Badge variant="light" color={COULEUR_STATUT[l.statut] ?? 'gray'}>
                            {STATUTS.find((s) => s.value === l.statut)?.label ?? l.statut}
                          </Badge>
                        </Table.Td>
                        <Table.Td>{LIBELLES_AUTORISATION[l.autorisation]}</Table.Td>
                        <Table.Td>
                          {l.derniereInspection ? (
                            <>
                              {jour(l.derniereInspection)}{' '}
                              <Badge size="xs" variant="light" color={l.nonConforme ? 'red' : 'green'}>
                                {l.nonConforme ? 'Non conforme' : 'Conforme'}
                              </Badge>
                            </>
                          ) : (
                            <Text size="sm" c="dimmed">
                              Jamais
                            </Text>
                          )}
                        </Table.Td>
                        <Table.Td ta="right">{l.signalementsOuverts === 0 ? '—' : l.signalementsOuverts}</Table.Td>
                        <Table.Td>
                          {l.sansDonnees ? (
                            <Badge size="sm" color="yellow" variant="light">
                              Non transmises
                            </Badge>
                          ) : l.derniereDonnee ? (
                            jour(l.derniereDonnee)
                          ) : (
                            '—'
                          )}
                        </Table.Td>
                        <Table.Td>
                          <Button size="compact-xs" variant="light" onClick={() => setChoisi(l.id)}>
                            Suivi
                          </Button>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            )}
          </Card>
        </>
      )}
      {ligneChoisie && (
        <SuiviEtablissementModal etablissement={ligneChoisie} peutAutoriser={ministere} onFermer={() => setChoisi(null)} />
      )}
    </Stack>
  )
}

function Cliquable({ actif, onClick, children }: { actif: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') onClick()
      }}
      style={{
        cursor: 'pointer',
        borderRadius: 'var(--mantine-radius-default)',
        outline: actif ? '2px solid var(--mantine-color-blue-filled)' : 'none',
      }}
    >
      {children}
    </div>
  )
}
