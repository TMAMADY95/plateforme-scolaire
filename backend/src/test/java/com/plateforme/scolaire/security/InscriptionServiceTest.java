package com.plateforme.scolaire.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.service.AuditService;
import com.plateforme.scolaire.service.InscriptionService;
import com.plateforme.scolaire.web.dto.DecisionInscription;
import com.plateforme.scolaire.web.dto.DecisionRequest;
import com.plateforme.scolaire.web.dto.EleveRequest;
import com.plateforme.scolaire.web.dto.InscriptionDto;
import com.plateforme.scolaire.web.dto.InscriptionRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Règles métier du cycle d'inscription (sans base de données). */
class InscriptionServiceTest {

    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);
    private final EleveRepository eleveRepository = mock(EleveRepository.class);
    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final AnneeScolaireRepository anneeRepository = mock(AnneeScolaireRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final PerimetreService perimetreService = mock(PerimetreService.class);
    private final InscriptionService service = new InscriptionService(
            inscriptionRepository, eleveRepository, etablissementRepository, anneeRepository, classeRepository,
            mock(UtilisateurRepository.class), perimetreService, mock(AuditService.class));

    private Etablissement ecole1;
    private AnneeScolaire annee;

    @BeforeEach
    void preparer() {
        ecole1 = etablissement(1L);
        annee = new AnneeScolaire();
        annee.setId(1L);
        when(etablissementRepository.findById(1L)).thenReturn(Optional.of(ecole1));
        when(anneeRepository.findById(1L)).thenReturn(Optional.of(annee));
        when(eleveRepository.save(any(Eleve.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eleveRepository.prochainNumeroMatricule()).thenReturn(1L);
    }

    // ---------- Aides ----------

    private static Etablissement etablissement(long id) {
        Etablissement e = new Etablissement();
        e.setId(id);
        return e;
    }

    private Classe classe(long id, Etablissement etablissement, AnneeScolaire anneeClasse, Integer capacite) {
        Classe c = new Classe();
        c.setId(id);
        c.setEtablissement(etablissement);
        c.setAnneeScolaire(anneeClasse);
        c.setCapacite(capacite);
        c.setNom("A");
        return c;
    }

    private Inscription enAttente(long id) {
        Eleve eleve = new Eleve();
        eleve.setId(50L);
        eleve.setNom("Dupont");
        Inscription i = new Inscription();
        i.setId(id);
        i.setEleve(eleve);
        i.setEtablissement(ecole1);
        i.setAnneeScolaire(annee);
        i.setStatut("EN_ATTENTE");
        when(inscriptionRepository.findById(id)).thenReturn(Optional.of(i));
        return i;
    }

    private static void attendreStatut(ThrowingCallable appel, int statutHttp) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(statutHttp));
    }

    private static EleveRequest nouvelEleve() {
        return new EleveRequest("Dupont", "Jean", LocalDate.of(2015, 5, 4), "M");
    }

    // ---------- Décision ----------

    @Test
    void accepterNecessiteUneClasse() {
        enAttente(10L);
        attendreStatut(() -> service.decider(10L, new DecisionRequest(DecisionInscription.ACCEPTEE, null)), 400);
    }

    @Test
    void accepterDansUneClasseDUnAutreEtablissementEstRefuse() {
        enAttente(10L);
        when(classeRepository.findByIdPourMiseAJour(5L))
                .thenReturn(Optional.of(classe(5L, etablissement(2L), annee, 30)));
        attendreStatut(() -> service.decider(10L, new DecisionRequest(DecisionInscription.ACCEPTEE, 5L)), 400);
    }

    @Test
    void accepterDansUneClasseDUneAutreAnneeEstRefuse() {
        enAttente(10L);
        AnneeScolaire autre = new AnneeScolaire();
        autre.setId(2L);
        when(classeRepository.findByIdPourMiseAJour(5L)).thenReturn(Optional.of(classe(5L, ecole1, autre, 30)));
        attendreStatut(() -> service.decider(10L, new DecisionRequest(DecisionInscription.ACCEPTEE, 5L)), 400);
    }

    @Test
    void accepterDansUneClasseCompleteEstRefuse() {
        enAttente(10L);
        when(classeRepository.findByIdPourMiseAJour(5L)).thenReturn(Optional.of(classe(5L, ecole1, annee, 2)));
        when(inscriptionRepository.countByClasseIdAndStatut(5L, "ACCEPTEE")).thenReturn(2L);
        attendreStatut(() -> service.decider(10L, new DecisionRequest(DecisionInscription.ACCEPTEE, 5L)), 409);
    }

    @Test
    void accepterAffecteLEleveALaClasse() {
        Inscription inscription = enAttente(10L);
        Classe classe = classe(5L, ecole1, annee, 30);
        when(classeRepository.findByIdPourMiseAJour(5L)).thenReturn(Optional.of(classe));
        when(inscriptionRepository.countByClasseIdAndStatut(5L, "ACCEPTEE")).thenReturn(3L);

        InscriptionDto resultat = service.decider(10L, new DecisionRequest(DecisionInscription.ACCEPTEE, 5L));

        assertThat(resultat.statut()).isEqualTo("ACCEPTEE");
        assertThat(inscription.getClasse()).isSameAs(classe);
        assertThat(inscription.getDateDecision()).isNotNull();
    }

    @Test
    void refuserNAffectePasDeClasse() {
        Inscription inscription = enAttente(10L);
        InscriptionDto resultat = service.decider(10L, new DecisionRequest(DecisionInscription.REFUSEE, null));
        assertThat(resultat.statut()).isEqualTo("REFUSEE");
        assertThat(inscription.getClasse()).isNull();
    }

    @Test
    void uneInscriptionDejaTraiteeNePeutPlusEtreDecidee() {
        Inscription inscription = enAttente(10L);
        inscription.setStatut("REFUSEE");
        attendreStatut(() -> service.decider(10L, new DecisionRequest(DecisionInscription.REFUSEE, null)), 409);
    }

    @Test
    void uneInscriptionAccepteePeutEtreAnnulee() {
        Inscription inscription = enAttente(10L);
        inscription.setStatut("ACCEPTEE");
        assertThat(service.annuler(10L).statut()).isEqualTo("ANNULEE");
    }

    @Test
    void uneInscriptionRefuseeNePeutPasEtreAnnulee() {
        Inscription inscription = enAttente(10L);
        inscription.setStatut("REFUSEE");
        attendreStatut(() -> service.annuler(10L), 409);
    }

    // ---------- Demande ----------

    @Test
    void ilFautIndiquerUnMatriculeOuUnNouvelEleve() {
        attendreStatut(() -> service.demander(1L, new InscriptionRequest(1L, null, null, null)), 400);
        attendreStatut(() -> service.demander(1L, new InscriptionRequest(1L, "ELV-00000001", nouvelEleve(), null)),
                400);
    }

    @Test
    void unEtablissementInactifNeRecoitPasDInscription() {
        ecole1.setStatut("FERME");
        attendreStatut(() -> service.demander(1L, new InscriptionRequest(1L, null, nouvelEleve(), null)), 409);
    }

    @Test
    void unMatriculeInconnuEstRefuse() {
        attendreStatut(() -> service.demander(1L, new InscriptionRequest(1L, "ELV-99999999", null, null)), 404);
    }

    @Test
    void unNouvelEleveDeMemeIdentiteEstRefuseSansConfirmation() {
        when(eleveRepository.findParIdentite("Dupont", "Jean", LocalDate.of(2015, 5, 4)))
                .thenReturn(List.of(new Eleve()));
        attendreStatut(() -> service.demander(1L, new InscriptionRequest(1L, null, nouvelEleve(), null)), 409);
    }

    @Test
    void unHomonymeConfirmeEstCreeAvecUnMatricule() {
        when(eleveRepository.findParIdentite("Dupont", "Jean", LocalDate.of(2015, 5, 4)))
                .thenReturn(List.of(new Eleve()));

        InscriptionDto resultat = service.demander(1L, new InscriptionRequest(1L, null, nouvelEleve(), true));

        assertThat(resultat.statut()).isEqualTo("EN_ATTENTE");
        assertThat(resultat.eleve().matricule()).isEqualTo("ELV-00000001");
        assertThat(resultat.etablissementId()).isEqualTo(1L);
    }

    @Test
    void unElevePeutEtreReinscritParMatricule() {
        Eleve eleve = new Eleve();
        eleve.setId(50L);
        eleve.setMatricule("ELV-00000050");
        when(eleveRepository.findByMatricule("ELV-00000050")).thenReturn(Optional.of(eleve));

        InscriptionDto resultat = service.demander(1L, new InscriptionRequest(1L, "ELV-00000050", null, null));

        assertThat(resultat.statut()).isEqualTo("EN_ATTENTE");
        assertThat(resultat.eleve().matricule()).isEqualTo("ELV-00000050");
    }

    @Test
    void unEleveDejaInscritCetteAnneeNePeutPasEtreInscritDeNouveau() {
        Eleve eleve = new Eleve();
        eleve.setId(50L);
        when(eleveRepository.findByMatricule("ELV-00000050")).thenReturn(Optional.of(eleve));
        when(inscriptionRepository.findByEleveIdAndAnneeScolaireIdAndStatutIn(eq(50L), eq(1L), any()))
                .thenReturn(Optional.of(new Inscription()));
        attendreStatut(() -> service.demander(1L, new InscriptionRequest(1L, "ELV-00000050", null, null)), 409);
    }
}
