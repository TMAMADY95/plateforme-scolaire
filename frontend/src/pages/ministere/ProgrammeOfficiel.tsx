import { useState } from 'react'
import { Alert, Button, Card, Group, NumberInput, Select, Stack, Table, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  copierProgramme,
  creerLigneProgramme,
  importerProgrammeCoc,
  listerAnnees,
  listerMatieres,
  listerNiveaux,
  listerProgramme,
  listerTypesEtablissement,
  modifierLigneProgramme,
  supprimerLigneProgramme,
} from '../../api/endpoints'
import { SERIES_LYCEE, libelleSerie } from '../../api/types'
import type { LigneProgramme } from '../../api/types'

const heures = (h: number | null) => (h === null ? '—' : `${String(h).replace('.', ',')} h`)

function LigneEditable({ ligne, onChange }: { ligne: LigneProgramme; onChange: () => void }) {
  const [h, setH] = useState<number | string>(ligne.heuresHebdo ?? '')
  const [c, setC] = useState<number | string>(ligne.coefficient ?? '')
  const modifie = h !== (ligne.heuresHebdo ?? '') || c !== (ligne.coefficient ?? '')
  const enregistrer = useMutation({
    mutationFn: () =>
      modifierLigneProgramme(ligne.id, {
        heuresHebdo: h === '' ? null : Number(h),
        coefficient: c === '' ? null : Number(c),
        note: ligne.note,
      }),
    onSuccess: onChange,
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const supprimer = useMutation({
    mutationFn: () => supprimerLigneProgramme(ligne.id),
    onSuccess: onChange,
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  return (
    <Table.Tr>
      <Table.Td>{ligne.cycle ?? '—'}</Table.Td>
      <Table.Td>{ligne.niveau}</Table.Td>
      <Table.Td>{ligne.typeEtablissement ?? 'Tous types'}</Table.Td>
      <Table.Td>{libelleSerie(ligne.serie) === '—' ? 'Toutes' : libelleSerie(ligne.serie)}</Table.Td>
      <Table.Td>
        <Text size="sm">{ligne.matiere}</Text>
        {ligne.note && <Text size="xs" c="dimmed">{ligne.note}</Text>}
      </Table.Td>
      <Table.Td w={110}>
        <NumberInput aria-label={`Heures de ${ligne.matiere}`} size="xs" min={0} max={99} step={0.25} decimalScale={2} decimalSeparator="," value={h} onChange={setH} />
      </Table.Td>
      <Table.Td w={110}>
        <NumberInput aria-label={`Coefficient de ${ligne.matiere}`} size="xs" min={0.01} max={99} decimalScale={2} decimalSeparator="," value={c} onChange={setC} />
      </Table.Td>
      <Table.Td>
        <Group gap="xs" wrap="nowrap">
          <Button size="compact-xs" disabled={!modifie} loading={enregistrer.isPending} onClick={() => enregistrer.mutate()}>
            Enregistrer
          </Button>
          <Button size="compact-xs" variant="light" color="red" loading={supprimer.isPending} onClick={() => supprimer.mutate()}>
            Retirer
          </Button>
        </Group>
      </Table.Td>
    </Table.Tr>
  )
}

/**
 * Programme officiel : matières, horaires hebdomadaires et coefficients de chaque niveau (et série du lycée), année par année.
 * Les horaires évoluent : tout est modifiable ; la grille du COC 2023 est un point de départ importable.
 */
export default function ProgrammeOfficiel() {
  const queryClient = useQueryClient()
  const annees = useQuery({ queryKey: ['annees'], queryFn: listerAnnees })
  const niveaux = useQuery({ queryKey: ['niveaux'], queryFn: listerNiveaux })
  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres })
  const [anneeId, setAnneeId] = useState<string | null>(null)
  const [niveauId, setNiveauId] = useState<string | null>(null)
  const [typeFiltre, setTypeFiltre] = useState<string | null>(null)
  const [cycleFiltre, setCycleFiltre] = useState<string | null>(null)
  const types = useQuery({ queryKey: ['types-etablissement'], queryFn: listerTypesEtablissement })
  const [source, setSource] = useState<string | null>(null)
  const anneeCourante = anneeId ?? (annees.data && annees.data.length > 0 ? String(annees.data[0].id) : null)

  const lignes = useQuery({
    queryKey: ['programme', anneeCourante, niveauId],
    queryFn: () => listerProgramme(Number(anneeCourante), niveauId === null ? undefined : Number(niveauId)),
    enabled: anneeCourante !== null,
  })
  // Filtres de lecture : le programme dépend du cycle, du niveau, de la série et du type d'établissement.
  const cycles = [...new Map((lignes.data ?? []).filter((l) => l.cycleId !== null).map((l): [string, string] => [String(l.cycleId), l.cycle ?? ''])).entries()]
  const affichees = (lignes.data ?? []).filter(
    (l) =>
      (cycleFiltre === null || String(l.cycleId) === cycleFiltre) &&
      (typeFiltre === null || (typeFiltre === '0' ? l.typeEtablissementId === null : l.typeEtablissementId === null || String(l.typeEtablissementId) === typeFiltre)),
  )
  const rafraichir = () => void queryClient.invalidateQueries({ queryKey: ['programme'] })
  const bilan = (titre: string) => (r: { lignesCreees: number; lignesIgnorees: number; matieresCreees: number; avertissements: string[] }) => {
    rafraichir()
    void queryClient.invalidateQueries({ queryKey: ['matieres'] })
    notifications.show({
      color: r.avertissements.length > 0 ? 'orange' : 'green',
      title: titre,
      message: `${r.lignesCreees} ligne(s) ajoutée(s), ${r.lignesIgnorees} déjà présente(s) ou ignorée(s), ${r.matieresCreees} matière(s) créée(s).${r.avertissements.length > 0 ? ' ' + r.avertissements.join(' ; ') : ''}`,
    })
  }
  const importer = useMutation({
    mutationFn: () => importerProgrammeCoc(Number(anneeCourante)),
    onSuccess: bilan('Grille du COC 2023 importée'),
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })
  const copier = useMutation({
    mutationFn: () => copierProgramme(Number(source), Number(anneeCourante)),
    onSuccess: bilan('Programme copié'),
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })

  const [nMatiere, setNMatiere] = useState<string | null>(null)
  const [nNiveau, setNNiveau] = useState<string | null>(null)
  const [nSerie, setNSerie] = useState<string | null>(null)
  const [nType, setNType] = useState<string | null>(null)
  const [nHeures, setNHeures] = useState<number | string>('')
  const [nCoef, setNCoef] = useState<number | string>('')
  const ajouter = useMutation({
    mutationFn: () =>
      creerLigneProgramme({
        anneeScolaireId: Number(anneeCourante),
        niveauId: Number(nNiveau),
        serie: nSerie,
        matiereId: Number(nMatiere),
        heuresHebdo: nHeures === '' ? null : Number(nHeures),
        coefficient: nCoef === '' ? null : Number(nCoef),
        note: null,
        typeEtablissementId: nType === null ? null : Number(nType),
      }),
    onSuccess: () => {
      rafraichir()
      setNMatiere(null)
      setNHeures('')
      setNCoef('')
    },
    onError: (e) => notifications.show({ color: 'red', message: e.message }),
  })

  // Total d'heures par niveau et série affiché en tête de la liste.
  const totaux = new Map<string, number>()
  for (const l of affichees) {
    const cle = `${l.niveau} · ${libelleSerie(l.serie) === '—' ? 'toutes séries' : libelleSerie(l.serie)}`
    totaux.set(cle, (totaux.get(cle) ?? 0) + (l.heuresHebdo ?? 0))
  }

  return (
    <Card withBorder padding="lg">
      <Stack>
        <div>
          <Title order={4}>Programme officiel</Title>
          <Text c="dimmed" size="sm">
            Matières, horaires hebdomadaires et coefficients par cycle, niveau, type d'établissement (classique, franco-arabe, technique…) et série au lycée, année par année. Une ligne propre à un type remplace la ligne commune pour la même matière ; elle peut aussi ajouter une matière. Ces valeurs
            évoluent : modifiez-les à tout moment. Elles servent à préremplir les coefficients des classes.
          </Text>
        </div>
        <Group align="flex-end">
          <Select label="Année scolaire" data={(annees.data ?? []).map((a) => ({ value: String(a.id), label: a.libelle }))} value={anneeCourante} onChange={setAnneeId} allowDeselect={false} w={180} />
          <Select label="Niveau" placeholder="Tous" clearable data={(niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))} value={niveauId} onChange={setNiveauId} w={180} />
          <Select label="Cycle" placeholder="Tous" clearable data={cycles.map(([value, label]) => ({ value, label }))} value={cycleFiltre} onChange={setCycleFiltre} w={160} />
          <Select
            label="Type d'établissement"
            placeholder="Tous"
            clearable
            data={[{ value: '0', label: 'Communes à tous les types' }, ...(types.data ?? []).map((t) => ({ value: String(t.id), label: t.libelle }))]}
            value={typeFiltre}
            onChange={setTypeFiltre}
            w={230}
          />
          <Button variant="light" loading={importer.isPending} disabled={anneeCourante === null} onClick={() => importer.mutate()}>
            Importer la grille du COC 2023
          </Button>
        </Group>
        <Group align="flex-end">
          <Select label="Copier depuis l'année" data={(annees.data ?? []).filter((a) => String(a.id) !== anneeCourante).map((a) => ({ value: String(a.id), label: a.libelle }))} value={source} onChange={setSource} clearable w={220} />
          <Button variant="light" disabled={source === null || anneeCourante === null} loading={copier.isPending} onClick={() => copier.mutate()}>
            Copier vers {annees.data?.find((a) => String(a.id) === anneeCourante)?.libelle ?? 'cette année'}
          </Button>
        </Group>
        <Alert color="blue" variant="light">
          L'import ne remplace aucune ligne existante. La grille du COC 2023 est documentaire (horaires à stabiliser par le ministère) : seules les
          grilles du primaire, du collège et de la 11e année y figurent ; les 12e et Terminale sont à saisir ici. Pour la 11e, la colonne littéraire est
          appliquée aux sciences sociales et la colonne scientifique aux sciences mathématiques et expérimentales : à confirmer.
        </Alert>
        {totaux.size > 0 && (
          <Text size="sm">
            {[...totaux.entries()].map(([k, v]) => `${k} : ${heures(v)}`).join('  ·  ')}
          </Text>
        )}
        <Table.ScrollContainer minWidth={760}>
          <Table striped withTableBorder verticalSpacing="xs">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Cycle</Table.Th>
                <Table.Th>Niveau</Table.Th>
                <Table.Th>Type d'établissement</Table.Th>
                <Table.Th>Série</Table.Th>
                <Table.Th>Matière</Table.Th>
                <Table.Th>Heures / sem.</Table.Th>
                <Table.Th>Coefficient</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {affichees.length === 0 && (
                <Table.Tr><Table.Td colSpan={8}><Text c="dimmed">Aucune ligne pour cette sélection.</Text></Table.Td></Table.Tr>
              )}
              {affichees.map((l) => (
                <LigneEditable key={`${l.id}-${l.heuresHebdo}-${l.coefficient}`} ligne={l} onChange={rafraichir} />
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
        <Group align="flex-end">
          <Select label="Niveau" data={(niveaux.data ?? []).map((n) => ({ value: String(n.id), label: n.libelle }))} value={nNiveau} onChange={setNNiveau} w={160} />
          <Select label="Série" placeholder="Toutes" clearable data={SERIES_LYCEE} value={nSerie} onChange={setNSerie} w={200} />
          <Select label="Type d'établissement" placeholder="Tous les types" clearable data={(types.data ?? []).map((t) => ({ value: String(t.id), label: t.libelle }))} value={nType} onChange={setNType} w={220} />
          <Select label="Matière" searchable data={(matieres.data ?? []).map((m) => ({ value: String(m.id), label: m.libelle }))} value={nMatiere} onChange={setNMatiere} w={220} />
          <NumberInput label="Heures" min={0} max={99} step={0.25} decimalScale={2} decimalSeparator="," value={nHeures} onChange={setNHeures} w={90} />
          <NumberInput label="Coef." min={0.01} max={99} decimalScale={2} decimalSeparator="," value={nCoef} onChange={setNCoef} w={90} />
          <Button disabled={nNiveau === null || nMatiere === null || anneeCourante === null} loading={ajouter.isPending} onClick={() => ajouter.mutate()}>
            Ajouter
          </Button>
        </Group>
      </Stack>
    </Card>
  )
}
