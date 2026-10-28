import { useState } from 'react'
import { Alert, Badge, Button, Loader, Stack, Table, Text, TextInput } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { useQuery } from '@tanstack/react-query'
import { listerComptes } from '../../api/endpoints'
import type { Compte } from '../../api/types'
import { LIBELLES_ROLES } from '../../auth/roles'
import ReinitialisationMotDePasseModal from '../../components/ReinitialisationMotDePasseModal'

interface Props {
  etablissementId: number
  /** Direction : recherche de parents et d'élèves. Ministère : seulement les directions, sans recherche. */
  avecRecherche: boolean
}

/** Comptes d'un établissement avec réinitialisation du mot de passe. */
export default function ListeComptes({ etablissementId, avecRecherche }: Props) {
  const [saisie, setSaisie] = useState('')
  const [recherche] = useDebouncedValue(saisie, 300)
  const [pourMotDePasse, setPourMotDePasse] = useState<Compte | null>(null)

  const comptes = useQuery({
    queryKey: ['comptes', etablissementId, avecRecherche ? recherche.trim() : ''],
    queryFn: () => listerComptes(etablissementId, avecRecherche ? recherche.trim() : ''),
  })

  return (
    <Stack>
      {avecRecherche && (
        <TextInput
          label="Rechercher"
          description="Nom, prénom ou e-mail. Les parents et élèves apparaissent à partir de 2 caractères."
          value={saisie}
          onChange={(e) => setSaisie(e.currentTarget.value)}
        />
      )}
      {comptes.isPending && <Loader />}
      {comptes.isError && (
        <Alert color="red" variant="light">
          {comptes.error.message}
        </Alert>
      )}
      {comptes.data && comptes.data.length === 0 && <Text c="dimmed">Aucun compte trouvé.</Text>}
      {comptes.data && comptes.data.length > 0 && (
        <Table.ScrollContainer minWidth={560}>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Nom</Table.Th>
                <Table.Th>E-mail</Table.Th>
                <Table.Th>Rôle</Table.Th>
                <Table.Th>État</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {comptes.data.map((c) => (
                <Table.Tr key={`${c.id}-${c.role}`}>
                  <Table.Td>
                    {c.prenom} {c.nom}
                  </Table.Td>
                  <Table.Td>{c.email}</Table.Td>
                  <Table.Td>{LIBELLES_ROLES[c.role]}</Table.Td>
                  <Table.Td>
                    {!c.actif ? (
                      <Badge color="gray" variant="light">
                        Désactivé
                      </Badge>
                    ) : c.doitChangerMotDePasse ? (
                      <Badge color="orange" variant="light">
                        Mot de passe à changer
                      </Badge>
                    ) : (
                      <Badge color="green" variant="light">
                        Actif
                      </Badge>
                    )}
                  </Table.Td>
                  <Table.Td>
                    <Button size="compact-xs" variant="light" onClick={() => setPourMotDePasse(c)}>
                      Réinitialiser le mot de passe
                    </Button>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
      {pourMotDePasse && (
        <ReinitialisationMotDePasseModal compte={pourMotDePasse} onFermer={() => setPourMotDePasse(null)} />
      )}
    </Stack>
  )
}
