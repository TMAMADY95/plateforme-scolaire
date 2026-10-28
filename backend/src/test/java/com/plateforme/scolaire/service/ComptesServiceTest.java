package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreComptesService;
import com.plateforme.scolaire.web.dto.ComptePersonnelDto;
import com.plateforme.scolaire.web.dto.CompteDto;
import com.plateforme.scolaire.web.dto.PersonnelRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class ComptesServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-28T10:00:00Z");

    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
    private final AffectationRoleRepository affectationRoleRepository = mock(AffectationRoleRepository.class);
    private final LienParentalRepository lienParentalRepository = mock(LienParentalRepository.class);
    private final EleveRepository eleveRepository = mock(EleveRepository.class);
    private final PerimetreComptesService perimetreComptes = mock(PerimetreComptesService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditService auditService = mock(AuditService.class);

    private final ComptesService service = new ComptesService(etablissementRepository, utilisateurRepository,
            affectationRoleRepository, lienParentalRepository, eleveRepository, perimetreComptes, passwordEncoder,
            auditService, Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private static Utilisateur utilisateur(Long id, String email) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        u.setNom("Nom");
        u.setPrenom("Prénom");
        return u;
    }

    private static PersonnelRequest requete(String email) {
        return new PersonnelRequest(email, "Diop", "Awa", null, "motdepasse-long");
    }

    @Test
    void laReinitialisationImposeUnChangementInvalideLesJetonsEtLeveLeVerrou() {
        Utilisateur u = utilisateur(5L, "prof@example.org");
        u.setEchecsConnexion(4);
        u.setVerrouilleJusquA(MAINTENANT.plusSeconds(600));
        u.setDoitChangerMotDePasse(false);
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("temporaire-123")).thenReturn("hash-temporaire");

        service.reinitialiserMotDePasse(5L, "temporaire-123");

        assertThat(u.getMotDePasseHash()).isEqualTo("hash-temporaire");
        assertThat(u.isDoitChangerMotDePasse()).isTrue();
        assertThat(u.getMotDePasseModifieLe()).isEqualTo(MAINTENANT);
        assertThat(u.getEchecsConnexion()).isZero();
        assertThat(u.getVerrouilleJusquA()).isNull();
    }

    @Test
    void lAuditNeContientJamaisLeMotDePasse() {
        Utilisateur u = utilisateur(5L, "prof@example.org");
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        service.reinitialiserMotDePasse(5L, "temporaire-123");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> details = ArgumentCaptor.forClass(Map.class);
        verify(auditService).enregistrer(eq("REINITIALISATION_MOT_DE_PASSE"), eq("utilisateur"), eq(5L), details.capture());
        assertThat(details.getValue().toString()).doesNotContain("temporaire-123").doesNotContain("hash");
    }

    @Test
    void reinitialiserUnCompteInconnuRenvoieUneErreur() {
        when(utilisateurRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reinitialiserMotDePasse(9L, "temporaire-123"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void lePersonnelAdministratifEstCreeActifAvecUnMotDePasseAChanger() {
        Etablissement e = new Etablissement();
        e.setId(7L);
        when(etablissementRepository.findById(7L)).thenReturn(Optional.of(e));
        when(utilisateurRepository.existsByEmailIgnoreCase("awa@example.org")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse-long")).thenReturn("hash");

        ComptePersonnelDto dto = service.creerPersonnel(7L, requete(" awa@example.org "));

        assertThat(dto.role()).isEqualTo("PERSONNEL_ADMIN");
        assertThat(dto.email()).isEqualTo("awa@example.org");
        ArgumentCaptor<Utilisateur> compte = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(compte.capture());
        assertThat(compte.getValue().isDoitChangerMotDePasse()).isTrue();
        ArgumentCaptor<AffectationRole> affectation = ArgumentCaptor.forClass(AffectationRole.class);
        verify(affectationRoleRepository).save(affectation.capture());
        assertThat(affectation.getValue().getRole()).isEqualTo(Role.PERSONNEL_ADMIN);
        assertThat(affectation.getValue().getEtablissement()).isSameAs(e);
    }

    @Test
    void onNeCreePasDePersonnelDansUnEtablissementInactifOuAvecUnEmailDejaPris() {
        Etablissement ferme = new Etablissement();
        ferme.setId(7L);
        ferme.setStatut("FERME");
        when(etablissementRepository.findById(7L)).thenReturn(Optional.of(ferme));
        assertThatThrownBy(() -> service.creerPersonnel(7L, requete("a@example.org")))
                .isInstanceOf(ResponseStatusException.class);

        Etablissement actif = new Etablissement();
        actif.setId(8L);
        when(etablissementRepository.findById(8L)).thenReturn(Optional.of(actif));
        when(utilisateurRepository.existsByEmailIgnoreCase("pris@example.org")).thenReturn(true);
        assertThatThrownBy(() -> service.creerPersonnel(8L, requete("pris@example.org")))
                .isInstanceOf(ResponseStatusException.class);

        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void leMinistereNeVoitQueLesDirectionsEtPasLesFamilles() {
        when(perimetreComptes.estDirectionDe(7L)).thenReturn(false);
        AffectationRole a = new AffectationRole();
        a.setRole(Role.DIRECTION_ETABLISSEMENT);
        a.setUtilisateur(utilisateur(2L, "dir@example.org"));
        when(affectationRoleRepository.rechercherComptes(eq(7L), anyCollection(), anyString())).thenReturn(List.of(a));

        List<CompteDto> comptes = service.lister(7L, "jean");

        assertThat(comptes).extracting(CompteDto::role).containsExactly("DIRECTION_ETABLISSEMENT");
        verify(lienParentalRepository, never()).rechercherParents(anyLong(), anyString());
        verify(eleveRepository, never()).rechercherComptesEleves(anyLong(), anyString());
    }

    @Test
    void laDirectionNeListeLesFamillesQuAvecUneRechercheAssezLongue() {
        when(perimetreComptes.estDirectionDe(7L)).thenReturn(true);
        when(affectationRoleRepository.rechercherComptes(eq(7L), anyCollection(), anyString())).thenReturn(List.of());
        when(lienParentalRepository.rechercherParents(7L, "ba")).thenReturn(List.of(utilisateur(3L, "p@example.org")));
        when(eleveRepository.rechercherComptesEleves(7L, "ba")).thenReturn(List.of(utilisateur(4L, "e@example.org")));

        assertThat(service.lister(7L, "")).isEmpty();
        verify(lienParentalRepository, never()).rechercherParents(anyLong(), eq(""));

        assertThat(service.lister(7L, "ba")).extracting(CompteDto::role).containsExactly("PARENT", "ELEVE");
    }
}
