package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.domain.Utilisateur;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.web.dto.ComptePersonnelDto;
import com.plateforme.scolaire.web.dto.DirectionRequest;
import com.plateforme.scolaire.web.dto.EtablissementDto;
import com.plateforme.scolaire.web.dto.EtablissementRequest;
import com.plateforme.scolaire.web.dto.SyntheseNationaleDto;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AdministrationServiceTest {

    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
    private final AffectationRoleRepository affectationRoleRepository = mock(AffectationRoleRepository.class);
    private final ClasseRepository classeRepository = mock(ClasseRepository.class);
    private final InscriptionRepository inscriptionRepository = mock(InscriptionRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final AdministrationService service = new AdministrationService(
            mock(ZoneGeographiqueRepository.class), etablissementRepository, utilisateurRepository,
            affectationRoleRepository, classeRepository, inscriptionRepository, passwordEncoder,
            mock(AuditService.class));

    private static EtablissementRequest requete(String code) {
        return new EtablissementRequest(code, "École test", null, "PRIMAIRE", null, 300);
    }

    @Test
    void lesCodesOfficielsSontUniquesEtNormalises() {
        when(etablissementRepository.existsByCodeOfficiel("ECO-1")).thenReturn(true);

        assertThatThrownBy(() -> service.creerEtablissement(requete(" eco-1 ")))
                .isInstanceOf(ResponseStatusException.class);
        verify(etablissementRepository, never()).save(any(Etablissement.class));
    }

    @Test
    void unNouvelEtablissementEstActif() {
        when(etablissementRepository.save(any(Etablissement.class))).thenAnswer(i -> i.getArgument(0));

        EtablissementDto dto = service.creerEtablissement(requete("eco-2"));

        assertThat(dto.codeOfficiel()).isEqualTo("ECO-2");
        assertThat(dto.statut()).isEqualTo("ACTIF");
    }

    @Test
    void laDirectionNEstCreeeQueDansUnEtablissementActif() {
        Etablissement ferme = new Etablissement();
        ferme.setId(3L);
        ferme.setStatut("FERME");
        when(etablissementRepository.findById(3L)).thenReturn(Optional.of(ferme));

        assertThatThrownBy(() -> service.creerDirection(3L,
                new DirectionRequest("d@example.org", "Nom", "Prénom", null, "motdepasse-long")))
                .isInstanceOf(ResponseStatusException.class);
        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void leCompteDeDirectionDoitChangerSonMotDePasseEtEstLimiteASonEtablissement() {
        Etablissement actif = new Etablissement();
        actif.setId(4L);
        when(etablissementRepository.findById(4L)).thenReturn(Optional.of(actif));
        when(passwordEncoder.encode("motdepasse-long")).thenReturn("hash");

        ComptePersonnelDto compte = service.creerDirection(4L,
                new DirectionRequest("d@example.org", "Nom", "Prénom", null, "motdepasse-long"));

        ArgumentCaptor<Utilisateur> utilisateur = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(utilisateur.capture());
        assertThat(utilisateur.getValue().isDoitChangerMotDePasse()).isTrue();
        assertThat(utilisateur.getValue().getMotDePasseHash()).isEqualTo("hash");

        ArgumentCaptor<AffectationRole> role = ArgumentCaptor.forClass(AffectationRole.class);
        verify(affectationRoleRepository).save(role.capture());
        assertThat(role.getValue().getRole()).isEqualTo(Role.DIRECTION_ETABLISSEMENT);
        assertThat(role.getValue().getPerimetre()).isEqualTo(Perimetre.ETABLISSEMENT);
        assertThat(role.getValue().getEtablissement()).isSameAs(actif);
        assertThat(compte.role()).isEqualTo("DIRECTION_ETABLISSEMENT");
    }

    @Test
    void laSyntheseAgregueLesCompteurs() {
        when(etablissementRepository.count()).thenReturn(10L);
        when(etablissementRepository.countByStatut("ACTIF")).thenReturn(8L);
        when(etablissementRepository.countByStatut("SUSPENDU")).thenReturn(1L);
        when(etablissementRepository.countByStatut("FERME")).thenReturn(1L);
        when(classeRepository.countByAnneeScolaireId(2L)).thenReturn(40L);
        when(inscriptionRepository.countByAnneeScolaireIdAndStatut(2L, "ACCEPTEE")).thenReturn(1200L);

        SyntheseNationaleDto s = service.synthese(2L);

        assertThat(s.etablissements()).isEqualTo(10L);
        assertThat(s.actifs()).isEqualTo(8L);
        assertThat(s.classes()).isEqualTo(40L);
        assertThat(s.elevesInscrits()).isEqualTo(1200L);
    }
}
