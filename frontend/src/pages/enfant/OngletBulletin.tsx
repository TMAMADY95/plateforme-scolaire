import { useState } from 'react'
import { Alert, Badge, Card, Group, Loader, Select, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerPeriodes, obtenirBulletin, telechargerBulletin } from '../../api/endpoints'
import BoutonTelechargement from '../../components/BoutonTelechargement'
import type { Enfant, Periode } from '../../api/types'
import { aujourdhui } from '../../utils/dates'
import { formaterEcart, formaterNote, formaterRang } from '../../utils/notes'

/** Période en cours si elle existe, sinon la dernière de l'année. */
function periodeParDefaut(periodes: Periode[]): string | null {
  const jour = aujourdhui()
  const choisie = periodes.find((p) => p.dateDebut <= jour && jour <= p.dateFin) ?? periodes[periodes.length - 1]
  return choisie ? String(choisie.id) : null
}

export default function OngletBulletin({ enfant }: { enfant: Enfant }) {
  const anneeId = enfant.anneeScolaireId
  const [choix, setChoix] = useState<string | null>(null)

  const periodes = useQuery({
    queryKey: ['periodes', anneeId],
    queryFn: () => listerPeriodes(anneeId as number),
    enabled: anneeId !== null,
  })

  const periodeId = choix ?? (periodes.data ? periodeParDefaut(periodes.data) : null)

  const bulletin = useQuery({
    queryKey: ['bulletin', enfant.eleveId, periodeId],
    queryFn: () => obtenirBulletin(enfant.eleveId, Number(periodeId)),
    enabled: periodeId !== null,
  })

  if (anneeId === null) {
    return (
      <Alert color="blue" variant="light">
        {enfant.prenom} n'a pas d'inscription active : aucun bulletin n'est disponible.
      </Alert>
    )
  }

  const options = (periodes.data ?? []).map((p) => ({ value: String(p.id), label: p.libelle }))

  return (
    <Stack>
      <Group align="flex-end">
        <Select
          label="Période"
          data={options}
          value={periodeId}
          onChange={setChoix}
          allowDeselect={false}
          w={240}
        />
        {bulletin.data && periodeId !== null && (
          <BoutonTelechargement telecharger={() => telechargerBulletin(enfant.eleveId, Number(periodeId))}>
            Télécharger en PDF
          </BoutonTelechargement>
        )}
      </Group>

      {(periodes.isPending || (bulletin.isPending && periodeId !== null)) && <Loader />}
      {periodes.isError && (
        <Alert color="red" variant="light">
          {periodes.error.message}
        </Alert>
      )}
      {periodes.data && periodes.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucune période n'est encore définie pour cette année scolaire.
        </Alert>
      )}
      {bulletin.isError && (
        <Alert color="red" variant="light">
          {bulletin.error.message}
        </Alert>
      )}

      {bulletin.data && (
        <>
          <Card withBorder padding="lg" maw={560}>
            <Group justify="space-between" align="flex-end">
              <div>
                <Text c="dimmed" size="sm">
                  Moyenne générale — {bulletin.data.periode}
                </Text>
                <Title order={1}>
                  {formaterNote(bulletin.data.moyenneGenerale)}
                  <Text span c="dimmed" size="lg">
                    {' '}
                    / 20
                  </Text>
                </Title>
              </div>
              <Stack gap={4} align="flex-end">
                {bulletin.data.rang !== null && (
                  <Badge size="lg" variant="light">
                    {formaterRang(bulletin.data.rang)} sur {bulletin.data.effectif}
                  </Badge>
                )}
                <Text size="sm" c="dimmed">
                  Moyenne de la classe : {formaterNote(bulletin.data.moyenneGeneraleClasse)}
                </Text>
              </Stack>
            </Group>
          </Card>

          {bulletin.data.matieres.length === 0 ? (
            <Text c="dimmed">Aucune note n'a encore été saisie pour cette période.</Text>
          ) : (
            <Table.ScrollContainer minWidth={480}>
              <Table striped withTableBorder maw={640}>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Matière</Table.Th>
                    <Table.Th>Coef.</Table.Th>
                    <Table.Th>Moyenne</Table.Th>
                    <Table.Th>Moyenne de la classe</Table.Th>
                    <Table.Th>Écart</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {bulletin.data.matieres.map((m) => (
                    <Table.Tr key={m.matiereId}>
                      <Table.Td>{m.matiere}</Table.Td>
                      <Table.Td>{m.coefficient}</Table.Td>
                      <Table.Td fw={600}>{formaterNote(m.moyenneEleve)}</Table.Td>
                      <Table.Td>{formaterNote(m.moyenneClasse)}</Table.Td>
                      <Table.Td
                        c={
                          m.moyenneEleve === null || m.moyenneClasse === null
                            ? undefined
                            : m.moyenneEleve >= m.moyenneClasse
                              ? 'green'
                              : 'red'
                        }
                      >
                        {formaterEcart(m.moyenneEleve, m.moyenneClasse)}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </>
      )}
    </Stack>
  )
}
