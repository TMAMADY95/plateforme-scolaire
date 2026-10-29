import { useState } from 'react'
import { Alert, Card, Group, Loader, Select, SimpleGrid, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerPeriodes } from '../api/endpoints'
import type { GroupeReussite, ReussiteReseau } from '../api/types'
import { Barres } from './graphiques/Graphiques'

const AFFICHES = 15

interface Props {
  /** Clé de cache distincte selon le périmètre (national, régional). */
  cle: string
  anneeScolaireId: number
  charger: (anneeScolaireId: number, periodeId: number) => Promise<ReussiteReseau>
}

function pourcentage(taux: number | null): string {
  return taux === null ? '—' : `${String(taux).replace('.', ',')} %`
}

function points(groupes: GroupeReussite[]) {
  return groupes.filter((g) => g.taux !== null).map((g) => ({ libelle: g.libelle, valeur: g.taux as number }))
}

/** Taux de réussite d'un réseau d'établissements : par niveau, par zone et par établissement. */
export default function CarteReussiteReseau({ cle, anneeScolaireId, charger }: Props) {
  const [periodeChoisie, setPeriodeChoisie] = useState<string | null>(null)
  const periodes = useQuery({ queryKey: ['periodes', anneeScolaireId], queryFn: () => listerPeriodes(anneeScolaireId) })
  const periodeId = periodeChoisie ?? (periodes.data && periodes.data.length > 0 ? String(periodes.data[0].id) : null)

  const reussite = useQuery({
    queryKey: [cle, anneeScolaireId, periodeId],
    queryFn: () => charger(anneeScolaireId, Number(periodeId)),
    enabled: periodeId !== null,
  })

  return (
    <Card withBorder padding="lg">
      <Group justify="space-between" align="flex-end" mb="sm">
        <div>
          <Text fw={600}>Taux de réussite</Text>
          <Text size="xs" c="dimmed">
            Part des élèves notés dont la moyenne générale atteint 10 sur 20
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
              : `Taux global : ${pourcentage(reussite.data.taux)} (${new Intl.NumberFormat('fr-FR').format(reussite.data.admis)} sur ${new Intl.NumberFormat('fr-FR').format(reussite.data.evalues)} élèves notés)`}
          </Text>
          {reussite.data.taux !== null && (
            <>
              <SimpleGrid cols={{ base: 1, md: 2 }}>
                <div>
                  <Text size="sm" fw={500} mb={4}>
                    Par niveau
                  </Text>
                  <Barres points={points(reussite.data.parNiveau)} suffixe=" %" max={100} />
                </div>
                <div>
                  <Text size="sm" fw={500} mb={4}>
                    Par zone
                  </Text>
                  <Barres points={points(reussite.data.parZone)} suffixe=" %" max={100} />
                </div>
              </SimpleGrid>

              <Text size="sm" fw={500}>
                Par établissement
              </Text>
              <Table.ScrollContainer minWidth={420}>
                <Table striped>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Établissement</Table.Th>
                      <Table.Th>Élèves notés</Table.Th>
                      <Table.Th>Ayant réussi</Table.Th>
                      <Table.Th>Taux</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {reussite.data.etablissements.slice(0, AFFICHES).map((e) => (
                      <Table.Tr key={e.libelle}>
                        <Table.Td>{e.libelle}</Table.Td>
                        <Table.Td>{e.evalues}</Table.Td>
                        <Table.Td>{e.admis}</Table.Td>
                        <Table.Td>{pourcentage(e.taux)}</Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
              {reussite.data.etablissements.length > AFFICHES && (
                <Text size="xs" c="dimmed">
                  {AFFICHES} meilleurs taux affichés sur {reussite.data.etablissements.length} établissements.
                </Text>
              )}
            </>
          )}
        </Stack>
      )}
    </Card>
  )
}
