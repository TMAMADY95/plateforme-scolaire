import { useState } from 'react'
import { Alert, Button, Checkbox, Group, Modal, NumberInput, Select, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { creerFrais, listerNiveaux, modifierFrais } from '../../api/endpoints'
import type { Frais } from '../../api/types'

interface Props {
  etablissementId: number
  anneeScolaireId: number
  /** Devise déjà utilisée par la grille de l'année (une seule devise par établissement et par année). */
  deviseExistante: string | null
  frais?: Frais
  onFermer: () => void
}

interface Valeurs {
  libelle: string
  niveauId: string | null
  montant: number | string
  devise: string
  echeance: string
  obligatoire: boolean
}

/** Création ou modification d'une ligne du barème (frais de scolarité, cantine, transport…). */
export default function FraisModal({ etablissementId, anneeScolaireId, deviseExistante, frais, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })

  const formulaire = useForm<Valeurs>({
    initialValues: {
      libelle: frais?.libelle ?? '',
      niveauId: frais?.niveauId != null ? String(frais.niveauId) : null,
      montant: frais?.montant ?? '',
      devise: frais?.devise ?? deviseExistante ?? '',
      echeance: frais?.echeance ?? '',
      obligatoire: frais?.obligatoire ?? true,
    },
    validate: {
      libelle: (v) => (v.trim() === '' ? 'Libellé requis' : null),
      montant: (v) => (v === '' || Number(v) < 0 ? 'Montant requis (0 ou plus)' : null),
      devise: (v) => (/^[A-Z]{3}$/.test(v.trim().toUpperCase()) ? null : 'Code ISO à 3 lettres (ex. EUR, XOF)'),
    },
  })

  const enregistrement = useMutation({
    mutationFn: (v: Valeurs) => {
      const requete = {
        anneeScolaireId,
        niveauId: v.niveauId === null ? null : Number(v.niveauId),
        libelle: v.libelle.trim(),
        montant: Number(v.montant),
        devise: v.devise.trim().toUpperCase(),
        echeance: v.echeance === '' ? null : v.echeance,
        obligatoire: v.obligatoire,
        actif: frais?.actif ?? true,
      }
      return frais ? modifierFrais(frais.id, requete) : creerFrais(etablissementId, requete)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['frais'] })
      notifications.show({ color: 'green', title: frais ? 'Frais modifié' : 'Frais ajouté', message: '' })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={frais ? 'Modifier le frais' : 'Nouveau frais'}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          enregistrement.mutate(valeurs)
        })}
      >
        <Stack>
          <TextInput label="Libellé" placeholder="Frais de scolarité" {...formulaire.getInputProps('libelle')} />
          <Select
            label="Niveau concerné"
            placeholder="Tous les niveaux"
            clearable
            data={(niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))}
            {...formulaire.getInputProps('niveauId')}
          />
          <Group grow align="flex-start">
            <NumberInput label="Montant" min={0} decimalScale={2} {...formulaire.getInputProps('montant')} />
            <TextInput
              label="Devise"
              description={deviseExistante ? 'Devise de la grille de cette année' : undefined}
              maxLength={3}
              disabled={deviseExistante !== null && frais === undefined}
              {...formulaire.getInputProps('devise')}
            />
          </Group>
          <TextInput label="Échéance (facultatif)" type="date" {...formulaire.getInputProps('echeance')} />
          <Checkbox
            label="Frais obligatoire"
            {...formulaire.getInputProps('obligatoire', { type: 'checkbox' })}
          />
          <Alert color="blue" variant="light">
            Les factures déjà émises ne changent pas : leur montant est figé à l'émission.
          </Alert>
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
