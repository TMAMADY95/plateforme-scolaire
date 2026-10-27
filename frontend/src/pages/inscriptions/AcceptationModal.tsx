import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Select, Stack, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deciderInscription, listerClasses } from '../../api/endpoints'
import type { Inscription } from '../../api/types'

interface Props {
  inscription: Inscription
  onFermer: () => void
}

/** Choix de la classe d'affectation pour accepter une inscription. */
export default function AcceptationModal({ inscription, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [classeId, setClasseId] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const classes = useQuery({
    queryKey: ['classes', inscription.etablissementId, String(inscription.anneeScolaireId)],
    queryFn: () => listerClasses(inscription.etablissementId, inscription.anneeScolaireId),
  })

  const acceptation = useMutation({
    mutationFn: () => deciderInscription(inscription.id, { decision: 'ACCEPTEE', classeId: Number(classeId) }),
    onSuccess: (resultat) => {
      void queryClient.invalidateQueries({ queryKey: ['inscriptions', inscription.etablissementId] })
      notifications.show({
        color: 'green',
        title: 'Inscription acceptée',
        message: `${inscription.eleve.prenom} ${inscription.eleve.nom} est affecté(e) à la classe ${resultat.classeNom}.`,
      })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  const options = (classes.data ?? []).map((c) => ({ value: String(c.id), label: `${c.nom} — ${c.niveauLibelle}` }))

  return (
    <Modal opened onClose={onFermer} title="Accepter l'inscription">
      <Stack>
        <Text>
          Élève : <b>{inscription.eleve.prenom} {inscription.eleve.nom}</b> ({inscription.eleve.matricule})
        </Text>
        {classes.isPending && <Loader size="sm" />}
        {classes.isError && (
          <Alert color="red" variant="light">
            {classes.error.message}
          </Alert>
        )}
        {classes.data && classes.data.length === 0 && (
          <Alert color="orange" variant="light">
            Aucune classe n'existe pour cette année scolaire : créez-en une d'abord dans la page Classes.
          </Alert>
        )}
        {classes.data && classes.data.length > 0 && (
          <Select label="Classe" placeholder="Choisir une classe" data={options} value={classeId} onChange={setClasseId} />
        )}
        {erreur !== null && (
          <Alert color="red" variant="light">
            {erreur}
          </Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>
            Retour
          </Button>
          <Button
            color="green"
            disabled={classeId === null}
            loading={acceptation.isPending}
            onClick={() => {
              setErreur(null)
              acceptation.mutate()
            }}
          >
            Accepter
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
