import { useEffect, useState } from 'react'
import { Alert, Badge, Button, Checkbox, Group, Loader, NumberInput, Select, SimpleGrid, Stack, Text, Textarea } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { enregistrerInfrastructure, infrastructureEtablissement } from '../api/endpoints'
import type { InfrastructureRequete } from '../api/types'

const EAU = [
  { value: 'AUCUNE', label: 'Aucun point d\'eau' },
  { value: 'PUITS', label: 'Puits' },
  { value: 'FORAGE', label: 'Forage' },
  { value: 'RESEAU', label: 'Réseau (SEG)' },
]
const ELECTRICITE = [
  { value: 'AUCUNE', label: 'Aucune' },
  { value: 'RESEAU', label: 'Réseau (EDG)' },
  { value: 'SOLAIRE', label: 'Solaire' },
  { value: 'GROUPE', label: 'Groupe électrogène' },
]
const EQUIPEMENTS: { cle: keyof InfrastructureRequete; libelle: string }[] = [
  { cle: 'cloture', libelle: 'Clôture' },
  { cle: 'lavageMains', libelle: 'Dispositif de lavage des mains' },
  { cle: 'bibliotheque', libelle: 'Bibliothèque' },
  { cle: 'laboratoire', libelle: 'Laboratoire' },
  { cle: 'salleInformatique', libelle: 'Salle informatique' },
  { cle: 'cantine', libelle: 'Cantine' },
  { cle: 'terrainSport', libelle: 'Terrain de sport' },
]
const NOMBRES: { cle: keyof InfrastructureRequete; libelle: string }[] = [
  { cle: 'sallesDur', libelle: 'Salles de classe en dur' },
  { cle: 'sallesProvisoires', libelle: 'Salles provisoires (paillotes, abris)' },
  { cle: 'sallesARehabiliter', libelle: 'Salles à réhabiliter' },
  { cle: 'placesAssises', libelle: 'Places assises (tables-bancs)' },
  { cle: 'latrinesFilles', libelle: 'Latrines filles' },
  { cle: 'latrinesGarcons', libelle: 'Latrines garçons' },
  { cle: 'latrinesPersonnel', libelle: 'Latrines du personnel' },
]

const ratio = (v: number | null) => (v === null ? '—' : String(v).replace('.', ','))

/** Infrastructures et équipements de l'établissement (carte scolaire) : la direction les saisit, les autres profils les consultent. */
export default function InfrastructuresEtablissement({ etablissementId, anneeId, modifiable }: { etablissementId: number; anneeId: number; modifiable: boolean }) {
  const queryClient = useQueryClient()
  const fiche = useQuery({
    queryKey: ['infrastructure', etablissementId, anneeId],
    queryFn: () => infrastructureEtablissement(etablissementId, anneeId),
  })
  const [form, setForm] = useState<InfrastructureRequete | null>(null)
  useEffect(() => {
    if (fiche.data) setForm(fiche.data.fiche)
  }, [fiche.data])

  const enregistrer = useMutation({
    mutationFn: (f: InfrastructureRequete) => enregistrerInfrastructure(etablissementId, anneeId, f),
    onSuccess: () => {
      notifications.show({ color: 'green', message: 'Infrastructures enregistrées.' })
      void queryClient.invalidateQueries({ queryKey: ['infrastructure'] })
      void queryClient.invalidateQueries({ queryKey: ['infrastructures-synthese'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })

  const maj = <K extends keyof InfrastructureRequete>(cle: K, valeur: InfrastructureRequete[K]) => setForm((f) => (f === null ? f : { ...f, [cle]: valeur }))
  const d = fiche.data

  if (fiche.isLoading) return <Loader size="sm" />
  if (fiche.isError || !d || form === null) return <Alert color="red">Infrastructures indisponibles.</Alert>

  return (
    <Stack>
      <Group gap="xs">
        <Badge variant="light" color={d.renseignee ? 'green' : 'gray'}>{d.renseignee ? 'Renseignées' : 'Non renseignées'}</Badge>
        <Text size="sm" c="dimmed">
          {d.effectifs.total} élèves inscrits ({d.effectifs.filles} filles, {d.effectifs.garcons} garçons)
        </Text>
        {d.misAJourLe && <Text size="xs" c="dimmed">Mis à jour le {new Date(d.misAJourLe).toLocaleDateString('fr-FR')}{d.misAJourPar ? ` par ${d.misAJourPar}` : ''}</Text>}
      </Group>
      {d.renseignee && (
        <Group gap="xl">
          <Text size="sm">Élèves par salle : <b>{ratio(d.indicateurs.elevesParSalle)}</b></Text>
          <Text size="sm">Élèves par place assise : <b>{ratio(d.indicateurs.elevesParPlace)}</b></Text>
          <Text size="sm">Filles par latrine : <b>{ratio(d.indicateurs.fillesParLatrine)}</b></Text>
          <Text size="sm">Garçons par latrine : <b>{ratio(d.indicateurs.garconsParLatrine)}</b></Text>
        </Group>
      )}
      {d.renseignee && d.indicateurs.alertes.length > 0 && (
        <Alert color="orange" variant="light" title="Points d'attention">
          {d.indicateurs.alertes.map((a) => <Text key={a} size="sm">• {a}</Text>)}
        </Alert>
      )}
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
        {NOMBRES.map((n) => (
          <NumberInput key={n.cle} label={n.libelle} min={0} value={form[n.cle] as number} disabled={!modifiable}
            onChange={(v) => maj(n.cle, Number(v) || 0 as never)} />
        ))}
        <Select label="Eau" data={EAU} value={form.eau} onChange={(v) => maj('eau', v ?? 'AUCUNE')} allowDeselect={false} disabled={!modifiable} />
        <Select label="Électricité" data={ELECTRICITE} value={form.electricite} onChange={(v) => maj('electricite', v ?? 'AUCUNE')} allowDeselect={false} disabled={!modifiable} />
      </SimpleGrid>
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
        {EQUIPEMENTS.map((e) => (
          <Checkbox key={e.cle} label={e.libelle} checked={form[e.cle] as boolean} disabled={!modifiable}
            onChange={(ev) => maj(e.cle, ev.currentTarget.checked as never)} />
        ))}
      </SimpleGrid>
      <Textarea label="Observation" maxLength={500} value={form.observation ?? ''} disabled={!modifiable} onChange={(e) => maj('observation', e.currentTarget.value)} />
      {modifiable && (
        <Group>
          <Button loading={enregistrer.isPending} onClick={() => enregistrer.mutate({ ...form, observation: form.observation?.trim() ? form.observation : null })}>Enregistrer</Button>
        </Group>
      )}
    </Stack>
  )
}
