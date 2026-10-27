import { useState } from 'react'
import { Alert, Button, Group, Loader, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { listerClasses, listerInscriptions } from '../api/endpoints'
import type { Inscription } from '../api/types'
import { etablissementCourant } from '../auth/roles'
import { useUtilisateur } from '../auth/useAuth'
import { useAnneeScolaire } from '../hooks/useAnneeScolaire'
import { formaterDate } from '../utils/dates'
import ModifierEleveModal from './eleves/ModifierEleveModal'
import ParentsModal from './eleves/ParentsModal'

export default function PageEleves() {
  const utilisateur = useUtilisateur()
  const etablissementId = etablissementCourant(utilisateur)
  const { anneeId, setAnneeChoisie, options } = useAnneeScolaire()
  const [classeFiltre, setClasseFiltre] = useState<string | null>(null)
  const [recherche, setRecherche] = useState('')
  const [aModifier, setAModifier] = useState<Inscription | null>(null)
  const [parentsDe, setParentsDe] = useState<Inscription | null>(null)

  const inscriptions = useQuery({
    queryKey: ['inscriptions', etablissementId, anneeId, 'ACCEPTEE'],
    queryFn: () => listerInscriptions(etablissementId as number, Number(anneeId), 'ACCEPTEE'),
    enabled: etablissementId !== undefined && anneeId !== null,
  })
  const classes = useQuery({
    queryKey: ['classes', etablissementId, anneeId],
    queryFn: () => listerClasses(etablissementId as number, Number(anneeId)),
    enabled: etablissementId !== undefined && anneeId !== null,
  })

  if (etablissementId === undefined) {
    return (
      <Alert color="orange" variant="light">
        Aucun établissement n'est associé à votre compte.
      </Alert>
    )
  }

  const terme = recherche.trim().toLowerCase()
  const visibles = (inscriptions.data ?? [])
    .filter((i) => classeFiltre === null || String(i.classeId) === classeFiltre)
    .filter((i) => terme === '' || `${i.eleve.nom} ${i.eleve.prenom} ${i.eleve.matricule}`.toLowerCase().includes(terme))
    .sort((a, b) => a.eleve.nom.localeCompare(b.eleve.nom, 'fr') || a.eleve.prenom.localeCompare(b.eleve.prenom, 'fr'))

  const optionsClasses = (classes.data ?? []).map((c) => ({ value: String(c.id), label: `${c.nom} — ${c.niveauLibelle}` }))

  return (
    <Stack>
      <Title order={2}>Élèves</Title>

      <Group align="flex-end">
        <Select
          label="Année scolaire"
          data={options}
          value={anneeId}
          onChange={(valeur) => {
            setAnneeChoisie(valeur)
            setClasseFiltre(null)
          }}
          allowDeselect={false}
          w={200}
        />
        <Select
          label="Classe"
          placeholder="Toutes"
          data={optionsClasses}
          value={classeFiltre}
          onChange={setClasseFiltre}
          clearable
          w={260}
        />
        <TextInput
          label="Rechercher"
          placeholder="Nom, prénom ou matricule"
          value={recherche}
          onChange={(e) => setRecherche(e.currentTarget.value)}
          w={260}
        />
      </Group>

      {inscriptions.isPending && anneeId !== null && <Loader />}
      {inscriptions.isError && (
        <Alert color="red" variant="light">
          {inscriptions.error.message}
        </Alert>
      )}
      {inscriptions.data && visibles.length === 0 && (
        <Text c="dimmed">Aucun élève ne correspond à ces critères. Seuls les élèves dont l'inscription est acceptée sont listés ici.</Text>
      )}
      {visibles.length > 0 && (
        <>
          <Table.ScrollContainer minWidth={760}>
            <Table striped highlightOnHover withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Nom</Table.Th>
                  <Table.Th>Prénom</Table.Th>
                  <Table.Th>Matricule</Table.Th>
                  <Table.Th>Naissance</Table.Th>
                  <Table.Th>Classe</Table.Th>
                  <Table.Th>Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {visibles.map((i) => (
                  <Table.Tr key={i.id}>
                    <Table.Td>{i.eleve.nom}</Table.Td>
                    <Table.Td>{i.eleve.prenom}</Table.Td>
                    <Table.Td>{i.eleve.matricule}</Table.Td>
                    <Table.Td>{formaterDate(i.eleve.dateNaissance)}</Table.Td>
                    <Table.Td>{i.classeNom ?? '—'}</Table.Td>
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        <Button size="compact-sm" variant="light" onClick={() => setAModifier(i)}>
                          Modifier
                        </Button>
                        <Button size="compact-sm" variant="light" color="teal" onClick={() => setParentsDe(i)}>
                          Parents
                        </Button>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
          <Text size="sm" c="dimmed">
            {visibles.length} élève(s)
          </Text>
        </>
      )}

      {aModifier !== null && (
        <ModifierEleveModal eleve={aModifier.eleve} etablissementId={etablissementId} onFermer={() => setAModifier(null)} />
      )}
      {parentsDe !== null && <ParentsModal eleve={parentsDe.eleve} onFermer={() => setParentsDe(null)} />}
    </Stack>
  )
}
