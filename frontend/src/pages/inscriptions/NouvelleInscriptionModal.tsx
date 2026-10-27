import { useState } from 'react'
import { Alert, Button, Checkbox, Group, Modal, Select, SegmentedControl, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { demanderInscription } from '../../api/endpoints'
import type { InscriptionRequete } from '../../api/types'
import { aujourdhui } from '../../utils/dates'

interface Props {
  etablissementId: number
  anneeIdInitiale: string | null
  optionsAnnees: { value: string; label: string }[]
  onFermer: () => void
}

interface Valeurs {
  mode: 'nouveau' | 'existant'
  anneeScolaireId: string | null
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
  matricule: string
  confirmerHomonyme: boolean
}

export default function NouvelleInscriptionModal({ etablissementId, anneeIdInitiale, optionsAnnees, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const [homonymePossible, setHomonymePossible] = useState(false)

  const formulaire = useForm<Valeurs>({
    initialValues: {
      mode: 'nouveau',
      anneeScolaireId: anneeIdInitiale,
      nom: '',
      prenom: '',
      dateNaissance: '',
      sexe: null,
      matricule: '',
      confirmerHomonyme: false,
    },
    validate: (v) => {
      const nouveau = v.mode === 'nouveau'
      return {
        anneeScolaireId: v.anneeScolaireId === null ? 'Année requise' : null,
        nom: nouveau && v.nom.trim() === '' ? 'Nom requis' : null,
        prenom: nouveau && v.prenom.trim() === '' ? 'Prénom requis' : null,
        dateNaissance: nouveau
          ? v.dateNaissance === ''
            ? 'Date de naissance requise'
            : v.dateNaissance >= aujourdhui()
              ? 'La date de naissance doit être passée'
              : null
          : null,
        matricule: !nouveau && v.matricule.trim() === '' ? 'Matricule requis' : null,
      }
    },
  })

  const demande = useMutation({
    mutationFn: (v: Valeurs) => {
      const requete: InscriptionRequete = { anneeScolaireId: Number(v.anneeScolaireId) }
      if (v.mode === 'existant') {
        requete.matriculeEleve = v.matricule.trim()
      } else {
        requete.nouvelEleve = {
          nom: v.nom.trim(),
          prenom: v.prenom.trim(),
          dateNaissance: v.dateNaissance,
          sexe: v.sexe,
        }
        if (v.confirmerHomonyme) requete.confirmerHomonyme = true
      }
      return demanderInscription(etablissementId, requete)
    },
    onSuccess: (inscription) => {
      void queryClient.invalidateQueries({ queryKey: ['inscriptions', etablissementId] })
      notifications.show({
        color: 'green',
        title: 'Inscription enregistrée',
        message: `${inscription.eleve.prenom} ${inscription.eleve.nom} (matricule ${inscription.eleve.matricule}) est en attente de décision.`,
      })
      onFermer()
    },
    onError: (e) => {
      setErreur(e.message)
      // Le serveur propose de confirmer qu'il s'agit d'un homonyme quand l'identité existe déjà.
      setHomonymePossible(e.message.includes('homonyme'))
    },
  })

  const nouveau = formulaire.values.mode === 'nouveau'

  return (
    <Modal opened onClose={onFermer} title="Nouvelle inscription" size="lg">
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          demande.mutate(valeurs)
        })}
      >
        <Stack>
          <SegmentedControl
            fullWidth
            data={[
              { value: 'nouveau', label: 'Nouvel élève' },
              { value: 'existant', label: 'Élève déjà connu (matricule)' },
            ]}
            {...formulaire.getInputProps('mode')}
          />
          <Select
            label="Année scolaire"
            data={optionsAnnees}
            allowDeselect={false}
            {...formulaire.getInputProps('anneeScolaireId')}
          />

          {nouveau ? (
            <>
              <Group grow align="flex-start">
                <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
                <TextInput label="Prénom" {...formulaire.getInputProps('prenom')} />
              </Group>
              <Group grow align="flex-start">
                <TextInput label="Date de naissance" type="date" {...formulaire.getInputProps('dateNaissance')} />
                <Select
                  label="Sexe"
                  data={[
                    { value: 'F', label: 'Féminin' },
                    { value: 'M', label: 'Masculin' },
                  ]}
                  clearable
                  {...formulaire.getInputProps('sexe')}
                />
              </Group>
            </>
          ) : (
            <TextInput
              label="Matricule de l'élève"
              description="Réinscription ou arrivée d'un autre établissement"
              placeholder="ELV-00000001"
              {...formulaire.getInputProps('matricule')}
            />
          )}

          {erreur !== null && (
            <Alert color="red" variant="light">
              {erreur}
            </Alert>
          )}
          {nouveau && homonymePossible && (
            <Checkbox
              label="Il s'agit bien d'un autre élève (homonyme) : créer malgré tout"
              {...formulaire.getInputProps('confirmerHomonyme', { type: 'checkbox' })}
            />
          )}

          <Group justify="flex-end">
            <Button variant="default" onClick={onFermer}>
              Annuler
            </Button>
            <Button type="submit" loading={demande.isPending}>
              Enregistrer
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
