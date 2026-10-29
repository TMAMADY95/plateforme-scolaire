import { useState } from 'react'
import { Alert, Card, Group, Loader, Select, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerPeriodes, reussiteEtablissement } from '../api/endpoints'
import { Barres } from './graphiques/Graphiques'

interface Props {
  etablissementId: number
  anneeScolaireId: number
}

/** Taux de réussite par classe pour une période : moyenne générale d'au moins 10 sur 20. */
export default function CarteReussite({ etablissementId, anneeScolaireId }: Props) {
  const [periodeChoisie, setPeriodeChoisie] = useState<string | null>(null)
  const periodes = useQuery({ queryKey: ['periodes', anneeScolaireId], queryFn: () => listerPeriodes(anneeScolaireId) })
  const periodeId = periodeChoisie ?? (periodes.data && periodes.data.length > 0 ? String(periodes.data[0].id) : null)

  const reussite = useQuery({
    queryKey: ['reussite', etablissementId, anneeScolaireId, periodeId],
    queryFn: () => reussiteEtablissement(etablissementId, anneeScolaireId, Number(periodeId)),
    enabled: periodeId !== null,
  })

  return (
    <Card withBorder padding="lg">
      <Group justify="space-between" align="flex-end" mb="sm">
        <div>
          <Text fw={600}>Taux de réussite par classe</Text>
          <Text size="xs" c="dimmed">
            Part des élèves dont la moyenne générale atteint 10 sur 20
          </Text>
        </div>
        <Select
          aria-label="Période"
          placeholder="Période"
          data={(periodes.data ?? []).map((p) => ({ value: String(p.id), label: p.libelle }))}
          value={periodeId}
          onChange={setPeriodeChoisie}
          allowDeselect={false}
          w={200}
        />
      </Group>

      {periodes.isSuccess && periodes.data.length === 0 && (
        <Text c="dimmed" size="sm">
          Aucune période n'est définie pour cette année scolaire.
        </Text>
      )}
      {(periodes.isError || reussite.isError) && (
        <Alert color="red" variant="light">
          {(periodes.error ?? reussite.error)?.message}
        </Alert>
      )}
      {reussite.isPending && periodeId !== null && <Loader />}

      {reussite.data && (
        <Stack>
          <Text size="sm">
            {reussite.data.taux === null
              ? 'Aucune note saisie pour cette période.'
              : `Établissement : ${String(reussite.data.taux).replace('.', ',')} % (${reussite.data.admis} sur ${reussite.data.evalues} élèves notés)`}
          </Text>
          <Barres
            points={reussite.data.classes.filter((c) => c.taux !== null).map((c) => ({ libelle: c.classe, valeur: c.taux as number }))}
            suffixe=" %"
            max={100}
            vide="Aucune classe n'a encore de notes pour cette période."
          />
          {reussite.data.classes.length > 0 && (
            <Table.ScrollContainer minWidth={420}>
              <Table striped>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Classe</Table.Th>
                    <Table.Th>Niveau</Table.Th>
                    <Table.Th>Élèves notés</Table.Th>
                    <Table.Th>Ayant réussi</Table.Th>
                    <Table.Th>Taux</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {reussite.data.classes.map((c) => (
                    <Table.Tr key={c.classeId}>
                      <Table.Td>{c.classe}</Table.Td>
                      <Table.Td>{c.niveau}</Table.Td>
                      <Table.Td>{c.evalues}</Table.Td>
                      <Table.Td>{c.admis}</Table.Td>
                      <Table.Td>{c.taux === null ? '—' : `${String(c.taux).replace('.', ',')} %`}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </Stack>
      )}
    </Card>
  )
}
