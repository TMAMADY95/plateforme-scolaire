import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Stack, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { rapportEtablissementPdf, statistiquesEtablissement } from '../api/endpoints'
import type { LigneEtablissementStat } from '../api/types'
import { BadgeSecteur, BadgesCycles } from './BadgesEtablissement'
import PanneauStatistiques from './PanneauStatistiques'

/** Tous les chiffres d'un établissement (effectifs, cycles, niveaux, sexe, inscriptions) pour une année. */
export default function DetailEtablissementModal({
  etablissement,
  anneeScolaireId,
  onFermer,
}: {
  etablissement: LigneEtablissementStat
  anneeScolaireId: number
  onFermer: () => void
}) {
  const [erreurRapport, setErreurRapport] = useState<string | null>(null)
  const statistiques = useQuery({
    queryKey: ['statistiques-etablissement', etablissement.id, anneeScolaireId],
    queryFn: () => statistiquesEtablissement(etablissement.id, anneeScolaireId),
  })

  return (
    <Modal opened onClose={onFermer} size="xl" title={etablissement.nom}>
      <Stack>
        <Group gap="xs">
          <Text size="sm" c="dimmed">
            {etablissement.codeOfficiel}
            {etablissement.zone ? ` · ${etablissement.zone}` : ''}
          </Text>
          <BadgeSecteur secteur={etablissement.secteur} />
          <BadgesCycles cycles={etablissement.cycles} />
          <Button
            size="compact-sm"
            variant="light"
            ml="auto"
            onClick={() => {
              setErreurRapport(null)
              rapportEtablissementPdf(etablissement.id, anneeScolaireId).catch((e: Error) => setErreurRapport(e.message))
            }}
          >
            Rapport PDF
          </Button>
        </Group>
        {erreurRapport && (
          <Alert color="red" variant="light">
            {erreurRapport}
          </Alert>
        )}
        {statistiques.isPending && <Loader />}
        {statistiques.isError && (
          <Alert color="red" variant="light">
            {statistiques.error.message}
          </Alert>
        )}
        {statistiques.data && <PanneauStatistiques statistiques={statistiques.data} />}
      </Stack>
    </Modal>
  )
}
