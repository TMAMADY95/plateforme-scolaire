import { useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { enregistrerProfilEnseignant, listerMatieres, listerProfilsEnseignants } from '../../api/endpoints'
import {
  FORMATIONS_ACADEMIQUES,
  FORMATIONS_PROFESSIONNELLES,
  STATUTS_ENSEIGNANT,
  TYPES_HANDICAP,
  libelleDans,
} from '../../api/types'
import type { ProfilEnseignant } from '../../api/types'

function FicheModal({ etablissementId, profil, onFermer }: { etablissementId: number; profil: ProfilEnseignant; onFermer: () => void }) {
  const queryClient = useQueryClient()
  const matieres = useQuery({ queryKey: ['matieres'], queryFn: listerMatieres })
  const [naissance, setNaissance] = useState(profil.dateNaissance ?? '')
  const [sexe, setSexe] = useState<string | null>(profil.sexe)
  const [statut, setStatut] = useState<string | null>(profil.statut)
  const [academique, setAcademique] = useState<string | null>(profil.formationAcademique)
  const [professionnelle, setProfessionnelle] = useState<string | null>(profil.formationProfessionnelle)
  const [matiere, setMatiere] = useState<string | null>(profil.matierePrincipaleId === null ? null : String(profil.matierePrincipaleId))
  const [service, setService] = useState(profil.datePriseService ?? '')
  const [handicap, setHandicap] = useState<string | null>(profil.handicap)
  const [erreur, setErreur] = useState<string | null>(null)
  const enregistrement = useMutation({
    mutationFn: () =>
      enregistrerProfilEnseignant(etablissementId, profil.enseignantId, {
        dateNaissance: naissance === '' ? null : naissance,
        sexe,
        statut,
        formationAcademique: academique,
        formationProfessionnelle: professionnelle,
        matierePrincipaleId: matiere === null ? null : Number(matiere),
        datePriseService: service === '' ? null : service,
        handicap,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['profils-enseignants', etablissementId] })
      onFermer()
    },
    onError: (e) => setErreur(e.message),
  })
  return (
    <Modal opened onClose={onFermer} title={`Fiche de ${profil.prenom} ${profil.nom}`}>
      <Stack>
        <Group grow>
          <TextInput label="Date de naissance" type="date" value={naissance} onChange={(e) => setNaissance(e.currentTarget.value)} />
          <Select label="Sexe" clearable data={[{ value: 'F', label: 'Féminin' }, { value: 'M', label: 'Masculin' }]} value={sexe} onChange={setSexe} />
        </Group>
        <Select label="Statut" clearable data={STATUTS_ENSEIGNANT} value={statut} onChange={setStatut} />
        <Select label="Formation académique" clearable data={FORMATIONS_ACADEMIQUES} value={academique} onChange={setAcademique} />
        <Select label="Formation professionnelle" clearable data={FORMATIONS_PROFESSIONNELLES} value={professionnelle} onChange={setProfessionnelle} />
        <Select label="Matière principale" searchable clearable data={(matieres.data ?? []).map((m) => ({ value: String(m.id), label: m.libelle }))} value={matiere} onChange={setMatiere} />
        <TextInput label="Prise de service" type="date" value={service} onChange={(e) => setService(e.currentTarget.value)} />
        <Select label="Handicap (facultatif)" clearable data={TYPES_HANDICAP} value={handicap} onChange={setHandicap} />
        {erreur && <Alert color="red" variant="light">{erreur}</Alert>}
        <Group justify="flex-end">
          <Button variant="default" onClick={onFermer}>Annuler</Button>
          <Button loading={enregistrement.isPending} onClick={() => enregistrement.mutate()}>Enregistrer</Button>
        </Group>
      </Stack>
    </Modal>
  )
}

/** Fiches professionnelles des enseignants de l'établissement (rubriques de l'annuaire statistique). */
export default function ProfilsEnseignants({ etablissementId }: { etablissementId: number }) {
  const [choisi, setChoisi] = useState<ProfilEnseignant | null>(null)
  const profils = useQuery({ queryKey: ['profils-enseignants', etablissementId], queryFn: () => listerProfilsEnseignants(etablissementId) })
  const aCompleter = (profils.data ?? []).filter((p) => !p.renseigne).length
  return (
    <Stack mt="md">
      <Text c="dimmed" size="sm">
        Ces informations alimentent l'annuaire statistique et le suivi des besoins en enseignants. Seule la direction de l'établissement voit les fiches ;
        le ministère ne reçoit que des totaux.
      </Text>
      {profils.isPending && <Loader />}
      {profils.isError && <Alert color="red" variant="light">{profils.error.message}</Alert>}
      {profils.data && aCompleter > 0 && <Alert color="orange" variant="light">{aCompleter} fiche(s) à compléter (statut et formation académique).</Alert>}
      {profils.data && profils.data.length === 0 && <Text c="dimmed">Aucun enseignant.</Text>}
      {profils.data && profils.data.length > 0 && (
        <Table.ScrollContainer minWidth={760}>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Enseignant</Table.Th>
                <Table.Th>Âge</Table.Th>
                <Table.Th>Statut</Table.Th>
                <Table.Th>Formation</Table.Th>
                <Table.Th>Formé au métier</Table.Th>
                <Table.Th>Matière principale</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {profils.data.map((p) => (
                <Table.Tr key={p.enseignantId}>
                  <Table.Td>{p.prenom} {p.nom}</Table.Td>
                  <Table.Td>{p.age ?? '—'}</Table.Td>
                  <Table.Td>{libelleDans(STATUTS_ENSEIGNANT, p.statut)}</Table.Td>
                  <Table.Td>{libelleDans(FORMATIONS_ACADEMIQUES, p.formationAcademique)}</Table.Td>
                  <Table.Td>{p.formationProfessionnelle === null ? '—' : p.forme ? 'Oui' : 'Non'}</Table.Td>
                  <Table.Td>{p.matierePrincipale ?? '—'}</Table.Td>
                  <Table.Td><Button size="compact-xs" variant="light" onClick={() => setChoisi(p)}>Compléter</Button></Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
      {choisi && <FicheModal key={choisi.enseignantId} etablissementId={etablissementId} profil={choisi} onFermer={() => setChoisi(null)} />}
    </Stack>
  )
}
