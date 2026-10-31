package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AffectationRoleRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.web.dto.ZoneDto;
import com.plateforme.scolaire.web.dto.ZoneRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AdministrationZonesTest {

    private final ZoneGeographiqueRepository zoneRepository = mock(ZoneGeographiqueRepository.class);
    private final EtablissementRepository etablissementRepository = mock(EtablissementRepository.class);
    private final AffectationRoleRepository affectationRoleRepository = mock(AffectationRoleRepository.class);

    private final AdministrationService service = new AdministrationService(zoneRepository,
            etablissementRepository, mock(UtilisateurRepository.class), affectationRoleRepository,
            mock(ClasseRepository.class), mock(InscriptionRepository.class), mock(PasswordEncoder.class),
            mock(AuditService.class), mock(com.plateforme.scolaire.repository.CycleScolaireRepository.class),
            mock(com.plateforme.scolaire.repository.TypeEtablissementRepository.class));

    private ZoneGeographique zone(Long id, String nom, ZoneGeographique parent) {
        ZoneGeographique z = new ZoneGeographique();
        z.setId(id);
        z.setNom(nom);
        z.setTypeZone("région");
        z.setParent(parent);
        when(zoneRepository.findById(id)).thenReturn(Optional.of(z));
        return z;
    }

    @Test
    void uneZoneNePeutPasDevenirSonPropreParentNiCeluiDeSesAscendants() {
        ZoneGeographique pays = zone(1L, "Pays", null);
        ZoneGeographique region = zone(2L, "Région", pays);
        ZoneGeographique dept = zone(3L, "Département", region);

        assertThatThrownBy(() -> service.modifierZone(2L, new ZoneRequest(2L, "Région", "région")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.modifierZone(1L, new ZoneRequest(3L, "Pays", "pays")))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(region.getParent()).isSameAs(pays);
        assertThat(dept.getParent()).isSameAs(region);
    }

    @Test
    void onPeutRenommerEtDeplacerUneZone() {
        ZoneGeographique pays = zone(1L, "Pays", null);
        ZoneGeographique region = zone(2L, "Région", pays);
        ZoneGeographique autre = zone(4L, "Autre pays", null);

        ZoneDto dto = service.modifierZone(2L, new ZoneRequest(4L, "  Nouvelle région ", "région"));

        assertThat(dto.nom()).isEqualTo("Nouvelle région");
        assertThat(region.getParent()).isSameAs(autre);
    }

    @Test
    void uneZoneUtiliseeNeSeSupprimePas() {
        zone(2L, "Région", null);

        when(zoneRepository.existsByParentId(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.supprimerZone(2L)).isInstanceOf(ResponseStatusException.class);

        when(zoneRepository.existsByParentId(2L)).thenReturn(false);
        when(etablissementRepository.existsByZoneId(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.supprimerZone(2L)).isInstanceOf(ResponseStatusException.class);

        when(etablissementRepository.existsByZoneId(2L)).thenReturn(false);
        when(affectationRoleRepository.existsByZoneId(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.supprimerZone(2L)).isInstanceOf(ResponseStatusException.class);

        verify(zoneRepository, never()).delete(any(ZoneGeographique.class));
    }

    @Test
    void uneZoneVideEstSupprimee() {
        ZoneGeographique z = zone(2L, "Région", null);

        service.supprimerZone(2L);

        verify(zoneRepository).delete(z);
    }
}
