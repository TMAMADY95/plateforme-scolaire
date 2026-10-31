import { useRef, useState } from 'react'
import { Alert, Button, Code, FileButton, Group, List, Modal, Progress, ScrollArea, Stack, Text } from '@mantine/core'
import { exporterCsv } from '../utils/exportCsv'
import { lireCsv } from '../utils/lireCsv'

interface Props<T> {
  titre: string
  /** Explication du format attendu. */
  description: string
  /** Colonnes du modèle téléchargeable, et un exemple de ligne. */
  colonnes: string[]
  exemple: string[]
  nomModele: string
  /** Transforme une ligne du fichier ; retourne le texte de l'erreur si elle est inutilisable. */
  /** Reçoit aussi le rang de la ligne et toutes les lignes du fichier (pour repérer les doublons). */
  analyser: (ligne: Record<string, string>, rang: number, toutes: Record<string, string>[]) => T | string
  importer: (element: T) => Promise<unknown>
  libelle: (element: T) => string
  /** Appelé quand l'import est terminé (pour rafraîchir les listes). */
  onTermine: () => void
  onFermer: () => void
}

const SIMULTANES = 4

/** Import d'un fichier CSV (Excel : « Enregistrer sous… CSV ») avec contrôle avant, puis avancement et bilan. */
export default function ImportCsvModal<T>({
  titre,
  description,
  colonnes,
  exemple,
  nomModele,
  analyser,
  importer,
  libelle,
  onTermine,
  onFermer,
}: Props<T>) {
  const reinitialiser = useRef<() => void>(null)
  const [fichier, setFichier] = useState<string | null>(null)
  const [valides, setValides] = useState<T[]>([])
  const [refusees, setRefusees] = useState<string[]>([])
  const [erreurFichier, setErreurFichier] = useState<string | null>(null)
  const [fait, setFait] = useState(0)
  const [echecs, setEchecs] = useState<string[]>([])
  const [etat, setEtat] = useState<'choix' | 'import' | 'bilan'>('choix')

  const lire = async (f: File | null) => {
    if (f === null) return
    setErreurFichier(null)
    try {
      const { lignes } = lireCsv(await f.text())
      if (lignes.length === 0) {
        setErreurFichier('Le fichier ne contient aucune ligne de données.')
        return
      }
      const ok: T[] = []
      const ko: string[] = []
      lignes.forEach((l, i) => {
        const r = analyser(l, i, lignes)
        if (typeof r === 'string') ko.push(`Ligne ${i + 2} : ${r}`)
        else ok.push(r)
      })
      setFichier(f.name)
      setValides(ok)
      setRefusees(ko)
    } catch {
      setErreurFichier('Impossible de lire ce fichier.')
    }
  }

  const lancer = async () => {
    setEtat('import')
    setFait(0)
    const erreurs: string[] = []
    let suivant = 0
    const travailleur = async () => {
      while (suivant < valides.length) {
        const element = valides[suivant++]
        try {
          await importer(element)
        } catch (e) {
          erreurs.push(`${libelle(element)} : ${e instanceof Error ? e.message : 'échec'}`)
        }
        setFait((n) => n + 1)
      }
    }
    await Promise.all(Array.from({ length: Math.min(SIMULTANES, valides.length) }, travailleur))
    setEchecs(erreurs)
    setEtat('bilan')
    onTermine()
  }

  return (
    <Modal opened onClose={etat === 'import' ? () => undefined : onFermer} title={titre} size="lg" closeOnClickOutside={etat !== 'import'}>
      <Stack>
        {etat === 'choix' && (
          <>
            <Text size="sm">{description}</Text>
            <Text size="sm">
              Colonnes : <Code>{colonnes.join(' ; ')}</Code>
            </Text>
            <Group>
              <Button variant="default" size="xs" onClick={() => exporterCsv(nomModele, colonnes.map((titre, i) => ({ titre, valeur: (l: string[]) => l[i] })), [exemple])}>
                Télécharger le modèle
              </Button>
              <FileButton resetRef={reinitialiser} accept=".csv,text/csv,text/plain" onChange={(f) => void lire(f)}>
                {(props) => (
                  <Button size="xs" {...props}>
                    Choisir le fichier CSV
                  </Button>
                )}
              </FileButton>
            </Group>
            {erreurFichier && (
              <Alert color="red" variant="light">
                {erreurFichier}
              </Alert>
            )}
            {fichier && (
              <>
                <Alert color={valides.length > 0 ? 'green' : 'red'} variant="light" title={fichier}>
                  {valides.length} ligne{valides.length > 1 ? 's' : ''} prête{valides.length > 1 ? 's' : ''} à importer
                  {refusees.length > 0 ? `, ${refusees.length} refusée${refusees.length > 1 ? 's' : ''}` : ''}.
                </Alert>
                {refusees.length > 0 && (
                  <ScrollArea.Autosize mah={200}>
                    <List size="sm" spacing={2}>
                      {refusees.map((r) => (
                        <List.Item key={r}>{r}</List.Item>
                      ))}
                    </List>
                  </ScrollArea.Autosize>
                )}
                <Group justify="flex-end">
                  <Button variant="default" onClick={onFermer}>
                    Annuler
                  </Button>
                  <Button disabled={valides.length === 0} onClick={() => void lancer()}>
                    Importer {valides.length} ligne{valides.length > 1 ? 's' : ''}
                  </Button>
                </Group>
              </>
            )}
          </>
        )}
        {etat === 'import' && (
          <>
            <Text size="sm">
              Import en cours : {fait} sur {valides.length}…
            </Text>
            <Progress value={(fait * 100) / Math.max(valides.length, 1)} animated />
          </>
        )}
        {etat === 'bilan' && (
          <>
            <Alert color={echecs.length === 0 ? 'green' : 'orange'} variant="light" title="Import terminé">
              {valides.length - echecs.length} ligne{valides.length - echecs.length > 1 ? 's' : ''} importée
              {valides.length - echecs.length > 1 ? 's' : ''}
              {echecs.length > 0 ? `, ${echecs.length} en échec` : ''}.
            </Alert>
            {echecs.length > 0 && (
              <ScrollArea.Autosize mah={240}>
                <List size="sm" spacing={2}>
                  {echecs.map((e) => (
                    <List.Item key={e}>{e}</List.Item>
                  ))}
                </List>
              </ScrollArea.Autosize>
            )}
            <Group justify="flex-end">
              <Button onClick={onFermer}>Fermer</Button>
            </Group>
          </>
        )}
      </Stack>
    </Modal>
  )
}
