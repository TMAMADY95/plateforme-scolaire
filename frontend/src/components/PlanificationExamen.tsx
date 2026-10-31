import { useState } from 'react'
import { ActionIcon, Alert, Anchor, Badge, Button, Card, Collapse, Group, MultiSelect, NumberInput, Select, SimpleGrid, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ajouterEpreuve,
  creerExamen,
  listerCycles,
  listerMatieres,
  listerNiveaux,
  listerTypesEtablissement,
  listerTypesExamen,
  niveauxOrganisationExamen,
} from '../api/endpoints'
import type { Examen } from '../api/types'
import { formaterDate } from '../utils/dates'
import { libelleOrganisation } from '../utils/organisationExamen'
import EnteteSection from './EnteteSection'
import ModelesExamenModal from './ModelesExamenModal'

/** Créneaux proposés ; « Autre » laisse saisir les heures. */
const CRENEAUX: Record<string, { libelle: string; debut: string; fin: string }> = {
  MATIN: { libelle: 'Matin (08:00 – 12:00)', debut: '08:00', fin: '12:00' },
  APRES_MIDI: { libelle: 'Après-midi (14:00 – 17:00)', debut: '14:00', fin: '17:00' },
}

interface LigneEpreuve {
  cle: number
  matiereId: number | null
  matiere: string
  date: string
  debut: string
  fin: string
  coefficient: number
}

/**
 * Page d'accueil des examens pour le ministère : on choisit le type d'examen, la période et l'emploi du temps
 * (date, créneau, matière), puis on planifie en une fois.
 */
export default function PlanificationExamen({
  anneeId,
  onPlanifie,
}: {
  anneeId: number
  onPlanifie: (examen: Examen) => void
}) {
  const queryClient = useQueryClient()
  const modeles = useQuery({ queryKey: ['types-examen'], queryFn: listerTypesExamen })
  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres })
  const cycles = useQuery({ queryKey: ['cycles'], queryFn: listerCycles })
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })
  const typesEtablissement = useQuery({ queryKey: ['types-etablissement'], queryFn: listerTypesEtablissement })
  const organisations = useQuery({ queryKey: ['niveaux-organisation'], queryFn: niveauxOrganisationExamen })

  const [modele, setModele] = useState<string | null>(null)
  const [libelle, setLibelle] = useState('')
  const [organisation, setOrganisation] = useState<string | null>('NATIONAL')
  const [cycleId, setCycleId] = useState<string | null>(null)
  const [niveauId, setNiveauId] = useState<string | null>(null)
  const [typesIds, setTypesIds] = useState<string[]>([])
  const [debut, setDebut] = useState('')
  const [fin, setFin] = useState('')
  const sixMois = new Date()
  sixMois.setMonth(sixMois.getMonth() + 6)
  const delaiCourt = debut !== '' && new Date(debut) < sixMois
  const [lignes, setLignes] = useState<LigneEpreuve[]>([])
  const [options, setOptions] = useState(false)
  const [gererModeles, setGererModeles] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  // Saisie d'une épreuve.
  const [matiere, setMatiere] = useState<string | null>(null)
  const [libelleEpreuve, setLibelleEpreuve] = useState('')
  const [date, setDate] = useState('')
  const [creneau, setCreneau] = useState<string | null>('MATIN')
  const [heureDebut, setHeureDebut] = useState('08:00')
  const [heureFin, setHeureFin] = useState('12:00')
  const [coefficient, setCoefficient] = useState<number | string>(1)

  // Choisir un type d'examen préremplit le libellé, l'organisateur, le cycle et la classe ; tout reste modifiable.
  const choisirModele = (valeur: string | null) => {
    setModele(valeur)
    const m = modeles.data?.find((x) => String(x.id) === valeur)
    if (m) {
      setLibelle(m.libelle)
      setOrganisation(m.niveauOrganisation)
      setNiveauId(m.niveauId === null ? null : String(m.niveauId))
      setCycleId(m.cycleId === null ? null : String(m.cycleId))
    }
  }

  const choisirCreneau = (valeur: string | null) => {
    setCreneau(valeur)
    const c = valeur ? CRENEAUX[valeur] : undefined
    if (c) {
      setHeureDebut(c.debut)
      setHeureFin(c.fin)
    }
  }

  const periodeValide = debut !== '' && fin !== '' && fin >= debut
  const epreuvePrete =
    date !== '' && (matiere !== null || libelleEpreuve.trim() !== '') && heureDebut !== '' && heureFin !== '' && heureFin > heureDebut

  const ajouterLigne = () => {
    if (periodeValide && (date < debut || date > fin)) {
      setErreur(`La date de l'épreuve (${formaterDate(date)}) doit être comprise dans la période (${formaterDate(debut)} – ${formaterDate(fin)}).`)
      return
    }
    const m = matieres.data?.find((x) => String(x.id) === matiere)
    setErreur(null)
    setLignes((l) =>
      [
        ...l,
        {
          cle: Date.now() + l.length,
          matiereId: m ? m.id : null,
          matiere: m ? m.libelle : libelleEpreuve.trim(),
          date,
          debut: heureDebut,
          fin: heureFin,
          coefficient: Number(coefficient) >= 1 ? Number(coefficient) : 1,
        },
      ].sort((a, b) => (a.date + a.debut).localeCompare(b.date + b.debut)),
    )
    setMatiere(null)
    setLibelleEpreuve('')
  }

  const planification = useMutation({
    mutationFn: async () => {
      const examen = await creerExamen({
        libelle: libelle.trim(),
        anneeScolaireId: anneeId,
        cycleId: cycleId === null ? null : Number(cycleId),
        niveauId: niveauId === null ? null : Number(niveauId),
        niveauOrganisation: organisation,
        dateDebut: debut,
        dateFin: fin,
        typesEtablissementIds: typesIds.map(Number),
      })
      // L'examen existe déjà : si une épreuve échoue, on le signale sans perdre ce qui est fait.
      const echecs: string[] = []
      for (const l of lignes) {
        try {
          await ajouterEpreuve(examen.id, {
            matiereId: l.matiereId,
            libelle: l.matiereId === null ? l.matiere : null,
            date: l.date,
            heureDebut: l.debut,
            heureFin: l.fin,
            coefficient: l.coefficient,
          })
        } catch (e) {
          echecs.push(`${l.matiere} : ${(e as Error).message}`)
        }
      }
      return { examen, echecs }
    },
    onSuccess: ({ examen, echecs }) => {
      void queryClient.invalidateQueries({ queryKey: ['examens'] })
      void queryClient.invalidateQueries({ queryKey: ['epreuves', examen.id] })
      if (echecs.length > 0) {
        setErreur(`Examen créé, mais certaines épreuves n'ont pas été enregistrées : ${echecs.join(' ; ')}. Complétez-les dans « Calendrier et centres ».`)
      } else {
        setErreur(null)
      }
      notifications.show({ color: 'green', title: 'Examen planifié', message: examen.libelle })
      setModele(null)
      setLibelle('')
      setDebut('')
      setFin('')
      setTypesIds([])
      setLignes([])
      onPlanifie(examen)
    },
    onError: (e) => setErreur(e.message),
  })

  const pret = libelle.trim() !== '' && periodeValide && niveauId !== null
  const aide =
    libelle.trim() === ''
      ? "Choisissez un type d'examen."
      : niveauId === null
        ? "Indiquez le niveau d'étude."
        : debut === '' || fin === ''
          ? 'Indiquez les dates de début et de fin.'
          : fin < debut
            ? 'La fin ne peut pas précéder le début.'
            : null

  return (
    <Stack gap="lg">
      <EnteteSection
        titre="Planifier un examen"
        description="Type d'examen, période des épreuves et emploi du temps. Les centres se désignent ensuite."
        action={
          <Button variant="subtle" size="xs" onClick={() => setGererModeles(true)}>
            Gérer les types d'examen
          </Button>
        }
      />

      <Card withBorder padding="lg">
        <Stack gap="md">
          <Text fw={600}>1. Type d'examen et période</Text>
          <SimpleGrid cols={{ base: 1, sm: 2, md: 4 }}>
            <Select
              label="Type d'examen"
              placeholder="CEE, BEPC, BAC…"
              searchable
              clearable
              data={(modeles.data ?? []).map((m) => ({ value: String(m.id), label: `${m.code} — ${m.libelle}` }))}
              value={modele}
              onChange={choisirModele}
            />
            <TextInput label="Intitulé" placeholder="Ex. BEPC 2027" value={libelle} onChange={(e) => setLibelle(e.currentTarget.value)} />
            <TextInput label="Début des épreuves" type="date" value={debut} onChange={(e) => setDebut(e.currentTarget.value)} />
            <TextInput
              label="Fin des épreuves"
              type="date"
              value={fin}
              min={debut || undefined}
              onChange={(e) => setFin(e.currentTarget.value)}
              error={debut !== '' && fin !== '' && fin < debut ? 'Avant le début' : undefined}
            />
          </SimpleGrid>
          <SimpleGrid cols={{ base: 1, sm: 2 }}>
            <Select
              label="Niveau d'étude"
              description="Classe d'examen : détermine les élèves qui peuvent être inscrits"
              placeholder="Choisir"
              searchable
              data={(niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))}
              value={niveauId}
              onChange={setNiveauId}
            />
            <MultiSelect
              label="Types d'établissement participants"
              description="Laissez vide pour tous les types"
              placeholder={typesIds.length === 0 ? 'Tous les types' : undefined}
              data={(typesEtablissement.data ?? []).map((t) => ({ value: String(t.id), label: t.libelle }))}
              value={typesIds}
              onChange={setTypesIds}
              clearable
            />
          </SimpleGrid>
          <Anchor component="button" type="button" size="sm" onClick={() => setOptions((o) => !o)}>
            {options ? 'Masquer les options' : `Options : organisé par ${libelleOrganisation(organisation ?? 'NATIONAL').toLowerCase()}, cycle`}
          </Anchor>
          <Collapse expanded={options}>
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <Select
                label="Organisé par"
                description="Commune, préfecture, région ou national"
                data={(organisations.data ?? ['NATIONAL']).map((o) => ({ value: o, label: libelleOrganisation(o) }))}
                value={organisation}
                onChange={setOrganisation}
                allowDeselect={false}
              />
              <Select
                label="Cycle concerné"
                placeholder="Tous"
                clearable
                data={(cycles.data ?? []).map((c) => ({ value: String(c.id), label: c.libelle }))}
                value={cycleId}
                onChange={setCycleId}
              />
            </SimpleGrid>
          </Collapse>
        </Stack>
      </Card>

      <Card withBorder padding="lg">
        <Stack gap="md">
          <Text fw={600}>2. Emploi du temps</Text>
          <Group align="flex-end">
            <TextInput
              label="Date"
              type="date"
              value={date}
              min={debut || undefined}
              max={fin || undefined}
              onChange={(e) => setDate(e.currentTarget.value)}
            />
            <Select
              label="Créneau"
              data={[...Object.entries(CRENEAUX).map(([value, c]) => ({ value, label: c.libelle })), { value: 'AUTRE', label: 'Autre horaire…' }]}
              value={creneau}
              onChange={choisirCreneau}
              allowDeselect={false}
              w={220}
            />
            {creneau === 'AUTRE' && (
              <>
                <TextInput label="De" type="time" value={heureDebut} onChange={(e) => setHeureDebut(e.currentTarget.value)} w={110} />
                <TextInput label="À" type="time" value={heureFin} onChange={(e) => setHeureFin(e.currentTarget.value)} w={110} />
              </>
            )}
            <Select
              label="Matière"
              placeholder="Choisir"
              searchable
              clearable
              data={(matieres.data ?? []).map((m) => ({ value: String(m.id), label: m.libelle }))}
              value={matiere}
              onChange={setMatiere}
              w={220}
            />
            {matiere === null && (
              <TextInput label="Ou intitulé libre" placeholder="Ex. Dictée" value={libelleEpreuve} onChange={(e) => setLibelleEpreuve(e.currentTarget.value)} w={170} />
            )}
            <NumberInput label="Coef." min={1} allowDecimal={false} value={coefficient} onChange={setCoefficient} w={80} />
            <Button variant="light" disabled={!epreuvePrete} onClick={ajouterLigne}>
              Ajouter l'épreuve
            </Button>
          </Group>

          {lignes.length === 0 ? (
            <Text size="sm" c="dimmed">
              Aucune épreuve ajoutée. Vous pourrez aussi les ajouter plus tard.
            </Text>
          ) : (
            <Table verticalSpacing="xs">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Créneau</Table.Th>
                  <Table.Th>Matière</Table.Th>
                  <Table.Th>Coef.</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {lignes.map((l) => (
                  <Table.Tr key={l.cle}>
                    <Table.Td>{formaterDate(l.date)}</Table.Td>
                    <Table.Td>
                      {l.debut} – {l.fin}
                    </Table.Td>
                    <Table.Td>{l.matiere}</Table.Td>
                    <Table.Td>{l.coefficient}</Table.Td>
                    <Table.Td ta="right">
                      <ActionIcon variant="subtle" color="red" aria-label="Retirer l'épreuve" onClick={() => setLignes((x) => x.filter((y) => y.cle !== l.cle))}>
                        ×
                      </ActionIcon>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </Stack>
      </Card>

      {delaiCourt && (
        <Alert color="orange" variant="light">
          Le règlement général des examens (arrêté de 2022) demande de publier les épreuves, leurs durées et leurs coefficients au
          moins six mois avant le début de la session : la date de début choisie est plus proche.
        </Alert>
      )}
      {erreur && (
        <Alert color="red" variant="light">
          {erreur}
        </Alert>
      )}
      <Group justify="space-between">
        <Group gap="xs">
          {libelle.trim() !== '' && <Badge variant="light">{libelle.trim()}</Badge>}
          {periodeValide && (
            <Text size="sm" c="dimmed">
              du {formaterDate(debut)} au {formaterDate(fin)} · {lignes.length} épreuve(s)
            </Text>
          )}
          {aide && (
            <Text size="sm" c="dimmed">
              {aide}
            </Text>
          )}
        </Group>
        <Button size="md" disabled={!pret} loading={planification.isPending} onClick={() => planification.mutate()}>
          Planifier l'examen
        </Button>
      </Group>
      {gererModeles && <ModelesExamenModal onFermer={() => setGererModeles(false)} />}
    </Stack>
  )
}
