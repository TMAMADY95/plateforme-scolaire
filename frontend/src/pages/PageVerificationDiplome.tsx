import { useState } from 'react'
import { Alert, Button, Container, Paper, Stack, Text, TextInput, Title } from '@mantine/core'
import { verifierDiplome } from '../api/endpoints'
import type { VerificationDiplome } from '../api/types'
import { LIBELLE_MENTION } from '../components/DiplomesExamenModal'

/** Page publique : contrôle d'authenticité d'un diplôme avec le code imprimé dessus. */
export default function PageVerificationDiplome() {
  const [code, setCode] = useState('')
  const [resultat, setResultat] = useState<VerificationDiplome | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [enCours, setEnCours] = useState(false)

  async function verifier() {
    setEnCours(true)
    setErreur(null)
    try {
      setResultat(await verifierDiplome(code.trim()))
    } catch (e) {
      setResultat(null)
      setErreur(e instanceof Error ? e.message : 'Vérification impossible.')
    } finally {
      setEnCours(false)
    }
  }

  return (
    <Container size={520} my={60}>
      <Title ta="center" order={2}>Vérifier un diplôme</Title>
      <Text c="dimmed" size="sm" ta="center" mt={5} mb={30}>Saisissez le code de vérification imprimé sur le diplôme</Text>
      <Paper withBorder shadow="sm" p={30} radius="md">
        <Stack>
          <TextInput label="Code de vérification" placeholder="ABCDE-FGHJK" value={code} onChange={(e) => setCode(e.currentTarget.value)} />
          <Button loading={enCours} disabled={code.trim() === ''} onClick={() => void verifier()}>Vérifier</Button>
          {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
          {resultat?.statut === 'INCONNU' && (
            <Alert color="red" variant="light">Aucun diplôme ne correspond à ce code : le document n'est pas authentique ou le code est mal recopié.</Alert>
          )}
          {resultat && resultat.statut !== 'INCONNU' && (
            <Alert color={resultat.statut === 'AUTHENTIQUE' ? 'green' : 'orange'} variant="light" title={resultat.statut === 'AUTHENTIQUE' ? 'Diplôme authentique' : 'Diplôme révoqué'}>
              <Text size="sm"><b>{resultat.titulaire}</b></Text>
              <Text size="sm">{resultat.examen} — {resultat.anneeScolaire}</Text>
              <Text size="sm">Mention : {LIBELLE_MENTION[resultat.mention as keyof typeof LIBELLE_MENTION] ?? resultat.mention}</Text>
              <Text size="sm">N° {resultat.numero}, délivré le {resultat.delivreLe ? new Date(resultat.delivreLe).toLocaleDateString('fr-FR') : ''}</Text>
              {resultat.motifRevocation && <Text size="sm">Motif de la révocation : {resultat.motifRevocation}</Text>}
            </Alert>
          )}
        </Stack>
      </Paper>
    </Container>
  )
}
