import { useState } from 'react'
import { Alert, Button, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { identiteNationale, modifierEtatCivil } from '../../api/endpointsEcole'
import { ACTES_NAISSANCE, libelleActe } from '../../api/typesEcole'
import { useQuery, useQueryClient } from '@tanstack/react-query'

interface Props {
  eleveId: number
  ine?: string | null
  onFermer: () => void
}

interface Valeurs {
  typeActeNaissance: string | null
  numeroActeNaissance: string
}

/**
 * Pièce d'état civil d'un élève : extrait de naissance, jugement supplétif, ou aucun acte.
 * Un élève sans acte ne peut pas être inscrit aux examens nationaux (CEE, BEPC, BAC).
 */
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
        valeurs.typeActeNaissance !== 'AUCUN' && v.trim() === '' ? "Numéro de l'acte requis" : null,
    },
  })

  const enregistrement = useMutation({
    mutationFn: (valeurs: Valeurs) =>
      modifierEtatCivil(eleveId, {
        // Le type est validé par le formulaire ; le serveur refusera toute autre valeur.
        typeActeNaissance: valeurs.typeActeNaissance as 'EXTRAIT' | 'JUGEMENT_SUPPLETIF' | 'AUCUN',
        numeroActeNaissance:
          valeurs.typeActeNaissance === 'AUCUN' ? null : valeurs.numeroActeNaissance.trim(),
      }),
    onSuccess: (resultat) => {
      queryClient.setQueryData(['identite-nationale', eleveId], resultat)
      notifications.show({
        color: 'green',
        title: 'État civil enregistré',
        message:
          resultat.sansActe
            ? `${resultat.nom} ${resultat.prenom} reste sans acte de naissance`
            : `${resultat.nom} ${resultat.prenom} : ${libelleActe(resultat.typeActeNaissance)}`,
      })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={ine ? `État civil — INE ${ine}` : "État civil de l'élève"}>
      <form onSubmit={formulaire.onSubmit((valeurs) => {
        setErreur(null)
        enregistrement.mutate(valeurs)
      })}>
        <Stack>
          {identite.data && (
            <Text size="sm" c="dimmed">
              {identite.data.prenom} {identite.data.nom} — né(e) le {identite.data.dateNaissance}
              {identite.data.lieuNaissance ? ` à ${identite.data.lieuNaissance}` : ''}
            </Text>
          )}
          <Select
            label="Pièce d'état civil"
            description="Figure dans le dossier d'examen national"
            data={ACTES_NAISSANCE}
            {...formulaire.getInputProps('typeActeNaissance')}
          />
          {formulaire.values.typeActeNaissance !== 'AUCUN' && (
            <TextInput
              label="Numéro de l'acte"
              placeholder="Numéro porté sur l'extrait ou le jugement"
              {...formulaire.getInputProps('numeroActeNaissance')}
            />
          )}
          {identite.data?.sansActe && (
            <Alert color="orange" variant="light">
              Cet élève est actuellement sans acte de naissance : il ne pourra pas être inscrit aux examens
              nationaux tant que la situation n'est pas régularisée.
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
