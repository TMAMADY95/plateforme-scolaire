import { useState } from 'react'
import { Alert, Button, Group, Select, Stack, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation } from '@tanstack/react-query'
import { inscrireCandidatLibre } from '../api/endpoints'
import { SERIES_LYCEE } from '../api/types'

/** Candidature libre (BEPC et BAC) : un candidat non scolarisé dépose son dossier auprès de l'établissement. */
export default function CandidatLibreForm({ etablissementId, examenId, lycee, cycle, onInscrit }: { etablissementId: number; examenId: number; lycee: boolean; cycle: string | null; onInscrit: () => void }) {
  const [nom, setNom] = useState('')
  const [prenom, setPrenom] = useState('')
  const [naissance, setNaissance] = useState('')
  const [sexe, setSexe] = useState<string | null>(null)
  const [lieu, setLieu] = useState('')
  const [pere, setPere] = useState('')
  const [mere, setMere] = useState('')
  const [serie, setSerie] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const inscription = useMutation({
    mutationFn: () => inscrireCandidatLibre(etablissementId, examenId, { nom: nom.trim(), prenom: prenom.trim(), dateNaissance: naissance, sexe, lieuNaissance: lieu.trim(), nomPere: pere.trim(), nomMere: mere.trim(), serie: lycee ? serie : null }),
    onSuccess: () => {
      setNom('')
      setPrenom('')
      setNaissance('')
      setSexe(null)
      setLieu('')
      setPere('')
      setMere('')
      setSerie(null)
      setErreur(null)
      notifications.show({ color: 'green', title: 'Candidat libre inscrit', message: 'Complétez maintenant son dossier de candidature.' })
      onInscrit()
    },
    onError: (e) => setErreur(e.message),
  })
  return (
    <Stack gap="xs">
      <Text fw={600} size="sm">Candidature libre</Text>
      <Text size="xs" c="dimmed">
        Pour un candidat non scolarisé cette année, du cycle de l'examen{cycle ? ` (${cycle})` : ''}{lycee ? ' et de la série dans laquelle il compose' : ''}. Son dossier doit comporter la demande, l'acte de naissance et le diplôme précédent.
      </Text>
      <Group align="flex-end" gap="xs">
        <TextInput size="xs" label="Nom" value={nom} onChange={(e) => setNom(e.currentTarget.value)} w={160} />
        <TextInput size="xs" label="Prénom" value={prenom} onChange={(e) => setPrenom(e.currentTarget.value)} w={160} />
        <TextInput size="xs" label="Date de naissance" type="date" value={naissance} onChange={(e) => setNaissance(e.currentTarget.value)} />
        <Select size="xs" label="Sexe" clearable data={[{ value: 'F', label: 'Féminin' }, { value: 'M', label: 'Masculin' }]} value={sexe} onChange={setSexe} w={110} />
        <TextInput size="xs" label="Lieu de naissance" value={lieu} onChange={(e) => setLieu(e.currentTarget.value)} w={150} />
        {lycee && <Select size="xs" label="Série" required data={SERIES_LYCEE} value={serie} onChange={setSerie} w={190} />}
        <TextInput size="xs" label="Père" value={pere} onChange={(e) => setPere(e.currentTarget.value)} w={150} />
        <TextInput size="xs" label="Mère" value={mere} onChange={(e) => setMere(e.currentTarget.value)} w={150} />
        <Button size="xs" variant="light" disabled={nom.trim() === '' || prenom.trim() === '' || naissance === '' || (lycee && serie === null)} loading={inscription.isPending} onClick={() => inscription.mutate()}>
          Inscrire
        </Button>
      </Group>
      {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
    </Stack>
  )
}
