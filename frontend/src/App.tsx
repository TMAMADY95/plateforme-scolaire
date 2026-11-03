import { Center, Loader } from '@mantine/core'
import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router'
import RequiertRole from './auth/RequiertRole'
import RequiertSession from './auth/RequiertSession'
import { estChefCentre, estDirectionDeZone, estDirectionEtablissement, estEleve, estEnseignant, estGestionnaire, estNational, estParent } from './auth/roles'
import MiseEnPage from './layout/MiseEnPage'
import PageConnexion from './pages/PageConnexion'

// Chaque page est chargée à la demande : le premier chargement ne télécharge que le socle commun.
const PageAccueil = lazy(() => import('./pages/PageAccueil'))
const PageAlertesParents = lazy(() => import('./pages/PageAlertesParents'))
const PageChangementMotDePasse = lazy(() => import('./pages/PageChangementMotDePasse'))
const PageClasseEnseignant = lazy(() => import('./pages/PageClasseEnseignant'))
const PageClasses = lazy(() => import('./pages/PageClasses'))
const PageComptes = lazy(() => import('./pages/PageComptes'))
const PageEleves = lazy(() => import('./pages/PageEleves'))
const PageEmploiDuTempsClasse = lazy(() => import('./pages/PageEmploiDuTempsClasse'))
const PageEmploisDuTemps = lazy(() => import('./pages/PageEmploisDuTemps'))
const PageEnfant = lazy(() => import('./pages/PageEnfant'))
const PageEnseignants = lazy(() => import('./pages/PageEnseignants'))
const PageFinances = lazy(() => import('./pages/PageFinances'))
const PageInscriptions = lazy(() => import('./pages/PageInscriptions'))
const PageMessages = lazy(() => import('./pages/PageMessages'))
const PageMesClasses = lazy(() => import('./pages/PageMesClasses'))
const PageMesEnfants = lazy(() => import('./pages/PageMesEnfants'))
const PageMinistere = lazy(() => import('./pages/PageMinistere'))
const PageMonCentre = lazy(() => import('./pages/PageMonCentre'))
const PageMonEspace = lazy(() => import('./pages/PageMonEspace'))
const PageMotDePasseOublie = lazy(() => import('./pages/PageMotDePasseOublie'))
const PageReinitialisation = lazy(() => import('./pages/PageReinitialisation'))
const PagePaiementSimule = lazy(() => import('./pages/PagePaiementSimule'))
const PagePresences = lazy(() => import('./pages/PagePresences'))
const PageRegion = lazy(() => import('./pages/PageRegion'))
const PageResultats = lazy(() => import('./pages/PageResultats'))
const PageVerificationDiplome = lazy(() => import('./pages/PageVerificationDiplome'))

/** Route paramétrée : l'emploi du temps d'une classe choisie dans la liste. */
function EmploiDuTempsClasse() {
  const { classeId } = useParams()
  return <PageEmploiDuTempsClasse classeId={Number(classeId)} />
}

export default function App() {
  return (
    <Suspense fallback={<Center h="60vh"><Loader /></Center>}>
    <Routes>
      <Route path="/connexion" element={<PageConnexion />} />
      <Route path="/mot-de-passe-oublie" element={<PageMotDePasseOublie />} />
      <Route path="/reinitialisation" element={<PageReinitialisation />} />
      <Route path="/verification-diplome" element={<PageVerificationDiplome />} />
      <Route
        path="/changer-mot-de-passe"
        element={
          <RequiertSession>
            <PageChangementMotDePasse />
          </RequiertSession>
        }
      />
      <Route
        element={
          <RequiertSession motDePasseAJour>
            <MiseEnPage />
          </RequiertSession>
        }
      >
        <Route index element={<PageAccueil />} />

        {/* Direction et personnel administratif */}
        <Route path="classes" element={<RequiertRole autorise={estGestionnaire}><PageClasses /></RequiertRole>} />
        <Route path="inscriptions" element={<RequiertRole autorise={estGestionnaire}><PageInscriptions /></RequiertRole>} />
        <Route path="eleves" element={<RequiertRole autorise={estGestionnaire}><PageEleves /></RequiertRole>} />
        <Route path="enseignants" element={<RequiertRole autorise={estGestionnaire}><PageEnseignants /></RequiertRole>} />
        <Route path="presences" element={<RequiertRole autorise={estGestionnaire}><PagePresences /></RequiertRole>} />
        <Route path="resultats" element={<RequiertRole autorise={estGestionnaire}><PageResultats /></RequiertRole>} />
        <Route path="emplois-du-temps" element={<RequiertRole autorise={estGestionnaire}><PageEmploisDuTemps /></RequiertRole>} />
        <Route
          path="emplois-du-temps/:classeId"
          element={<RequiertRole autorise={estGestionnaire}><EmploiDuTempsClasse /></RequiertRole>}
        />
        <Route path="finances" element={<RequiertRole autorise={estGestionnaire}><PageFinances /></RequiertRole>} />
        <Route path="comptes" element={<RequiertRole autorise={estDirectionEtablissement}><PageComptes /></RequiertRole>} />
        <Route path="alertes-parents" element={<RequiertRole autorise={estDirectionEtablissement}><PageAlertesParents /></RequiertRole>} />

        {/* Retour du prestataire de paiement simulé (dev) */}
        <Route path="paiement-simule/:reference" element={<PagePaiementSimule />} />

        {/* Ministère */}
        <Route path="ministere" element={<RequiertRole autorise={estNational}><PageMinistere /></RequiertRole>} />

        {/* Direction régionale */}
        <Route path="region" element={<RequiertRole autorise={estDirectionDeZone}><PageRegion /></RequiertRole>} />

        {/* Chef de centre d'examen */}
        <Route path="mon-centre" element={<RequiertRole autorise={estChefCentre}><PageMonCentre /></RequiertRole>} />

        {/* Enseignants */}
        <Route path="mes-classes" element={<RequiertRole autorise={estEnseignant}><PageMesClasses /></RequiertRole>} />
        <Route
          path="mes-classes/:classeId"
          element={<RequiertRole autorise={estEnseignant}><PageClasseEnseignant /></RequiertRole>}
        />

        {/* Parents */}
        <Route
          path="messages"
          element={
            <RequiertRole autorise={(u) => estParent(u) || estEnseignant(u) || estGestionnaire(u)}>
              <PageMessages />
            </RequiertRole>
          }
        />
        <Route path="mes-enfants" element={<RequiertRole autorise={estParent}><PageMesEnfants /></RequiertRole>} />
        <Route path="mes-enfants/:eleveId" element={<RequiertRole autorise={estParent}><PageEnfant /></RequiertRole>} />

        {/* Élèves */}
        <Route path="mon-espace" element={<RequiertRole autorise={estEleve}><PageMonEspace /></RequiertRole>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
    </Suspense>
  )
}
