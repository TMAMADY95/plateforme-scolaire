import { useEffect, useMemo, useState } from 'react'
import { Alert, Badge, Button, Card, Group, Loader, NumberInput, Select, Stack, Switch, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  creerCentreExamen,
  criteresCentres,
  centresExamen,
  etablissementsCentres,
  listerExamens,
  modifierCriteresCentres,
  supprimerCentreExamen,
} from '../api/endpoints'
import type { CriteresCentre } from '../api/types'
import EnteteSection from './EnteteSection'

const nombre = (n: number) => new Intl.NumberFormat('fr-FR').format(n)

/** Conditions pour être centre d'examen, établissements éligibles et désignation des centres d'un examen. */
export default function CentresExamen({ anneeId }: { anneeId: number }) {
  return (
    <Stack gap="lg">
      <EnteteSection
        titre="Centres d'examen"
        description="Fixez les conditions pour qu'un établissement puisse accueillir un examen, puis désignez les centres."
      />
      <Conditions />
      <Etablissements anneeId={anneeId} />
    </Stack>
  )
}

function Conditions() {
  const queryClient = useQueryClient()
  const criteres = useQuery({ queryKey: ['criteres-centres'], queryFn: criteresCentres })
  const [valeurs, setValeurs] = useState<CriteresCentre | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  useEffect(() => {
    if (criteres.data) setValeurs(criteres.data)
  }, [criteres.data])

  const enregistrement = useMutation({
    mutationFn: () => modifierCriteresCentres(valeurs as CriteresCentre),
    onSuccess: () => {
      setErreur(null)
      void queryClient.invalidateQueries({ queryKey: ['criteres-centres'] })
      void queryClient.invalidateQueries({ queryKey: ['etablissements-centres'] })
      notifications.show({ color: 'green', title: 'Conditions enregistrées', message: 'Elles s\'appliquent aux prochaines désignations.' })
    },
    onError: (e) => setErreur(e.message),
  })

  if (!valeurs) return <Loader />
  const modifie = JSON.stringify(valeurs) !== JSON.stringify(criteres.data)

  return (
    <Card withBorder padding="lg">
      <Stack gap="md">
        <div>
          <Text fw={600}>Conditions pour être centre d'examen</Text>
          <Text size="sm" c="dimmed">
            Un établissement doit être actif et remplir toutes les conditions ci-dessous. Les salles et leurs places se renseignent dans la fiche de chaque établissement.
          </Text>
        </div>
        <Group align="flex-end">
          <Select
            label="Secteur admis"
            data={[
              { value: 'TOUS', label: 'Public et privé' },
              { value: 'PUBLIC', label: 'Public seulement' },
            ]}
            value={valeurs.secteurAdmis}
            onChange={(v) => setValeurs({ ...valeurs, secteurAdmis: v ?? 'TOUS' })}
            allowDeselect={false}
            w={200}
          />
          <NumberInput
            label="Salles minimum"
            min={0}
            allowDecimal={false}
            value={valeurs.sallesMinimum}
            onChange={(v) => setValeurs({ ...valeurs, sallesMinimum: Number(v) || 0 })}
            w={150}
          />
          <NumberInput
            label="Places d'accueil minimum"
            description="Somme des places des salles"
            min={0}
            allowDecimal={false}
            value={valeurs.capaciteMinimum}
            onChange={(v) => setValeurs({ ...valeurs, capaciteMinimum: Number(v) || 0 })}
            w={200}
          />
          <Switch
            label="Autorisation d'ouverture exigée"
            checked={valeurs.autorisationRequise}
            onChange={(e) => setValeurs({ ...valeurs, autorisationRequise: e.currentTarget.checked })}
            mb={6}
          />
          <Button disabled={!modifie} loading={enregistrement.isPending} onClick={() => enregistrement.mutate()}>
            Enregistrer
          </Button>
        </Group>
        {erreur && (
          <Alert color="red" variant="light">
            {erreur}
          </Alert>
        )}
      </Stack>
    </Card>
  )
}

function Etablissements({ anneeId }: { anneeId: number }) {
  const queryClient = useQueryClient()
  const examens = useQuery({ queryKey: ['examens', anneeId], queryFn: () => listerExamens(anneeId) })
  const liste = useQuery({ queryKey: ['etablissements-centres'], queryFn: etablissementsCentres })
  const [examenChoisi, setExamenChoisi] = useState<string | null>(null)
  const [recherche, setRecherche] = useState('')
  const [zone, setZone] = useState<string | null>(null)
  const [seulementEligibles, setSeulementEligibles] = useState(true)
  const [erreur, setErreur] = useState<string | null>(null)

  const examenId = examenChoisi ?? (examens.data && examens.data.length > 0 ? String(examens.data[0].id) : null)
  const centres = useQuery({
    queryKey: ['centres-examen', examenId === null ? null : Number(examenId)],
    queryFn: () => centresExamen(Number(examenId)),
    enabled: examenId !== null,
  })
  const centreParEtablissement = useMemo(() => new Map((centres.data ?? []).map((c) => [c.etablissementId, c.id])), [centres.data])

  const rafraichir = () => {
    void queryClient.invalidateQueries({ queryKey: ['centres-examen'] })
    void queryClient.invalidateQueries({ queryKey: ['preparation-examen'] })
    void queryClient.invalidateQueries({ queryKey: ['examens-etablissement'] })
  }
  const designation = useMutation({
    mutationFn: (etablissementId: number) =>
      creerCentreExamen(Number(examenId), { nom: null, adresse: null, zoneId: null, dateDebut: null, etablissementId }),
    onSuccess: () => {
      setErreur(null)
      rafraichir()
    },
    onError: (e) => setErreur(e.message),
  })
  const retrait = useMutation({
    mutationFn: (centreId: number) => supprimerCentreExamen(centreId),
    onSuccess: () => {
      setErreur(null)
      rafraichir()
    },
    onError: (e) => setErreur(e.message),
  })

  const zones = useMemo(
    () => [...new Set((liste.data ?? []).map((e) => e.zone).filter((z): z is string => z !== null))].sort((a, b) => a.localeCompare(b, 'fr')),
    [liste.data],
  )
  const visibles = (liste.data ?? []).filter((e) => {
    if (seulementEligibles && !e.eligible) return false
    if (zone !== null && e.zone !== zone) return false
    const q = recherche.trim().toLowerCase()
    return q === '' || e.nom.toLowerCase().includes(q) || e.codeOfficiel.toLowerCase().includes(q)
  })
  const nbEligibles = (liste.data ?? []).filter((e) => e.eligible).length

  return (
    <Card withBorder padding="lg">
      <Stack gap="md">
        <Group justify="space-between" align="flex-end">
          <div>
            <Text fw={600}>Établissements</Text>
            <Text size="sm" c="dimmed">
              {liste.data ? `${nombre(nbEligibles)} éligible(s) sur ${nombre(liste.data.length)} établissement(s) actif(s).` : 'Chargement…'}
            </Text>
          </div>
          <Select
            label="Désigner les centres de l'examen"
            placeholder={examens.data && examens.data.length === 0 ? 'Aucun examen planifié' : 'Choisir un examen'}
            data={(examens.data ?? []).map((e) => ({ value: String(e.id), label: e.libelle }))}
            value={examenId}
            onChange={setExamenChoisi}
            allowDeselect={false}
            w={280}
          />
        </Group>
        <Group align="flex-end">
          <TextInput label="Rechercher" placeholder="Nom ou code" value={recherche} onChange={(e) => setRecherche(e.currentTarget.value)} w={240} />
          <Select label="Zone" placeholder="Toutes" clearable searchable data={zones} value={zone} onChange={setZone} w={220} />
          <Switch label="Éligibles seulement" checked={seulementEligibles} onChange={(e) => setSeulementEligibles(e.currentTarget.checked)} mb={6} />
        </Group>
        {erreur && (
          <Alert color="red" variant="light">
            {erreur}
          </Alert>
        )}
        {liste.isPending ? (
          <Loader />
        ) : visibles.length === 0 ? (
          <Text c="dimmed">Aucun établissement ne correspond.</Text>
        ) : (
          <Table.ScrollContainer minWidth={820}>
            <Table highlightOnHover verticalSpacing="xs">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Établissement</Table.Th>
                  <Table.Th>Secteur</Table.Th>
                  <Table.Th ta="right">Salles</Table.Th>
                  <Table.Th ta="right">Places</Table.Th>
                  <Table.Th>Éligibilité</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {visibles.map((e) => {
                  const centreId = centreParEtablissement.get(e.id)
                  return (
                    <Table.Tr key={e.id}>
                      <Table.Td>
                        <Text fw={600}>{e.nom}</Text>
                        <Text size="xs" c="dimmed">
                          {e.codeOfficiel}
                          {e.zone ? ` · ${e.zone}` : ''}
                          {e.typeEtablissement ? ` · ${e.typeEtablissement}` : ''}
                        </Text>
                      </Table.Td>
                      <Table.Td>{e.secteur === 'PUBLIC' ? 'Public' : 'Privé'}</Table.Td>
                      <Table.Td ta="right">{nombre(e.salles)}</Table.Td>
                      <Table.Td ta="right">{nombre(e.capacite)}</Table.Td>
                      <Table.Td>
                        {e.eligible ? (
                          <Badge color="green" variant="light">
                            Éligible
                          </Badge>
                        ) : (
                          <Stack gap={2}>
                            <Badge color="red" variant="light" w="fit-content">
                              Non éligible
                            </Badge>
                            {e.motifs.map((m) => (
                              <Text key={m} size="xs" c="dimmed">
                                {m}
                              </Text>
                            ))}
                          </Stack>
                        )}
                      </Table.Td>
                      <Table.Td ta="right">
                        {examenId !== null &&
                          (centreId !== undefined ? (
                            <Button size="compact-xs" color="red" variant="subtle" loading={retrait.isPending && retrait.variables === centreId} onClick={() => retrait.mutate(centreId)}>
                              Retirer des centres
                            </Button>
                          ) : (
                            <Button
                              size="compact-xs"
                              variant="light"
                              disabled={!e.eligible}
                              loading={designation.isPending && designation.variables === e.id}
                              onClick={() => designation.mutate(e.id)}
                            >
                              Désigner comme centre
                            </Button>
                          ))}
                      </Table.Td>
                    </Table.Tr>
                  )
                })}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
      </Stack>
    </Card>
  )
}
