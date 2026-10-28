import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Select, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  creerCreneau,
  listerEnseignants,
  listerMatieres,
  listerSalles,
  modifierCreneau,
} from '../../api/endpoints'
import type { Creneau } from '../../api/types'
import { JOURS, formaterHeure } from '../../utils/horaires'

interface Props {
  etablissementId: number
  classeId: number
  classeNom: string
  /** Absent : création. Présent : modification. */
  creneau?: Creneau
  onFermer: () => void
}

interface Valeurs {
  matiereId: string | null
  enseignantId: string | null
  salleId: string | null
  jourSemaine: string | null
  heureDebut: string
  heureFin: string
}

/** Création ou modification d'un créneau. Les conflits d'horaire sont contrôlés par le serveur. */
export default function CreneauModal({ etablissementId, classeId, classeNom, creneau, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres })
  const enseignants = useQuery({
    queryKey: ['enseignants', etablissementId],
    queryFn: () => listerEnseignants(etablissementId),
  })
  const salles = useQuery({ queryKey: ['salles', etablissementId], queryFn: () => listerSalles(etablissementId) })

  const formulaire = useForm<Valeurs>({
    initialValues: {
      matiereId: creneau ? String(creneau.matiereId) : null,
      enseignantId: creneau?.enseignantId != null ? String(creneau.enseignantId) : null,
      salleId: creneau?.salleId != null ? String(creneau.salleId) : null,
      jourSemaine: creneau ? String(creneau.jourSemaine) : null,
      heureDebut: creneau ? formaterHeure(creneau.heureDebut) : '',
      heureFin: creneau ? formaterHeure(creneau.heureFin) : '',
    },
    validate: {
      matiereId: (v) => (v === null ? 'Matière requise' : null),
      jourSemaine: (v) => (v === null ? 'Jour requis' : null),
      heureDebut: (v) => (v === '' ? 'Heure de début requise' : null),
      heureFin: (v, valeurs) =>
        v === ''
          ? 'Heure de fin requise'
          : valeurs.heureDebut !== '' && v <= valeurs.heureDebut
            ? "L'heure de fin doit être après l'heure de début"
            : null,
    },
  })

  const enregistrement = useMutation({
    mutationFn: (v: Valeurs) => {
      const requete = {
        matiereId: Number(v.matiereId),
        enseignantId: v.enseignantId === null ? null : Number(v.enseignantId),
        salleId: v.salleId === null ? null : Number(v.salleId),
        jourSemaine: Number(v.jourSemaine),
        heureDebut: v.heureDebut,
        heureFin: v.heureFin,
      }
      return creneau ? modifierCreneau(creneau.id, requete) : creerCreneau(classeId, requete)
    },
    onSuccess: () => {
      for (const cle of ['creneaux-etab', 'creneaux-classe']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: creneau ? 'Créneau modifié' : 'Créneau ajouté', message: classeNom })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  const chargement = matieres.isPending || enseignants.isPending || salles.isPending
  const echec = matieres.error ?? enseignants.error ?? salles.error

  return (
    <Modal opened onClose={onFermer} title={`${creneau ? 'Modifier le créneau' : 'Nouveau créneau'} — ${classeNom}`}>
      {chargement && <Loader />}
      {echec && (
        <Alert color="red" variant="light">
          {echec.message}
        </Alert>
      )}
      {!chargement && !echec && (
        <form
          onSubmit={formulaire.onSubmit((valeurs) => {
            setErreur(null)
            enregistrement.mutate(valeurs)
          })}
        >
          <Stack>
            <Select
              label="Matière"
              searchable
              data={(matieres.data ?? []).map((m) => ({ value: String(m.id), label: m.libelle }))}
              {...formulaire.getInputProps('matiereId')}
            />
            <Select
              label="Enseignant"
              placeholder="Non précisé"
              clearable
              searchable
              data={(enseignants.data ?? []).map((e) => ({ value: String(e.id), label: `${e.prenom} ${e.nom}` }))}
              {...formulaire.getInputProps('enseignantId')}
            />
            <Select
              label="Salle"
              placeholder="Non précisée"
              clearable
              data={(salles.data ?? []).map((s) => ({ value: String(s.id), label: s.nom }))}
              {...formulaire.getInputProps('salleId')}
            />
            <Select
              label="Jour"
              allowDeselect={false}
              data={JOURS.map((j) => ({ value: String(j.value), label: j.label }))}
              {...formulaire.getInputProps('jourSemaine')}
            />
            <Group grow align="flex-start">
              <TextInput label="Début" type="time" {...formulaire.getInputProps('heureDebut')} />
              <TextInput label="Fin" type="time" {...formulaire.getInputProps('heureFin')} />
            </Group>
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
      )}
    </Modal>
  )
}
