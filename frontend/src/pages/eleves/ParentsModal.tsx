import { useState } from 'react'
import { Alert, Button, Checkbox, Divider, Group, Loader, Modal, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { lierParent, listerParents } from '../../api/endpoints'
import type { Eleve } from '../../api/types'
import { genererMotDePasse } from '../../utils/motDePasse'

interface Props {
  eleve: Eleve
  onFermer: () => void
}

interface Valeurs {
  email: string
  nom: string
  prenom: string
  telephone: string
  relation: string | null
  responsableLegal: boolean
  motDePasseInitial: string
}

const RELATIONS = ['Mère', 'Père', 'Tuteur légal', 'Tutrice légale', 'Autre']

/** Liste des parents d'un élève et rattachement d'un nouveau parent (compte créé si besoin). */
export default function ParentsModal({ eleve, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState<string | null>(null)

  const parents = useQuery({ queryKey: ['parents', eleve.id], queryFn: () => listerParents(eleve.id) })

  const formulaire = useForm<Valeurs>({
    initialValues: {
      email: '',
      nom: '',
      prenom: '',
      telephone: '',
      relation: null,
      responsableLegal: true,
      motDePasseInitial: '',
    },
    validate: {
      email: (v) => (/^\S+@\S+$/.test(v.trim()) ? null : 'Adresse e-mail invalide'),
      nom: (v) => (v.trim() === '' ? 'Nom requis' : null),
      prenom: (v) => (v.trim() === '' ? 'Prénom requis' : null),
      relation: (v) => (v === null ? 'Lien de parenté requis' : null),
      motDePasseInitial: (v) => (v === '' || v.length >= 10 ? null : 'Au moins 10 caractères'),
    },
  })

  const rattachement = useMutation({
    mutationFn: (v: Valeurs) =>
      lierParent(eleve.id, {
        email: v.email.trim(),
        nom: v.nom.trim(),
        prenom: v.prenom.trim(),
        telephone: v.telephone.trim() || undefined,
        relation: v.relation as string,
        responsableLegal: v.responsableLegal,
        motDePasseInitial: v.motDePasseInitial || undefined,
      }),
    onSuccess: (lien) => {
      void queryClient.invalidateQueries({ queryKey: ['parents', eleve.id] })
      notifications.show({
        color: 'green',
        title: 'Parent rattaché',
        message: `${lien.prenom} ${lien.nom} peut désormais consulter le dossier de ${eleve.prenom}.`,
      })
      formulaire.reset()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Parents de ${eleve.prenom} ${eleve.nom}`}>
      <Stack>
        {parents.isPending && <Loader size="sm" />}
        {parents.isError && (
          <Alert color="red" variant="light">
            {parents.error.message}
          </Alert>
        )}
        {parents.data && parents.data.length === 0 && <Text c="dimmed">Aucun parent n'est encore rattaché.</Text>}
        {parents.data && parents.data.length > 0 && (
          <Table withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Nom</Table.Th>
                <Table.Th>Adresse e-mail</Table.Th>
                <Table.Th>Lien</Table.Th>
                <Table.Th>Responsable légal</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {parents.data.map((p) => (
                <Table.Tr key={p.id}>
                  <Table.Td>
                    {p.prenom} {p.nom}
                  </Table.Td>
                  <Table.Td>{p.email}</Table.Td>
                  <Table.Td>{p.relation}</Table.Td>
                  <Table.Td>{p.responsableLegal ? 'Oui' : 'Non'}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}

        <Divider my="xs" />
        <Title order={5}>Rattacher un parent</Title>
        <Text size="sm" c="dimmed">
          Si l'adresse e-mail correspond déjà à un compte parent, l'élève lui est simplement rattaché (nom, prénom et
          mot de passe sont alors ignorés). Sinon, un compte est créé : le mot de passe initial est obligatoire.
        </Text>

        <form
          onSubmit={formulaire.onSubmit((valeurs) => {
            setErreur(null)
            rattachement.mutate(valeurs)
          })}
        >
          <Stack>
            <TextInput label="Adresse e-mail" {...formulaire.getInputProps('email')} />
            <Group grow align="flex-start">
              <TextInput label="Prénom" {...formulaire.getInputProps('prenom')} />
              <TextInput label="Nom" {...formulaire.getInputProps('nom')} />
            </Group>
            <Group grow align="flex-start">
              <Select label="Lien de parenté" data={RELATIONS} {...formulaire.getInputProps('relation')} />
              <TextInput label="Téléphone (facultatif)" {...formulaire.getInputProps('telephone')} />
            </Group>
            <Checkbox
              label="Responsable légal"
              {...formulaire.getInputProps('responsableLegal', { type: 'checkbox' })}
            />
            <TextInput
              label="Mot de passe initial (nouveau compte uniquement)"
              description="À communiquer au parent : il devra le changer à sa première connexion"
              {...formulaire.getInputProps('motDePasseInitial')}
              rightSectionWidth={110}
              rightSection={
                <Button
                  size="compact-xs"
                  variant="light"
                  onClick={() => formulaire.setFieldValue('motDePasseInitial', genererMotDePasse())}
                >
                  Générer
                </Button>
              }
            />
            {erreur !== null && (
              <Alert color="red" variant="light">
                {erreur}
              </Alert>
            )}
            <Group justify="flex-end">
              <Button variant="default" onClick={onFermer}>
                Fermer
              </Button>
              <Button type="submit" loading={rattachement.isPending}>
                Rattacher
              </Button>
            </Group>
          </Stack>
        </form>
      </Stack>
    </Modal>
  )
}
