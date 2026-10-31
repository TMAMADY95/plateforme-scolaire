package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.PlanAction;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InspectionRepository;
import com.plateforme.scolaire.repository.PlanActionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.PlanActionDto;
import com.plateforme.scolaire.web.dto.PlanActionRequest;
import com.plateforme.scolaire.web.dto.PlanStatutRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Plans d'action après inspection. Le ministère et les directions régionales les créent, les suivent et peuvent les
 * annuler ; la direction de l'établissement les voit et signale leur avancement (en cours, terminé).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlanActionService {

    static final Set<String> STATUTS = Set.of("A_FAIRE", "EN_COURS", "TERMINE", "ANNULE");
    static final Set<String> STATUTS_DIRECTION = Set.of("EN_COURS", "TERMINE");

    private final PlanActionRepository planRepository;
    private final EtablissementRepository etablissementRepository;
    private final InspectionRepository inspectionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PerimetreService perimetreService;
    private final RegionService regionService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PlanActionDto> deLEtablissement(Long etablissementId) {
        LocalDate aujourdhui = LocalDate.now(clock);
        return planRepository.deLEtablissement(etablissementId).stream().map(p -> PlanActionDto.de(p, aujourdhui)).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanActionDto> nationale(Long zoneId) {
        List<Long> ids = (zoneId == null ? etablissementRepository.findAll() : etablissementRepository.findByZoneEtSousZones(zoneId))
                .stream().map(Etablissement::getId).toList();
        return liste(ids);
    }

    @Transactional(readOnly = true)
    public List<PlanActionDto> regionale() {
        return liste(List.copyOf(regionService.etablissementsDeLaRegion().keySet()));
    }

    private List<PlanActionDto> liste(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        LocalDate aujourdhui = LocalDate.now(clock);
        return planRepository.dansLePerimetre(ids).stream().map(p -> PlanActionDto.de(p, aujourdhui)).toList();
    }

    /** L'accès à l'établissement est contrôlé par le contrôleur (ministère et directions régionales). */
    public PlanActionDto creer(Long etablissementId, PlanActionRequest r) {
        Etablissement e = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        LocalDate aujourdhui = LocalDate.now(clock);
        if (r.echeance().isBefore(aujourdhui)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'échéance ne peut pas être dans le passé");
        }
        PlanAction p = new PlanAction();
        p.setEtablissement(e);
        if (r.inspectionId() != null) {
            var inspection = inspectionRepository.findById(r.inspectionId())
                    .filter(i -> i.getEtablissement().getId().equals(etablissementId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inspection inconnue pour cet établissement"));
            p.setInspection(inspection);
        }
        p.setCreePar(utilisateurRepository.getReferenceById(moi().getId()));
        p.setDescription(r.description().trim());
        p.setEcheance(r.echeance());
        p.setCreatedAt(Instant.now(clock));
        PlanAction enregistre = planRepository.save(p);
        auditService.enregistrer("PLAN_ACTION_CREE", "PlanAction", enregistre.getId(),
                Map.of("etablissement", e.getNom(), "echeance", r.echeance().toString()));
        return planRepository.deLEtablissement(etablissementId).stream()
                .filter(x -> x.getId().equals(enregistre.getId())).findFirst()
                .map(x -> PlanActionDto.de(x, aujourdhui)).orElseThrow();
    }

    public PlanActionDto changerStatut(Long planId, PlanStatutRequest r) {
        PlanAction p = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan d'action introuvable"));
        Long etablissementId = p.getEtablissement().getId();
        if (!STATUTS.contains(r.statut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu");
        }
        boolean supervision = estSuperviseur() && perimetreService.peutAccederEtablissement(etablissementId);
        boolean direction = perimetreService.peutGererEtablissement(etablissementId);
        if (!supervision && !direction) {
            throw new AccessDeniedException("Plan d'action hors de votre périmètre");
        }
        if (!supervision && !STATUTS_DIRECTION.contains(r.statut())) {
            throw new AccessDeniedException("La direction peut seulement indiquer « en cours » ou « terminé »");
        }
        p.setStatut(r.statut());
        p.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        p.setTermineLe("TERMINE".equals(r.statut()) ? Instant.now(clock) : null);
        auditService.enregistrer("PLAN_ACTION_MODIFIE", "PlanAction", p.getId(),
                Map.of("etablissement", p.getEtablissement().getNom(), "statut", r.statut()));
        return PlanActionDto.de(p, LocalDate.now(clock));
    }

    private boolean estSuperviseur() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        return p != null && p.getAffectations().stream()
                .anyMatch(a -> a.role() == Role.MINISTERE || a.role() == Role.DIRECTION_REGIONALE);
    }

    private UtilisateurPrincipal moi() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null) {
            throw new AccessDeniedException("Authentification requise");
        }
        return p;
    }
}
