import { useState } from 'react'
import { Alert, Badge, Button, Group, Modal, PasswordInput, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { chefsCentre, designerChefCentre, retirerChefCentre } from '../api/endpoints'
import type { ChefCentre } from '../api/types'

/** Comptes des chefs de centre : chacun n'accède qu'aux centres qui lui sont confiés (personnel, incidents, rapports). */
export default function ChefsCentreModal({ examenId, centre, onFermer }: { examenId: number; centre: { id: number; nom: string }; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const cle = ['chefs-centre', examenId, centre.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => chefsCentre(examenId, centre.id) })
  const [valeurs, setValeurs] = useState({ prenom: '', nom: '', email: '', telephone: '', motDePasseInitial: '' })
  const [erreur, setErreur] = useState<string | null>(null)
  const maj = (champ: keyof typeof valeurs) => (e: React.ChangeEvent<HTMLInputElement>) => setValeurs({ ...valeurs, [champ]: e.currentTarget.value })
  const appliquer = (liste: ChefCentre[]) => {
    queryClient.setQueryData(cle, liste)
    setErreur(null)
  }
  const designation = useMutation({
    mutationFn: () =>
      designerChefCentre(examenId, centre.id, {
        prenom: valeurs.prenom.trim(),
        nom: valeurs.nom.trim(),
        email: valeurs.email.trim(),
        telephone: valeurs.telephone.trim() === '' ? undefined : valeurs.telephone.trim(),
        motDePasseInitial: valeurs.motDePasseInitial,
      }),
    onSuccess: (liste) => {
      appliquer(liste)
      notifications.show({ color: 'green', title: 'Chef de centre désigné', message: valeurs.email })
      setValeurs({ prenom: '', nom: '', email: '', telephone: '', motDePasseInitial: '' })
    },
    onError: (e) => setErreur(e.message),
  })
  const retrait = useMutation({
    mutationFn: (id: number) => retirerChefCentre(examenId, centre.id, id),
    onSuccess: appliquer,
    onError: (e) => setErreur(e.message),
  })
  const complet = valeurs.prenom.trim() !== '' && valeurs.nom.trim() !== '' && valeurs.email.trim() !== '' && valeurs.motDePasseInitial.length >= 10

  return (
    <Modal opened onClose={onFermer} size="lg" title={`Chefs de centre : ${centre.nom}`}>
      <Stack gap="sm">
        <Text size="sm" c="dimmed">
          Le chef de centre se connecte pour saisir le personnel, signaler les incidents et remplir les rapports de surveillance de son centre,
          et de lui seul. Si l'adresse e-mail est déjà celle d'un chef de centre, le compte est réutilisé.
        </Text>
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {donnees.data && donnees.data.length === 0 && <Text size="sm" c="dimmed">Aucun chef de centre désigné.</Text>}
        {donnees.data && donnees.data.length > 0 && (
          <Table verticalSpacing="xs">
            <Table.Tbody>
              {donnees.data.map((c) => (
                <Table.Tr key={c.utilisateurId}>
                  <Table.Td>
                    {c.prenom} {c.nom}
                    <Text size="xs" c="dimmed">{c.email}</Text>
                  </Table.Td>
                  <Table.Td>{!c.actif && <Badge color="gray" variant="light">Désactivé</Badge>}</Table.Td>
                  <Table.Td ta="right">
                    <Button size="compact-xs" color="red" variant="subtle" loading={retrait.isPending} onClick={() => retrait.mutate(c.utilisateurId)}>
                      Retirer l'accès
                    </Button>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
        <Text fw={600}>Désigner un chef de centre</Text>
        <Group grow align="flex-end">
          <TextInput label="Prénom" value={valeurs.prenom} onChange={maj('prenom')} />
          <TextInput label="Nom" value={valeurs.nom} onChange={maj('nom')} />
        </Group>
        <Group grow align="flex-end">
          <TextInput label="Adresse e-mail" value={valeurs.email} onChange={maj('email')} />
          <TextInput label="Téléphone" value={valeurs.telephone} onChange={maj('telephone')} />
        </Group>
        <PasswordInput
          label="Mot de passe initial"
          description="10 caractères minimum ; à changer à la première connexion (ignoré si le compte existe déjà)."
          value={valeurs.motDePasseInitial}
          onChange={maj('motDePasseInitial')}
        />
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>Fermer</Button>
          <Button disabled={!complet} loading={designation.isPending} onClick={() => designation.mutate()}>Désigner</Button>
        </Group>
      </Stack>
    </Modal>
  )
}
