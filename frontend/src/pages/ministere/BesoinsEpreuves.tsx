import { Alert, Badge, Group, Loader, NumberInput, Stack, Table, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { besoinsEpreuves, definirBesoinEpreuve } from '../../api/endpoints'
import { useDroitsExamens } from '../../auth/useDroitsExamens'

const jour = (d: string | null) => (d === null ? '—' : new Date(`${d}T00:00:00`).toLocaleDateString('fr-FR', { weekday: 'short', day: '2-digit', month: '2-digit' }))

/** Besoins en personnel par épreuve ; le pic sur l'examen est comparé au personnel disponible. */
export default function BesoinsEpreuves({ examenId }: { examenId: number }) {
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const suivi = useQuery({ queryKey: ['besoins-epreuves', examenId], queryFn: () => besoinsEpreuves(examenId) })
  const definir = useMutation({
    mutationFn: (v: { epreuveId: number; fonction: string; requis: number }) => definirBesoinEpreuve(examenId, v.epreuveId, { fonction: v.fonction, requis: v.requis }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['besoins-epreuves'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const d = suivi.data

  return (
    <Stack>
      <div>
        <Title order={4}>Besoins en personnel par épreuve</Title>
        <Text c="dimmed" size="sm">Le personnel est mobilisé pour tout l'examen : le besoin à couvrir est le plus grand besoin déclaré sur une épreuve (pic).</Text>
      </div>
      {suivi.isLoading && <Loader />}
      {suivi.isError && <Alert color="red">Besoins indisponibles.</Alert>}
      {d && (
        <>
          <Group>
            {d.fonctions.map((f) => (
              <Badge key={f.fonction} color={f.pic === null ? 'gray' : f.manque ? 'red' : 'green'} variant="light" size="lg">
                {f.libelle} : {f.pic === null ? 'non déclaré' : `pic ${f.pic}, ${f.disponibles} dispo${f.manque ? `, manque ${f.manque}` : ''}`}
              </Badge>
            ))}
          </Group>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Épreuve</Table.Th>
                {d.fonctions.map((f) => <Table.Th key={f.fonction}>{f.libelle}</Table.Th>)}
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {d.epreuves.map((e) => (
                <Table.Tr key={e.epreuveId}>
                  <Table.Td>{e.discipline}<Text size="xs" c="dimmed">{jour(e.date)} {e.heureDebut?.slice(0, 5) ?? ''}</Text></Table.Td>
                  {d.fonctions.map((f) => (
                    <Table.Td key={f.fonction} w={110}>
                      {dgecs ? (
                        <NumberInput size="xs" min={0} aria-label={`${f.libelle} pour ${e.discipline}`} defaultValue={e.requis[f.fonction] ?? ''}
                          onBlur={(ev) => {
                            const v = ev.currentTarget.value.trim()
                            if (v !== '' && Number(v) !== e.requis[f.fonction]) definir.mutate({ epreuveId: e.epreuveId, fonction: f.fonction, requis: Number(v) })
                          }} />
                      ) : (
                        e.requis[f.fonction] ?? '—'
                      )}
                    </Table.Td>
                  ))}
                </Table.Tr>
              ))}
              {d.epreuves.length === 0 && <Table.Tr><Table.Td colSpan={d.fonctions.length + 1}><Text c="dimmed" ta="center">Aucune épreuve programmée.</Text></Table.Td></Table.Tr>}
            </Table.Tbody>
          </Table>
        </>
      )}
    </Stack>
  )
}
