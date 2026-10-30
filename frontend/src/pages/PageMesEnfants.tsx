import { Alert, Avatar, Badge, Button, Card, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { resumeParent } from '../api/endpoints'
import type { ResumeEnfant } from '../api/types'
import { useMesEnfants } from '../hooks/useMesEnfants'
import { formaterDate } from '../utils/dates'
import { formaterMontant } from '../utils/monnaie'
import { formaterNote, formaterRang } from '../utils/notes'

/** Une ligne « libellé : valeur » du point du jour. */
function Ligne({ libelle, valeur, alerte }: { libelle: string; valeur: string; alerte?: boolean }) {
  return (
    <Group justify="space-between" wrap="nowrap" gap="xs">
      <Text size="sm" c="dimmed">
        {libelle}
      </Text>
      <Text size="sm" fw={600} c={alerte ? 'red' : undefined} ta="right">
        {valeur}
      </Text>
    </Group>
  )
}

/** Le point du jour d'un enfant : moyenne, présences de la semaine, prochain devoir, factures. */
function PointDuJour({ r }: { r: ResumeEnfant }) {
  const devoir = r.prochainsDevoirs[0]
  return (
    <Stack gap={6} mb="md">
      {r.classeId !== null && (
        <Ligne
          libelle={r.periode ?? 'Moyenne'}
          valeur={
            r.moyenneGenerale === null
              ? 'Pas encore de note'
              : `${formaterNote(r.moyenneGenerale)}/20${r.rang !== null ? ` · ${formaterRang(r.rang)}/${r.effectif}` : ''}`
          }
        />
      )}
      {r.classeId !== null && (
        <Ligne
          libelle="7 derniers jours"
          valeur={
            r.absencesSemaine === 0 && r.retardsSemaine === 0
              ? 'Aucune absence'
              : [
                  r.absencesSemaine > 0
                    ? `${r.absencesSemaine} absence${r.absencesSemaine > 1 ? 's' : ''}${r.absencesNonJustifiees > 0 ? ` (${r.absencesNonJustifiees} à justifier)` : ''}`
                    : null,
                  r.retardsSemaine > 0 ? `${r.retardsSemaine} retard${r.retardsSemaine > 1 ? 's' : ''}` : null,
                ]
                  .filter(Boolean)
                  .join(', ')
          }
          alerte={r.absencesNonJustifiees > 0}
        />
      )}
      {r.classeId !== null && (
        <Ligne
          libelle="Prochain devoir"
          valeur={devoir ? `${devoir.matiere} · ${formaterDate(devoir.dateLimite)}` : 'Aucun'}
        />
      )}
      <Ligne
        libelle="Frais"
        valeur={
          r.facturesImpayees === 0
            ? 'À jour'
            : `${
                r.resteAPayer === null
                  ? `${r.facturesImpayees} facture${r.facturesImpayees > 1 ? 's' : ''}`
                  : formaterMontant(r.resteAPayer, r.devise)
              } à payer${r.facturesEnRetard > 0 ? ' (en retard)' : ''}`
        }
        alerte={r.facturesEnRetard > 0}
      />
    </Stack>
  )
}

export default function PageMesEnfants() {
  const enfants = useMesEnfants()
  // Le point du jour est un plus : s'il échoue, les cartes s'affichent sans lui.
  const resume = useQuery({ queryKey: ['resume-parent'], queryFn: resumeParent, retry: false })

  return (
    <Stack>
      <div>
        <Title order={2}>Mes enfants</Title>
        {enfants.data && enfants.data.length > 0 && (
          <Text c="dimmed">
            {enfants.data.length} {enfants.data.length > 1 ? 'enfants rattachés' : 'enfant rattaché'} à votre compte
          </Text>
        )}
      </div>

      {enfants.isPending && <Loader />}
      {enfants.isError && (
        <Alert color="red" variant="light">
          {enfants.error.message}
        </Alert>
      )}
      {enfants.data && enfants.data.length === 0 && (
        <Alert color="blue" variant="light">
          Aucun enfant n'est encore rattaché à votre compte. Rapprochez-vous de l'établissement pour qu'il vous
          rattache à votre enfant.
        </Alert>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
        {enfants.data?.map((enfant) => (
          <Card key={enfant.eleveId} withBorder padding="lg">
            <Group wrap="nowrap" align="flex-start" mb="xs">
              <Avatar size={52} radius="xl" color="marine">
                {(enfant.prenom.charAt(0) + enfant.nom.charAt(0)).toUpperCase()}
              </Avatar>
              <div style={{ flex: 1 }}>
                <Group justify="space-between" wrap="nowrap" gap="xs">
                  <Text fw={600} size="lg">
                    {enfant.prenom} {enfant.nom}
                  </Text>
                  <Badge variant="light">{enfant.relation}</Badge>
                </Group>
                <Text c="dimmed" size="sm">
                  Né(e) le {formaterDate(enfant.dateNaissance)} · {enfant.matricule}
                </Text>
              </div>
            </Group>
            <Text size="sm" mt="sm" mb="sm">
              {enfant.classeNom !== null
                ? `Classe ${enfant.classeNom} — ${enfant.etablissementNom} (${enfant.anneeLibelle})`
                : "Pas d'inscription active pour le moment"}
            </Text>
            {resume.data?.find((r) => r.eleveId === enfant.eleveId) !== undefined && (
              <PointDuJour r={resume.data.find((r) => r.eleveId === enfant.eleveId) as ResumeEnfant} />
            )}
            <Button component={Link} to={`/mes-enfants/${enfant.eleveId}`} fullWidth>
              Voir le détail
            </Button>
          </Card>
        ))}
      </SimpleGrid>
    </Stack>
  )
}
