import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Modal, NumberInput, Progress, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ajouterMembreCommission,
  cloturerCommission,
  correctionExamen,
  genererSousCommissions,
  modifierCommission,
  retirerMembreCommission,
  supprimerCommission,
} from '../api/endpoints'
import type { CorrectionExamen, Examen } from '../api/types'

type Commission = CorrectionExamen['commissions'][number]

const ROLES = [
  { value: 'PRESIDENT', label: 'Président' },
  { value: 'SECRETAIRE', label: 'Secrétaire' },
  { value: 'CORRECTEUR', label: 'Correcteur' },
]
const LIBELLE_ROLE = Object.fromEntries(ROLES.map((r) => [r.value, r.label]))

/** Commissions de correction d'un examen : commission générale et sous-commissions par discipline, avancement de la correction. */
export default function CorrectionExamenModal({ examen, onFermer }: { examen: Examen; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const cle = ['correction-examen', examen.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => correctionExamen(examen.id) })
  const appliquer = (d: CorrectionExamen) => queryClient.setQueryData(cle, d)
  const erreur = (titre: string) => (e: Error) => notifications.show({ color: 'red', title: titre, message: e.message })
  const generation = useMutation({ mutationFn: () => genererSousCommissions(examen.id), onSuccess: appliquer, onError: erreur('Création impossible') })
  const d = donnees.data
  const figee = d?.statutResultats === 'PROCLAME'

  return (
    <Modal opened onClose={onFermer} size="xl" title={`Commissions de correction : ${examen.libelle}`}>
      <Stack gap="sm">
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && (
          <>
            <Text size="sm" c="dimmed">
              {d.niveau}, organisée en sous-commissions par discipline. Chaque commission a un président, un secrétaire et au moins deux correcteurs.
            </Text>
            {d.nbCopies > 0 && (
              <div>
                <Text size="sm">Correction : {d.nbCopiesCorrigees} copies corrigées sur {d.nbCopies}</Text>
                <Progress value={(100 * d.nbCopiesCorrigees) / d.nbCopies} />
              </div>
            )}
            {figee && <Alert color="gray" variant="light">Les résultats sont proclamés : la correction est close.</Alert>}
            {!figee && (
              <Group>
                <Button size="xs" variant="light" loading={generation.isPending} onClick={() => generation.mutate()}>
                  {d.commissions.length === 0 ? 'Constituer les commissions' : 'Ajouter les sous-commissions manquantes'}
                </Button>
                {d.epreuves.length === 0 && <Text size="xs" c="dimmed">Aucune épreuve au calendrier : seule la commission générale sera créée.</Text>}
              </Group>
            )}
            {d.commissions.map((c) => (
              <CarteCommission key={c.id} commission={c} examenId={examen.id} figee={figee} onChange={appliquer} onErreur={erreur} />
            ))}
          </>
        )}
      </Stack>
    </Modal>
  )
}

function CarteCommission({ commission: c, examenId, figee, onChange, onErreur }: {
  commission: Commission
  examenId: number
  figee: boolean
  onChange: (d: CorrectionExamen) => void
  onErreur: (titre: string) => (e: Error) => void
}) {
  const [lieu, setLieu] = useState(c.lieu ?? '')
  const [copies, setCopies] = useState<number | string>(c.nbCopies)
  const [corrigees, setCorrigees] = useState<number | string>(c.nbCopiesCorrigees)
  const [role, setRole] = useState<string | null>('CORRECTEUR')
  const [nom, setNom] = useState('')
  const [telephone, setTelephone] = useState('')
  const [erreurLocale, setErreurLocale] = useState<string | null>(null)
  const verrouillee = figee || c.cloturee

  const enregistrement = useMutation({
    mutationFn: () =>
      modifierCommission(examenId, c.id, {
        epreuveId: c.epreuveId,
        lieu: lieu.trim() === '' ? null : lieu.trim(),
        nbCopies: Number(copies) || 0,
        nbCopiesCorrigees: Number(corrigees) || 0,
      }),
    onSuccess: (d) => {
      onChange(d)
      setErreurLocale(null)
    },
    onError: (e) => setErreurLocale(e.message),
  })
  const ajout = useMutation({
    mutationFn: () => ajouterMembreCommission(examenId, c.id, { role: role as string, nom: nom.trim(), telephone: telephone.trim() === '' ? null : telephone.trim() }),
    onSuccess: (d) => {
      onChange(d)
      setNom('')
      setTelephone('')
      setErreurLocale(null)
    },
    onError: (e) => setErreurLocale(e.message),
  })
  const retrait = useMutation({ mutationFn: (id: number) => retirerMembreCommission(examenId, c.id, id), onSuccess: onChange, onError: onErreur('Retrait impossible') })
  const cloture = useMutation({ mutationFn: () => cloturerCommission(examenId, c.id), onSuccess: onChange, onError: (e) => setErreurLocale(e.message) })
  const suppression = useMutation({ mutationFn: () => supprimerCommission(examenId, c.id), onSuccess: onChange, onError: onErreur('Suppression impossible') })

  return (
    <Card withBorder padding="sm">
      <Stack gap="xs">
        <Group justify="space-between">
          <Group gap="xs">
            <Text fw={600}>{c.nom}</Text>
            {c.cloturee ? <Badge color="green" variant="light">Correction close</Badge>
              : c.complete ? <Badge color="blue" variant="light">Composée</Badge>
              : <Badge color="orange" variant="light">Incomplète : {c.manques.join(', ')}</Badge>}
          </Group>
          {!figee && (
            <Button size="compact-xs" color="red" variant="subtle" loading={suppression.isPending} onClick={() => suppression.mutate()}>
              Supprimer
            </Button>
          )}
        </Group>
        {c.membres.length > 0 && (
          <Table verticalSpacing={4}>
            <Table.Tbody>
              {c.membres.map((m) => (
                <Table.Tr key={m.id}>
                  <Table.Td w={110}>{LIBELLE_ROLE[m.role]}</Table.Td>
                  <Table.Td>{m.nom}</Table.Td>
                  <Table.Td>{m.telephone ?? ''}</Table.Td>
                  <Table.Td w={90}>
                    {!verrouillee && (
                      <Button size="compact-xs" color="red" variant="subtle" onClick={() => retrait.mutate(m.id)}>
                        Retirer
                      </Button>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
        {!verrouillee && (
          <Group align="flex-end" gap="xs">
            <Select size="xs" label="Fonction" data={ROLES} value={role} onChange={setRole} allowDeselect={false} w={130} />
            <TextInput size="xs" label="Nom" value={nom} onChange={(e) => setNom(e.currentTarget.value)} w={200} />
            <TextInput size="xs" label="Téléphone" value={telephone} onChange={(e) => setTelephone(e.currentTarget.value)} w={140} />
            <Button size="xs" variant="light" disabled={nom.trim() === '' || role === null} loading={ajout.isPending} onClick={() => ajout.mutate()}>
              Ajouter
            </Button>
          </Group>
        )}
        <Group align="flex-end" gap="xs">
          <TextInput size="xs" label="Lieu de correction" value={lieu} onChange={(e) => setLieu(e.currentTarget.value)} disabled={verrouillee} w={220} />
          <NumberInput size="xs" label="Copies reçues" min={0} allowDecimal={false} value={copies} onChange={setCopies} disabled={verrouillee} w={110} />
          <NumberInput size="xs" label="Copies corrigées" min={0} allowDecimal={false} value={corrigees} onChange={setCorrigees} disabled={verrouillee} w={120} />
          {!verrouillee && (
            <>
              <Button size="xs" variant="light" loading={enregistrement.isPending} onClick={() => enregistrement.mutate()}>
                Enregistrer
              </Button>
              <Button size="xs" color="green" variant="light" loading={cloture.isPending} onClick={() => cloture.mutate()}>
                Clore la correction
              </Button>
            </>
          )}
        </Group>
        {erreurLocale && <Alert color="red" variant="light">{erreurLocale}</Alert>}
      </Stack>
    </Card>
  )
}
