import { useState } from 'react'
import { Alert, Button, Group, Loader, Pagination, Popover, Stack, Table, Text, TextInput } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { rechercherAudit } from '../../api/endpoints'
import { formaterAdresse } from '../../utils/adresses'
import { lignesDetails } from '../../utils/detailsAudit'

const TAILLE = 25

/** Journal d'audit national : qui a fait quoi, sur qui, quand, de quelle adresse. */
export default function JournalAuditNational() {
  const [action, setAction] = useState('')
  const [entite, setEntite] = useState('')
  const [eleve, setEleve] = useState('')
  const [page, setPage] = useState(0)
  const [actionDiffere] = useDebouncedValue(action, 300)
  const [entiteDiffere] = useDebouncedValue(entite, 300)
  const [eleveDiffere] = useDebouncedValue(eleve, 300)

  const journal = useQuery({
    queryKey: ['audit', actionDiffere.trim(), entiteDiffere.trim(), eleveDiffere.trim(), page],
    queryFn: () => rechercherAudit({
        action: actionDiffere.trim(),
        entite: entiteDiffere.trim(),
        eleve: eleveDiffere.trim(),
        page,
        taille: TAILLE,
      }),
    placeholderData: keepPreviousData,
  })

  const nombrePages = journal.data ? Math.max(1, Math.ceil(journal.data.total / journal.data.taille)) : 1

  return (
    <Stack mt="md">
      <Group align="flex-end">
        <TextInput
          label="Action"
          placeholder="CREATION, SUSPENSION…"
          value={action}
          onChange={(e) => {
            setAction(e.currentTarget.value)
            setPage(0)
          }}
        />
        <TextInput
          label="Entité"
          placeholder="utilisateur, facture…"
          value={entite}
          onChange={(e) => {
            setEntite(e.currentTarget.value)
            setPage(0)
          }}
        />
        <TextInput
          label="Élève"
          placeholder="Nom, prénom ou matricule"
          description="Inscriptions, factures, paiements, présences et notes"
          value={eleve}
          onChange={(e) => {
            setEleve(e.currentTarget.value)
            setPage(0)
          }}
        />
      </Group>

      {journal.isPending && <Loader />}
      {journal.isError && (
        <Alert color="red" variant="light">
          {journal.error.message}
        </Alert>
      )}
      {journal.data && journal.data.contenu.length === 0 && <Text c="dimmed">Aucune entrée.</Text>}
      {journal.data && journal.data.contenu.length > 0 && (
        <>
          <Text size="sm" c="dimmed">
            {journal.data.total} entrée(s), de la plus récente à la plus ancienne.
          </Text>
          <Table.ScrollContainer minWidth={900}>
            <Table striped withTableBorder verticalSpacing="xs">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Auteur</Table.Th>
                  <Table.Th>Action</Table.Th>
                  <Table.Th>Concerne</Table.Th>
                  <Table.Th>Adresse</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {journal.data.contenu.map((j) => (
                  <Table.Tr key={j.id}>
                    <Table.Td>{new Date(j.date).toLocaleString('fr-FR')}</Table.Td>
                    <Table.Td>
                      <Text size="sm">{j.utilisateurNom ?? 'Système'}</Text>
                      {j.utilisateurEmail && (
                        <Text size="xs" c="dimmed">
                          {j.utilisateurEmail}
                        </Text>
                      )}
                    </Table.Td>
                    <Table.Td>
                      <Text size="sm" fw={500}>
                        {j.resume}
                      </Text>
                      <Text size="xs" c="dimmed">
                        {j.action}
                      </Text>
                    </Table.Td>
                    <Table.Td>
                      {j.contexte.length === 0 ? (
                        <Text size="sm" c="dimmed">
                          {j.entite}
                          {j.entiteId !== null ? ` #${j.entiteId}` : ''}
                        </Text>
                      ) : (
                        j.contexte.map((c) => (
                          <Text key={`${c.libelle}-${c.valeur}`} size="sm">
                            <Text span c="dimmed" size="xs">
                              {c.libelle} :{' '}
                            </Text>
                            {c.valeur}
                          </Text>
                        ))
                      )}
                    </Table.Td>
                    <Table.Td>{formaterAdresse(j.adresseIp)}</Table.Td>
                    <Table.Td>
                      {(() => {
                        const lignes = lignesDetails(j.action, j.details)
                        if (lignes.length === 0) return null
                        return (
                          <Popover width={320} position="left" withArrow shadow="md">
                            <Popover.Target>
                              <Button size="compact-xs" variant="subtle">
                                Détails
                              </Button>
                            </Popover.Target>
                            <Popover.Dropdown>
                              <Stack gap={6}>
                                {lignes.map((l) => (
                                  <Group key={l.libelle} justify="space-between" wrap="nowrap" align="flex-start">
                                    <Text size="sm" c="dimmed">
                                      {l.libelle}
                                    </Text>
                                    <Text size="sm" fw={500} ta="right" style={{ wordBreak: 'break-word' }}>
                                      {l.valeur}
                                    </Text>
                                  </Group>
                                ))}
                              </Stack>
                            </Popover.Dropdown>
                          </Popover>
                        )
                      })()}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
          <Group justify="center">
            <Pagination total={nombrePages} value={page + 1} onChange={(p) => setPage(p - 1)} />
          </Group>
        </>
      )}
    </Stack>
  )
}
