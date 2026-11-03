import { Alert, Loader, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { verificationsDiplomes } from '../../api/endpoints'
import CarteChiffre from '../../components/CarteChiffre'

const nombre = (n: number) => new Intl.NumberFormat('fr-FR').format(n)
const pourcent = (t: number | null) => (t === null ? '—' : `${String(t).replace('.', ',')} %`)

/** Contrôles publics d'authenticité des diplômes : volumes, part de codes inconnus et tentatives répétées (faux diplômes possibles). */
export default function VerificationsDiplomes() {
  const requete = useQuery({ queryKey: ['verifications-diplomes'], queryFn: verificationsDiplomes })
  const v = requete.data
  return (
    <Stack>
      <div>
        <Title order={4}>Authentification des diplômes</Title>
        <Text c="dimmed" size="sm">Vérifications faites par le public avec le code imprimé sur le diplôme. Seuls le code saisi et le résultat sont conservés.</Text>
      </div>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">Vérifications indisponibles.</Alert>}
      {v && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Vérifications" valeur={nombre(v.total)} icone="resultats" couleur="blue" detail={`${nombre(v.derniers30Jours)} sur 30 jours`} />
            <CarteChiffre libelle="Authentiques" valeur={nombre(v.authentiques)} icone="resultats" couleur="green" />
            <CarteChiffre libelle="Révoqués" valeur={nombre(v.revoques)} icone="alerte" couleur="orange" />
            <CarteChiffre libelle="Codes inconnus" valeur={nombre(v.inconnus)} icone="alerte" couleur="red" detail={`${pourcent(v.tauxInconnus)} des vérifications`} />
          </SimpleGrid>
          {v.suspects.length > 0 && (
            <Table withTableBorder w={520}>
              <Table.Thead>
                <Table.Tr><Table.Th>Code inconnu saisi ≥ {v.seuilSuspect} fois (30 jours)</Table.Th><Table.Th ta="right">Tentatives</Table.Th><Table.Th>Dernière</Table.Th></Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {v.suspects.map((s) => (
                  <Table.Tr key={s.code}>
                    <Table.Td>{s.code}</Table.Td>
                    <Table.Td ta="right">{nombre(s.tentatives)}</Table.Td>
                    <Table.Td>{new Date(s.derniere).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </>
      )}
    </Stack>
  )
}
