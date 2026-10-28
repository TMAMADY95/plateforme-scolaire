import { useState } from 'react'
import { Alert, Button, Group, Modal, NumberInput, Select, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { creerEtablissement, modifierEtablissement } from '../../api/endpoints'
import type { Etablissement, Zone } from '../../api/types'

interface Props {
  /** Absent = création. */
  etablissement?: Etablissement
  zones: Zone[]
  onFermer: () => void
}

interface Valeurs {
  codeOfficiel: string
  nom: string
  zoneId: string | null
  typeEtablissement: string
  adresse: string
  capacite: number | string
}

export default function EtablissementModal({ etablissement, zones, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const formulaire = useForm<Valeurs>({
    initialValues: {
      codeOfficiel: etablissement?.codeOfficiel ?? '',
      nom: etablissement?.nom ?? '',
      zoneId: etablissement?.zoneId != null ? String(etablissement.zoneId) : null,
      typeEtablissement: etablissement?.typeEtablissement ?? '',
      adresse: etablissement?.adresse ?? '',
      capacite: etablissement?.capacite ?? '',
    },
    validate: {
      codeOfficiel: (v) => (v.trim() === '' ? 'Code requis' : null),
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      capacite: (v) => (v === '' || Number(v) > 0 ? null : 'Nombre positif attendu'),
    },
  })

  const enregistrement = useMutation({
    mutationFn: (v: Valeurs) => {
      const requete = {
        codeOfficiel: v.codeOfficiel.trim(),
        nom: v.nom.trim(),
        zoneId: v.zoneId === null ? null : Number(v.zoneId),
        typeEtablissement: v.typeEtablissement.trim() || undefined,
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
          <Group grow align="flex-start">
            <TextInput
              label="Code officiel"
              description={etablissement ? 'Non modifiable' : 'Identifiant unique'}
              disabled={etablissement !== undefined}
              {...formulaire.getInputProps('codeOfficiel')}
            />
            <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
          </Group>
          <Group grow align="flex-start">
            <TextInput label="Type (facultatif)" placeholder="Primaire, collège, lycée…" {...formulaire.getInputProps('typeEtablissement')} />
            <Select
              label="Zone (facultatif)"
              data={zones.map((z) => ({ value: String(z.id), label: `${z.nom} (${z.typeZone})` }))}
              clearable
              searchable
              {...formulaire.getInputProps('zoneId')}
            />
          </Group>
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
