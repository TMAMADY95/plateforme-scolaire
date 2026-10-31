package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CommissionCorrection;
import com.plateforme.scolaire.domain.EpreuveExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.MembreCommissionCorrection;
import com.plateforme.scolaire.repository.CommissionCorrectionRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.MembreCommissionCorrectionRepository;
import com.plateforme.scolaire.web.dto.CommissionCorrectionRequest;
import com.plateforme.scolaire.web.dto.CorrectionExamenDto;
import com.plateforme.scolaire.web.dto.MembreCommissionRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Commissions de correction : la correction des copies est confiée à une commission (préfectorale ou communale pour le CEE,
 * régionale pour le BEPC, nationale pour le baccalauréat), organisée en sous-commissions par discipline. Chaque commission
 * a un président, un secrétaire et au moins deux correcteurs, et suit le nombre de copies corrigées.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CorrectionExamenService {

    static final Set<String> ROLES = Set.of("PRESIDENT", "SECRETAIRE", "CORRECTEUR");
    static final int CORRECTEURS_MINIMUM = 2;

    private final CommissionCorrectionRepository commissions;
    private final MembreCommissionCorrectionRepository membres;
    private final EpreuveExamenRepository epreuves;
    private final ExamenNationalRepository examens;
    private final AuditService audit;
    private final Clock clock;

    static String niveau(String niveauOrganisation) {
        return switch (niveauOrganisation == null ? "" : niveauOrganisation.toUpperCase()) {
            case "NATIONAL" -> "Commission nationale de correction";
            case "REGION" -> "Commission régionale de correction";
            default -> "Commission préfectorale ou communale de correction";
        };
    }

    /** Éléments manquants pour qu'une commission puisse corriger : président, secrétaire, deux correcteurs au moins. */
    static List<String> manques(List<String> roles) {
        List<String> manques = new ArrayList<>();
        if (roles.stream().noneMatch("PRESIDENT"::equals)) {
            manques.add("président");
        }
        if (roles.stream().noneMatch("SECRETAIRE"::equals)) {
            manques.add("secrétaire");
        }
        long correcteurs = roles.stream().filter("CORRECTEUR"::equals).count();
        if (correcteurs < CORRECTEURS_MINIMUM) {
            manques.add((CORRECTEURS_MINIMUM - correcteurs) + " correcteur(s)");
        }
        return manques;
    }

    @Transactional(readOnly = true)
    public CorrectionExamenDto correction(Long examenId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen);
        return dto(examen);
    }

    public CorrectionExamenDto creer(Long examenId, CommissionCorrectionRequest r) {
        ExamenNational examen = examen(examenId);
        verifierModifiable(examen);
        EpreuveExamen epreuve = r.epreuveId() == null ? null : epreuveDe(examenId, r.epreuveId());
        if (commissions.deLExamen(examenId).stream().anyMatch(c -> java.util.Objects.equals(c.getEpreuve() == null ? null : c.getEpreuve().getId(), r.epreuveId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, epreuve == null ? "La commission générale existe déjà" : "Cette discipline a déjà sa sous-commission");
        }
        verifierCopies(r.nbCopies(), r.nbCopiesCorrigees());
        commissions.save(nouvelle(examen, epreuve, r.lieu(), r.nbCopies(), r.nbCopiesCorrigees()));
        audit.enregistrer("COMMISSION_CORRECTION_CREEE", "ExamenNational", examenId, Map.of("epreuve", String.valueOf(r.epreuveId())));
        return dto(examen);
    }

    /** Crée la commission générale puis une sous-commission pour chaque épreuve qui n'en a pas encore. */
    public CorrectionExamenDto genererSousCommissions(Long examenId) {
        ExamenNational examen = examen(examenId);
        verifierModifiable(examen);
        List<CommissionCorrection> existantes = commissions.deLExamen(examenId);
        if (existantes.stream().noneMatch(c -> c.getEpreuve() == null)) {
            commissions.save(nouvelle(examen, null, null, 0, 0));
        }
        Set<Long> couvertes = existantes.stream().filter(c -> c.getEpreuve() != null).map(c -> c.getEpreuve().getId()).collect(Collectors.toSet());
        int crees = 0;
        for (EpreuveExamen e : epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId)) {
            if (!couvertes.contains(e.getId())) {
                commissions.save(nouvelle(examen, e, null, 0, 0));
                crees++;
            }
        }
        audit.enregistrer("COMMISSIONS_CORRECTION_GENEREES", "ExamenNational", examenId, Map.of("sousCommissions", crees));
        return dto(examen);
    }

    public CorrectionExamenDto modifier(Long examenId, Long commissionId, CommissionCorrectionRequest r) {
        ExamenNational examen = examen(examenId);
        verifierModifiable(examen);
        CommissionCorrection c = commission(examenId, commissionId);
        if (c.isCloturee()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette commission a clos sa correction");
        }
        verifierCopies(r.nbCopies(), r.nbCopiesCorrigees());
        c.setLieu(r.lieu() == null || r.lieu().isBlank() ? null : r.lieu().trim());
        c.setNbCopies(r.nbCopies());
        c.setNbCopiesCorrigees(r.nbCopiesCorrigees());
        commissions.flush();
        return dto(examen);
    }

    public CorrectionExamenDto supprimer(Long examenId, Long commissionId) {
        ExamenNational examen = examen(examenId);
        verifierModifiable(examen);
        CommissionCorrection c = commission(examenId, commissionId);
        commissions.delete(c);
        commissions.flush();
        audit.enregistrer("COMMISSION_CORRECTION_SUPPRIMEE", "ExamenNational", examenId, Map.of("commission", c.getNom()));
        return dto(examen);
    }

    public CorrectionExamenDto ajouterMembre(Long examenId, Long commissionId, MembreCommissionRequest r) {
        ExamenNational examen = examen(examenId);
        verifierModifiable(examen);
        CommissionCorrection c = commission(examenId, commissionId);
        if (!ROLES.contains(r.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fonction inconnue : " + r.role());
        }
        List<MembreCommissionCorrection> existants = membres.findByCommissionId(commissionId);
        String nom = r.nom().trim();
        if (!"CORRECTEUR".equals(r.role()) && existants.stream().anyMatch(m -> m.getRole().equals(r.role()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette fonction est déjà attribuée dans la commission");
        }
        if (existants.stream().anyMatch(m -> m.getNom().equalsIgnoreCase(nom))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, nom + " fait déjà partie de cette commission");
        }
        MembreCommissionCorrection m = new MembreCommissionCorrection();
        m.setCommission(c);
        m.setRole(r.role());
        m.setNom(nom);
        m.setTelephone(r.telephone() == null || r.telephone().isBlank() ? null : r.telephone().trim());
        m.setCreatedAt(Instant.now(clock));
        membres.save(m);
        audit.enregistrer("COMMISSION_CORRECTION_MEMBRE_AJOUTE", "ExamenNational", examenId, Map.of("role", r.role(), "nom", nom));
        return dto(examen);
    }

    public CorrectionExamenDto retirerMembre(Long examenId, Long commissionId, Long membreId) {
        ExamenNational examen = examen(examenId);
        verifierModifiable(examen);
        CommissionCorrection c = commission(examenId, commissionId);
        if (c.isCloturee()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette commission a clos sa correction");
        }
        MembreCommissionCorrection m = membres.findById(membreId).filter(x -> x.getCommission().getId().equals(commissionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));
        membres.delete(m);
        membres.flush();
        return dto(examen);
    }

    /** Clôt la correction : commission complète et toutes les copies corrigées. */
    public CorrectionExamenDto cloturer(Long examenId, Long commissionId) {
        ExamenNational examen = examen(examenId);
        verifierModifiable(examen);
        CommissionCorrection c = commission(examenId, commissionId);
        List<String> manques = manques(membres.findByCommissionId(commissionId).stream().map(MembreCommissionCorrection::getRole).toList());
        if (!manques.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Commission incomplète : il manque " + String.join(", ", manques));
        }
        if (c.getNbCopies() == 0 || c.getNbCopiesCorrigees() < c.getNbCopies()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Toutes les copies doivent être corrigées (" + c.getNbCopiesCorrigees() + " sur " + c.getNbCopies() + ")");
        }
        c.setCloturee(true);
        audit.enregistrer("COMMISSION_CORRECTION_CLOTUREE", "ExamenNational", examenId, Map.of("commission", c.getNom()));
        return dto(examen);
    }

    private CommissionCorrection nouvelle(ExamenNational examen, EpreuveExamen epreuve, String lieu, int copies, int corrigees) {
        CommissionCorrection c = new CommissionCorrection();
        c.setExamen(examen);
        c.setEpreuve(epreuve);
        c.setNom(epreuve == null ? niveau(examen.getNiveauOrganisation()) : "Sous-commission : " + epreuve.nom());
        c.setLieu(lieu == null || lieu.isBlank() ? null : lieu.trim());
        c.setNbCopies(copies);
        c.setNbCopiesCorrigees(corrigees);
        c.setCreatedAt(Instant.now(clock));
        return c;
    }

    private CorrectionExamenDto dto(ExamenNational examen) {
        List<CommissionCorrection> liste = commissions.deLExamen(examen.getId());
        Map<Long, List<MembreCommissionCorrection>> parCommission = liste.isEmpty() ? Map.of()
                : membres.findByCommissionIdInOrderByRoleAscNomAsc(liste.stream().map(CommissionCorrection::getId).toList()).stream()
                        .collect(Collectors.groupingBy(m -> m.getCommission().getId()));
        List<CorrectionExamenDto.Commission> dtos = liste.stream().map(c -> {
            List<MembreCommissionCorrection> ms = parCommission.getOrDefault(c.getId(), List.of());
            List<String> manques = manques(ms.stream().map(MembreCommissionCorrection::getRole).toList());
            return new CorrectionExamenDto.Commission(c.getId(), c.getEpreuve() == null ? null : c.getEpreuve().getId(), c.getNom(), c.getLieu(),
                    c.getNbCopies(), c.getNbCopiesCorrigees(), c.isCloturee(), manques.isEmpty(), manques,
                    ms.stream().map(m -> new CorrectionExamenDto.Membre(m.getId(), m.getRole(), m.getNom(), m.getTelephone())).toList());
        }).toList();
        Set<Long> avecSousCommission = liste.stream().filter(c -> c.getEpreuve() != null).map(c -> c.getEpreuve().getId()).collect(Collectors.toSet());
        return new CorrectionExamenDto(examen.getId(), examen.getStatutResultats(), niveau(examen.getNiveauOrganisation()),
                epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examen.getId()).stream()
                        .map(e -> new CorrectionExamenDto.EpreuveCitee(e.getId(), e.nom(), avecSousCommission.contains(e.getId()))).toList(),
                dtos, liste.stream().filter(c -> c.getEpreuve() != null).mapToInt(CommissionCorrection::getNbCopies).sum(),
                liste.stream().filter(c -> c.getEpreuve() != null).mapToInt(CommissionCorrection::getNbCopiesCorrigees).sum());
    }

    private static void verifierCopies(int copies, int corrigees) {
        if (corrigees > copies) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nombre de copies corrigées dépasse le nombre de copies reçues");
        }
    }

    private EpreuveExamen epreuveDe(Long examenId, Long epreuveId) {
        return epreuves.findById(epreuveId).filter(e -> e.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Épreuve inconnue pour cet examen"));
    }

    private CommissionCorrection commission(Long examenId, Long commissionId) {
        return commissions.findById(commissionId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commission introuvable"));
    }

    private void verifierModifiable(ExamenNational examen) {
        verifierDroit(examen);
        if ("PROCLAME".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les résultats sont proclamés : la correction est close");
        }
    }

    /** Le ministère gère toutes les commissions ; une direction régionale seulement celles des examens non nationaux. */
    private void verifierDroit(ExamenNational examen) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean ministere = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_MINISTERE".equals(a.getAuthority()));
        if (!ministere && "NATIONAL".equalsIgnoreCase(examen.getNiveauOrganisation())) {
            throw new AccessDeniedException("La correction d'un examen national relève du ministère");
        }
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
