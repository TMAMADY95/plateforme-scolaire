import { Modal } from '@mantine/core'
import PanneauAnnonces from '../../components/PanneauAnnonces'
import type { Classe } from '../../api/types'

/** Annonces d'une classe, pour la direction : lire, publier, supprimer. */
export default function AnnoncesModal({ classe, onFermer }: { classe: Classe; onFermer: () => void }) {
  return (
    <Modal opened onClose={onFermer} title={`Annonces — classe ${classe.nom}`} size="lg">
      <PanneauAnnonces classeId={classe.id} peutPublier />
    </Modal>
  )
}
