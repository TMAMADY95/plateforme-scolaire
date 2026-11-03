import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, Modal, PasswordInput, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { activerCompteLocal, comptesLocaux, creerDirectionLocale, listerZones, reinitialiserCompteLocal } from '../../api/endpoints'
import type { CompteInstitutionnel } from '../../api/types'
import EnteteSection from '../../components/EnteteSection'

/** L'IRE administre les comptes des directions préfectorales et communales (DPE/DCE) de sa région. */
export default function ComptesLocaux() {
  const queryClient = useQueryClient()
  const comptes = useQuery({ queryKey: ['comptes-locaux'], queryFn: comptesLocaux })
  const [creation, setCreation] = useState(false)
  const [reinit, setReinit] = useState<CompteInstitutionnel | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const activation = useMutation({
    mutationFn: (c: { compte: CompteInstitutionnel; actif: boolean }) => activerCompteLocal(c.compte.id, c.actif),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['comptes-locaux'] }),
    onError: (e) => setErreur(e.message),
  })

  return (
    <Stack mt="md" gap="lg">
      <EnteteSection
        titre="Directions préfectorales et communales"
        description="Les établissements relèvent des DPE/DCE, qui relèvent de l'IRE : vous créez et suivez les comptes de votre région."
        action={<Button onClick={() => setCreation(true)}>Nouveau compte DPE/DCE</Button>}
      />
      {erreur && <Alert color="red" variant="light" withCloseButton onClose={() => setErreur(null)}>{erreur}</Alert>}
      {comptes.isPending && <Loader />}
      {comptes.isError && <Alert color="red" variant="light">{comptes.error.message}</Alert>}
      {comptes.data && comptes.data.length === 0 && <Text c="dimmed">Aucun compte DPE/DCE dans votre région.</Text>}
      {comptes.data && comptes.data.length > 0 && (
        <Card withBorder padding="lg">
          <Table verticalSpacing="xs" highlightOnHover>
            <Table.Tbody>
              {comptes.data.map((c) => (
                <Table.Tr key={`${c.id}-${c.zoneId}`}>
                  <Table.Td>
                    <Text fw={600}>{c.prenom} {c.nom}</Text>
                    <Text size="xs" c="dimmed">{c.email}{c.telephone ? ` · ${c.telephone}` : ''}</Text>
                  </Table.Td>
                  <Table.Td>{c.zone ?? '—'}</Table.Td>
                  <Table.Td><Badge variant="light" color={c.actif ? 'green' : 'gray'}>{c.actif ? 'Actif' : 'Désactivé'}</Badge></Table.Td>
                  <Table.Td>
                    <Group gap="xs" justify="flex-end">
                      <Button size="compact-xs" variant="light" onClick={() => setReinit(c)}>Mot de passe</Button>
                      <Button size="compact-xs" variant="light" color={c.actif ? 'red' : 'green'} loading={activation.isPending} onClick={() => activation.mutate({ compte: c, actif: !c.actif })}>
                        {c.actif ? 'Désactiver' : 'Réactiver'}
                      </Button>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>
      )}
      {creation && <NouveauCompte onFermer={() => setCreation(false)} />}
      {reinit && <Reinitialisation compte={reinit} onFermer={() => setReinit(null)} />}
    </Stack>
  )
}

function NouveauCompte({ onFermer }: { onFermer: () => void }) {
  const queryClient = useQueryClient()
  const zones = useQuery({ queryKey: ['zones'], queryFn: listerZones })
  const [zone, setZone] = useState<string | null>(null)
  const [v, setV] = useState({ prenom: '', nom: '', email: '', telephone: '', motDePasseInitial: '' })
  const [erreur, setErreur] = useState<string | null>(null)
  const maj = (c: keyof typeof v) => (e: React.ChangeEvent<HTMLInputElement>) => setV({ ...v, [c]: e.currentTarget.value })
  const creation = useMutation({
    mutationFn: () => creerDirectionLocale(Number(zone), {
      prenom: v.prenom.trim(), nom: v.nom.trim(), email: v.email.trim(),
      telephone: v.telephone.trim() === '' ? undefined : v.telephone.trim(), motDePasseInitial: v.motDePasseInitial,
    }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['comptes-locaux'] })
      notifications.show({ color: 'green', title: 'Compte créé', message: v.email })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })
  const options = (zones.data ?? []).filter((z) => ['PREFECTURE', 'COMMUNE'].includes(z.typeZone))
    .map((z) => ({ value: String(z.id), label: `${z.nom} (${z.typeZone === 'COMMUNE' ? 'DCE' : 'DPE'})` }))
  const complet = zone !== null && v.prenom.trim() !== '' && v.nom.trim() !== '' && v.email.trim() !== '' && v.motDePasseInitial.length >= 10

  return (
    <Modal opened onClose={onFermer} title="Nouveau compte DPE/DCE">
      <Stack>
        <Select label="Préfecture ou commune" searchable data={options} value={zone} onChange={setZone} />
        <TextInput label="Prénom" value={v.prenom} onChange={maj('prenom')} />
        <TextInput label="Nom" value={v.nom} onChange={maj('nom')} />
        <TextInput label="Adresse e-mail" value={v.email} onChange={maj('email')} />
        <TextInput label="Téléphone" value={v.telephone} onChange={maj('telephone')} />
        <PasswordInput label="Mot de passe initial" description="10 caractères minimum ; à changer à la première connexion." value={v.motDePasseInitial} onChange={maj('motDePasseInitial')} />
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>Annuler</Button>
          <Button onClick={() => creation.mutate()} loading={creation.isPending} disabled={!complet}>Créer</Button>
        </Group>
      </Stack>
    </Modal>
  )
}

function Reinitialisation({ compte, onFermer }: { compte: CompteInstitutionnel; onFermer: () => void }) {
  const [mdp, setMdp] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const reinit = useMutation({
    mutationFn: () => reinitialiserCompteLocal(compte.id, mdp),
    onSuccess: () => {
      notifications.show({ color: 'green', title: 'Mot de passe réinitialisé', message: compte.email })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })
  return (
    <Modal opened onClose={onFermer} title={`Mot de passe de ${compte.prenom} ${compte.nom}`}>
      <Stack>
        <PasswordInput label="Mot de passe temporaire" description="10 caractères minimum ; à changer à la prochaine connexion." value={mdp} onChange={(e) => setMdp(e.currentTarget.value)} />
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>Annuler</Button>
          <Button onClick={() => reinit.mutate()} loading={reinit.isPending} disabled={mdp.length < 10}>Réinitialiser</Button>
        </Group>
      </Stack>
    </Modal>
  )
}
