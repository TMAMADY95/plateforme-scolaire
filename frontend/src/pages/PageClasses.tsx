import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, NumberInput, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { creerClasse, listerAnnees, listerClasses, listerNiveaux } from '../api/endpoints'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'

interface ValeursClasse {
  anneeScolaireId: string | null
  niveauId: string | null
  nom: string
  capacite: number | string
}

export default function PageClasses() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const queryClient = useQueryClient()
  const [modalOuverte, { open, close }] = useDisclosure()
  const [anneeChoisie, setAnneeChoisie] = useState<string | null>(null)

  const annees = useQuery({ queryKey: ['annees'], queryFn: listerAnnees })
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })

  // Par défaut : l'année la plus récente (le serveur les renvoie de la plus récente à la plus ancienne)
  const anneeId = anneeChoisie ?? (annees.data && annees.data.length > 0 ? String(annees.data[0].id) : null)

  const classes = useQuery({
    queryKey: ['classes', etablissementId, anneeId],
    queryFn: () => listerClasses(etablissementId as number, Number(anneeId)),
    enabled: etablissementId !== undefined && anneeId !== null,
  })

  const formulaire = useForm<ValeursClasse>({
    initialValues: { anneeScolaireId: null, niveauId: null, nom: '', capacite: '' },
    validate: {
      anneeScolaireId: (v) => (v === null ? 'Année requise' : null),
      niveauId: (v) => (v === null ? 'Niveau requis' : null),
      nom: (v) => (v.trim().length > 0 ? null : 'Nom requis'),
      capacite: (v) => (v === '' || Number(v) >= 1 ? null : 'La capacité doit être au moins 1'),
    },
  })

  const creation = useMutation({
    mutationFn: (valeurs: ValeursClasse) =>
      creerClasse(etablissementId as number, {
        anneeScolaireId: Number(valeurs.anneeScolaireId),
        niveauId: Number(valeurs.niveauId),
        nom: valeurs.nom.trim(),
        capacite: valeurs.capacite === '' ? null : Number(valeurs.capacite),
      }),
    onSuccess: (classe) => {
      void queryClient.invalidateQueries({ queryKey: ['classes', etablissementId] })
      notifications.show({ color: 'green', title: 'Classe créée', message: `La classe ${classe.nom} a été ajoutée.` })
      formulaire.reset()
      close()
    },
    onError: (erreur) => {
      notifications.show({ color: 'red', title: 'Création impossible', message: erreur.message })
    },
  })

  function ouvrirCreation() {
    formulaire.setValues({ anneeScolaireId: anneeId })
    open()
  }

  if (etablissementId === undefined) {
    return (
      <Alert color="orange" variant="light">
        Aucun établissement n'est associé à votre compte.
      </Alert>
    )
  }

  const optionsAnnees = (annees.data ?? []).map((a) => ({ value: String(a.id), label: a.libelle }))
  const optionsNiveaux = (niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Classes</Title>
        <Button onClick={ouvrirCreation} disabled={annees.isPending || niveaux.isPending}>
          Nouvelle classe
        </Button>
      </Group>

      <Select
        label="Année scolaire"
        data={optionsAnnees}
        value={anneeId}
        onChange={setAnneeChoisie}
        allowDeselect={false}
        maw={260}
      />

      {(annees.isError || niveaux.isError || classes.isError) && (
        <Alert color="red" variant="light">
          {(annees.error ?? niveaux.error ?? classes.error)?.message}
        </Alert>
      )}
      {classes.isPending && anneeId !== null && <Loader />}

      {classes.data && classes.data.length === 0 && (
        <Text c="dimmed">Aucune classe pour cette année scolaire. Créez la première avec « Nouvelle classe ».</Text>
      )}
      {classes.data && classes.data.length > 0 && (
        <Table striped highlightOnHover withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Nom</Table.Th>
              <Table.Th>Niveau</Table.Th>
              <Table.Th>Capacité</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {classes.data.map((classe) => (
              <Table.Tr key={classe.id}>
                <Table.Td>{classe.nom}</Table.Td>
                <Table.Td>{classe.niveauLibelle}</Table.Td>
                <Table.Td>{classe.capacite ?? '—'}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}

      <Modal opened={modalOuverte} onClose={close} title="Nouvelle classe">
        <form onSubmit={formulaire.onSubmit((valeurs) => creation.mutate(valeurs))}>
          <Stack>
            <Select
              label="Année scolaire"
              data={optionsAnnees}
              allowDeselect={false}
              {...formulaire.getInputProps('anneeScolaireId')}
            />
            <Select label="Niveau" data={optionsNiveaux} {...formulaire.getInputProps('niveauId')} />
            <TextInput label="Nom de la classe" placeholder="A" {...formulaire.getInputProps('nom')} />
            <NumberInput
              label="Capacité"
              description="Laisser vide si illimitée"
              min={1}
              {...formulaire.getInputProps('capacite')}
            />
            <Group justify="flex-end">
              <Button variant="default" onClick={close}>
                Annuler
              </Button>
              <Button type="submit" loading={creation.isPending}>
                Créer
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>
    </Stack>
  )
}
