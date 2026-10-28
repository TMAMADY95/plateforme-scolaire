import { Modal } from '@mantine/core'
import type { Etablissement } from '../../api/types'
import ListeComptes from '../comptes/ListeComptes'

/** Comptes de direction d'un établissement : le ministère peut réinitialiser leur mot de passe. */
export default function ComptesDirectionModal({
  etablissement,
  onFermer,
}: {
  etablissement: Etablissement
  onFermer: () => void
}) {
  return (
    <Modal opened onClose={onFermer} title={`Direction de « ${etablissement.nom} »`} size="xl">
      <ListeComptes etablissementId={etablissement.id} avecRecherche={false} />
    </Modal>
  )
}
