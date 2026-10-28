import { useState } from 'react'
import { Alert, Button, Group, Modal, SegmentedControl, Select, Stack, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { genererFactures, listerClasses, listerNiveaux } from '../../api/endpoints'
import type { ResultatGeneration } from '../../api/types'
import { formaterMontant } from '../../utils/monnaie'

interface Props {
  etablissementId: number
  anneeScolaireId: number
  devise: string | null
  onFermer: () => void
}

type Portee = 'tous' | 'niveau' | 'classe'

/** Génération des factures à partir du barème. L'opération est idempotente : relancer ne crée pas de doublon. */
export default function GenererFacturesModal({ etablissementId, anneeScolaireId, devise, onFermer }: Props) {
  const queryClient = useQueryClient()
  const [portee, setPortee] = useState<Portee>('tous')
  const [niveauId, setNiveauId] = useState<string | null>(null)
  const [classeId, setClasseId] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [resultat, setResultat] = useState<ResultatGeneration | null>(null)

  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })
  const classes = useQuery({
    queryKey: ['classes-etab', etablissementId, anneeScolaireId],
    queryFn: () => listerClasses(etablissementId, anneeScolaireId),
  })

  const generation = useMutation({
    mutationFn: () =>
      genererFactures(etablissementId, {
        anneeScolaireId,
        niveauId: portee === 'niveau' && niveauId !== null ? Number(niveauId) : null,
        classeId: portee === 'classe' && classeId !== null ? Number(classeId) : null,
      }),
    onSuccess: (r) => {
      for (const cle of ['factures-etab', 'bilan-financier', 'factures-retard']) {
        void queryClient.invalidateQueries({ queryKey: [cle] })
      }
      notifications.show({ color: 'green', title: 'Factures générées', message: `${r.facturesCreees} créée(s)` })
      setResultat(r)
    },
    onError: (e) => setErreur(e.message),
  })

  const incomplet = (portee === 'niveau' && niveauId === null) || (portee === 'classe' && classeId === null)

  return (
    <Modal opened onClose={onFermer} title="Générer les factures">
      <Stack>
        {resultat === null ? (
          <>
            <Text size="sm" c="dimmed">
              Une facture est créée pour chaque élève inscrit et chaque frais actif qui le concerne. Les
              factures déjà émises sont ignorées : vous pouvez relancer sans risque de doublon.
            </Text>
            <SegmentedControl
              value={portee}
              onChange={(v) => setPortee(v as Portee)}
              data={[
                { value: 'tous', label: "Tout l'établissement" },
                { value: 'niveau', label: 'Un niveau' },
                { value: 'classe', label: 'Une classe' },
              ]}
            />
            {portee === 'niveau' && (
              <Select
                label="Niveau"
                searchable
                data={(niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))}
                value={niveauId}
                onChange={setNiveauId}
              />
            )}
            {portee === 'classe' && (
              <Select
                label="Classe"
                searchable
                data={(classes.data ?? []).map((c) => ({ value: String(c.id), label: c.nom }))}
                value={classeId}
                onChange={setClasseId}
              />
            )}
            {erreur !== null && (
              <Alert color="red" variant="light">
                {erreur}
              </Alert>
            )}
            <Group justify="flex-end">
              <Button variant="default" onClick={onFermer}>
                Annuler
              </Button>
              <Button
                disabled={incomplet}
                loading={generation.isPending}
                onClick={() => {
                  setErreur(null)
                  generation.mutate()
                }}
              >
                Générer
              </Button>
            </Group>
          </>
        ) : (
          <>
            <Alert color="green" variant="light">
              {resultat.facturesCreees} facture(s) créée(s) pour {resultat.elevesTraites} élève(s), soit{' '}
              {formaterMontant(resultat.montantTotalEmis, devise)}.
              {resultat.facturesIgnorees > 0 && ` ${resultat.facturesIgnorees} déjà existante(s), ignorée(s).`}
            </Alert>
            <Group justify="flex-end">
              <Button onClick={onFermer}>Fermer</Button>
            </Group>
          </>
        )}
      </Stack>
    </Modal>
  )
}
