import { useState } from 'react'
import {
  Alert,
  Button,
  Chip,
  Divider,
  Group,
  Input,
  Modal,
  NumberInput,
  SegmentedControl,
  Select,
  Stack,
  Text,
  TextInput,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { creerEtablissement, listerTypesEtablissement, modifierEtablissement } from '../../api/endpoints'
import type { Cycle, Etablissement, Secteur, Zone } from '../../api/types'

interface Props {
  /** Absent = création. */
  etablissement?: Etablissement
  zones: Zone[]
  /** Les cycles du référentiel (primaire, collège, lycée…). */
  cycles: Cycle[]
  onFermer: () => void
}

interface Valeurs {
  codeOfficiel: string
  nom: string
  secteur: Secteur
  typeId: string | null
  cycleIds: string[]
  zoneId: string | null
  adresse: string
  capacite: number | string
}

export default function EtablissementModal({ etablissement, zones, cycles, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const types = useQuery({ queryKey: ['types-etablissement'], queryFn: listerTypesEtablissement })

  const formulaire = useForm<Valeurs>({
    initialValues: {
      codeOfficiel: etablissement?.codeOfficiel ?? '',
      nom: etablissement?.nom ?? '',
      secteur: etablissement?.secteur ?? 'PUBLIC',
      typeId: etablissement?.typeEtablissementId != null ? String(etablissement.typeEtablissementId) : null,
      cycleIds: etablissement?.cycles.map((c) => String(c.id)) ?? [],
      zoneId: etablissement?.zoneId != null ? String(etablissement.zoneId) : null,
      adresse: etablissement?.adresse ?? '',
      capacite: etablissement?.capacite ?? '',
    },
    validate: {
      codeOfficiel: (v) => (v.trim() === '' ? 'Code requis' : null),
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      cycleIds: (v) => (v.length === 0 ? 'Choisissez au moins un cycle' : null),
      capacite: (v) => (v === '' || Number(v) > 0 ? null : 'Nombre positif attendu'),
    },
  })

  const enregistrement = useMutation({
    mutationFn: (v: Valeurs) => {
      const requete = {
        codeOfficiel: v.codeOfficiel.trim(),
        nom: v.nom.trim(),
        zoneId: v.zoneId === null ? null : Number(v.zoneId),
        secteur: v.secteur,
        typeEtablissementId: v.typeId === null ? null : Number(v.typeId),
        cycleIds: v.cycleIds.map(Number),
        adresse: v.adresse.trim() || undefined,
        capacite: v.capacite === '' ? undefined : Number(v.capacite),
      }
      return etablissement ? modifierEtablissement(etablissement.id, requete) : creerEtablissement(requete)
    },
    onSuccess: (resultat) => {
      void queryClient.invalidateQueries({ queryKey: ['etablissements'] })
      void queryClient.invalidateQueries({ queryKey: ['synthese-nationale'] })
      notifications.show({
        color: 'green',
        title: etablissement ? 'Établissement modifié' : 'Établissement créé',
        message: resultat.nom,
      })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={etablissement ? "Modifier l'établissement" : 'Nouvel établissement'} size="lg">
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          enregistrement.mutate(valeurs)
        })}
      >
        <Stack>
          <Divider label="Identification" labelPosition="left" />
          <Group grow align="flex-start">
            <TextInput
              label="Code officiel"
              description={etablissement ? 'Non modifiable' : 'Identifiant unique'}
              disabled={etablissement !== undefined}
              {...formulaire.getInputProps('codeOfficiel')}
            />
            <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
          </Group>

          <Divider label="Nature de l'établissement" labelPosition="left" />
          <Input.Wrapper label="Secteur">
            <SegmentedControl
              fullWidth
              mt={4}
              data={[
                { value: 'PUBLIC', label: 'Public' },
                { value: 'PRIVE', label: 'Privé' },
              ]}
              value={formulaire.values.secteur}
              onChange={(v) => formulaire.setFieldValue('secteur', v as Secteur)}
            />
          </Input.Wrapper>
          <Select
            label="Type d'établissement (facultatif)"
            description="Les types se gèrent dans l'onglet « Référentiels »."
            data={(types.data ?? []).map((t) => ({ value: String(t.id), label: t.libelle }))}
            clearable
            {...formulaire.getInputProps('typeId')}
          />
          <Input.Wrapper
            label="Cycles proposés"
            description="Un seul, ou plusieurs si l'établissement regroupe plusieurs cycles."
            error={formulaire.errors.cycleIds}
          >
            <Chip.Group multiple value={formulaire.values.cycleIds} onChange={(v) => formulaire.setFieldValue('cycleIds', v)}>
              <Group mt="xs" gap="xs">
                {cycles.map((c) => (
                  <Chip key={c.id} value={String(c.id)} variant="outline">
                    {c.libelle}
                  </Chip>
                ))}
              </Group>
            </Chip.Group>
            {cycles.length === 0 && (
              <Text size="sm" c="dimmed" mt="xs">
                Aucun cycle défini : ajoutez-en dans l'onglet « Référentiels ».
              </Text>
            )}
          </Input.Wrapper>

          <Divider label="Localisation" labelPosition="left" />
          <Select
            label="Zone (facultatif)"
            data={zones.map((z) => ({ value: String(z.id), label: `${z.nom} (${z.typeZone})` }))}
            clearable
            searchable
            {...formulaire.getInputProps('zoneId')}
          />
          <TextInput label="Adresse (facultatif)" {...formulaire.getInputProps('adresse')} />
          <NumberInput label="Capacité d'accueil (facultatif)" min={1} allowDecimal={false} {...formulaire.getInputProps('capacite')} />
          {erreur !== null && (
            <Alert color="red" variant="light">
              {erreur}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onFermer}>
              Annuler
            </Button>
            <Button type="submit" loading={enregistrement.isPending}>
              Enregistrer
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
