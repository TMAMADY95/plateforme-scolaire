import { useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, Stack, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { mesCentres } from '../api/endpoints'
import type { MonCentre } from '../api/types'
import DeroulementCentreModal from '../components/DeroulementCentreModal'
import IncidentsCentreModal from '../components/IncidentsCentreModal'
import PersonnelCentreModal from '../components/PersonnelCentreModal'
import PreparationCentreModal from '../components/PreparationCentreModal'

type Fenetre = { type: 'personnel' | 'incidents' | 'epreuves' | 'preparation'; centre: MonCentre }

/** Espace du chef de centre : les centres qui lui sont confiés, avec le personnel, les incidents et le déroulement des épreuves. */
export default function PageMonCentre() {
  const centres = useQuery({ queryKey: ['mes-centres'], queryFn: mesCentres })
  const [fenetre, setFenetre] = useState<Fenetre | null>(null)
  const cible = fenetre ? { id: fenetre.centre.centreId, nom: fenetre.centre.centre } : null

  return (
    <Stack>
      <div>
        <Title order={2}>Mon centre d'examen</Title>
        <Text c="dimmed">Personnel de surveillance, incidents et rapports de surveillance de votre centre.</Text>
      </div>
      {centres.isPending && <Loader />}
      {centres.isError && <Alert color="red" variant="light">{centres.error.message}</Alert>}
      {centres.data && centres.data.length === 0 && (
        <Alert color="blue" variant="light">Aucun centre ne vous est confié pour le moment. Rapprochez-vous de la direction préfectorale ou communale.</Alert>
      )}
      {centres.data?.map((c) => (
        <Card key={`${c.examenId}-${c.centreId}`} withBorder padding="md">
          <Group justify="space-between" align="flex-start">
            <div>
              <Text fw={600}>{c.centre}</Text>
              <Text size="sm" c="dimmed">{c.examen}{c.zone ? ` · ${c.zone}` : ''}</Text>
            </div>
            {c.statutResultats === 'PROCLAME' && <Badge color="green" variant="light">Résultats proclamés</Badge>}
          </Group>
          <Group mt="sm" gap="xs">
            <Button size="xs" variant="light" onClick={() => setFenetre({ type: 'preparation', centre: c })}>Préparation du centre</Button>
            <Button size="xs" variant="light" onClick={() => setFenetre({ type: 'personnel', centre: c })}>Personnel</Button>
            <Button size="xs" variant="light" onClick={() => setFenetre({ type: 'epreuves', centre: c })}>Épreuves et rapports</Button>
            <Button size="xs" variant="light" onClick={() => setFenetre({ type: 'incidents', centre: c })}>Incidents</Button>
          </Group>
        </Card>
      ))}
      {fenetre && cible && fenetre.type === 'preparation' && <PreparationCentreModal examenId={fenetre.centre.examenId} centre={cible} onFermer={() => setFenetre(null)} />}
      {fenetre && cible && fenetre.type === 'personnel' && <PersonnelCentreModal examenId={fenetre.centre.examenId} centre={cible} onFermer={() => setFenetre(null)} />}
      {fenetre && cible && fenetre.type === 'incidents' && <IncidentsCentreModal examenId={fenetre.centre.examenId} centre={cible} onFermer={() => setFenetre(null)} />}
      {fenetre && cible && fenetre.type === 'epreuves' && <DeroulementCentreModal examenId={fenetre.centre.examenId} centre={cible} onFermer={() => setFenetre(null)} />}
    </Stack>
  )
}
