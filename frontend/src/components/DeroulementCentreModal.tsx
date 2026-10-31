import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Modal, NumberInput, Stack, Text, Textarea, TextInput } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { avancerPli, deroulementCentre, enregistrerRapportEpreuve, ouvrirPli } from '../api/endpoints'
import type { DeroulementCentre, EtatPli } from '../api/types'

type Epreuve = DeroulementCentre['epreuves'][number]

const ETAPES: Record<EtatPli, string> = {
  A_CONDITIONNER: 'À conditionner (DGECS)',
  CONDITIONNE: 'Conditionné et scellé (DGECS)',
  EXPEDIE_IRE: 'Expédié à l\'inspection régionale',
  RECU_DPE: 'Reçu par la direction préfectorale ou communale',
  RECU_PRESIDENT: 'Reçu par le président de la commission de surveillance',
  OUVERT: 'Pli ouvert',
}
const MARQUEUR: Record<string, string> = {
  CONDITIONNE: 'Marquer conditionné',
  EXPEDIE_IRE: 'Marquer expédié à l\'IRE',
  RECU_DPE: 'Marquer reçu par la DPE/DCE',
  RECU_PRESIDENT: 'Marquer reçu par le président',
}
const heure = (iso: string | null) => (iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : null)

/** Déroulement des épreuves dans un centre : circuit du pli, ouverture en présence de deux témoins, rapport de surveillance. */
export default function DeroulementCentreModal({ examenId, centre, onFermer }: { examenId: number; centre: { id: number; nom: string }; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const cle = ['deroulement-centre', examenId, centre.id]
  const donnees = useQuery({ queryKey: cle, queryFn: () => deroulementCentre(examenId, centre.id) })
  const appliquer = (d: DeroulementCentre) => queryClient.setQueryData(cle, d)
  const d = donnees.data
  return (
    <Modal opened onClose={onFermer} size="xl" title={`Déroulement des épreuves : ${centre.nom}`}>
      <Stack gap="sm">
        <Text size="sm" c="dimmed">
          Le pli de chaque épreuve suit le circuit sécurisé jusqu'au président de la commission de surveillance. Il n'est ouvert qu'au début de
          l'épreuve, en présence de deux témoins : aucune autorité ne peut l'ouvrir seule.
        </Text>
        {donnees.isError && <Alert color="red" variant="light">{donnees.error.message}</Alert>}
        {d && <Text size="sm">{d.candidats} candidat(s) dans ce centre.</Text>}
        {d?.epreuves.map((e) => <CarteEpreuve key={e.epreuveId} epreuve={e} examenId={examenId} centreId={centre.id} onChange={appliquer} />)}
      </Stack>
    </Modal>
  )
}

function CarteEpreuve({ epreuve: e, examenId, centreId, onChange }: { epreuve: Epreuve; examenId: number; centreId: number; onChange: (d: DeroulementCentre) => void }) {
  const [erreur, setErreur] = useState<string | null>(null)
  const options = { onSuccess: (d: DeroulementCentre) => { onChange(d); setErreur(null) }, onError: (x: Error) => setErreur(x.message) }
  const avancement = useMutation({ mutationFn: () => avancerPli(examenId, centreId, e.epreuveId), ...options })
  const [president, setPresident] = useState('')
  const [temoin1, setTemoin1] = useState('')
  const [temoin2, setTemoin2] = useState('')
  const ouverture = useMutation({ mutationFn: () => ouvrirPli(examenId, centreId, e.epreuveId, { ouvertPar: president.trim(), temoin1: temoin1.trim(), temoin2: temoin2.trim() }), ...options })

  return (
    <Card withBorder padding="sm">
      <Stack gap="xs">
        <Group justify="space-between">
          <Group gap="xs">
            <Text fw={600}>{e.nom}</Text>
            <Text size="xs" c="dimmed">{new Date(e.date).toLocaleDateString('fr-FR')} · {e.heureDebut.slice(0, 5)}-{e.heureFin.slice(0, 5)}</Text>
          </Group>
          <Badge color={e.statutPli === 'OUVERT' ? 'green' : e.statutPli === 'A_CONDITIONNER' ? 'gray' : 'blue'} variant="light">{ETAPES[e.statutPli]}</Badge>
        </Group>
        <Text size="xs" c="dimmed">
          {e.nbExemplaires} exemplaire(s) du sujet pour le centre
          {[heure(e.conditionneLe) && `conditionné ${heure(e.conditionneLe)}`, heure(e.expedieLe) && `expédié ${heure(e.expedieLe)}`,
            heure(e.recuDpeLe) && `reçu DPE/DCE ${heure(e.recuDpeLe)}`, heure(e.recuPresidentLe) && `reçu président ${heure(e.recuPresidentLe)}`]
            .filter(Boolean).map((t) => ` · ${t}`).join('')}
        </Text>
        {e.etapeSuivante && (
          <Group>
            <Button size="xs" variant="light" loading={avancement.isPending} onClick={() => avancement.mutate()}>{MARQUEUR[e.etapeSuivante]}</Button>
          </Group>
        )}
        {e.statutPli === 'RECU_PRESIDENT' && (
          <Group align="flex-end" gap="xs">
            <TextInput size="xs" label="Président de la commission" value={president} onChange={(x) => setPresident(x.currentTarget.value)} w={200} />
            <TextInput size="xs" label="Témoin 1" value={temoin1} onChange={(x) => setTemoin1(x.currentTarget.value)} w={180} />
            <TextInput size="xs" label="Témoin 2" value={temoin2} onChange={(x) => setTemoin2(x.currentTarget.value)} w={180} />
            <Button size="xs" disabled={president.trim() === '' || temoin1.trim() === '' || temoin2.trim() === ''} loading={ouverture.isPending} onClick={() => ouverture.mutate()}>
              Ouvrir le pli
            </Button>
          </Group>
        )}
        {e.statutPli === 'OUVERT' && (
          <>
            <Text size="xs" c="dimmed">Ouvert {heure(e.ouvertLe)} par {e.ouvertPar}, en présence de {e.temoin1} et {e.temoin2}.</Text>
            <Rapport epreuve={e} examenId={examenId} centreId={centreId} onChange={onChange} />
          </>
        )}
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
      </Stack>
    </Card>
  )
}

const versLocal = (iso: string | null) => (iso ? new Date(new Date(iso).getTime() - new Date(iso).getTimezoneOffset() * 60000).toISOString().slice(0, 16) : '')

function Rapport({ epreuve: e, examenId, centreId, onChange }: { epreuve: Epreuve; examenId: number; centreId: number; onChange: (d: DeroulementCentre) => void }) {
  const [debut, setDebut] = useState(versLocal(e.compositionDebut))
  const [fin, setFin] = useState(versLocal(e.compositionFin))
  const [presents, setPresents] = useState<number | string>(e.presents ?? 0)
  const [absents, setAbsents] = useState<number | string>(e.absents ?? 0)
  const [elimines, setElimines] = useState<number | string>(e.elimines ?? 0)
  const [copies, setCopies] = useState<number | string>(e.copiesRamassees ?? 0)
  const [observations, setObservations] = useState(e.observations ?? '')
  const [erreur, setErreur] = useState<string | null>(null)
  const envoi = (cloturer: boolean) => enregistrerRapportEpreuve(examenId, centreId, e.epreuveId, {
    compositionDebut: debut === '' ? null : new Date(debut).toISOString(),
    compositionFin: fin === '' ? null : new Date(fin).toISOString(),
    presents: Number(presents) || 0,
    absents: Number(absents) || 0,
    elimines: Number(elimines) || 0,
    copiesRamassees: Number(copies) || 0,
    observations: observations.trim() === '' ? null : observations.trim(),
    cloturer,
  })
  const enregistrement = useMutation({ mutationFn: (cloturer: boolean) => envoi(cloturer), onSuccess: (d) => { onChange(d); setErreur(null) }, onError: (x) => setErreur(x.message) })
  const clos = e.rapportClos
  const ecart = (Number(copies) || 0) - ((Number(presents) || 0) + (Number(elimines) || 0))

  return (
    <Stack gap="xs">
      <Group gap="xs">
        <Text fw={600} size="sm">Rapport de surveillance</Text>
        {clos && <Badge color="green" variant="light">Clos</Badge>}
        {ecart !== 0 && <Badge color="orange" variant="light">Écart de copies : {ecart > 0 ? `+${ecart}` : ecart}</Badge>}
      </Group>
      <Group align="flex-end" gap="xs">
        <TextInput size="xs" label="Début réel" type="datetime-local" value={debut} onChange={(x) => setDebut(x.currentTarget.value)} disabled={clos} />
        <TextInput size="xs" label="Fin réelle" type="datetime-local" value={fin} onChange={(x) => setFin(x.currentTarget.value)} disabled={clos} />
        <NumberInput size="xs" label="Présents" min={0} allowDecimal={false} value={presents} onChange={setPresents} disabled={clos} w={90} />
        <NumberInput size="xs" label="Absents" min={0} allowDecimal={false} value={absents} onChange={setAbsents} disabled={clos} w={90} />
        <NumberInput size="xs" label="Éliminés" min={0} allowDecimal={false} value={elimines} onChange={setElimines} disabled={clos} w={90} />
        <NumberInput size="xs" label="Copies ramassées" min={0} allowDecimal={false} value={copies} onChange={setCopies} disabled={clos} w={120} />
      </Group>
      <Textarea size="xs" label="Observations" minRows={2} value={observations} onChange={(x) => setObservations(x.currentTarget.value)} disabled={clos} />
      {!clos && (
        <Group>
          <Button size="xs" variant="light" loading={enregistrement.isPending} onClick={() => enregistrement.mutate(false)}>Enregistrer</Button>
          <Button size="xs" color="green" variant="light" loading={enregistrement.isPending} onClick={() => enregistrement.mutate(true)}>Clore le rapport</Button>
        </Group>
      )}
      {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
    </Stack>
  )
}
