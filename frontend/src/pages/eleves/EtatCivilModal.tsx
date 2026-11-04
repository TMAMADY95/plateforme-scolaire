import { useState } from 'react'
import { Alert, Button, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { identiteNationale, modifierEtatCivil } from '../../api/endpoints'
import { ACTES_NAISSANCE, libelleActe } from '../../api/aideIdentite'
import type { EtatCivilRequete, IdentiteNationale } from '../../api/types'

interface Props {
  eleveId: number
  ine?: string | null
  onFermer: () => void
}

interface Valeurs {
  typeActeNaissance: string | null
  numeroActeNaissance: string
}

/** Piece d'etat civil d'un eleve : extrait, jugement suppletif, ou aucun acte. */
export default function EtatCivilModal({ eleveId, ine, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const identite = useQuery({
    queryKey: ['identite-nationale', eleveId],
    queryFn: () => identiteNationale(eleveId),
  })

  const formulaire = useForm<Valeurs>({
    initialValues: {
      typeActeNaissance: identite.data?.typeActeNaissance ?? null,
      numeroActeNaissance: identite.data?.numeroActeNaissance ?? '',
    },
    validate: {
      typeActeNaissance: (v) => (v === null ? "Type d'acte requis" : null),
      numeroActeNaissance: (v, valeurs) =>
        valeurs.typeActeNaissance !== 'AUCUN' && v.trim() === '' ? "Numero de l'acte requis" : null,
    },
  })

  const enregistrement = useMutation<IdentiteNationale, Error, EtatCivilRequete>({
    mutationFn: (requete) => modifierEtatCivil(eleveId, requete),
    onSuccess: (resultat) => {
      queryClient.setQueryData(['identite-nationale', eleveId], resultat)
      notifications.show({
        color: 'green',
        title: 'Etat civil enregistre',
        message: resultat.sansActe
          ? `${resultat.nom} ${resultat.prenom} reste sans acte de naissance`
          : `${resultat.nom} ${resultat.prenom} : ${libelleActe(resultat.typeActeNaissance)}`,
      })
      onFermer()
    },
    onError: (e: Error) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={ine ? `Etat civil - INE ${ine}` : "Etat civil de l'eleve"}>
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          enregistrement.mutate({
            typeActeNaissance: valeurs.typeActeNaissance as EtatCivilRequete['typeActeNaissance'],
            numeroActeNaissance: valeurs.typeActeNaissance === 'AUCUN' ? null : valeurs.numeroActeNaissance.trim(),
          })
        })}
      >
        <Stack>
          {identite.data && (
            <Text size="sm" c="dimmed">
              {identite.data.prenom} {identite.data.nom} - ne(e) le {identite.data.dateNaissance}
              {identite.data.lieuNaissance ? ` a ${identite.data.lieuNaissance}` : ''}
            </Text>
          )}
          <Select
            label="Piece d'etat civil"
            description="Figure dans le dossier d'examen national"
            data={[...ACTES_NAISSANCE]}
            {...formulaire.getInputProps('typeActeNaissance')}
          />
          {formulaire.values.typeActeNaissance !== 'AUCUN' && (
            <TextInput
              label="Numero de l'acte"
              placeholder="Numero porte sur l'extrait ou le jugement"
              {...formulaire.getInputProps('numeroActeNaissance')}
            />
          )}
          {identite.data?.sansActe && (
            <Alert color="orange" variant="light">
              Cet eleve est sans acte de naissance : il ne pourra pas etre inscrit aux examens nationaux tant
              que la situation n'est pas regularisee.
            </Alert>
          )}
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
