import { useState } from 'react'
import { Alert, Button, Group, Modal, NumberInput, SimpleGrid, Stack, Text } from '@mantine/core'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { saisirResultatExamen } from '../api/endpoints'
import type { LigneExamen, SerieBac } from '../api/types'

const SERIES: { valeur: SerieBac; libelle: string }[] = [
  { valeur: 'SCIENCES_MATHEMATIQUES', libelle: 'Sciences mathématiques' },
  { valeur: 'SCIENCES_SOCIALES', libelle: 'Sciences sociales' },
  { valeur: 'SCIENCES_EXPERIMENTALES', libelle: 'Sciences expérimentales' },
]

type Valeur = number | string
const num = (v: Valeur) => (v === '' ? null : Number(v))

/** Absents, éliminés pour fraude, filles et séries du baccalauréat : informations facultatives des statistiques. */
export default function DetailResultatsModal({ examenId, ligne, onFermer }: { examenId: number; ligne: LigneExamen; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const [absents, setAbsents] = useState<Valeur>(ligne.absents ?? 0)
  const [elimines, setElimines] = useState<Valeur>(ligne.elimines ?? 0)
  const [fillesP, setFillesP] = useState<Valeur>(ligne.presentesFilles ?? '')
  const [fillesA, setFillesA] = useState<Valeur>(ligne.admisFilles ?? '')
  const [series, setSeries] = useState<Record<SerieBac, { p: Valeur; a: Valeur }>>(() => {
    const base = {} as Record<SerieBac, { p: Valeur; a: Valeur }>
    for (const s of SERIES) {
      const existante = ligne.series.find((x) => x.serie === s.valeur)
      base[s.valeur] = { p: existante?.presentes ?? '', a: existante?.admis ?? '' }
    }
    return base
  })
  const [erreur, setErreur] = useState<string | null>(null)

  const enregistrement = useMutation({
    mutationFn: () => {
      const saisies = SERIES.filter((s) => series[s.valeur].p !== '' || series[s.valeur].a !== '')
      return saisirResultatExamen(examenId, ligne.id, {
        presentes: ligne.presentes ?? 0,
        admis: ligne.admis ?? 0,
        absents: num(absents) ?? 0,
        elimines: num(elimines) ?? 0,
        presentesFilles: num(fillesP),
        admisFilles: num(fillesA),
        series: saisies.map((s) => ({ serie: s.valeur, presentes: num(series[s.valeur].p) ?? 0, admis: num(series[s.valeur].a) ?? 0 })),
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rapport-examen'] })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })

  return (
    <Modal opened onClose={onFermer} title={`Détail des résultats : ${ligne.nom}`} size="lg">
      <Stack gap="md">
        {ligne.presentes === null && (
          <Alert color="orange" variant="light">
            Enregistrez d'abord les présentés et les admis de l'établissement.
          </Alert>
        )}
        <SimpleGrid cols={2}>
          <NumberInput label="Absents" min={0} allowDecimal={false} value={absents} onChange={setAbsents} />
          <NumberInput label="Éliminés pour fraude" min={0} allowDecimal={false} value={elimines} onChange={setElimines} />
          <NumberInput label="Filles présentes" min={0} allowDecimal={false} value={fillesP} onChange={setFillesP} />
          <NumberInput label="Filles admises" min={0} allowDecimal={false} value={fillesA} onChange={setFillesA} />
        </SimpleGrid>
        <Text fw={600}>Baccalauréat : résultats par série</Text>
        <Text size="xs" c="dimmed">À remplir seulement pour le baccalauréat ; le total des séries doit égaler les présentés et les admis.</Text>
        {SERIES.map((s) => (
          <Group key={s.valeur} grow align="flex-end">
            <Text size="sm">{s.libelle}</Text>
            <NumberInput label="Présentés" min={0} allowDecimal={false} value={series[s.valeur].p}
              onChange={(v) => setSeries((x) => ({ ...x, [s.valeur]: { ...x[s.valeur], p: v } }))} />
            <NumberInput label="Admis" min={0} allowDecimal={false} value={series[s.valeur].a}
              onChange={(v) => setSeries((x) => ({ ...x, [s.valeur]: { ...x[s.valeur], a: v } }))} />
          </Group>
        ))}
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>Annuler</Button>
          <Button loading={enregistrement.isPending} disabled={ligne.presentes === null} onClick={() => enregistrement.mutate()}>Enregistrer</Button>
        </Group>
      </Stack>
    </Modal>
  )
}
