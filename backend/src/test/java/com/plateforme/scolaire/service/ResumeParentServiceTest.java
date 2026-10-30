package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.Devoir;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.domain.Presence;
import com.plateforme.scolaire.repository.DevoirRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import com.plateforme.scolaire.web.dto.BulletinDto;
import com.plateforme.scolaire.web.dto.EleveDto;
import com.plateforme.scolaire.web.dto.EnfantDto;
import com.plateforme.scolaire.web.dto.FactureDto;
import com.plateforme.scolaire.web.dto.ResumeEnfantDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ResumeParentServiceTest {

    private static final long PARENT = 10L;
    private static final long ELEVE = 1L;
    private static final long CLASSE = 5L;
    private static final long ANNEE = 2L;
    private static final LocalDate JOUR = LocalDate.of(2026, 10, 30);

    private final ParentService parentService = mock(ParentService.class);
    private final MoyennesService moyennes = mock(MoyennesService.class);
    private final PeriodeRepository periodes = mock(PeriodeRepository.class);
    private final PresenceRepository presences = mock(PresenceRepository.class);
    private final DevoirRepository devoirs = mock(DevoirRepository.class);
    private final MesFacturesService factures = mock(MesFacturesService.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-10-30T09:00:00Z"), ZoneOffset.UTC);
    private final ResumeParentService service = new ResumeParentService(parentService, moyennes, periodes, presences,
            devoirs, factures, horloge);

    private static Periode periode(long id, int ordre, String debut, String fin) {
        Periode p = new Periode();
        p.setId(id);
        p.setOrdre(ordre);
        p.setLibelle("Trimestre " + ordre);
        p.setDateDebut(LocalDate.parse(debut));
        p.setDateFin(LocalDate.parse(fin));
        return p;
    }

    private static EnfantDto enfant(Long classeId, Long annee) {
        return new EnfantDto(ELEVE, "M-1", "Diallo", "Fanta", LocalDate.of(2014, 3, 1), "F", "Mère", true, classeId,
                classeId == null ? null : "6e A", 8L, "École", annee, "2026-2027");
    }

    private static Presence presence(String statut, boolean justifie) {
        Presence p = new Presence();
        p.setInscription(new Inscription());
        p.setStatut(statut);
        p.setJustifie(justifie);
        p.setDateSeance(JOUR);
        return p;
    }

    private static FactureDto facture(String reste, String statut, boolean enRetard) {
        return facture(reste, statut, enRetard, "XOF");
    }

    private static FactureDto facture(String reste, String statut, boolean enRetard, String devise) {
        BigDecimal r = new BigDecimal(reste);
        return new FactureDto(1L, 1L, "F-1", "Scolarité", ELEVE, "Diallo", "Fanta", "M-1", 8L, ANNEE, r,
                BigDecimal.ZERO, BigDecimal.ZERO, r, devise, JOUR, statut, JOUR, enRetard);
    }

    @Test
    void laPeriodeCourantePrendCelleQuiContientLaDate() {
        List<Periode> liste = List.of(periode(1, 1, "2026-09-01", "2026-12-15"), periode(2, 2, "2027-01-05", "2027-03-30"));
        assertThat(ResumeParentService.periodeCourante(liste, LocalDate.of(2026, 10, 30)).getId()).isEqualTo(1L);
        // Entre deux périodes : la dernière commencée.
        assertThat(ResumeParentService.periodeCourante(liste, LocalDate.of(2026, 12, 20)).getId()).isEqualTo(1L);
        assertThat(ResumeParentService.periodeCourante(liste, LocalDate.of(2027, 2, 1)).getId()).isEqualTo(2L);
        // Avant la rentrée : la première.
        assertThat(ResumeParentService.periodeCourante(liste, LocalDate.of(2026, 8, 1)).getId()).isEqualTo(1L);
        assertThat(ResumeParentService.periodeCourante(List.of(), JOUR)).isNull();
    }

    @Test
    void leResumeRassembleMoyennePresencesDevoirsEtFactures() {
        when(parentService.mesEnfants(PARENT)).thenReturn(List.of(enfant(CLASSE, ANNEE)));
        when(periodes.findByAnneeScolaireIdOrderByOrdre(ANNEE))
                .thenReturn(List.of(periode(7, 1, "2026-09-01", "2026-12-15")));
        when(moyennes.bulletin(ELEVE, 7L)).thenReturn(new BulletinDto(new EleveDto(ELEVE, "M-1", "Diallo", "Fanta", null, "F"),
                CLASSE, "6e A", 7L, "Trimestre 1", List.of(), new BigDecimal("14.50"), new BigDecimal("12.10"), 3, 28));
        when(presences.findByInscriptionEleveIdAndDateSeanceBetweenOrderByDateSeanceDesc(ELEVE, JOUR.minusDays(6), JOUR))
                .thenReturn(List.of(presence("ABSENT", false), presence("ABSENT", true), presence("RETARD", false),
                        presence("PRESENT", false)));

        Matiere maths = new Matiere();
        maths.setLibelle("Maths");
        List<Devoir> quatre = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Devoir d = new Devoir();
            d.setId((long) i);
            d.setMatiere(maths);
            d.setTitre("Devoir " + i);
            d.setDateLimite(JOUR.plusDays(i));
            quatre.add(d);
        }
        when(devoirs.findByClasseIdAndDateLimiteGreaterThanEqualOrderByDateLimiteAscIdAsc(CLASSE, JOUR)).thenReturn(quatre);
        when(factures.facturesDeMonEnfant(PARENT, ELEVE)).thenReturn(List.of(
                facture("10000", "IMPAYEE", true), facture("2500", "PARTIELLE", false), facture("0", "PAYEE", false)));

        ResumeEnfantDto r = service.resume(PARENT).get(0);

        assertThat(r.periode()).isEqualTo("Trimestre 1");
        assertThat(r.moyenneGenerale()).isEqualByComparingTo("14.50");
        assertThat(r.moyenneClasse()).isEqualByComparingTo("12.10");
        assertThat(r.rang()).isEqualTo(3);
        assertThat(r.effectif()).isEqualTo(28);
        assertThat(r.absencesSemaine()).isEqualTo(2);
        assertThat(r.absencesNonJustifiees()).isEqualTo(1);
        assertThat(r.retardsSemaine()).isEqualTo(1);
        assertThat(r.prochainsDevoirs()).extracting("titre").containsExactly("Devoir 1", "Devoir 2", "Devoir 3");
        assertThat(r.facturesImpayees()).isEqualTo(2);
        assertThat(r.facturesEnRetard()).isEqualTo(1);
        assertThat(r.resteAPayer()).isEqualByComparingTo("12500");
        assertThat(r.devise()).isEqualTo("XOF");
    }

    @Test
    void desFacturesDeDevisesDifferentesNeSontPasAdditionnees() {
        when(parentService.mesEnfants(PARENT)).thenReturn(List.of(enfant(null, null)));
        when(factures.facturesDeMonEnfant(PARENT, ELEVE)).thenReturn(List.of(
                facture("10000", "IMPAYEE", false, "XOF"), facture("20", "IMPAYEE", true, "EUR")));

        ResumeEnfantDto r = service.resume(PARENT).get(0);

        assertThat(r.facturesImpayees()).isEqualTo(2);
        assertThat(r.facturesEnRetard()).isEqualTo(1);
        assertThat(r.resteAPayer()).isNull();
        assertThat(r.devise()).isNull();
    }

    @Test
    void sansClasseOnNeCalculeNiMoyenneNiDevoirs() {
        when(parentService.mesEnfants(PARENT)).thenReturn(List.of(enfant(null, null)));
        when(factures.facturesDeMonEnfant(PARENT, ELEVE)).thenReturn(List.of());

        ResumeEnfantDto r = service.resume(PARENT).get(0);

        assertThat(r.moyenneGenerale()).isNull();
        assertThat(r.periode()).isNull();
        assertThat(r.prochainsDevoirs()).isEmpty();
        assertThat(r.facturesImpayees()).isZero();
        assertThat(r.resteAPayer()).isEqualByComparingTo("0");
        assertThat(r.devise()).isNull();
        verify(moyennes, never()).bulletin(any(), any());
    }

    @Test
    void unBulletinIntrouvableNeCasseLeResume() {
        when(parentService.mesEnfants(PARENT)).thenReturn(List.of(enfant(CLASSE, ANNEE)));
        when(periodes.findByAnneeScolaireIdOrderByOrdre(ANNEE))
                .thenReturn(List.of(periode(7, 1, "2026-09-01", "2026-12-15")));
        when(moyennes.bulletin(ELEVE, 7L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune inscription"));
        when(factures.facturesDeMonEnfant(PARENT, ELEVE)).thenReturn(List.of());

        ResumeEnfantDto r = service.resume(PARENT).get(0);

        assertThat(r.moyenneGenerale()).isNull();
        assertThat(r.classeNom()).isEqualTo("6e A");
    }
}
