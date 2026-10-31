package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ElevesMinistereRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.service.ElevesMinistereService.Filtres;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class ElevesMinistereServiceTest {

    private final ElevesMinistereRepository repository = mock(ElevesMinistereRepository.class);
    private final AnneeScolaireRepository annees = mock(AnneeScolaireRepository.class);
    private final ElevesMinistereService service = new ElevesMinistereService(repository, mock(EtablissementRepository.class),
            annees, mock(ZoneGeographiqueRepository.class), new GenerateurPdf(),
            Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC));

    private Inscription inscription() {
        Eleve eleve = new Eleve();
        eleve.setId(3L);
        eleve.setMatricule("M-0003");
        eleve.setNom("Diallo");
        eleve.setPrenom("Aminata");
        eleve.setSexe("F");
        eleve.setDateNaissance(LocalDate.of(2012, 5, 4));
        Etablissement etab = new Etablissement();
        etab.setId(7L);
        etab.setNom("École 7");
        etab.setSecteur("PUBLIC");
        Classe classe = new Classe();
        classe.setNom("6e A");
        Inscription i = new Inscription();
        i.setId(11L);
        i.setEleve(eleve);
        i.setEtablissement(etab);
        i.setClasse(classe);
        i.setStatut("ACCEPTEE");
        return i;
    }

    @Test
    void laListeRenvoieLesChampsUtilesEtLaPagination() {
        Page<Inscription> page = new PageImpl<>(List.of(inscription()));
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Inscription>>any(), any(Pageable.class)))
                .thenReturn(page);

        var resultat = service.lister(new Filtres(1L, null, null, null, null, null, null, null, null, null), 0, 500);

        assertThat(resultat.total()).isEqualTo(1);
        assertThat(resultat.taille()).isEqualTo(ElevesMinistereService.TAILLE_MAX);   // taille plafonnée
        assertThat(resultat.elements().get(0).nom()).isEqualTo("Diallo");
        assertThat(resultat.elements().get(0).etablissement()).isEqualTo("École 7");
        assertThat(resultat.elements().get(0).classe()).isEqualTo("6e A");
    }

    @Test
    void lExportPdfProduitUnDocument() {
        AnneeScolaire annee = new AnneeScolaire();
        annee.setId(1L);
        annee.setLibelle("2025-2026");
        when(annees.findById(1L)).thenReturn(Optional.of(annee));
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Inscription>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inscription())));

        byte[] pdf = service.exporterPdf(new Filtres(1L, null, null, null, "PUBLIC", null, null, null, "F", null));

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void lesStatutsSontEcritsEnFrancais() {
        assertThat(ElevesMinistereService.libelleStatut("ACCEPTEE")).isEqualTo("Inscrit");
        assertThat(ElevesMinistereService.libelleStatut("EN_ATTENTE")).isEqualTo("En attente");
    }
}
