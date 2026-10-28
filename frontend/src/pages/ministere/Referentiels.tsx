import { useState } from 'react'
import { Alert, Button, Card, Group, NumberInput, SimpleGrid, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  creerAnnee,
  creerDirectionRegionale,
  creerMatiere,
  creerNiveau,
  creerZone,
  listerAnnees,
  listerMatieres,
  listerNiveaux,
  listerZones,
  modifierAnnee,
  modifierMatiere,
  modifierNiveau,
  modifierZone,
  supprimerZone,
} from '../../api/endpoints'
import type { AnneeScolaire, Matiere, Niveau, Zone } from '../../api/types'
import { formaterDate } from '../../utils/dates'
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
    <Card withBorder padding="md">
      <Title order={4} mb="sm">
        Années scolaires
      </Title>
      <Table withTableBorder mb="md">
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
    </Card>
  )
}

function Niveaux() {
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })
  const formulaire = useForm({
    initialValues: { code: '', libelle: '', ordre: 1 as number | string },
    validate: {
      code: (v) => (v.trim() === '' ? 'Code requis' : null),
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
      ordre: (v) => (Number(v) >= 1 ? null : 'Au moins 1'),
    },
  })
  const { mutation, erreur } = useCreation(creerNiveau, ['niveaux'], 'Niveau créé', () => formulaire.reset())
  const [aModifier, setAModifier] = useState<Niveau | null>(null)

  return (
    <Card withBorder padding="md">
      <Title order={4} mb="sm">
        Niveaux
      </Title>
      <Table withTableBorder mb="md">
        <Table.Tbody>
          {(niveaux.data ?? []).map((n) => (
            <Table.Tr key={n.id}>
              <Table.Td>{n.ordre}</Table.Td>
              <Table.Td>{n.code}</Table.Td>
              <Table.Td>{n.libelle}</Table.Td>
              <Table.Td>
                <Button size="compact-xs" variant="light" onClick={() => setAModifier(n)}>
                  Modifier
                </Button>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <form
        onSubmit={formulaire.onSubmit((v) =>
          mutation.mutate({ code: v.code.trim(), libelle: v.libelle.trim(), ordre: Number(v.ordre) }),
        )}
      >
        <Stack gap="xs">
          <Group grow align="flex-start">
            <TextInput label="Code" {...formulaire.getInputProps('code')} />
            <NumberInput label="Ordre" min={1} allowDecimal={false} {...formulaire.getInputProps('ordre')} />
          </Group>
          <TextInput label="Libellé" {...formulaire.getInputProps('libelle')} />
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
          ]}
          valeursInitiales={{ code: aModifier.code, libelle: aModifier.libelle, ordre: String(aModifier.ordre) }}
          cles={['niveaux', 'classes']}
          enregistrer={(v) =>
            modifierNiveau(aModifier.id, { code: v.code.trim(), libelle: v.libelle.trim(), ordre: Number(v.ordre) })
          }
          onFermer={() => setAModifier(null)}
        />
      )}
    </Card>
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
    <Card withBorder padding="md">
      <Title order={4} mb="sm">
        Matières
      </Title>
      <Table withTableBorder mb="md">
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
    </Card>
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
    <Card withBorder padding="md">
      <Title order={4} mb="sm">
        Zones géographiques
      </Title>
      {(zones.data ?? []).length === 0 ? (
        <Text c="dimmed" mb="md">
          Aucune zone.
        </Text>
      ) : (
        <Table withTableBorder mb="md">
          <Table.Tbody>
            {(zones.data ?? []).map((z) => (
              <Table.Tr key={z.id}>
                <Table.Td>{z.nom}</Table.Td>
                <Table.Td>{z.typeZone}</Table.Td>
                <Table.Td>{z.parentId === null ? '—' : (noms.get(z.parentId) ?? '—')}</Table.Td>
                <Table.Td>
                  <Group gap="xs" wrap="nowrap">
                    <Button size="compact-xs" variant="light" onClick={() => setPourRegion(z)}>
                      Direction régionale
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
          titre={`Direction régionale — ${pourRegion.nom}`}
          creer={(requete) => creerDirectionRegionale(pourRegion.id, requete)}
          onFermer={() => setPourRegion(null)}
        />
      )}
    </Card>
  )
}

export default function Referentiels() {
  return (
    <SimpleGrid cols={{ base: 1, md: 2 }} mt="md">
      <Annees />
      <Niveaux />
      <Matieres />
      <Zones />
    </SimpleGrid>
  )
}
