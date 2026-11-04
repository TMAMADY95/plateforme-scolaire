import { useState } from 'react'
import { Alert, Button, FileInput, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { envoyerPhotoEleve, infoPhotoEleve, modifierEleve, supprimerPhotoEleve } from '../../api/endpoints'
import { identiteNationale } from '../../api/endpoints'
import { libelleActe } from '../../api/aideIdentite'
import type { Eleve } from '../../api/types'
import { aujourdhui } from '../../utils/dates'

interface Props {
  eleve: Eleve
  etablissementId: number
  onFermer: () => void
}

interface Valeurs {
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
  lieuNaissance: string
  nomPere: string
  nomMere: string
}

/** Correction de l'identite d'un eleve. Le matricule et l'INE ne changent jamais. */
export default function ModifierEleveModal({ eleve, etablissementId, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const [fichier, setFichier] = useState<File | null>(null)
  const photo = useQuery({ queryKey: ['photo-eleve', eleve.id], queryFn: () => infoPhotoEleve(eleve.id) })
  const identite = useQuery({
    queryKey: ['identite-nationale', eleve.id],
    queryFn: () => identiteNationale(eleve.id),
  })
  const envoi = useMutation({
    mutationFn: () => envoyerPhotoEleve(eleve.id, fichier as File),
    onSuccess: () => {
      setFichier(null)
      void queryClient.invalidateQueries({ queryKey: ['photo-eleve', eleve.id] })
      notifications.show({ color: 'green', title: 'Photo enregistree', message: `Matricule ${eleve.matricule}` })
    },
    onError: (e: Error) => setErreur(e.message),
  })
  const retrait = useMutation({
    mutationFn: () => supprimerPhotoEleve(eleve.id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['photo-eleve', eleve.id] }),
    onError: (e: Error) => setErreur(e.message),
  })

  const formulaire = useForm<Valeurs>({
    initialValues: { nom: eleve.nom, prenom: eleve.prenom, dateNaissance: eleve.dateNaissance, sexe: eleve.sexe,
      lieuNaissance: eleve.lieuNaissance ?? '', nomPere: eleve.nomPere ?? '', nomMere: eleve.nomMere ?? '' },
    validate: {
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      prenom: (v) => (v.trim() === '' ? 'Prenom requis' : null),
      dateNaissance: (v) =>
        v === '' ? 'Date de naissance requise' : v >= aujourdhui() ? 'La date de naissance doit etre passee' : null,
    },
  })

  const modification = useMutation({
    mutationFn: (v: Valeurs) =>
      modifierEleve(eleve.id, {
        nom: v.nom.trim(),
        prenom: v.prenom.trim(),
        dateNaissance: v.dateNaissance,
        sexe: v.sexe,
        lieuNaissance: v.lieuNaissance.trim(),
        nomPere: v.nomPere.trim(),
        nomMere: v.nomMere.trim(),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['inscriptions', etablissementId] })
      notifications.show({ color: 'green', title: 'Identite corrigee', message: `Matricule ${eleve.matricule}` })
      onFermer()
    },
    onError: (e: Error) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={`Modifier l'eleve ${eleve.matricule}`}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          modification.mutate(valeurs)
        })}
      >
        <Stack>
          <Stack gap={2}>
            <Text size="sm" fw={500}>INE : {identite.data?.ine ?? eleve.ine ?? '—'}</Text>
            <Text size="xs" c="dimmed">
              Identifiant national attribue une seule fois. Il ne peut pas etre modifie.
            </Text>
            {identite.data?.sansActe && (
              <Text size="xs" c="orange">
                Etat civil : {libelleActe(identite.data.typeActeNaissance)} — a regulariser avant les examens
                nationaux.
              </Text>
            )}
          </Stack>
          <Group grow align="flex-start">
            <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
            <TextInput label="Prenom" {...formulaire.getInputProps('prenom')} />
          </Group>
          <Group grow align="flex-start">
            <TextInput label="Date de naissance" type="date" {...formulaire.getInputProps('dateNaissance')} />
            <Select
              label="Sexe"
              data={[
                { value: 'F', label: 'Feminin' },
                { value: 'M', label: 'Masculin' },
              ]}
              clearable
              {...formulaire.getInputProps('sexe')}
            />
          </Group>
          <TextInput label="Lieu de naissance" description="Figure sur les diplomes" {...formulaire.getInputProps('lieuNaissance')} />
          <Group grow align="flex-start">
            <TextInput label="Pere" {...formulaire.getInputProps('nomPere')} />
            <TextInput label="Mere" {...formulaire.getInputProps('nomMere')} />
          </Group>
          <Stack gap={4}>
            <Text size="sm" fw={500}>Photo d'identite (JPEG ou PNG, 1 Mo au plus)</Text>
            <Text size="xs" c="dimmed">
              {photo.data?.presente ? `Photo enregistree (${Math.round(photo.data.taille / 1024)} Ko) : elle figure sur le diplome.` : 'Aucune photo enregistree.'}
            </Text>
            <Group align="flex-end" gap="xs">
              <FileInput size="xs" placeholder="Choisir une photo" accept="image/jpeg,image/png" value={fichier} onChange={setFichier} clearable w={220} />
              <Button size="xs" variant="light" disabled={fichier === null} loading={envoi.isPending} onClick={() => envoi.mutate()}>
                Envoyer
              </Button>
              {photo.data?.presente && (
                <Button size="xs" variant="subtle" color="red" loading={retrait.isPending} onClick={() => retrait.mutate()}>
                  Retirer
                </Button>
              )}
            </Group>
          </Stack>
          {erreur !== null && (
            <Alert color="red" variant="light">
              {erreur}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onFermer}>
              Annuler
            </Button>
            <Button type="submit" loading={modification.isPending}>
              Enregistrer
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
