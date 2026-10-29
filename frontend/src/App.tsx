import { Navigate, Route, Routes, useParams } from 'react-router'
import RequiertRole from './auth/RequiertRole'
import RequiertSession from './auth/RequiertSession'
import { estDirectionEtablissement, estDirectionRegionale, estEleve, estEnseignant, estGestionnaire, estMinistere, estParent } from './auth/roles'
import MiseEnPage from './layout/MiseEnPage'
import PageAccueil from './pages/PageAccueil'
import PageChangementMotDePasse from './pages/PageChangementMotDePasse'
import PageClasseEnseignant from './pages/PageClasseEnseignant'
import PageClasses from './pages/PageClasses'
import PageComptes from './pages/PageComptes'
import PageConnexion from './pages/PageConnexion'
import PageEleves from './pages/PageEleves'
import PageEmploiDuTempsClasse from './pages/PageEmploiDuTempsClasse'
import PageEmploisDuTemps from './pages/PageEmploisDuTemps'
import PageEnfant from './pages/PageEnfant'
import PageEnseignants from './pages/PageEnseignants'
import PageFinances from './pages/PageFinances'
import PageInscriptions from './pages/PageInscriptions'
import PageMessages from './pages/PageMessages'
import PageMesClasses from './pages/PageMesClasses'
import PageMesEnfants from './pages/PageMesEnfants'
import PageMinistere from './pages/PageMinistere'
import PageMonEspace from './pages/PageMonEspace'
import PageMotDePasseOublie from './pages/PageMotDePasseOublie'
import PageReinitialisation from './pages/PageReinitialisation'
import PagePaiementSimule from './pages/PagePaiementSimule'
import PagePresences from './pages/PagePresences'
import PageRegion from './pages/PageRegion'
import PageResultats from './pages/PageResultats'

/** Route paramétrée : l'emploi du temps d'une classe choisie dans la liste. */
function EmploiDuTempsClasse() {
  const { classeId } = useParams()
  return <PageEmploiDuTempsClasse classeId={Number(classeId)} />
}

export default function App() {
  return (
    <Routes>
      <Route path="/connexion" element={<PageConnexion />} />
      <Route path="/mot-de-passe-oublie" element={<PageMotDePasseOublie />} />
      <Route path="/reinitialisation" element={<PageReinitialisation />} />
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

        {/* Retour du prestataire de paiement simulé (dev) */}
        <Route path="paiement-simule/:reference" element={<PagePaiementSimule />} />

        {/* Ministère */}
        <Route path="ministere" element={<RequiertRole autorise={estMinistere}><PageMinistere /></RequiertRole>} />

        {/* Direction régionale */}
        <Route path="region" element={<RequiertRole autorise={estDirectionRegionale}><PageRegion /></RequiertRole>} />

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
  )
}
