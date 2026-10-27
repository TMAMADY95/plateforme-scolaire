import { Navigate, Route, Routes } from 'react-router'
import RequiertRole from './auth/RequiertRole'
import RequiertSession from './auth/RequiertSession'
import { estEnseignant, estGestionnaire, estParent } from './auth/roles'
import MiseEnPage from './layout/MiseEnPage'
import PageAccueil from './pages/PageAccueil'
import PageChangementMotDePasse from './pages/PageChangementMotDePasse'
import PageClasseEnseignant from './pages/PageClasseEnseignant'
import PageClasses from './pages/PageClasses'
import PageConnexion from './pages/PageConnexion'
import PageEleves from './pages/PageEleves'
import PageEnfant from './pages/PageEnfant'
import PageEnseignants from './pages/PageEnseignants'
import PageInscriptions from './pages/PageInscriptions'
import PageMesClasses from './pages/PageMesClasses'
import PageMesEnfants from './pages/PageMesEnfants'
import PagePresences from './pages/PagePresences'
import PageResultats from './pages/PageResultats'

export default function App() {
  return (
    <Routes>
      <Route path="/connexion" element={<PageConnexion />} />
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

        {/* Enseignants */}
        <Route path="mes-classes" element={<RequiertRole autorise={estEnseignant}><PageMesClasses /></RequiertRole>} />
        <Route
          path="mes-classes/:classeId"
          element={<RequiertRole autorise={estEnseignant}><PageClasseEnseignant /></RequiertRole>}
        />

        {/* Parents */}
        <Route path="mes-enfants" element={<RequiertRole autorise={estParent}><PageMesEnfants /></RequiertRole>} />
        <Route path="mes-enfants/:eleveId" element={<RequiertRole autorise={estParent}><PageEnfant /></RequiertRole>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
