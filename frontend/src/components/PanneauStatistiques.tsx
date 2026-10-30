import type { ReactNode } from 'react'
import { Card, SimpleGrid, Text } from '@mantine/core'
import type { Statistiques } from '../api/types'
import CarteChiffre from './CarteChiffre'
import { Anneau, Barres, Courbe, libelleMois } from './graphiques/Graphiques'

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

interface Props {
  statistiques: Statistiques
  /** Afficher le secteur, les cycles, et la répartition par zone (ministère, région). */
  avecReseau?: boolean
}

/** Chiffres et graphiques d'un tableau de bord. */
export default function PanneauStatistiques({ statistiques: s, avecReseau = false }: Props) {
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2, lg: avecReseau ? 3 : 5 }}>
        {avecReseau && <CarteChiffre libelle="Établissements" valeur={s.etablissements} icone="ecole" />}
        <CarteChiffre libelle="Élèves inscrits" valeur={new Intl.NumberFormat('fr-FR').format(s.elevesInscrits)} icone="eleves" couleur="cyan" />
        <CarteChiffre libelle="Enseignants" valeur={new Intl.NumberFormat('fr-FR').format(s.enseignants)} icone="enseignants" couleur="violet" />
        <CarteChiffre
          libelle="Personnel de direction"
          valeur={new Intl.NumberFormat('fr-FR').format(s.personnelDirection)}
          detail="direction et administration"
          icone="comptes"
          couleur="orange"
        />
        <CarteChiffre libelle="Classes" valeur={new Intl.NumberFormat('fr-FR').format(s.classes)} icone="classes" />
        <CarteChiffre
          libelle="Taux de présence"
          valeur={s.tauxPresence === null ? '—' : `${String(s.tauxPresence).replace('.', ',')} %`}
          detail={`sur les ${s.joursPresence} derniers jours`}
          icone="presences"
          couleur="green"
        />
      </SimpleGrid>

      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <Cadre titre="Évolution des inscriptions">
          <Courbe
            points={s.inscriptionsParMois.map((p) => ({ libelle: libelleMois(p.mois), valeur: p.nombre }))}
            vide="Aucune demande d'inscription pour cette année."
          />
        </Cadre>
        {avecReseau ? (
          <Cadre titre="Établissements publics et privés">
            <Anneau points={s.repartitionParSecteur} libelleTotal="établissements" />
          </Cadre>
        ) : (
          <Cadre titre="Effectifs par niveau">
            <Barres points={s.effectifsParNiveau.map((p) => ({ libelle: p.libelle, valeur: p.valeur }))} vide="Aucun élève placé en classe." />
          </Cadre>
        )}
      </SimpleGrid>

      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <Cadre titre="Élèves par cycle">
          <Anneau points={s.effectifsParCycle} libelleTotal="élèves" vide="Aucun élève placé en classe." />
        </Cadre>
        <Cadre titre="Filles et garçons">
          <Anneau points={s.effectifsParSexe} libelleTotal="élèves" vide="Aucun élève placé en classe." />
        </Cadre>
      </SimpleGrid>

      {avecReseau && (
        <>
          <SimpleGrid cols={{ base: 1, md: 2 }}>
            <Cadre titre="Établissements par cycle">
              <Anneau points={s.repartitionParType} libelleTotal="établissements" vide="Aucun cycle renseigné." />
            </Cadre>
            <Cadre titre="Élèves par niveau">
              <Barres points={s.effectifsParNiveau} vide="Aucun élève placé en classe." />
            </Cadre>
          </SimpleGrid>
          <Cadre titre="Élèves par zone">
            <Barres points={s.effectifsParZone} vide="Aucun établissement rattaché à une zone." />
          </Cadre>
        </>
      )}
    </>
  )
}
