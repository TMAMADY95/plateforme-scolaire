import { Button, Modal, Select, Stack, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { definirAmenagement } from '../api/endpoints'
import type { Candidat } from '../api/types'

const HANDICAPS = [
  { value: 'MOTEUR', label: 'Handicap moteur' },
  { value: 'VISUEL', label: 'Handicap visuel' },
  { value: 'AUDITIF', label: 'Handicap auditif' },
  { value: 'INTELLECTUEL', label: 'Handicap intellectuel' },
  { value: 'AUTRE', label: 'Autre' },
]

/** Déclare le type de handicap d'un candidat et l'aménagement accordé (tiers-temps, assistant, sujet adapté…). Un type vide retire la mention. */
export default function AmenagementCandidatModal({ candidat, onClose, onEnregistre }: { candidat: Candidat; onClose: () => void; onEnregistre: () => void }) {
  const [handicap, setHandicap] = useState<string | null>(candidat.handicap)
  const [amenagement, setAmenagement] = useState(candidat.amenagement ?? '')
  const enregistrer = useMutation({
    mutationFn: () => definirAmenagement(candidat.id, { handicap, amenagement: amenagement.trim() || null }),
    onSuccess: onEnregistre,
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  return (
    <Modal opened onClose={onClose} title={`Aménagement : ${candidat.nom}`}>
      <Stack>
        <Text size="sm" c="dimmed">Donnée sensible : elle n'est visible que des responsables des examens et sert à prévoir salles et surveillants adaptés.</Text>
        <Select label="Situation de handicap" placeholder="Aucune" data={HANDICAPS} value={handicap} onChange={setHandicap} clearable />
        <TextInput label="Aménagement accordé" value={amenagement} onChange={(e) => setAmenagement(e.currentTarget.value)} maxLength={200}
          placeholder="Tiers-temps, assistant, sujet en gros caractères…" disabled={handicap === null} />
        <Button loading={enregistrer.isPending} onClick={() => enregistrer.mutate()}>Enregistrer</Button>
      </Stack>
    </Modal>
  )
}
