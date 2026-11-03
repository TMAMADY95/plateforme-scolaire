package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.PreparationCentre;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.PreparationCentreRepository;
import com.plateforme.scolaire.security.AccesCentreService;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.PreparationCentreDto;
import com.plateforme.scolaire.web.dto.PreparationCentreRequest;
import com.plateforme.scolaire.web.dto.ResumePreparationDto;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Préparation matérielle des centres (règlement général) : salles, tables-bancs, affichage, secrétariat, sécurité, personnel
 * et matériel. Un centre est « prêt » quand les sept travaux sont faits.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PreparationCentreService {

    /** Code, libellé, travaux préparatoires : dans l'ordre du règlement. */
    static final Map<String, String[]> ELEMENTS = new LinkedHashMap<>();

    static {
        ELEMENTS.put("SALLES", new String[] {"Salles", "Nettoyage et aménagement"});
        ELEMENTS.put("TABLES_BANCS", new String[] {"Tables-bancs", "Vérification et disposition"});
        ELEMENTS.put("AFFICHAGE", new String[] {"Affichage", "Numérotation des salles et des tables"});
        ELEMENTS.put("SECRETARIAT", new String[] {"Secrétariat", "Installation des espaces administratifs"});
        ELEMENTS.put("SECURITE", new String[] {"Sécurité", "Organisation du contrôle des accès"});
        ELEMENTS.put("PERSONNEL", new String[] {"Personnel", "Répartition des surveillants"});
        ELEMENTS.put("MATERIEL", new String[] {"Matériel", "Préparation des feuilles et des fournitures"});
    }

    private final PreparationCentreRepository preparations;
    private final CentreExamenRepository centres;
    private final AccesCentreService acces;
    private final PerimetreService perimetreService;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : le centre est prêt quand chacun des sept travaux figure parmi les travaux faits. */
    static boolean pret(Collection<String> faits) {
        return faits.containsAll(ELEMENTS.keySet());
    }

    @Transactional(readOnly = true)
    public PreparationCentreDto preparation(Long examenId, Long centreId) {
        return dto(centre(examenId, centreId), preparations.findByCentreId(centreId));
    }

    public PreparationCentreDto marquer(Long examenId, Long centreId, PreparationCentreRequest r) {
        CentreExamen centre = centre(examenId, centreId);
        if (!ELEMENTS.containsKey(r.element())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Travail inconnu : " + r.element());
        }
        PreparationCentre p = preparations.findByCentreIdAndElement(centreId, r.element()).orElseGet(() -> {
            PreparationCentre n = new PreparationCentre();
            n.setCentre(centre);
            n.setElement(r.element());
            return n;
        });
        String observation = r.observation() == null || r.observation().isBlank() ? null : r.observation().trim();
        p.setFait(r.fait());
        p.setObservation(observation);
        var moi = perimetreService.utilisateurCourant();
        p.setFaitLe(r.fait() ? Instant.now(clock) : null);
        p.setFaitPar(r.fait() && moi != null ? moi.getUsername() : null);
        preparations.save(p);
        audit.enregistrer("PREPARATION_CENTRE_" + (r.fait() ? "FAIT" : "ANNULE"), "CentreExamen", centreId,
                Map.of("element", r.element()));
        return dto(centre, preparations.findByCentreId(centreId));
    }

    /** État de tous les centres de l'examen visibles par l'utilisateur, pour le suivi par les directions. */
    @Transactional(readOnly = true)
    public List<ResumePreparationDto> suivi(Long examenId) {
        Map<Long, List<PreparationCentre>> parCentre = preparations.deLExamen(examenId).stream()
                .collect(Collectors.groupingBy(p -> p.getCentre().getId()));
        return centres.deLExamen(examenId).stream().filter(c -> acces.peut(c.getId())).map(c -> {
            List<String> faits = parCentre.getOrDefault(c.getId(), List.of()).stream()
                    .filter(PreparationCentre::isFait).map(PreparationCentre::getElement).toList();
            return new ResumePreparationDto(c.getId(), c.getNom(), c.getZone() == null ? null : c.getZone().getNom(),
                    faits.size(), ELEMENTS.size(), pret(faits));
        }).toList();
    }

    private PreparationCentreDto dto(CentreExamen centre, List<PreparationCentre> liste) {
        Map<String, PreparationCentre> parElement = liste.stream().collect(Collectors.toMap(PreparationCentre::getElement, p -> p, (a, b) -> a));
        List<PreparationCentreDto.Element> elements = ELEMENTS.entrySet().stream().map(e -> {
            PreparationCentre p = parElement.get(e.getKey());
            return new PreparationCentreDto.Element(e.getKey(), e.getValue()[0], e.getValue()[1], p != null && p.isFait(),
                    p == null ? null : p.getObservation(), p == null ? null : p.getFaitLe(), p == null ? null : p.getFaitPar());
        }).toList();
        List<String> faits = elements.stream().filter(PreparationCentreDto.Element::fait).map(PreparationCentreDto.Element::code).toList();
        return new PreparationCentreDto(centre.getId(), centre.getNom(), pret(faits), faits.size(), ELEMENTS.size(), elements);
    }

    private CentreExamen centre(Long examenId, Long centreId) {
        return centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
    }
}
