import { useState } from 'react'
import { Alert, Button, Card, Group, Loader, Select, Stack, Text, TextInput, Textarea } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ajouterCahierTexte, cahierTexteClasse, supprimerCahierTexte } from '../../api/endpoints'
import { aujourdhui, formaterDate } from '../../utils/dates'

/** Cahier de texte de la classe : ce qui a été enseigné, séance par séance. */
export default function OngletCahierTexte({ classeId, mesMatieres }: { classeId: number; mesMatieres: { id: number; libelle: string }[] }) {
  const queryClient = useQueryClient()
  const entrees = useQuery({ queryKey: ['cahier-texte', classeId], queryFn: () => cahierTexteClasse(classeId) })
  const [matiere, setMatiere] = useState<string | null>(mesMatieres.length === 1 ? String(mesMatieres[0].id) : null)
  const [date, setDate] = useState(aujourdhui())
  const [contenu, setContenu] = useState('')
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['cahier-texte'] })

  const ajout = useMutation({
    mutationFn: () => ajouterCahierTexte(classeId, { matiereId: Number(matiere), dateSeance: date, contenu: contenu.trim() }),
    onSuccess: () => {
      rafraichir()
      setContenu('')
      notifications.show({ color: 'green', title: 'Séance enregistrée', message: 'Les familles peuvent la consulter.' })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })
  const suppression = useMutation({
    mutationFn: supprimerCahierTexte,
    onSuccess: rafraichir,
    onError: (e) => notifications.show({ color: 'red', title: 'Suppression impossible', message: e.message }),
  })

  return (
    <Stack>
      <Card withBorder padding="md">
        <Stack gap="xs">
          <Group align="flex-end">
            <Select label="Matière" data={mesMatieres.map((m) => ({ value: String(m.id), label: m.libelle }))} value={matiere} onChange={setMatiere} />
            <TextInput label="Date de la séance" type="date" max={aujourdhui()} value={date} onChange={(e) => setDate(e.currentTarget.value)} />
          </Group>
          <Textarea label="Contenu de la séance" placeholder="Leçon traitée, exercices faits, notions à revoir…" autosize minRows={3} maxLength={4000} value={contenu} onChange={(e) => setContenu(e.currentTarget.value)} />
          <Group justify="flex-end">
            <Button disabled={matiere === null || contenu.trim() === '' || date === ''} loading={ajout.isPending} onClick={() => ajout.mutate()}>
              Enregistrer la séance
            </Button>
          </Group>
        </Stack>
      </Card>
      {entrees.isPending && <Loader />}
      {entrees.isError && <Alert color="red" variant="light">{entrees.error.message}</Alert>}
      {entrees.data && entrees.data.length === 0 && <Text c="dimmed">Aucune séance notée sur les 30 derniers jours.</Text>}
      {entrees.data?.map((e) => (
        <Card key={e.id} withBorder padding="sm">
          <Group justify="space-between" align="flex-start" wrap="nowrap">
            <div>
              <Text fw={600} size="sm">{e.matiere} — {formaterDate(e.dateSeance)}</Text>
              <Text size="xs" c="dimmed">{e.enseignant}</Text>
              <Text size="sm" mt={4} style={{ whiteSpace: 'pre-wrap' }}>{e.contenu}</Text>
            </div>
            <Button size="compact-xs" variant="subtle" color="red" onClick={() => suppression.mutate(e.id)}>Supprimer</Button>
          </Group>
        </Card>
      ))}
    </Stack>
  )
}
