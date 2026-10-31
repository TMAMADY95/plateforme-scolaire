import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, Modal, NumberInput, SegmentedControl, Select, SimpleGrid, Stack, Table, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { listerExamens, listerZones, saisirResultatExamen, supprimerExamen } from '../api/endpoints'
import { libelleOrganisation } from '../utils/organisationExamen'
import type { Examen, LigneExamen, RapportExamen } from '../api/types'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { exporterCsv } from '../utils/exportCsv'
import { BadgeSecteur } from './BadgesEtablissement'
import CarteChiffre from './CarteChiffre'
import EnteteSection from './EnteteSection'
import CalendrierExamenModal from './CalendrierExamenModal'
import BanqueSujetsModal from './BanqueSujetsModal'
import CorrectionExamenModal from './CorrectionExamenModal'
import DiplomesExamenModal from './DiplomesExamenModal'
import DeliberationExamenModal, { COULEUR_STATUT, LIBELLE_STATUT } from './DeliberationExamenModal'
import DetailResultatsModal from './DetailResultatsModal'
import NotesCandidatsModal from './NotesCandidatsModal'
import ImportResultatsExamenModal from './ImportResultatsExamenModal'
import StatistiquesExamen from './StatistiquesExamen'
import CentresExamen from './CentresExamen'
import PlanificationExamen from './PlanificationExamen'

const nombre = (n: number) => new Intl.NumberFormat('fr-FR').format(n)
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)

interface Props {
  cle: string
  charger: (examenId: number, zoneId?: number) => Promise<RapportExamen>
  /** Ministère : création des examens et filtre par zone. */
  ministere?: boolean
}

/** Résultats des examens nationaux : taux de réussite par établissement, zone et secteur ; saisie des chiffres. */
export default function PanneauExamens({ cle, charger, ministere = false }: Props) {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const [examenId, setExamenId] = useState<string | null>(null)
  const [zoneId, setZoneId] = useState<string | null>(null)
  const [import_, setImport] = useState(false)
  const [calendrierId, setCalendrierId] = useState<number | null>(null)
  // Le ministère arrive sur la planification ; les autres comptes sur les résultats.
  const [vue, setVue] = useState<'planifier' | 'planifies' | 'centres' | 'resultats'>(ministere ? 'planifier' : 'resultats')

  const examens = useQuery({
    queryKey: ['examens', anneeId],
    queryFn: () => listerExamens(anneeId as number),
    enabled: anneeId !== null,
  })
  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones, enabled: ministere })
  // Résultats : le ministère ne suit que les examens qu'il organise ; les examens locaux sont suivis par leur organisateur.
  const examensResultats = (examens.data ?? []).filter((e) => !ministere || e.niveauOrganisation === 'NATIONAL')
  const examenCourant =
    examenId !== null && examensResultats.some((e) => String(e.id) === examenId)
      ? examenId
      : examensResultats.length > 0
        ? String(examensResultats[0].id)
        : null
  const filtreZone = ministere && zoneId ? Number(zoneId) : undefined

  const rapport = useQuery({
    queryKey: ['rapport-examen', cle, examenCourant, filtreZone ?? null],
    queryFn: () => charger(Number(examenCourant), filtreZone),
    enabled: examenCourant !== null,
  })

  const planifie = (examen: Examen) => {
    setExamenId(String(examen.id))
    setVue('planifies')
  }

  return (
    <Stack mt="md" gap="lg">
      <Group justify="space-between" align="flex-end">
        {ministere ? (
          <SegmentedControl
            value={vue}
            onChange={(v) => setVue(v as typeof vue)}
            data={[
              { value: 'planifier', label: 'Planifier un examen' },
              { value: 'planifies', label: `Examens planifiés${examens.data ? ` (${examens.data.length})` : ''}` },
              { value: 'centres', label: 'Centres d\'examen' },
              { value: 'resultats', label: 'Résultats' },
            ]}
          />
        ) : (
          <EnteteSection titre="Examens" description="Taux de réussite par établissement, zone et secteur." />
        )}
        <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
      </Group>
      {ministere && vue === 'planifier' && anneeId !== null && <PlanificationExamen anneeId={anneeId} onPlanifie={planifie} />}
      {ministere && vue === 'centres' && anneeId !== null && <CentresExamen anneeId={anneeId} />}
      {ministere && vue === 'planifies' && (
        <ExamensPlanifies
          examens={examens.data ?? []}
          chargement={examens.isPending}
          onCalendrier={(id) => {
            setCalendrierId(id)
          }}
          onResultats={(id) => {
            setExamenId(String(id))
            setVue('resultats')
          }}
          onNouveau={() => setVue('planifier')}
        />
      )}
      {vue === 'resultats' && examens.isSuccess && examensResultats.length === 0 && (
        <Alert color="blue" variant="light">
          {ministere ? 'Aucun examen organisé par le ministère pour cette année : planifiez-en un.' : 'Aucun examen pour cette année.'}
        </Alert>
      )}
      {vue === 'resultats' && examensResultats.length > 0 && (
        <Group align="flex-end">
          <Select
            label="Examen"
            data={examensResultats.map((e) => ({ value: String(e.id), label: `${e.libelle}${e.cycle ? ` (${e.cycle})` : ''} — ${libelleOrganisation(e.niveauOrganisation).toLowerCase()}` }))}
            value={examenCourant}
            onChange={setExamenId}
            allowDeselect={false}
            w={300}
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
              w={260}
            />
          )}
        </Group>
      )}
      {vue === 'resultats' && examenCourant !== null && examens.data && (
        <Group>
          <Button variant="default" size="xs" onClick={() => setImport(true)}>
            Importer les résultats (CSV)
          </Button>
          <Button variant="light" size="xs" onClick={() => setCalendrierId(Number(examenCourant))}>
            Calendrier et centres
          </Button>
          {(() => {
            const e = examens.data.find((x) => String(x.id) === examenCourant)
            return e?.dateDebut ? (
              <Text size="sm" c="dimmed">
                Épreuves du {new Date(e.dateDebut).toLocaleDateString('fr-FR')}
                {e.dateFin ? ` au ${new Date(e.dateFin).toLocaleDateString('fr-FR')}` : ''}
              </Text>
            ) : null
          })()}
        </Group>
      )}
      {calendrierId !== null && examens.data?.find((e) => e.id === calendrierId) && (
        <CalendrierExamenModal
          examen={examens.data.find((e) => e.id === calendrierId)!}
          onFermer={() => setCalendrierId(null)}
        />
      )}
      {import_ && examenCourant !== null && (
        <ImportResultatsExamenModal
          examenId={Number(examenCourant)}
          libelleExamen={examens.data?.find((e) => String(e.id) === examenCourant)?.libelle ?? ''}
          onFermer={() => setImport(false)}
        />
      )}
      {vue === 'resultats' && rapport.isPending && examenCourant !== null && <Loader />}
      {vue === 'resultats' && rapport.isError && (
        <Alert color="red" variant="light">
          {rapport.error.message}
        </Alert>
      )}
      {vue === 'resultats' && rapport.data && <Rapport rapport={rapport.data} ministere={ministere} />}
    </Stack>
  )
}

function ExamensPlanifies({
  examens,
  chargement,
  onCalendrier,
  onResultats,
  onNouveau,
}: {
  examens: Examen[]
  chargement: boolean
  onCalendrier: (id: number) => void
  onResultats: (id: number) => void
  onNouveau: () => void
}) {
  const queryClient = useQueryClient()
  const [aSupprimer, setASupprimer] = useState<Examen | null>(null)
  // Renseigné quand l'examen contient déjà des candidats ou des résultats : le serveur demande une confirmation.
  const [avertissement, setAvertissement] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const fermer = () => {
    setASupprimer(null)
    setAvertissement(null)
    setErreur(null)
  }
  const suppression = useMutation({
    mutationFn: (forcer: boolean) => supprimerExamen((aSupprimer as Examen).id, forcer),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['examens'] })
      void queryClient.invalidateQueries({ queryKey: ['rapport-examen'] })
      notifications.show({ color: 'green', title: 'Examen supprimé', message: aSupprimer?.libelle ?? '' })
      fermer()
    },
    onError: (e) => {
      if (e instanceof ApiError && e.statut === 409) setAvertissement(e.message)
      else setErreur(e.message)
    },
  })

  if (chargement) return <Loader />
  if (examens.length === 0) {
    return (
      <Alert color="blue" variant="light">
        Aucun examen planifié pour cette année.{' '}
        <Button size="compact-xs" variant="light" onClick={onNouveau}>
          Planifier un examen
        </Button>
      </Alert>
    )
  }
  return (
    <Card withBorder padding="lg">
      <Table.ScrollContainer minWidth={640}>
        <Table verticalSpacing="sm" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Examen</Table.Th>
              <Table.Th>Organisé par</Table.Th>
              <Table.Th>Période</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {examens.map((e) => (
              <Table.Tr key={e.id}>
                <Table.Td>
                  <Text fw={600}>{e.libelle}</Text>
                  <Text size="xs" c="dimmed">
                    {[e.niveau ?? e.cycle].filter(Boolean).join(' · ') || 'Tous niveaux'}
                    {' · '}
                    {e.typesEtablissement.length > 0 ? e.typesEtablissement.join(', ') : 'Tous types d\'établissement'}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Badge variant="light">{libelleOrganisation(e.niveauOrganisation)}</Badge>
                </Table.Td>
                <Table.Td>
                  {e.dateDebut ? (
                    `${new Date(e.dateDebut).toLocaleDateString('fr-FR')}${e.dateFin ? ` au ${new Date(e.dateFin).toLocaleDateString('fr-FR')}` : ''}`
                  ) : (
                    <Text size="sm" c="dimmed">
                      Non publiée
                    </Text>
                  )}
                </Table.Td>
                <Table.Td>
                  <Group gap="xs" justify="flex-end" wrap="nowrap">
                    <Button size="compact-xs" variant="light" onClick={() => onCalendrier(e.id)}>
                      Calendrier et centres
                    </Button>
                    {e.niveauOrganisation === 'NATIONAL' && (
                      <Button size="compact-xs" variant="default" onClick={() => onResultats(e.id)}>
                        Résultats
                      </Button>
                    )}
                    <Button size="compact-xs" variant="subtle" color="red" onClick={() => setASupprimer(e)}>
                      Supprimer
                    </Button>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
      <Modal opened={aSupprimer !== null} onClose={fermer} title="Supprimer l'examen" centered>
        <Stack>
          <Text>
            Supprimer <b>{aSupprimer?.libelle}</b> avec son calendrier et ses centres d'examen ? Cette action est définitive.
          </Text>
          {avertissement && (
            <Alert color="orange" variant="light">
              {avertissement}
            </Alert>
          )}
          {erreur && (
            <Alert color="red" variant="light">
              {erreur}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={fermer}>
              Annuler
            </Button>
            <Button color="red" loading={suppression.isPending} onClick={() => suppression.mutate(avertissement !== null)}>
              {avertissement ? 'Supprimer quand même' : 'Supprimer'}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Card>
  )
}

function Rapport({ rapport, ministere }: { rapport: RapportExamen; ministere: boolean }) {
  const [juryOuvert, setJuryOuvert] = useState(false)
  const [correctionOuverte, setCorrectionOuverte] = useState(false)
  const [sujetsOuverts, setSujetsOuverts] = useState(false)
  const [diplomesOuverts, setDiplomesOuverts] = useState(false)
  const national = rapport.examen.niveauOrganisation === 'NATIONAL'
  const s = rapport.synthese
  const public_ = rapport.parSecteur.find((x) => x.libelle === 'PUBLIC')
  const prive = rapport.parSecteur.find((x) => x.libelle === 'PRIVE')

  // Regroupement par zone d'après la zone de chaque établissement.
  const parZone = new Map<string, { presentes: number; admis: number; etablissements: number }>()
  for (const l of rapport.lignes) {
    // Examen local : on regroupe par la zone qui l'organise (commune, préfecture, région) ; sinon par zone de l'école.
    const nom = (national ? l.zone : l.organisateur) ?? 'Sans zone'
    const z = parZone.get(nom) ?? { presentes: 0, admis: 0, etablissements: 0 }
    z.etablissements += 1
    z.presentes += l.presentes ?? 0
    z.admis += l.admis ?? 0
    parZone.set(nom, z)
  }
  const taux = (admis: number, presentes: number) => (presentes === 0 ? null : Math.round((admis * 1000) / presentes) / 10)

  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2, md: 3, lg: 5 }}>
        <CarteChiffre libelle="Taux de réussite" valeur={pourcent(s.taux)} icone="resultats" couleur="green" detail={`${nombre(s.admis)} admis sur ${nombre(s.presentes)}`} />
        <CarteChiffre libelle="Public" valeur={pourcent(public_?.taux ?? null)} icone="ecole" couleur="blue" />
        <CarteChiffre libelle="Privé" valeur={pourcent(prive?.taux ?? null)} icone="ecole" couleur="grape" />
        <CarteChiffre libelle="Résultats transmis" valeur={`${nombre(s.transmis)} / ${nombre(s.etablissements)}`} icone="inscriptions" couleur="teal" />
        <CarteChiffre libelle="Sans résultats" valeur={nombre(s.etablissements - s.transmis)} icone="alerte" couleur="orange" detail="établissements" />
      </SimpleGrid>

      {ministere && (
        <Card withBorder padding="md">
          <Group justify="space-between">
            <Text fw={600}>Diplômes</Text>
            <Button size="xs" variant="light" onClick={() => setDiplomesOuverts(true)}>
              Ouvrir
            </Button>
          </Group>
          {diplomesOuverts && <DiplomesExamenModal examen={rapport.examen} onFermer={() => setDiplomesOuverts(false)} />}
        </Card>
      )}

      <Card withBorder padding="md">
        <Group justify="space-between">
          <Text fw={600}>Banque de sujets</Text>
          <Button size="xs" variant="light" onClick={() => setSujetsOuverts(true)}>
            Ouvrir
          </Button>
        </Group>
        {sujetsOuverts && <BanqueSujetsModal examen={rapport.examen} onFermer={() => setSujetsOuverts(false)} />}
      </Card>

      <Card withBorder padding="md">
        <Group justify="space-between">
          <Text fw={600}>Commissions de correction</Text>
          <Button size="xs" variant="light" onClick={() => setCorrectionOuverte(true)}>
            Ouvrir
          </Button>
        </Group>
        {correctionOuverte && <CorrectionExamenModal examen={rapport.examen} onFermer={() => setCorrectionOuverte(false)} />}
      </Card>

      <Card withBorder padding="md">
        <Group justify="space-between">
          <Group gap="xs">
            <Text fw={600}>Jury et proclamation</Text>
            <Badge color={COULEUR_STATUT[rapport.examen.statutResultats]} variant="light">
              {LIBELLE_STATUT[rapport.examen.statutResultats]}
            </Badge>
          </Group>
          <Button size="xs" variant="light" onClick={() => setJuryOuvert(true)}>
            Ouvrir
          </Button>
        </Group>
        {juryOuvert && <DeliberationExamenModal examen={rapport.examen} onFermer={() => setJuryOuvert(false)} />}
      </Card>

      <StatistiquesExamen statistiques={rapport.statistiques} />

      <Card withBorder padding="lg">
        <EnteteSection titre={national ? 'Par zone' : `Par ${rapport.examen.niveauOrganisation.toLowerCase()} organisatrice`} />
        <Table.ScrollContainer minWidth={500} mt="md">
          <Table verticalSpacing="xs">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{national ? 'Zone' : libelleOrganisation(rapport.examen.niveauOrganisation)}</Table.Th>
                <Table.Th ta="right">Établissements</Table.Th>
                <Table.Th ta="right">Présentés</Table.Th>
                <Table.Th ta="right">Admis</Table.Th>
                <Table.Th ta="right">Taux</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {[...parZone.entries()].sort((a, b) => a[0].localeCompare(b[0], 'fr')).map(([nom, z]) => (
                <Table.Tr key={nom}>
                  <Table.Td>{nom}</Table.Td>
                  <Table.Td ta="right">{nombre(z.etablissements)}</Table.Td>
                  <Table.Td ta="right">{nombre(z.presentes)}</Table.Td>
                  <Table.Td ta="right">{nombre(z.admis)}</Table.Td>
                  <Table.Td ta="right">{pourcent(taux(z.admis, z.presentes))}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Card>

      <Card withBorder padding="lg">
        <EnteteSection
          titre="Par établissement"
          action={
            <Button
              size="xs"
              variant="light"
              disabled={rapport.lignes.length === 0}
              onClick={() =>
                exporterCsv(
                  `examen-${rapport.examen.libelle.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
                  [
                    { titre: 'Code', valeur: (l: LigneExamen) => l.codeOfficiel },
                    { titre: 'Établissement', valeur: (l) => l.nom },
                    { titre: 'Zone', valeur: (l) => l.zone },
                    { titre: 'Organisateur', valeur: (l) => l.organisateur },
                    { titre: 'Secteur', valeur: (l) => (l.secteur === 'PUBLIC' ? 'Public' : 'Privé') },
                    { titre: 'Présentés', valeur: (l) => l.presentes },
                    { titre: 'Admis', valeur: (l) => l.admis },
                    { titre: 'Taux de réussite (%)', valeur: (l) => (l.taux === null ? '' : String(l.taux).replace('.', ',')) },
                  ],
                  rapport.lignes,
                )
              }
            >
              Exporter (Excel)
            </Button>
          }
        />
        {rapport.lignes.length === 0 ? (
          <Text c="dimmed" mt="md">
            Aucun établissement concerné par cet examen.
          </Text>
        ) : (
          <Table.ScrollContainer minWidth={760} mt="md">
            <Table highlightOnHover verticalSpacing="xs">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Établissement</Table.Th>
                  <Table.Th>Secteur</Table.Th>
                  <Table.Th>Présentés</Table.Th>
                  <Table.Th>Admis</Table.Th>
                  <Table.Th ta="right">Taux</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {rapport.lignes.map((l) => (
                  <LigneSaisie key={`${l.id}-${l.presentes}-${l.admis}`} examenId={rapport.examen.id} ligne={l} />
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
      </Card>
    </>
  )
}

function LigneSaisie({ examenId, ligne }: { examenId: number; ligne: LigneExamen }) {
  const queryClient = useQueryClient()
  const [presentes, setPresentes] = useState<number | string>(ligne.presentes ?? '')
  const [admis, setAdmis] = useState<number | string>(ligne.admis ?? '')
  const [erreur, setErreur] = useState<string | null>(null)
  const valide = presentes !== '' && admis !== '' && Number(admis) <= Number(presentes)
  const [detail, setDetail] = useState(false)
  const [notesOuvertes, setNotesOuvertes] = useState(false)

  const saisie = useMutation({
    mutationFn: () =>
      saisirResultatExamen(examenId, ligne.id, {
        presentes: Number(presentes),
        admis: Number(admis),
        absents: ligne.absents,
        elimines: ligne.elimines,
        presentesFilles: ligne.presentesFilles,
        admisFilles: ligne.admisFilles,
        series: ligne.series.length > 0 ? ligne.series : null,
      }),
    onSuccess: () => {
      setErreur(null)
      void queryClient.invalidateQueries({ queryKey: ['rapport-examen'] })
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
      <Table.Td>
        <NumberInput aria-label="Présentés" min={0} allowDecimal={false} size="xs" w={90} value={presentes} onChange={setPresentes} />
      </Table.Td>
      <Table.Td>
        <NumberInput aria-label="Admis" min={0} allowDecimal={false} size="xs" w={90} value={admis} onChange={setAdmis} error={presentes !== '' && admis !== '' && !valide} />
      </Table.Td>
      <Table.Td ta="right">{pourcent(ligne.taux)}</Table.Td>
      <Table.Td>
        <Button size="compact-xs" variant="light" disabled={!valide} loading={saisie.isPending} onClick={() => saisie.mutate()}>
          Enregistrer
        </Button>
        <Button size="compact-xs" variant="subtle" onClick={() => setDetail(true)}>
          Détail
        </Button>
        <Button size="compact-xs" variant="subtle" onClick={() => setNotesOuvertes(true)}>
          Notes
        </Button>
        {notesOuvertes && (
          <NotesCandidatsModal examenId={examenId} etablissementId={ligne.id} nom={ligne.nom} onFermer={() => setNotesOuvertes(false)} />
        )}
        {detail && <DetailResultatsModal examenId={examenId} ligne={ligne} onFermer={() => setDetail(false)} />}
      </Table.Td>
    </Table.Tr>
  )
}
