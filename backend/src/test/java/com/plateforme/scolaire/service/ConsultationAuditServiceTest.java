package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.JournalAudit;
import com.plateforme.scolaire.repository.JournalAuditRepository;
import com.plateforme.scolaire.web.dto.PageAuditDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ConsultationAuditServiceTest {

    private final JournalAuditRepository repository = mock(JournalAuditRepository.class);
    private final ResolveurContexteAudit resolveur = mock(ResolveurContexteAudit.class);
    private final ConsultationAuditService service = new ConsultationAuditService(repository, resolveur);

    {
        when(resolveur.decrire(any())).thenReturn(new ResolveurContexteAudit.Description("Résumé", List.of()));
    }

    private Pageable pageDemandee(int page, int taille) {
        JournalAudit j = new JournalAudit();
        j.setAction("CREATION_ZONE");
        j.setEntite("zone_geographique");
        Page<JournalAudit> resultat = new PageImpl<>(List.of(j), PageRequest.of(0, 1), 1);
        when(repository.rechercher(any(), any(), any(Pageable.class))).thenReturn(resultat);

        service.rechercher("  zone ", null, page, taille);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).rechercher(eq("zone"), eq(""), pageable.capture());
        return pageable.getValue();
    }

    @Test
    void laTailleDePageEstBorneeEtLesFiltresNettoyes() {
        Pageable p = pageDemandee(-3, 100000);

        assertThat(p.getPageSize()).isEqualTo(100);
        assertThat(p.getPageNumber()).isZero();
    }

    @Test
    void sansTailleOnPrendLaValeurParDefautEtLeTriEstAntechronologique() {
        Pageable p = pageDemandee(2, 0);

        assertThat(p.getPageSize()).isEqualTo(25);
        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(p.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void laReponseExposeLeTotalEtLesEntrees() {
        JournalAudit j = new JournalAudit();
        j.setAction("CREATION_ZONE");
        j.setEntite("zone_geographique");
        when(repository.rechercher(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(j), PageRequest.of(0, 25), 42));

        PageAuditDto dto = service.rechercher("", "", 0, 25);

        assertThat(dto.total()).isEqualTo(42);
        assertThat(dto.contenu()).hasSize(1);
        assertThat(dto.contenu().get(0).utilisateurEmail()).isNull();
    }
}
