import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Select, Stack, Text, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { contactsMessagerie, demarrerConversation } from '../../api/endpoints'
import { useMesEnfants } from '../../hooks/useMesEnfants'

interface Props {
  /** Élève concerné, quand on le connaît déjà (enseignant, direction). Sinon, le parent choisit parmi ses enfants. */
  eleve?: { id: number; libelle: string }
  onFermer: () => void
  onCree: (conversationId: number) => void
}

interface Valeurs {
  eleveId: string | null
  destinataireId: string | null
  sujet: string
  contenu: string
}

export default function NouveauMessageModal({ eleve, onFermer, onCree }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)
  const enfants = useMesEnfants()

  const formulaire = useForm<Valeurs>({
    initialValues: { eleveId: eleve ? String(eleve.id) : null, destinataireId: null, sujet: '', contenu: '' },
    validate: {
      eleveId: (v) => (v === null ? 'Élève requis' : null),
      destinataireId: (v) => (v === null ? 'Destinataire requis' : null),
      sujet: (v) => (v.trim() === '' ? 'Sujet requis' : null),
      contenu: (v) => (v.trim() === '' ? 'Message requis' : null),
    },
  })

  const eleveId = formulaire.values.eleveId === null ? null : Number(formulaire.values.eleveId)
  const contacts = useQuery({
    queryKey: ['messagerie', 'contacts', eleveId],
    queryFn: () => contactsMessagerie(eleveId as number),
    enabled: eleveId !== null,
    retry: false,
  })

  const envoi = useMutation({
    mutationFn: (v: Valeurs) =>
      demarrerConversation({
        eleveId: Number(v.eleveId),
        destinataireId: Number(v.destinataireId),
        sujet: v.sujet.trim(),
        contenu: v.contenu.trim(),
      }),
    onSuccess: (c) => {
      void queryClient.invalidateQueries({ queryKey: ['messagerie'] })
      onCree(c.id)
    },
    onError: (e) => setErreur(e.message),
  })

  const optionsEnfants = (enfants.data ?? []).map((e) => ({
    value: String(e.eleveId),
    label: `${e.prenom} ${e.nom}`,
  }))

  return (
    <Modal opened onClose={onFermer} title="Nouveau message">
      <form
        onSubmit={formulaire.onSubmit((valeurs) => {
          setErreur(null)
          envoi.mutate(valeurs)
        })}
      >
        <Stack>
          {eleve ? (
            <Text size="sm">
              À propos de <b>{eleve.libelle}</b>
            </Text>
          ) : (
            <Select
              label="À propos de"
              placeholder="Choisir un enfant"
              data={optionsEnfants}
              allowDeselect={false}
              {...formulaire.getInputProps('eleveId')}
              onChange={(v) => {
                formulaire.setFieldValue('eleveId', v)
                formulaire.setFieldValue('destinataireId', null)
              }}
            />
          )}

          {contacts.isFetching && <Loader size="sm" />}
          {contacts.isError && (
            <Alert color="orange" variant="light">
              {contacts.error.message}
            </Alert>
          )}
          {contacts.data && contacts.data.length === 0 && (
            <Alert color="blue" variant="light">
              Personne à contacter pour le moment.
            </Alert>
          )}
          {contacts.data && contacts.data.length > 0 && (
            <Select
              label="Destinataire"
              placeholder="Choisir"
              data={contacts.data.map((c) => ({ value: String(c.utilisateurId), label: `${c.nom} — ${c.role}` }))}
              allowDeselect={false}
              {...formulaire.getInputProps('destinataireId')}
            />
          )}

          <TextInput label="Sujet" maxLength={200} {...formulaire.getInputProps('sujet')} />
          <Textarea
            label="Message"
            autosize
            minRows={4}
            maxRows={10}
            maxLength={4000}
            {...formulaire.getInputProps('contenu')}
          />
          {erreur !== null && (
            <Alert color="red" variant="light">
              {erreur}
            </Alert>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onFermer}>
              Annuler
            </Button>
            <Button type="submit" loading={envoi.isPending}>
              Envoyer
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
