import { useState } from 'react'
import { Alert, Button, Checkbox, Group, Loader, Stack, Table, Text, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { enregistrerAppel, obtenirFeuille } from '../../api/endpoints'
import type { FeuilleAppel, LignePresenceRequete, StatutPresence } from '../../api/types'
import { aujourdhui } from '../../utils/dates'

interface Saisie {
  statut: StatutPresence | null
  motif: string
  justifie: boolean
}

const CHOIX: { statut: StatutPresence; libelle: string; couleur: string }[] = [
  { statut: 'PRESENT', libelle: 'Présent', couleur: 'green' },
  { statut: 'ABSENT', libelle: 'Absent', couleur: 'red' },
  { statut: 'RETARD', libelle: 'Retard', couleur: 'orange' },
]

export default function OngletAppel({ classeId }: { classeId: number }) {
  const [date, setDate] = useState(aujourdhui())

  const feuille = useQuery({
    queryKey: ['feuille', classeId, date],
    queryFn: () => obtenirFeuille(classeId, date),
    enabled: date !== '',
  })

  return (
    <Stack>
      <TextInput
        label="Date de l'appel"
        type="date"
        value={date}
        max={aujourdhui()}
        onChange={(e) => setDate(e.currentTarget.value)}
        w={200}
      />
      {feuille.isPending && date !== '' && <Loader />}
      {feuille.isError && (
        <Alert color="red" variant="light">
          {feuille.error.message}
        </Alert>
      )}
      {feuille.data && (
        // La clé recrée le formulaire quand les données du serveur changent (changement de date, enregistrement)
        <FeuilleEditable key={`${date}-${feuille.dataUpdatedAt}`} classeId={classeId} date={date} feuille={feuille.data} />
      )}
    </Stack>
  )
}

function FeuilleEditable({ classeId, date, feuille }: { classeId: number; date: string; feuille: FeuilleAppel }) {
  const queryClient = useQueryClient()
  const [saisies, setSaisies] = useState<Record<number, Saisie>>(
    () =>
      Object.fromEntries(
        feuille.lignes.map((l) => [l.inscriptionId, { statut: l.statut, motif: l.motif ?? '', justifie: l.justifie }]),
      ) as Record<number, Saisie>,
  )

  function modifier(inscriptionId: number, changements: Partial<Saisie>) {
    setSaisies((actuelles) => ({ ...actuelles, [inscriptionId]: { ...actuelles[inscriptionId], ...changements } }))
  }

  function toutPresent() {
    setSaisies((actuelles) => {
      const suivantes = { ...actuelles }
      for (const ligne of feuille.lignes) {
        if (suivantes[ligne.inscriptionId].statut === null) {
          suivantes[ligne.inscriptionId] = { ...suivantes[ligne.inscriptionId], statut: 'PRESENT' }
        }
      }
      return suivantes
    })
  }

  const enregistrement = useMutation({
    mutationFn: (lignes: LignePresenceRequete[]) => enregistrerAppel(classeId, date, lignes),
    onSuccess: () => {
      notifications.show({ color: 'green', title: 'Appel enregistré', message: "Les présences ont été sauvegardées." })
      void queryClient.invalidateQueries({ queryKey: ['feuille', classeId, date] })
    },
    onError: (e) => notifications.show({ color: 'red', title: 'Enregistrement impossible', message: e.message }),
  })

  function enregistrer() {
    const lignes: LignePresenceRequete[] = []
    for (const ligne of feuille.lignes) {
      const saisie = saisies[ligne.inscriptionId]
      if (saisie.statut === null) continue
      lignes.push({
        inscriptionId: ligne.inscriptionId,
        statut: saisie.statut,
        motif: saisie.motif.trim() || undefined,
        justifie: saisie.justifie,
      })
    }
    if (lignes.length === 0) {
      notifications.show({ color: 'orange', message: "Indiquez au moins un statut avant d'enregistrer." })
      return
    }
    enregistrement.mutate(lignes)
  }

  if (feuille.lignes.length === 0) {
    return <Text c="dimmed">Aucun élève n'est encore inscrit dans cette classe.</Text>
  }

  const valeurs = Object.values(saisies)
  const compte = (statut: StatutPresence) => valeurs.filter((s) => s.statut === statut).length
  const nonFaits = valeurs.filter((s) => s.statut === null).length

  return (
    <Stack>
      <Group justify="space-between">
        <Text size="sm" c="dimmed">
          {compte('PRESENT')} présent(s) · {compte('ABSENT')} absent(s) · {compte('RETARD')} en retard · {nonFaits} non
          renseigné(s)
        </Text>
        <Group>
          <Button variant="default" onClick={toutPresent} disabled={nonFaits === 0}>
            Tout le monde présent
          </Button>
          <Button onClick={enregistrer} loading={enregistrement.isPending}>
            Enregistrer l'appel
          </Button>
        </Group>
      </Group>

      <Table.ScrollContainer minWidth={720}>
        <Table striped withTableBorder verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Élève</Table.Th>
              <Table.Th>Statut</Table.Th>
              <Table.Th>Motif</Table.Th>
              <Table.Th>Justifié</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {feuille.lignes.map((ligne) => {
              const saisie = saisies[ligne.inscriptionId]
              const nonPresent = saisie.statut === 'ABSENT' || saisie.statut === 'RETARD'
              return (
                <Table.Tr key={ligne.inscriptionId}>
                  <Table.Td>
                    {ligne.eleve.nom} {ligne.eleve.prenom}
                  </Table.Td>
                  <Table.Td>
                    <Group gap={4} wrap="nowrap">
                      {CHOIX.map((choix) => (
                        <Button
                          key={choix.statut}
                          size="compact-sm"
                          color={choix.couleur}
                          variant={saisie.statut === choix.statut ? 'filled' : 'default'}
                          onClick={() => modifier(ligne.inscriptionId, { statut: choix.statut })}
                        >
                          {choix.libelle}
                        </Button>
                      ))}
                    </Group>
                  </Table.Td>
                  <Table.Td>
                    <TextInput
                      size="xs"
                      placeholder={nonPresent ? 'Motif (facultatif)' : ''}
                      disabled={!nonPresent}
                      value={saisie.motif}
                      onChange={(e) => modifier(ligne.inscriptionId, { motif: e.currentTarget.value })}
                    />
                  </Table.Td>
                  <Table.Td>
                    <Checkbox
                      disabled={!nonPresent}
                      checked={nonPresent && saisie.justifie}
                      onChange={(e) => modifier(ligne.inscriptionId, { justifie: e.currentTarget.checked })}
                      aria-label="Absence justifiée"
                    />
                  </Table.Td>
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Stack>
  )
}
