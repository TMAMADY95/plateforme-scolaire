import type { ChangeEvent } from 'react'
import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, Modal, PasswordInput, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { activerCompte, creerCompteMinistere, listerComptesInstitutionnels, reinitialiserCompteInstitutionnel } from '../../api/endpoints'
import type { CompteInstitutionnel } from '../../api/types'
import EnteteSection from '../../components/EnteteSection'

/** Comptes du ministère et des directions régionales : créer un compte ministère, désactiver, réinitialiser. */
export default function ComptesInstitutionnels() {
  const queryClient = useQueryClient()
  const comptes = useQuery({ queryKey: ['comptes-institutionnels'], queryFn: listerComptesInstitutionnels })
  const [creation, setCreation] = useState(false)
  const [reinit, setReinit] = useState<CompteInstitutionnel | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const activation = useMutation({
    mutationFn: (c: { compte: CompteInstitutionnel; actif: boolean }) => activerCompte(c.compte.id, c.actif),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['comptes-institutionnels'] }),
    onError: (e) => setErreur(e.message),
  })

  return (
    <Stack mt="md" gap="lg">
      <EnteteSection
        titre="Comptes du ministère et des régions"
        description="Les comptes régionaux se créent depuis la zone concernée (onglet Référentiels). Un compte désactivé ne peut plus se connecter."
        action={<Button onClick={() => setCreation(true)}>Nouveau compte ministère</Button>}
      />
      {erreur && (
        <Alert color="red" variant="light" withCloseButton onClose={() => setErreur(null)}>
          {erreur}
        </Alert>
      )}
      {comptes.isPending && <Loader />}
      {comptes.isError && (
        <Alert color="red" variant="light">
          {comptes.error.message}
        </Alert>
      )}
      {comptes.data && (
        <Card withBorder padding="lg">
          <Table.ScrollContainer minWidth={700}>
            <Table verticalSpacing="xs" highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Nom</Table.Th>
                  <Table.Th>Rôle</Table.Th>
                  <Table.Th>Zone</Table.Th>
                  <Table.Th>État</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {comptes.data.map((c) => (
                  <Table.Tr key={`${c.id}-${c.role}-${c.zoneId}`}>
                    <Table.Td>
                      <Text fw={600}>
                        {c.prenom} {c.nom}
                      </Text>
                      <Text size="xs" c="dimmed">
                        {c.email}
                        {c.telephone ? ` · ${c.telephone}` : ''}
                      </Text>
                    </Table.Td>
                    <Table.Td>{c.role === 'MINISTERE' ? 'Ministère' : 'Direction régionale'}</Table.Td>
                    <Table.Td>{c.zone ?? '—'}</Table.Td>
                    <Table.Td>
                      <Badge variant="light" color={c.actif ? 'green' : 'gray'}>
                        {c.actif ? 'Actif' : 'Désactivé'}
                      </Badge>
                    </Table.Td>
                    <Table.Td>
                      <Group gap="xs" justify="flex-end">
                        <Button size="compact-xs" variant="light" onClick={() => setReinit(c)}>
                          Mot de passe
                        </Button>
                        <Button
                          size="compact-xs"
                          variant="light"
                          color={c.actif ? 'red' : 'green'}
                          loading={activation.isPending}
                          onClick={() => activation.mutate({ compte: c, actif: !c.actif })}
                        >
                          {c.actif ? 'Désactiver' : 'Réactiver'}
                        </Button>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Card>
      )}
      {creation && <NouveauCompte onFermer={() => setCreation(false)} />}
      {reinit && <Reinitialisation compte={reinit} onFermer={() => setReinit(null)} />}
    </Stack>
  )
}

function NouveauCompte({ onFermer }: { onFermer: () => void }) {
  const queryClient = useQueryClient()
  const [valeurs, setValeurs] = useState({ prenom: '', nom: '', email: '', telephone: '', motDePasseInitial: '' })
  const [erreur, setErreur] = useState<string | null>(null)
  const maj = (cle: keyof typeof valeurs) => (e: ChangeEvent<HTMLInputElement>) => setValeurs({ ...valeurs, [cle]: e.currentTarget.value })

  const creation = useMutation({
    mutationFn: () =>
      creerCompteMinistere({
        prenom: valeurs.prenom.trim(),
        nom: valeurs.nom.trim(),
        email: valeurs.email.trim(),
        telephone: valeurs.telephone.trim() === '' ? undefined : valeurs.telephone.trim(),
        motDePasseInitial: valeurs.motDePasseInitial,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['comptes-institutionnels'] })
      notifications.show({ color: 'green', title: 'Compte créé', message: valeurs.email })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })
  const complet = valeurs.prenom.trim() !== '' && valeurs.nom.trim() !== '' && valeurs.email.trim() !== '' && valeurs.motDePasseInitial.length >= 10

  return (
    <Modal opened onClose={onFermer} title="Nouveau compte ministère">
      <Stack>
        <TextInput label="Prénom" value={valeurs.prenom} onChange={maj('prenom')} />
        <TextInput label="Nom" value={valeurs.nom} onChange={maj('nom')} />
        <TextInput label="Adresse e-mail" value={valeurs.email} onChange={maj('email')} />
        <TextInput label="Téléphone" description="Pour recevoir les alertes par SMS." value={valeurs.telephone} onChange={maj('telephone')} />
        <PasswordInput label="Mot de passe initial" description="10 caractères minimum ; la personne devra le changer à sa première connexion." value={valeurs.motDePasseInitial} onChange={maj('motDePasseInitial')} />
        {erreur && (
          <Alert color="red" variant="light">
            {erreur}
          </Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>
            Annuler
          </Button>
          <Button onClick={() => creation.mutate()} loading={creation.isPending} disabled={!complet}>
            Créer
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

function Reinitialisation({ compte, onFermer }: { compte: CompteInstitutionnel; onFermer: () => void }) {
  const [mdp, setMdp] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const reinit = useMutation({
    mutationFn: () => reinitialiserCompteInstitutionnel(compte.id, mdp),
    onSuccess: () => {
      notifications.show({ color: 'green', title: 'Mot de passe réinitialisé', message: compte.email })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })
  return (
    <Modal opened onClose={onFermer} title={`Réinitialiser le mot de passe de ${compte.prenom} ${compte.nom}`}>
      <Stack>
        <PasswordInput label="Mot de passe temporaire" description="10 caractères minimum ; à transmettre par un canal sûr. La personne devra le changer." value={mdp} onChange={(e) => setMdp(e.currentTarget.value)} />
        {erreur && (
          <Alert color="red" variant="light">
            {erreur}
          </Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>
            Annuler
          </Button>
          <Button onClick={() => reinit.mutate()} loading={reinit.isPending} disabled={mdp.length < 10}>
            Réinitialiser
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
