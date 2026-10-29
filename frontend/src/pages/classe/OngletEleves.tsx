import { useState } from 'react'
import { Alert, Button, Loader, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { obtenirFeuille } from '../../api/endpoints'
import NouveauMessageModal from '../../components/messagerie/NouveauMessageModal'
import { aujourdhui, formaterDate } from '../../utils/dates'

/** Liste des élèves acceptés de la classe (tirée de la feuille d'appel du jour). */
export default function OngletEleves({ classeId }: { classeId: number }) {
  const date = aujourdhui()
  const navigate = useNavigate()
  const [ecrireA, setEcrireA] = useState<{ id: number; libelle: string } | null>(null)
  const feuille = useQuery({ queryKey: ['feuille', classeId, date], queryFn: () => obtenirFeuille(classeId, date) })

  if (feuille.isPending) return <Loader />
  if (feuille.isError) {
    return (
      <Alert color="red" variant="light">
        {feuille.error.message}
      </Alert>
    )
  }
  if (feuille.data.lignes.length === 0) {
    return <Text c="dimmed">Aucun élève n'est encore inscrit dans cette classe.</Text>
  }

  return (
    <Table.ScrollContainer minWidth={520}>
      <Table striped withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Nom</Table.Th>
            <Table.Th>Prénom</Table.Th>
            <Table.Th>Matricule</Table.Th>
            <Table.Th>Naissance</Table.Th>
            <Table.Th />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {feuille.data.lignes.map((ligne) => (
            <Table.Tr key={ligne.inscriptionId}>
              <Table.Td>{ligne.eleve.nom}</Table.Td>
              <Table.Td>{ligne.eleve.prenom}</Table.Td>
              <Table.Td>{ligne.eleve.matricule}</Table.Td>
              <Table.Td>{formaterDate(ligne.eleve.dateNaissance)}</Table.Td>
              <Table.Td>
                <Button
                  size="compact-sm"
                  variant="subtle"
                  onClick={() => setEcrireA({ id: ligne.eleve.id, libelle: `${ligne.eleve.prenom} ${ligne.eleve.nom}` })}
                >
                  Écrire aux parents
                </Button>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Text size="sm" c="dimmed" mt="xs">
        {feuille.data.lignes.length} élève(s)
      </Text>
      {ecrireA !== null && (
        <NouveauMessageModal
          eleve={ecrireA}
          onFermer={() => setEcrireA(null)}
          onCree={(id) => void navigate(`/messages?c=${id}`)}
        />
      )}
    </Table.ScrollContainer>
  )
}
