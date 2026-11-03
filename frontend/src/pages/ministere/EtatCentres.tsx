import { Alert, Badge, Button, Checkbox, Group, Loader, Modal, NumberInput, Select, SimpleGrid, Stack, Switch, Table, Text, TextInput, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { enregistrerCaracteristiques, etatCentres } from '../../api/endpoints'
import type { CaracteristiquesCentre, EtatCentres as Etat } from '../../api/types'
import CarteChiffre from '../../components/CarteChiffre'
import { useAnneeScolaire } from '../../hooks/useAnneeScolaire'
import { useDroitsExamens } from '../../auth/useDroitsExamens'

type LigneCentre = Etat['centres'][number]
const nombre = (n: number | null) => (n === null ? '—' : new Intl.NumberFormat('fr-FR').format(n))
const ETATS = [{ value: 'BON', label: 'Bon' }, { value: 'MOYEN', label: 'Moyen' }, { value: 'MAUVAIS', label: 'Mauvais' }]
const COULEUR: Record<string, string> = { BON: 'green', MOYEN: 'orange', MAUVAIS: 'red' }
const HANDICAPS: Record<string, string> = { MOTEUR: 'Moteur', VISUEL: 'Visuel', AUDITIF: 'Auditif', INTELLECTUEL: 'Intellectuel', AUTRE: 'Autre' }

const etat = (e: string | null) => (e === null ? <Text span c="dimmed">—</Text> : <Badge color={COULEUR[e]} variant="light">{ETATS.find((x) => x.value === e)?.label}</Badge>)

/** État des centres d'examen (salles de réserve, installations, accessibilité, risques) et candidats en situation de handicap. */
export default function EtatCentres() {
  const annee = useAnneeScolaire()
  const anneeId = annee.anneeId === null ? null : Number(annee.anneeId)
  const { dgecs } = useDroitsExamens()
  const queryClient = useQueryClient()
  const [centre, setCentre] = useState<LigneCentre | null>(null)
  const [f, setF] = useState<CaracteristiquesCentre | null>(null)

  const requete = useQuery({ queryKey: ['etat-centres', anneeId], queryFn: () => etatCentres(anneeId as number), enabled: anneeId !== null })
  const enregistrer = useMutation({
    mutationFn: () => enregistrerCaracteristiques((centre as LigneCentre).examenId, (centre as LigneCentre).centreId, f as CaracteristiquesCentre),
    onSuccess: () => {
      setCentre(null)
      void queryClient.invalidateQueries({ queryKey: ['etat-centres'] })
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const ouvrir = (c: LigneCentre) => {
    setCentre(c)
    setF({ ...c })
  }
  const maj = (p: Partial<CaracteristiquesCentre>) => setF((a) => (a ? { ...a, ...p } : a))
  const r = requete.data?.resume

  return (
    <Stack mt="xl">
      <Group justify="space-between" align="end">
        <div>
          <Title order={3}>État des centres d'examen</Title>
          <Text c="dimmed" size="sm">Salles de réserve, installations, accessibilité, distances et risques ; candidats en situation de handicap. Année : {annee.options.find((o) => o.value === annee.anneeId)?.label ?? ''}.</Text>
        </div>
      </Group>
      {requete.isLoading && <Loader />}
      {requete.isError && <Alert color="red">État des centres indisponible.</Alert>}
      {r && requete.data && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
            <CarteChiffre libelle="Centres renseignés" valeur={`${nombre(r.renseignes)} / ${nombre(r.centres)}`} icone="classes" detail={`${r.moyenneCandidatsParCentre === null ? '—' : String(r.moyenneCandidatsParCentre).replace('.', ',')} candidats par centre en moyenne`} />
            <CarteChiffre libelle="Salles de réserve" valeur={nombre(r.sallesReserve)} icone="classes" />
            <CarteChiffre libelle="Centres à risque" valeur={nombre(r.centresARisque)} icone="alerte" couleur="red" detail={`inondation ${r.risqueInondation}, enclavement ${r.risqueEnclavement}, insécurité ${r.risqueInsecurite}`} />
            <CarteChiffre libelle="Installations en mauvais état" valeur={nombre(r.centresInstallationsMauvaises)} icone="alerte" couleur="orange" detail={`${nombre(r.centresInaccessibles)} centres inaccessibles`} />
            <CarteChiffre libelle="Candidats en situation de handicap" valeur={nombre(r.candidatsHandicapes)} icone="eleves" />
          </SimpleGrid>
          {requete.data.handicaps.length > 0 && (
            <Table withTableBorder w={480}>
              <Table.Thead><Table.Tr><Table.Th>Niveau d'examen</Table.Th><Table.Th>Handicap</Table.Th><Table.Th ta="right">Candidats</Table.Th></Table.Tr></Table.Thead>
              <Table.Tbody>
                {requete.data.handicaps.map((h) => (
                  <Table.Tr key={`${h.niveau}|${h.type}`}><Table.Td>{h.niveau}</Table.Td><Table.Td>{HANDICAPS[h.type] ?? h.type}</Table.Td><Table.Td ta="right">{nombre(h.nombre)}</Table.Td></Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
          <Table.ScrollContainer minWidth={980}>
            <Table striped withTableBorder>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Centre</Table.Th><Table.Th>Examen</Table.Th><Table.Th ta="right">Candidats</Table.Th><Table.Th ta="right">Réserve</Table.Th>
                  <Table.Th>Tables</Table.Th><Table.Th>Toilettes</Table.Th><Table.Th>Éclairage</Table.Th><Table.Th>Ventilation</Table.Th>
                  <Table.Th>Accès</Table.Th><Table.Th ta="right">Distance max</Table.Th><Table.Th>Risques</Table.Th><Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {requete.data.centres.map((c) => (
                  <Table.Tr key={c.centreId}>
                    <Table.Td>{c.centre}<Text size="xs" c="dimmed">{c.zone ?? ''}</Text></Table.Td>
                    <Table.Td>{c.examen}</Table.Td><Table.Td ta="right">{nombre(c.candidats)}</Table.Td>
                    <Table.Td ta="right">{c.renseigne ? nombre(c.sallesReserve) : '—'}</Table.Td>
                    <Table.Td>{etat(c.etatTablesBancs)}</Table.Td><Table.Td>{etat(c.etatToilettes)}</Table.Td>
                    <Table.Td>{etat(c.etatEclairage)}</Table.Td><Table.Td>{etat(c.etatVentilation)}</Table.Td>
                    <Table.Td>{c.accessible === null ? '—' : c.accessible ? 'Oui' : <Badge color="red">Non</Badge>}</Table.Td>
                    <Table.Td ta="right">{c.distanceMaxKm === null ? '—' : `${c.distanceMaxKm} km${c.tempsMaxMin === null ? '' : ` · ${c.tempsMaxMin} min`}`}</Table.Td>
                    <Table.Td>
                      <Group gap={4}>
                        {c.risqueInondation && <Badge color="red" variant="light">Inondation</Badge>}
                        {c.risqueEnclavement && <Badge color="red" variant="light">Enclavement</Badge>}
                        {c.risqueInsecurite && <Badge color="red" variant="light">Insécurité</Badge>}
                      </Group>
                    </Table.Td>
                    <Table.Td>{dgecs && <Button size="xs" variant="light" onClick={() => ouvrir(c)}>{c.renseigne ? 'Modifier' : 'Renseigner'}</Button>}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </>
      )}
      <Modal opened={centre !== null && f !== null} onClose={() => setCentre(null)} title={centre ? `Centre : ${centre.centre}` : ''}>
        {f && (
          <Stack>
            <NumberInput label="Salles de réserve" min={0} value={f.sallesReserve} onChange={(v) => maj({ sallesReserve: Number(v) || 0 })} />
            <SimpleGrid cols={2}>
              <Select label="Tables-bancs" data={ETATS} value={f.etatTablesBancs} onChange={(v) => maj({ etatTablesBancs: v })} clearable />
              <Select label="Toilettes" data={ETATS} value={f.etatToilettes} onChange={(v) => maj({ etatToilettes: v })} clearable />
              <Select label="Éclairage" data={ETATS} value={f.etatEclairage} onChange={(v) => maj({ etatEclairage: v })} clearable />
              <Select label="Ventilation" data={ETATS} value={f.etatVentilation} onChange={(v) => maj({ etatVentilation: v })} clearable />
              <NumberInput label="Distance maximale (km)" min={0} value={f.distanceMaxKm ?? ''} onChange={(v) => maj({ distanceMaxKm: v === '' ? null : Number(v) })} />
              <NumberInput label="Temps de trajet maximal (min)" min={0} value={f.tempsMaxMin ?? ''} onChange={(v) => maj({ tempsMaxMin: v === '' ? null : Number(v) })} />
            </SimpleGrid>
            <Switch label="Centre accessible (route praticable, accès pour personnes à mobilité réduite)" checked={f.accessible === true}
              onChange={(e) => maj({ accessible: e.currentTarget.checked })} />
            <Checkbox label="Risque d'inondation" checked={f.risqueInondation} onChange={(e) => maj({ risqueInondation: e.currentTarget.checked })} />
            <Checkbox label="Risque d'enclavement" checked={f.risqueEnclavement} onChange={(e) => maj({ risqueEnclavement: e.currentTarget.checked })} />
            <Checkbox label="Risque d'insécurité" checked={f.risqueInsecurite} onChange={(e) => maj({ risqueInsecurite: e.currentTarget.checked })} />
            <TextInput label="Observation" value={f.observation ?? ''} onChange={(e) => maj({ observation: e.currentTarget.value || null })} maxLength={500} />
            <Button loading={enregistrer.isPending} onClick={() => enregistrer.mutate()}>Enregistrer</Button>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
}
