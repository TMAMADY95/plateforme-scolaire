import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, Modal, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  genererPeriodes,
  modifierPeriodeEtablissement,
  periodesEtablissement,
  retablirPeriodes,
  supprimerPeriodeEtablissement,
} from '../api/endpoints'
import type { Periode } from '../api/types'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { formaterDate } from '../utils/dates'

const LIBELLES_TYPE: Record<string, string> = { TRIMESTRE: 'Trimestres', SEMESTRE: 'Semestres', AUTRE: 'Périodes personnalisées' }

/** Découpage de l'année de l'établissement : trimestres ou semestres, pour toutes ses classes (examens compris). */
export default function PeriodesEtablissement({ etablissementId }: { etablissementId: number }) {
  const queryClient = useQueryClient()
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const [aModifier, setAModifier] = useState<Periode | null>(null)

  const periodes = useQuery({
    queryKey: ['periodes', 'etablissement', etablissementId, anneeId],
    queryFn: () => periodesEtablissement(etablissementId, anneeId as number),
    enabled: anneeId !== null,
  })
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['periodes'] })
  const erreur = (titre: string) => (e: Error) => notifications.show({ color: 'red', title: titre, message: e.message })

  const generation = useMutation({
    mutationFn: (type: 'TRIMESTRE' | 'SEMESTRE') => genererPeriodes(etablissementId, anneeId as number, type),
    onSuccess: rafraichir,
    onError: erreur('Génération impossible'),
  })
  const retablissement = useMutation({
    mutationFn: () => retablirPeriodes(etablissementId, anneeId as number),
    onSuccess: rafraichir,
    onError: erreur('Rétablissement impossible'),
  })
  const suppression = useMutation({ mutationFn: supprimerPeriodeEtablissement, onSuccess: rafraichir, onError: erreur('Suppression impossible') })

  const propres = (periodes.data ?? []).some((p) => p.etablissementId !== null)

  return (
    <Card withBorder padding="lg">
      <Stack>
        <Group justify="space-between" align="flex-end">
          <div>
            <Text fw={600}>Périodes d'évaluation</Text>
            <Text size="sm" c="dimmed">
              Choisissez le découpage de votre année. Il s'applique à toutes vos classes, y compris celles qui préparent un examen.
            </Text>
          </div>
          <Select aria-label="Année scolaire" data={annee.options} value={annee.anneeId} onChange={annee.setAnneeChoisie} allowDeselect={false} w={200} />
        </Group>

        {periodes.isPending && anneeId !== null && <Loader />}
        {periodes.isError && <Alert color="red" variant="light">{periodes.error.message}</Alert>}
        {periodes.data && (
          <>
            <Group gap="xs">
              <Badge variant="light" color={propres ? 'green' : 'gray'}>
                {propres ? `Propres à l'établissement : ${LIBELLES_TYPE[periodes.data[0]?.typePeriode ?? 'AUTRE'].toLowerCase()}` : 'Périodes du ministère'}
              </Badge>
            </Group>
            {periodes.data.length === 0 && <Text c="dimmed">Aucune période définie pour cette année.</Text>}
            {periodes.data.length > 0 && (
              <Table verticalSpacing="xs">
                <Table.Tbody>
                  {periodes.data.map((p) => (
                    <Table.Tr key={p.id}>
                      <Table.Td fw={600}>{p.libelle}</Table.Td>
                      <Table.Td>du {formaterDate(p.dateDebut)} au {formaterDate(p.dateFin)}</Table.Td>
                      <Table.Td>
                        {p.etablissementId !== null && (
                          <Group gap="xs" wrap="nowrap">
                            <Button size="compact-xs" variant="light" onClick={() => setAModifier(p)}>Modifier</Button>
                            <Button size="compact-xs" variant="subtle" color="red" onClick={() => suppression.mutate(p.id)}>Supprimer</Button>
                          </Group>
                        )}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            )}
            <Group>
              {!propres && (
                <>
                  <Button variant="light" loading={generation.isPending} onClick={() => generation.mutate('TRIMESTRE')}>Passer en 3 trimestres</Button>
                  <Button variant="light" loading={generation.isPending} onClick={() => generation.mutate('SEMESTRE')}>Passer en 2 semestres</Button>
                </>
              )}
              {propres && (
                <Button variant="default" loading={retablissement.isPending} onClick={() => retablissement.mutate()}>
                  Revenir aux périodes du ministère
                </Button>
              )}
            </Group>
          </>
        )}
      </Stack>
      {aModifier && <ModifierPeriodeModal periode={aModifier} onFermer={() => setAModifier(null)} onFait={rafraichir} />}
    </Card>
  )
}

function ModifierPeriodeModal({ periode, onFermer, onFait }: { periode: Periode; onFermer: () => void; onFait: () => void }) {
  const [libelle, setLibelle] = useState(periode.libelle)
  const [debut, setDebut] = useState(periode.dateDebut)
  const [fin, setFin] = useState(periode.dateFin)
  const [message, setMessage] = useState<string | null>(null)
  const modification = useMutation({
    mutationFn: () => modifierPeriodeEtablissement(periode.id, { libelle: libelle.trim(), ordre: periode.ordre, dateDebut: debut, dateFin: fin, typePeriode: periode.typePeriode }),
    onSuccess: () => {
      onFait()
      onFermer()
    },
    onError: (e) => setMessage(e.message),
  })
  return (
    <Modal opened onClose={onFermer} title="Modifier la période">
      <Stack>
        {message && <Alert color="red" variant="light">{message}</Alert>}
        <TextInput label="Nom" maxLength={50} value={libelle} onChange={(e) => setLibelle(e.currentTarget.value)} />
        <Group grow>
          <TextInput label="Début" type="date" value={debut} onChange={(e) => setDebut(e.currentTarget.value)} />
          <TextInput label="Fin" type="date" value={fin} onChange={(e) => setFin(e.currentTarget.value)} />
        </Group>
        <Group justify="flex-end">
          <Button disabled={libelle.trim() === '' || debut === '' || fin === ''} loading={modification.isPending} onClick={() => modification.mutate()}>Enregistrer</Button>
        </Group>
      </Stack>
    </Modal>
  )
}
