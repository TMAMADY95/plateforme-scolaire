import { useState } from 'react'
import { Alert, Badge, Button, Divider, Group, NumberInput, Select, SimpleGrid, Stack, Table, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  creerAnnee,
  creerCycle,
  creerDirectionRegionale,
  creerMatiere,
  creerPeriode,
  creerNiveau,
  creerTypeEtablissement,
  creerZone,
  listerAnnees,
  listerCycles,
  listerMatieres,
  listerNiveaux,
  listerPeriodes,
  listerTypesEtablissement,
  listerZones,
  modifierAnnee,
  modifierCycle,
  modifierMatiere,
  modifierNiveau,
  modifierPeriode,
  modifierTypeEtablissement,
  modifierZone,
  supprimerTypeEtablissement,
  supprimerZone,
} from '../../api/endpoints'
import type { AnneeScolaire, Cycle, Matiere, Niveau, Periode, TypeEtablissement, Zone } from '../../api/types'
import { formaterDate } from '../../utils/dates'
import EnteteSection from '../../components/EnteteSection'
import CarteReferentiel from '../../components/CarteReferentiel'
import ConfirmationModal from '../../components/ConfirmationModal'
import DirectionModal from './DirectionModal'
import ModifierReferentielModal from './ModifierReferentielModal'

/** Petit formulaire de création : déclenche la mutation, vide les champs, affiche l'erreur éventuelle. */
function useCreation<T>(
  fonction: (valeurs: T) => Promise<unknown>,
  cles: string[],
  messageSucces: string,
  surSucces: () => void,
) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const mutation = useMutation({
    mutationFn: fonction,
    onSuccess: () => {
      setErreur(null)
      cles.forEach((cle) => void queryClient.invalidateQueries({ queryKey: [cle] }))
      notifications.show({ color: 'green', title: 'Enregistré', message: messageSucces })
      surSucces()
    },
    onError: (e) => setErreur(e.message),
  })
  return { mutation, erreur }
}

function Erreur({ message }: { message: string | null }) {
  return message === null ? null : (
    <Alert color="red" variant="light">
      {message}
    </Alert>
  )
}

function Annees() {
  const annees = useQuery({ queryKey: ['annees'], queryFn: listerAnnees })
  const formulaire = useForm({
    initialValues: { libelle: '', dateDebut: '', dateFin: '' },
    validate: {
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
      dateDebut: (v) => (v === '' ? 'Date requise' : null),
      dateFin: (v, valeurs) =>
        v === '' ? 'Date requise' : v <= valeurs.dateDebut ? 'La fin doit suivre le début' : null,
    },
  })
  const { mutation, erreur } = useCreation(creerAnnee, ['annees'], 'Année scolaire créée', () => formulaire.reset())
  const [aModifier, setAModifier] = useState<AnneeScolaire | null>(null)

  return (
    <CarteReferentiel titre="Années scolaires" description="Les années et leurs dates de début et de fin." nombre={annees.data?.length}>
      <Table highlightOnHover verticalSpacing="xs" mb="md">
        <Table.Tbody>
          {(annees.data ?? []).map((a) => (
            <Table.Tr key={a.id}>
              <Table.Td>{a.libelle}</Table.Td>
              <Table.Td>
                {formaterDate(a.dateDebut)} → {formaterDate(a.dateFin)}
              </Table.Td>
              <Table.Td>
                <Button size="compact-xs" variant="light" onClick={() => setAModifier(a)}>
                  Modifier
                </Button>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Divider label="Ajouter" labelPosition="left" my="sm" />
      <form onSubmit={formulaire.onSubmit((v) => mutation.mutate({ ...v, libelle: v.libelle.trim() }))}>
        <Stack gap="xs">
          <TextInput label="Libellé" placeholder="2026-2027" {...formulaire.getInputProps('libelle')} />
          <Group grow>
            <TextInput label="Début" type="date" {...formulaire.getInputProps('dateDebut')} />
            <TextInput label="Fin" type="date" {...formulaire.getInputProps('dateFin')} />
          </Group>
          <Erreur message={erreur} />
          <Button type="submit" loading={mutation.isPending}>
            Ajouter
          </Button>
        </Stack>
      </form>
      {aModifier && (
        <ModifierReferentielModal
          titre={`Modifier l'année ${aModifier.libelle}`}
          champs={[
            { nom: 'libelle', label: 'Libellé' },
            { nom: 'dateDebut', label: 'Début', type: 'date' },
            { nom: 'dateFin', label: 'Fin', type: 'date' },
          ]}
          valeursInitiales={{ libelle: aModifier.libelle, dateDebut: aModifier.dateDebut, dateFin: aModifier.dateFin }}
          cles={['annees']}
          enregistrer={(v) =>
            modifierAnnee(aModifier.id, { libelle: v.libelle.trim(), dateDebut: v.dateDebut, dateFin: v.dateFin })
          }
          onFermer={() => setAModifier(null)}
        />
      )}
    </CarteReferentiel>
  )
}

/** Les périodes d'une année (trimestres, semestres…) : leur nom apparaît dans les bulletins et les statistiques. */
function Periodes() {
  const annees = useQuery({ queryKey: ['annees'], queryFn: listerAnnees })
  const [anneeChoisie, setAnneeChoisie] = useState<string | null>(null)
  const anneeId = Number(anneeChoisie ?? annees.data?.[0]?.id ?? 0)
  const periodes = useQuery({
    queryKey: ['periodes', anneeId],
    queryFn: () => listerPeriodes(anneeId),
    enabled: anneeId > 0,
  })
  const formulaire = useForm({
    initialValues: { libelle: '', ordre: 1 as number | string, dateDebut: '', dateFin: '' },
    validate: {
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
      ordre: (v) => (Number(v) >= 1 ? null : 'Au moins 1'),
      dateDebut: (v) => (v === '' ? 'Date requise' : null),
      dateFin: (v, valeurs) =>
        v === '' ? 'Date requise' : v <= valeurs.dateDebut ? 'La fin doit suivre le début' : null,
    },
  })
  const { mutation, erreur } = useCreation(
    (v: { libelle: string; ordre: number; dateDebut: string; dateFin: string }) => creerPeriode(anneeId, v),
    ['periodes'],
    'Période créée',
    () => formulaire.reset(),
  )
  const [aModifier, setAModifier] = useState<Periode | null>(null)

  return (
    <CarteReferentiel titre="Périodes (trimestres, semestres…)" description="Le découpage de l'année : leurs noms apparaissent dans les bulletins et les statistiques." nombre={periodes.data?.length}>
      <Select
        mb="sm"
        label="Année scolaire"
        data={(annees.data ?? []).map((a) => ({ value: String(a.id), label: a.libelle }))}
        value={anneeChoisie ?? (annees.data?.[0] ? String(annees.data[0].id) : null)}
        onChange={setAnneeChoisie}
        allowDeselect={false}
      />
      {(periodes.data ?? []).length === 0 ? (
        <Text c="dimmed" mb="md">
          Aucune période pour cette année.
        </Text>
      ) : (
        <Table highlightOnHover verticalSpacing="xs" mb="md">
          <Table.Tbody>
            {(periodes.data ?? []).map((p) => (
              <Table.Tr key={p.id}>
                <Table.Td>{p.ordre}</Table.Td>
                <Table.Td>{p.libelle}</Table.Td>
                <Table.Td>
                  {formaterDate(p.dateDebut)} → {formaterDate(p.dateFin)}
                </Table.Td>
                <Table.Td>
                  <Button size="compact-xs" variant="light" onClick={() => setAModifier(p)}>
                    Modifier
                  </Button>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
      <Divider label="Ajouter" labelPosition="left" my="sm" />
      <form
        onSubmit={formulaire.onSubmit((v) =>
          mutation.mutate({ libelle: v.libelle.trim(), ordre: Number(v.ordre), dateDebut: v.dateDebut, dateFin: v.dateFin }),
        )}
      >
        <Stack gap="xs">
          <Group grow align="flex-start">
            <TextInput label="Nom" placeholder="Trimestre 1" {...formulaire.getInputProps('libelle')} />
            <NumberInput label="Ordre" min={1} allowDecimal={false} {...formulaire.getInputProps('ordre')} />
          </Group>
          <Group grow>
            <TextInput label="Début" type="date" {...formulaire.getInputProps('dateDebut')} />
            <TextInput label="Fin" type="date" {...formulaire.getInputProps('dateFin')} />
          </Group>
          <Erreur message={erreur} />
          <Button type="submit" loading={mutation.isPending} disabled={anneeId === 0}>
            Ajouter
          </Button>
        </Stack>
      </form>
      {aModifier && (
        <ModifierReferentielModal
          titre={`Modifier la période ${aModifier.libelle}`}
          champs={[
            { nom: 'libelle', label: 'Nom' },
            { nom: 'ordre', label: 'Ordre', type: 'number' },
            { nom: 'dateDebut', label: 'Début', type: 'date' },
            { nom: 'dateFin', label: 'Fin', type: 'date' },
          ]}
          valeursInitiales={{
            libelle: aModifier.libelle,
            ordre: String(aModifier.ordre),
            dateDebut: aModifier.dateDebut,
            dateFin: aModifier.dateFin,
          }}
          cles={['periodes']}
          enregistrer={(v) =>
            modifierPeriode(aModifier.id, {
              libelle: v.libelle.trim(),
              ordre: Number(v.ordre),
              dateDebut: v.dateDebut,
              dateFin: v.dateFin,
            })
          }
          onFermer={() => setAModifier(null)}
        />
      )}
    </CarteReferentiel>
  )
}

/** Les cycles d'enseignement (primaire, collège, lycée…) : un établissement en propose un ou plusieurs. */
function Cycles() {
  const cycles = useQuery({ queryKey: ['cycles'], queryFn: listerCycles })
  const formulaire = useForm({
    initialValues: { code: '', libelle: '', ordre: 1 as number | string },
    validate: {
      code: (v) => (v.trim() === '' ? 'Code requis' : null),
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
      ordre: (v) => (Number(v) >= 1 ? null : 'Au moins 1'),
    },
  })
  const { mutation, erreur } = useCreation(creerCycle, ['cycles'], 'Cycle créé', () => formulaire.reset())
  const [aModifier, setAModifier] = useState<Cycle | null>(null)

  return (
    <CarteReferentiel
      titre="Cycles d'enseignement"
      description="Primaire, collège, lycée… Un établissement peut proposer un seul cycle ou plusieurs."
      nombre={cycles.data?.length}
    >
      <Table highlightOnHover verticalSpacing="xs" mb="md">
        <Table.Tbody>
          {(cycles.data ?? []).map((c) => (
            <Table.Tr key={c.id}>
              <Table.Td>{c.ordre}</Table.Td>
              <Table.Td>{c.libelle}</Table.Td>
              <Table.Td c="dimmed">{c.code}</Table.Td>
              <Table.Td>
                <Button size="compact-xs" variant="light" onClick={() => setAModifier(c)}>
                  Modifier
                </Button>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Divider label="Ajouter" labelPosition="left" my="sm" />
      <form
        onSubmit={formulaire.onSubmit((v) =>
          mutation.mutate({ code: v.code.trim(), libelle: v.libelle.trim(), ordre: Number(v.ordre) }),
        )}
      >
        <Stack gap="xs">
          <Group grow align="flex-start">
            <TextInput label="Nom" placeholder="Maternelle" {...formulaire.getInputProps('libelle')} />
            <TextInput label="Code" placeholder="MATERNELLE" {...formulaire.getInputProps('code')} />
            <NumberInput label="Ordre" min={1} allowDecimal={false} {...formulaire.getInputProps('ordre')} />
          </Group>
          <Erreur message={erreur} />
          <Button type="submit" loading={mutation.isPending}>
            Ajouter
          </Button>
        </Stack>
      </form>
      {aModifier && (
        <ModifierReferentielModal
          titre={`Modifier le cycle ${aModifier.libelle}`}
          champs={[
            { nom: 'libelle', label: 'Nom' },
            { nom: 'code', label: 'Code' },
            { nom: 'ordre', label: 'Ordre', type: 'number' },
          ]}
          valeursInitiales={{ libelle: aModifier.libelle, code: aModifier.code, ordre: String(aModifier.ordre) }}
          cles={['cycles', 'etablissements']}
          enregistrer={(v) => modifierCycle(aModifier.id, { code: v.code.trim(), libelle: v.libelle.trim(), ordre: Number(v.ordre) })}
          onFermer={() => setAModifier(null)}
        />
      )}
    </CarteReferentiel>
  )
}

function Niveaux() {
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })
  const cycles = useQuery({ queryKey: ['cycles'], queryFn: listerCycles })
  const optionsCycles = (cycles.data ?? []).map((c) => ({ value: String(c.id), label: c.libelle }))
  const formulaire = useForm({
    initialValues: { code: '', libelle: '', ordre: 1 as number | string, cycleId: null as string | null },
    validate: {
      code: (v) => (v.trim() === '' ? 'Code requis' : null),
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
      ordre: (v) => (Number(v) >= 1 ? null : 'Au moins 1'),
    },
  })
  const { mutation, erreur } = useCreation(creerNiveau, ['niveaux'], 'Niveau créé', () => formulaire.reset())
  const [aModifier, setAModifier] = useState<Niveau | null>(null)

  return (
    <CarteReferentiel titre="Niveaux" description="Les niveaux de classe (CP1, 6e, Terminale…), dans l'ordre." nombre={niveaux.data?.length}>
      <Table highlightOnHover verticalSpacing="xs" mb="md">
        <Table.Tbody>
          {(niveaux.data ?? []).map((n) => (
            <Table.Tr key={n.id}>
              <Table.Td>{n.ordre}</Table.Td>
              <Table.Td>{n.code}</Table.Td>
              <Table.Td>{n.libelle}</Table.Td>
              <Table.Td>
                {n.cycle ? <Badge variant="light">{n.cycle}</Badge> : <Text size="xs" c="dimmed">Cycle non défini</Text>}
              </Table.Td>
              <Table.Td>
                <Button size="compact-xs" variant="light" onClick={() => setAModifier(n)}>
                  Modifier
                </Button>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Divider label="Ajouter" labelPosition="left" my="sm" />
      <form
        onSubmit={formulaire.onSubmit((v) =>
          mutation.mutate({
            code: v.code.trim(),
            libelle: v.libelle.trim(),
            ordre: Number(v.ordre),
            cycleId: v.cycleId === null ? null : Number(v.cycleId),
          }),
        )}
      >
        <Stack gap="xs">
          <Group grow align="flex-start">
            <TextInput label="Code" {...formulaire.getInputProps('code')} />
            <NumberInput label="Ordre" min={1} allowDecimal={false} {...formulaire.getInputProps('ordre')} />
          </Group>
          <TextInput label="Libellé" {...formulaire.getInputProps('libelle')} />
          <Select
            label="Cycle"
            description="Permet de compter les élèves par cycle."
            data={optionsCycles}
            value={formulaire.values.cycleId}
            onChange={(v) => formulaire.setFieldValue('cycleId', v)}
            clearable
            placeholder="Non défini"
          />
          <Erreur message={erreur} />
          <Button type="submit" loading={mutation.isPending}>
            Ajouter
          </Button>
        </Stack>
      </form>
      {aModifier && (
        <ModifierReferentielModal
          titre={`Modifier le niveau ${aModifier.code}`}
          champs={[
            { nom: 'code', label: 'Code' },
            { nom: 'libelle', label: 'Libellé' },
            { nom: 'ordre', label: 'Ordre', type: 'number' },
            { nom: 'cycleId', label: 'Cycle', type: 'select', options: optionsCycles, facultatif: true },
          ]}
          valeursInitiales={{
            code: aModifier.code,
            libelle: aModifier.libelle,
            ordre: String(aModifier.ordre),
            cycleId: aModifier.cycleId === null ? '' : String(aModifier.cycleId),
          }}
          cles={['niveaux', 'classes', 'synthese-nationale-statistiques']}
          enregistrer={(v) =>
            modifierNiveau(aModifier.id, {
              code: v.code.trim(),
              libelle: v.libelle.trim(),
              ordre: Number(v.ordre),
              cycleId: v.cycleId === '' ? null : Number(v.cycleId),
            })
          }
          onFermer={() => setAModifier(null)}
        />
      )}
    </CarteReferentiel>
  )
}

function Matieres() {
  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres })
  const formulaire = useForm({
    initialValues: { code: '', libelle: '' },
    validate: {
      code: (v) => (v.trim() === '' ? 'Code requis' : null),
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
    },
  })
  const { mutation, erreur } = useCreation(creerMatiere, ['matieres'], 'Matière créée', () => formulaire.reset())
  const [aModifier, setAModifier] = useState<Matiere | null>(null)

  return (
    <CarteReferentiel titre="Matières" description="Les matières enseignées." nombre={matieres.data?.length}>
      <Table highlightOnHover verticalSpacing="xs" mb="md">
        <Table.Tbody>
          {(matieres.data ?? []).map((m) => (
            <Table.Tr key={m.id}>
              <Table.Td>{m.code}</Table.Td>
              <Table.Td>{m.libelle}</Table.Td>
              <Table.Td>
                <Button size="compact-xs" variant="light" onClick={() => setAModifier(m)}>
                  Modifier
                </Button>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Divider label="Ajouter" labelPosition="left" my="sm" />
      <form onSubmit={formulaire.onSubmit((v) => mutation.mutate({ code: v.code.trim(), libelle: v.libelle.trim() }))}>
        <Stack gap="xs">
          <Group grow align="flex-start">
            <TextInput label="Code" {...formulaire.getInputProps('code')} />
            <TextInput label="Libellé" {...formulaire.getInputProps('libelle')} />
          </Group>
          <Erreur message={erreur} />
          <Button type="submit" loading={mutation.isPending}>
            Ajouter
          </Button>
        </Stack>
      </form>
      {aModifier && (
        <ModifierReferentielModal
          titre={`Modifier la matière ${aModifier.code}`}
          champs={[
            { nom: 'code', label: 'Code' },
            { nom: 'libelle', label: 'Libellé' },
          ]}
          valeursInitiales={{ code: aModifier.code, libelle: aModifier.libelle }}
          cles={['matieres']}
          enregistrer={(v) => modifierMatiere(aModifier.id, { code: v.code.trim(), libelle: v.libelle.trim() })}
          onFermer={() => setAModifier(null)}
        />
      )}
    </CarteReferentiel>
  )
}

function Zones() {
  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones })
  const formulaire = useForm({
    initialValues: { nom: '', typeZone: '', parentId: '' },
    validate: {
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      typeZone: (v) => (v.trim() === '' ? 'Type requis' : null),
    },
  })
  const { mutation, erreur } = useCreation(creerZone, ['zones'], 'Zone créée', () => formulaire.reset())
  const noms = new Map((zones.data ?? []).map((z) => [z.id, z.nom]))
  const [pourRegion, setPourRegion] = useState<Zone | null>(null)
  const [zoneAModifier, setZoneAModifier] = useState<Zone | null>(null)
  const [zoneASupprimer, setZoneASupprimer] = useState<Zone | null>(null)
  const queryClient = useQueryClient()
  const suppression = useMutation({
    mutationFn: (z: Zone) => supprimerZone(z.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['zones'] })
      notifications.show({ color: 'green', title: 'Zone supprimée', message: '' })
      setZoneASupprimer(null)
    },
    onError: (e) => {
      setZoneASupprimer(null)
      notifications.show({ color: 'red', title: 'Suppression impossible', message: e.message })
    },
  })

  return (
    <CarteReferentiel titre="Zones géographiques" description="Pays, régions, départements… qui regroupent les établissements." nombre={zones.data?.length}>
      {(zones.data ?? []).length === 0 ? (
        <Text c="dimmed" mb="md">
          Aucune zone.
        </Text>
      ) : (
        <Table highlightOnHover verticalSpacing="xs" mb="md">
          <Table.Tbody>
            {(zones.data ?? []).map((z) => (
              <Table.Tr key={z.id}>
                <Table.Td>{z.nom}</Table.Td>
                <Table.Td>{z.typeZone}</Table.Td>
                <Table.Td>{z.parentId === null ? '—' : (noms.get(z.parentId) ?? '—')}</Table.Td>
                <Table.Td>
                  <Group gap="xs" wrap="nowrap">
                    <Button size="compact-xs" variant="light" onClick={() => setPourRegion(z)}>
                      Compte de zone
                    </Button>
                    <Button size="compact-xs" variant="light" onClick={() => setZoneAModifier(z)}>
                      Modifier
                    </Button>
                    <Button size="compact-xs" variant="subtle" color="red" onClick={() => setZoneASupprimer(z)}>
                      Supprimer
                    </Button>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
      <Divider label="Ajouter" labelPosition="left" my="sm" />
      <form
        onSubmit={formulaire.onSubmit((v) =>
          mutation.mutate({
            nom: v.nom.trim(),
            typeZone: v.typeZone.trim(),
            parentId: v.parentId === '' ? null : Number(v.parentId),
          }),
        )}
      >
        <Stack gap="xs">
          <Group grow align="flex-start">
            <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
            <TextInput label="Type" placeholder="Pays, région, département…" {...formulaire.getInputProps('typeZone')} />
          </Group>
          <TextInput
            label="Zone parente (numéro, facultatif)"
            description="Laissez vide pour une zone racine"
            {...formulaire.getInputProps('parentId')}
          />
          <Erreur message={erreur} />
          <Button type="submit" loading={mutation.isPending}>
            Ajouter
          </Button>
        </Stack>
      </form>
      {zoneAModifier && (
        <ModifierReferentielModal
          titre={`Modifier la zone ${zoneAModifier.nom}`}
          champs={[
            { nom: 'nom', label: 'Nom' },
            { nom: 'typeZone', label: 'Type' },
            { nom: 'parentId', label: 'Zone parente (numéro)', type: 'number', facultatif: true },
          ]}
          valeursInitiales={{
            nom: zoneAModifier.nom,
            typeZone: zoneAModifier.typeZone,
            parentId: zoneAModifier.parentId === null ? '' : String(zoneAModifier.parentId),
          }}
          cles={['zones', 'etablissements']}
          enregistrer={(v) =>
            modifierZone(zoneAModifier.id, {
              nom: v.nom.trim(),
              typeZone: v.typeZone.trim(),
              parentId: v.parentId.trim() === '' ? null : Number(v.parentId),
            })
          }
          onFermer={() => setZoneAModifier(null)}
        />
      )}
      {zoneASupprimer && (
        <ConfirmationModal
          titre="Supprimer la zone"
          message={`Supprimer « ${zoneASupprimer.nom} » ? Impossible si elle contient des sous-zones, des établissements ou une direction régionale.`}
          libelleConfirmer="Supprimer"
          enCours={suppression.isPending}
          onConfirmer={() => suppression.mutate(zoneASupprimer)}
          onFermer={() => setZoneASupprimer(null)}
        />
      )}
      {pourRegion && (
        <DirectionModal
          titre={`Compte de zone (${pourRegion.typeZone}) — ${pourRegion.nom}`}
          creer={(requete) => creerDirectionRegionale(pourRegion.id, requete)}
          onFermer={() => setPourRegion(null)}
        />
      )}
    </CarteReferentiel>
  )
}

/** Types d'établissement (communautaire, franco-arabe, confessionnel…) proposés à la création d'un établissement. */
function TypesEtablissement() {
  const types = useQuery({ queryKey: ['types-etablissement'], queryFn: listerTypesEtablissement })
  const formulaire = useForm({
    initialValues: { code: '', libelle: '' },
    validate: {
      code: (v) => (v.trim() === '' ? 'Code requis' : null),
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
    },
  })
  const { mutation, erreur } = useCreation(creerTypeEtablissement, ['types-etablissement'], "Type d'établissement créé", () =>
    formulaire.reset(),
  )
  const [aModifier, setAModifier] = useState<TypeEtablissement | null>(null)
  const [aSupprimer, setASupprimer] = useState<TypeEtablissement | null>(null)
  const queryClient = useQueryClient()
  const suppression = useMutation({
    mutationFn: (t: TypeEtablissement) => supprimerTypeEtablissement(t.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['types-etablissement'] })
      notifications.show({ color: 'green', title: 'Type supprimé', message: '' })
      setASupprimer(null)
    },
    onError: (e) => {
      setASupprimer(null)
      notifications.show({ color: 'red', title: 'Suppression impossible', message: e.message })
    },
  })

  return (
    <CarteReferentiel
      titre="Types d'établissement"
      description="Communautaire, franco-arabe, confessionnel, technique… proposés dans la fiche d'un établissement."
      nombre={types.data?.length}
    >
      <Table highlightOnHover verticalSpacing="xs" mb="md">
        <Table.Tbody>
          {(types.data ?? []).map((t) => (
            <Table.Tr key={t.id}>
              <Table.Td>{t.code}</Table.Td>
              <Table.Td>{t.libelle}</Table.Td>
              <Table.Td>
                <Group gap="xs" wrap="nowrap">
                  <Button size="compact-xs" variant="light" onClick={() => setAModifier(t)}>
                    Modifier
                  </Button>
                  <Button size="compact-xs" variant="subtle" color="red" onClick={() => setASupprimer(t)}>
                    Supprimer
                  </Button>
                </Group>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Divider label="Ajouter" labelPosition="left" my="sm" />
      <form onSubmit={formulaire.onSubmit((v) => mutation.mutate({ code: v.code.trim(), libelle: v.libelle.trim() }))}>
        <Stack gap="xs">
          <Group grow align="flex-start">
            <TextInput label="Code" {...formulaire.getInputProps('code')} />
            <TextInput label="Libellé" {...formulaire.getInputProps('libelle')} />
          </Group>
          <Erreur message={erreur} />
          <Button type="submit" loading={mutation.isPending}>
            Ajouter
          </Button>
        </Stack>
      </form>
      {aModifier && (
        <ModifierReferentielModal
          titre={`Modifier le type ${aModifier.code}`}
          champs={[
            { nom: 'code', label: 'Code' },
            { nom: 'libelle', label: 'Libellé' },
          ]}
          valeursInitiales={{ code: aModifier.code, libelle: aModifier.libelle }}
          cles={['types-etablissement', 'etablissements']}
          enregistrer={(v) => modifierTypeEtablissement(aModifier.id, { code: v.code.trim(), libelle: v.libelle.trim() })}
          onFermer={() => setAModifier(null)}
        />
      )}
      {aSupprimer && (
        <ConfirmationModal
          titre="Supprimer le type"
          message={`Supprimer « ${aSupprimer.libelle} » ? Impossible s'il est utilisé par des établissements.`}
          libelleConfirmer="Supprimer"
          enCours={suppression.isPending}
          onConfirmer={() => suppression.mutate(aSupprimer)}
          onFermer={() => setASupprimer(null)}
        />
      )}
    </CarteReferentiel>
  )
}

export default function Referentiels() {
  return (
    <Stack mt="md" gap="lg">
      <EnteteSection
        titre="Référentiels"
        description="Les listes communes à tout le réseau : elles alimentent les formulaires, les bulletins et les statistiques."
      />
      <SimpleGrid cols={{ base: 1, lg: 2 }} spacing="lg">
      <Annees />
      <Periodes />
      <Cycles />
      <Niveaux />
      <Matieres />
      <TypesEtablissement />
      <Zones />
      </SimpleGrid>
    </Stack>
  )
}
