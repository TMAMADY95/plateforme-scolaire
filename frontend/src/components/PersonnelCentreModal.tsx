import { useState } from 'react'
import { Alert, Badge, Button, Group, Modal, NumberInput, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ajouterPersonnelCentre, personnelCentre, retirerPersonnelCentre } from '../api/endpoints'
import type { FonctionCentre, PersonnelCentre } from '../api/types'

const FONCTIONS: { value: FonctionCentre; label: string }[] = [
  { value: 'PRESIDENT', label: 'Président de la commission de surveillance' },
  { value: 'CHEF_CENTRE', label: 'Chef de centre' },
  { value: 'SECRETAIRE', label: 'Secrétariat' },
  { value: 'SURVEILLANT', label: 'Surveillant (salle)' },
  { value: 'CHEF_SALLE', label: 'Chef de salle' },
  { value: 'SECURITE', label: 'Agent de sécurité' },
  { value: 'SANTE', label: 'Personnel de santé' },
]
const LIBELLE = Object.fromEntries(FONCTIONS.map((f) => [f.value, f.label]))

/** Personnel d'un centre d'examen et couverture des besoins : deux surveillants par salle, président, chef de centre, secrétariat. */
export default function PersonnelCentreModal({ examenId, centre, onFermer }: { examenId: number; centre: { id: number; nom: string }; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const cle = ['personnel-centre', examenId, centre.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => personnelCentre(examenId, centre.id) })
  const [role, setRole] = useState<FonctionCentre | null>('SURVEILLANT')
  const [nom, setNom] = useState('')
  const [telephone, setTelephone] = useState('')
  const [salle, setSalle] = useState<number | string>(1)
  const [erreur, setErreur] = useState<string | null>(null)
  const parSalle = role === 'SURVEILLANT' || role === 'CHEF_SALLE'

  const appliquer = (p: PersonnelCentre) => queryClient.setQueryData(cle, p)
  const ajout = useMutation({
    mutationFn: () =>
      ajouterPersonnelCentre(examenId, centre.id, {
        role: role as FonctionCentre,
        nom: nom.trim(),
        telephone: telephone.trim() === '' ? null : telephone.trim(),
        salle: parSalle ? Number(salle) : null,
      }),
    onSuccess: (p) => {
      appliquer(p)
      setNom('')
      setTelephone('')
      setErreur(null)
    },
    onError: (e) => setErreur(e.message),
  })
  const retrait = useMutation({
    mutationFn: (id: number) => retirerPersonnelCentre(examenId, centre.id, id),
    onSuccess: appliquer,
    onError: (e) => notifications.show({ color: 'red', title: 'Retrait impossible', message: e.message }),
  })
  const d = donnees.data

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Personnel du centre : ${centre.nom}`}>
      <Stack gap="sm">
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Group gap="xs">
              <Badge color={d.complet ? 'green' : 'orange'} variant="light">{d.complet ? 'Personnel complet' : 'Personnel incomplet'}</Badge>
              <Badge variant="light" color="gray">{d.salles} salle(s)</Badge>
            </Group>
            {d.salles === 0 && (
              <Alert color="orange" variant="light">
                Les salles ne sont pas encore définies : générez la répartition en salles du centre pour connaître le nombre de surveillants nécessaires.
              </Alert>
            )}
            <Table verticalSpacing={2}>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Besoin</Table.Th>
                  <Table.Th ta="right">Désignés</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {d.besoins.map((b) => (
                  <Table.Tr key={b.libelle}>
                    <Table.Td>{b.libelle}</Table.Td>
                    <Table.Td ta="right">
                      <Badge color={b.affectes >= b.requis ? 'green' : 'red'} variant="light">{b.affectes} / {b.requis}</Badge>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            {d.personnel.length > 0 && (
              <Table verticalSpacing={2}>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Fonction</Table.Th>
                    <Table.Th>Nom</Table.Th>
                    <Table.Th>Téléphone</Table.Th>
                    <Table.Th>Salle</Table.Th>
                    <Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {d.personnel.map((p) => (
                    <Table.Tr key={p.id}>
                      <Table.Td>{LIBELLE[p.role] ?? p.role}</Table.Td>
                      <Table.Td>{p.nom}</Table.Td>
                      <Table.Td>{p.telephone ?? '—'}</Table.Td>
                      <Table.Td>{p.salle ?? '—'}</Table.Td>
                      <Table.Td ta="right">
                        <Button size="compact-xs" color="red" variant="subtle" onClick={() => retrait.mutate(p.id)}>Retirer</Button>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            )}
            <Group align="flex-end">
              <Select label="Fonction" data={FONCTIONS} value={role} onChange={(v) => setRole(v as FonctionCentre | null)} allowDeselect={false} w={270} />
              <TextInput label="Nom" value={nom} onChange={(e) => setNom(e.currentTarget.value)} w={200} />
              <TextInput label="Téléphone" value={telephone} onChange={(e) => setTelephone(e.currentTarget.value)} w={150} />
              {parSalle && <NumberInput label="Salle" min={1} allowDecimal={false} value={salle} onChange={setSalle} w={80} />}
              <Button disabled={role === null || nom.trim() === ''} loading={ajout.isPending} onClick={() => ajout.mutate()}>Désigner</Button>
            </Group>
            {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
            <Text size="xs" c="dimmed">Les surveillants sont désignés par note de service de l'autorité éducative : saisissez ici les noms qu'elle a arrêtés.</Text>
          </>
        )}
      </Stack>
    </Modal>
  )
}
