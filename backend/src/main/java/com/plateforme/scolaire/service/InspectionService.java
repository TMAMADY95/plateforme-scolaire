package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inspection;
import com.plateforme.scolaire.domain.SignalementSecurite;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InspectionRepository;
import com.plateforme.scolaire.repository.SignalementSecuriteRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.AutorisationRequest;
import com.plateforme.scolaire.web.dto.InspectionDto;
import com.plateforme.scolaire.web.dto.InspectionRequest;
import com.plateforme.scolaire.web.dto.SignalementDto;
import com.plateforme.scolaire.web.dto.SignalementRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
 * Inspections, signalements de sécurité et autorisation des établissements. Les contrôleurs réservent ces
 * opérations au ministère et aux directions régionales, chacun dans son périmètre ; seule la décision
 * d'autorisation est réservée au ministère.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InspectionService {

    static final Set<String> RESULTATS = Set.of("CONFORME", "NON_CONFORME");
    static final Set<String> CATEGORIES = Set.of("INCENDIE", "BATIMENT", "EAU_HYGIENE", "CLOTURE", "AUTRE");
    static final Set<String> GRAVITES = Set.of("FAIBLE", "MOYENNE", "ELEVEE");
    static final Set<String> AUTORISATIONS = Set.of("EN_ATTENTE", "AUTORISE", "NON_AUTORISE", "RETIRE");

    private final InspectionRepository inspectionRepository;
    private final SignalementSecuriteRepository signalementRepository;
    private final EtablissementRepository etablissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PerimetreService perimetreService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<InspectionDto> inspections(Long etablissementId) {
        etablissement(etablissementId);
        return inspectionRepository.deLEtablissement(etablissementId).stream().map(InspectionDto::de).toList();
    }

    public InspectionDto inspecter(Long etablissementId, InspectionRequest r) {
        Etablissement e = etablissement(etablissementId);
        if (!RESULTATS.contains(r.resultat())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Résultat inconnu : CONFORME ou NON_CONFORME");
        }
        if (r.dateInspection().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de l'inspection ne peut pas être dans le futur");
        }
        Inspection i = new Inspection();
        i.setEtablissement(e);
        i.setInspecteur(utilisateurRepository.getReferenceById(moiId()));
        i.setDateInspection(r.dateInspection());
        i.setResultat(r.resultat());
        i.setObservations(vide(r.observations()));
        i.setCreatedAt(Instant.now(clock));
        Inspection enregistree = inspectionRepository.save(i);
        auditService.enregistrer("INSPECTION_ENREGISTREE", "Inspection", enregistree.getId(),
                Map.of("etablissement", e.getNom(), "resultat", r.resultat()));
        return InspectionDto.de(inspectionRepository.deLEtablissement(etablissementId).stream()
                .filter(x -> x.getId().equals(enregistree.getId())).findFirst().orElse(enregistree));
    }

    @Transactional(readOnly = true)
    public List<SignalementDto> signalements(Long etablissementId) {
        etablissement(etablissementId);
        return signalementRepository.deLEtablissement(etablissementId).stream().map(SignalementDto::de).toList();
    }

    public SignalementDto signaler(Long etablissementId, SignalementRequest r) {
        Etablissement e = etablissement(etablissementId);
        if (!CATEGORIES.contains(r.categorie())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie de signalement inconnue");
        }
        if (!GRAVITES.contains(r.gravite())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gravité inconnue : FAIBLE, MOYENNE ou ELEVEE");
        }
        SignalementSecurite s = new SignalementSecurite();
        s.setEtablissement(e);
        s.setSignalePar(utilisateurRepository.getReferenceById(moiId()));
        s.setCategorie(r.categorie());
        s.setGravite(r.gravite());
        s.setDescription(r.description().trim());
        s.setCreatedAt(Instant.now(clock));
        SignalementSecurite enregistre = signalementRepository.save(s);
        auditService.enregistrer("SIGNALEMENT_SECURITE_CREE", "SignalementSecurite", enregistre.getId(),
                Map.of("etablissement", e.getNom(), "gravite", r.gravite()));
        return SignalementDto.de(signalementRepository.deLEtablissement(etablissementId).stream()
                .filter(x -> x.getId().equals(enregistre.getId())).findFirst().orElse(enregistre));
    }

    public SignalementDto resoudre(Long signalementId) {
        SignalementSecurite s = signalementRepository.findById(signalementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));
        if (!perimetreService.peutAccederEtablissement(s.getEtablissement().getId())) {
            throw new AccessDeniedException("Établissement hors de votre périmètre");
        }
        if ("RESOLU".equals(s.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce signalement est déjà résolu");
        }
        s.setStatut("RESOLU");
        s.setResoluAt(Instant.now(clock));
        auditService.enregistrer("SIGNALEMENT_SECURITE_RESOLU", "SignalementSecurite", s.getId(),
                Map.of("etablissement", s.getEtablissement().getNom()));
        return SignalementDto.de(s);
    }

    /** Décision d'autorisation : un retrait ou un refus demande un motif. */
    public void modifierAutorisation(Long etablissementId, AutorisationRequest r) {
        Etablissement e = etablissement(etablissementId);
        if (!AUTORISATIONS.contains(r.statut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut d'autorisation inconnu");
        }
        boolean negatif = "NON_AUTORISE".equals(r.statut()) || "RETIRE".equals(r.statut());
        if (negatif && vide(r.motif()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un motif est requis pour un refus ou un retrait");
        }
        if (r.dateAutorisation() != null && r.dateAutorisation().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de l'arrêté ne peut pas être dans le futur");
        }
        e.setAutorisationStatut(r.statut());
        e.setAutorisationReference(vide(r.reference()));
        e.setAutorisationDate(r.dateAutorisation());
        e.setMotifStatut(vide(r.motif()));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("etablissement", e.getNom());
        details.put("statut", r.statut());
        auditService.enregistrer("AUTORISATION_MODIFIEE", "Etablissement", e.getId(), details);
    }

    private Etablissement etablissement(Long id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
    }

    private Long moiId() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null) {
            throw new AccessDeniedException("Authentification requise");
        }
        return p.getId();
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
