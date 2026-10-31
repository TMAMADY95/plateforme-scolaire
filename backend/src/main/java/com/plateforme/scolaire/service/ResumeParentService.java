package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Devoir;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.domain.Presence;
import com.plateforme.scolaire.repository.DevoirRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.repository.PresenceRepository;
import com.plateforme.scolaire.web.dto.BulletinDto;
import com.plateforme.scolaire.web.dto.EnfantDto;
import com.plateforme.scolaire.web.dto.FactureDto;
import com.plateforme.scolaire.web.dto.ResumeEnfantDto;
import com.plateforme.scolaire.web.dto.ResumeEnfantDto.DevoirResume;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Le point du jour pour chaque enfant d'un parent : moyenne, présences, prochains devoirs, factures. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeParentService {

    static final int JOURS_PRESENCES = 7;
    static final int NB_DEVOIRS = 3;

    private final ParentService parentService;
    private final MoyennesService moyennesService;
    private final PeriodeRepository periodeRepository;
    private final PresenceRepository presenceRepository;
    private final DevoirRepository devoirRepository;
    private final MesFacturesService mesFacturesService;
    private final Clock clock;

    public List<ResumeEnfantDto> resume(Long parentUtilisateurId) {
        LocalDate aujourdhui = LocalDate.now(clock);
        return parentService.mesEnfants(parentUtilisateurId).stream()
                .map(e -> resumeDe(parentUtilisateurId, e, aujourdhui)).toList();
    }

    private ResumeEnfantDto resumeDe(Long parentId, EnfantDto enfant, LocalDate aujourdhui) {
        BulletinDto bulletin = bulletin(enfant, aujourdhui);

        List<Presence> presences = presenceRepository.findByInscriptionEleveIdAndDateSeanceBetweenOrderByDateSeanceDesc(
                enfant.eleveId(), aujourdhui.minusDays(JOURS_PRESENCES - 1L), aujourdhui);
        int absences = (int) presences.stream().filter(p -> "ABSENT".equals(p.getStatut())).count();
        int nonJustifiees = (int) presences.stream()
                .filter(p -> "ABSENT".equals(p.getStatut()) && !p.isJustifie()).count();
        int retards = (int) presences.stream().filter(p -> "RETARD".equals(p.getStatut())).count();

        List<DevoirResume> devoirs = enfant.classeId() == null ? List.of()
                : devoirRepository.findByClasseIdAndDateLimiteGreaterThanEqualOrderByDateLimiteAscIdAsc(
                        enfant.classeId(), aujourdhui).stream().limit(NB_DEVOIRS).map(this::devoir).toList();

        List<FactureDto> aPayer = mesFacturesService.facturesDeMonEnfant(parentId, enfant.eleveId()).stream()
                .filter(f -> f.resteAPayer() != null && f.resteAPayer().signum() > 0).toList();
        // On n'additionne que des montants dans une même devise ; sinon pas de total (reste = null, devise = null).
        boolean uneSeuleDevise = aPayer.stream().map(FactureDto::devise).distinct().count() <= 1;
        BigDecimal reste = !uneSeuleDevise ? null
                : aPayer.stream().map(FactureDto::resteAPayer).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumeEnfantDto(enfant.eleveId(), enfant.prenom(), enfant.nom(), enfant.relation(),
                enfant.classeId(), enfant.classeNom(),
                bulletin == null ? null : bulletin.periode(),
                bulletin == null ? null : bulletin.moyenneGenerale(),
                bulletin == null ? null : bulletin.moyenneGeneraleClasse(),
                bulletin == null ? null : bulletin.rang(),
                bulletin == null ? 0 : bulletin.effectif(),
                absences, nonJustifiees, retards, devoirs,
                aPayer.size(), (int) aPayer.stream().filter(FactureDto::enRetard).count(), reste,
                aPayer.isEmpty() || !uneSeuleDevise ? null : aPayer.get(0).devise());
    }

    private DevoirResume devoir(Devoir d) {
        return new DevoirResume(d.getId(), d.getMatiere().getLibelle(), d.getTitre(), d.getDateLimite());
    }

    /** Bulletin de la période en cours ; null si l'enfant n'a pas de classe ou si aucune période n'existe. */
    private BulletinDto bulletin(EnfantDto enfant, LocalDate aujourdhui) {
        if (enfant.classeId() == null || enfant.anneeScolaireId() == null) {
            return null;
        }
        Periode periode = periodeCourante(PeriodesApplicables.pour(periodeRepository.findByAnneeScolaireIdOrderByOrdre(
                enfant.anneeScolaireId()), enfant.etablissementId()), aujourdhui);
        if (periode == null) {
            return null;
        }
        try {
            return moyennesService.bulletin(enfant.eleveId(), periode.getId());
        } catch (ResponseStatusException e) {
            return null; // pas d'inscription acceptée sur l'année, etc. : le reste du résumé reste utile
        }
    }

    /** La période qui contient la date, sinon la dernière commencée, sinon la première. */
    static Periode periodeCourante(List<Periode> periodes, LocalDate jour) {
        if (periodes.isEmpty()) {
            return null;
        }
        Periode derniereCommencee = null;
        for (Periode p : periodes) {
            if (!jour.isBefore(p.getDateDebut()) && !jour.isAfter(p.getDateFin())) {
                return p;
            }
            if (!jour.isBefore(p.getDateDebut())) {
                derniereCommencee = p;
            }
        }
        return derniereCommencee != null ? derniereCommencee : periodes.get(0);
    }
}
