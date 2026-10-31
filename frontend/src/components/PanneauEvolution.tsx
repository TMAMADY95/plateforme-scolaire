import type { ReactNode } from 'react'
import { Alert, Card, Loader, SimpleGrid, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import type { Evolution } from '../api/types'
import { Barres, Courbe } from './graphiques/Graphiques'

/** Compare les années scolaires : élèves inscrits, classes et taux de réussite aux examens nationaux. */
export default function PanneauEvolution({
  cle,
  charger,
  zoneId,
}: {
  cle: string
  charger: (zoneId?: number) => Promise<Evolution>
  zoneId?: number
}) {
  const evolution = useQuery({ queryKey: [`${cle}-evolution`, zoneId ?? null], queryFn: () => charger(zoneId) })

  if (evolution.isPending) return <Loader />
  if (evolution.isError)
    return (
      <Alert color="red" variant="light">
        {evolution.error.message}
      </Alert>
    )
  const annees = evolution.data.annees
  if (annees.length < 2)
    return (
      <Alert color="blue" variant="light">
        La comparaison d'une année à l'autre apparaîtra dès qu'il y aura au moins deux années scolaires.
      </Alert>
    )
  const avecExamens = annees.filter((a) => a.tauxExamens !== null)

  return (
    <SimpleGrid cols={{ base: 1, md: 3 }}>
      <Cadre titre="Élèves inscrits par année">
        <Courbe points={annees.map((a) => ({ libelle: a.libelle, valeur: a.eleves }))} />
      </Cadre>
      <Cadre titre="Classes par année">
        <Barres points={annees.map((a) => ({ libelle: a.libelle, valeur: a.classes }))} />
      </Cadre>
      <Cadre titre="Réussite aux examens (%)">
        <Courbe
          points={avecExamens.map((a) => ({ libelle: a.libelle, valeur: a.tauxExamens ?? 0 }))}
          vide="Aucun résultat d'examen saisi."
        />
      </Cadre>
    </SimpleGrid>
  )
}

function Cadre({ titre, children }: { titre: string; children: ReactNode }) {
  return (
    <Card withBorder padding="lg">
      <Text fw={600} mb="sm">
        {titre}
      </Text>
      {children}
    </Card>
  )
}
