package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.CoefficientMatiere;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.CoefficientMatiereRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.web.dto.CoefficientMatiereDto;
import com.plateforme.scolaire.web.dto.CoefficientsRequest;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Coefficients des matières d'une classe : ils pondèrent la moyenne générale (1 par défaut). */
@Service
@RequiredArgsConstructor
@Transactional
public class CoefficientMatiereService {

    private final CoefficientMatiereRepository coefficientRepository;
    private final AffectationEnseignantRepository affectationRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final AuditService auditService;

    /** Coefficients définis pour la classe (matière -> coefficient) ; sert au calcul des moyennes. */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> coefficientsDe(Long classeId) {
        Map<Long, BigDecimal> resultat = new HashMap<>();
        coefficientRepository.findByClasseId(classeId)
                .forEach(c -> resultat.put(c.getMatiere().getId(), c.getCoefficient()));
        return resultat;
    }

    /** Les matières enseignées dans la classe, plus celles qui ont déjà un coefficient. */
    @Transactional(readOnly = true)
    public List<CoefficientMatiereDto> lister(Long classeId) {
        trouverClasse(classeId);
        Map<Long, BigDecimal> definis = coefficientsDe(classeId);
        Map<Long, Matiere> matieres = new TreeMap<>();
        affectationRepository.findByClasseId(classeId).forEach(a -> matieres.put(a.getMatiere().getId(), a.getMatiere()));
        coefficientRepository.findByClasseId(classeId).forEach(c -> matieres.put(c.getMatiere().getId(), c.getMatiere()));
        return matieres.values().stream()
                .sorted(Comparator.comparing(Matiere::getLibelle))
                .map(m -> new CoefficientMatiereDto(m.getId(), m.getCode(), m.getLibelle(),
                        definis.getOrDefault(m.getId(), BigDecimal.ONE), definis.containsKey(m.getId())))
                .toList();
    }

    public List<CoefficientMatiereDto> enregistrer(Long classeId, CoefficientsRequest requete) {
        Classe classe = trouverClasse(classeId);
        Map<Long, CoefficientMatiere> existants = new HashMap<>();
        coefficientRepository.findByClasseId(classeId).forEach(c -> existants.put(c.getMatiere().getId(), c));

        Map<Long, BigDecimal> demandes = new LinkedHashMap<>();
        for (CoefficientsRequest.Ligne l : requete.lignes()) {
            if (demandes.put(l.matiereId(), l.coefficient()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matière en double dans la demande");
            }
        }
        Map<String, Object> changements = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : demandes.entrySet()) {
            Matiere matiere = matiereRepository.findById(e.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Matière introuvable : " + e.getKey()));
            CoefficientMatiere actuel = existants.get(matiere.getId());
            BigDecimal avant = actuel == null ? BigDecimal.ONE : actuel.getCoefficient();
            if (avant.compareTo(e.getValue()) == 0 && actuel != null) {
                continue;
            }
            if (actuel == null) {
                actuel = new CoefficientMatiere();
                actuel.setClasse(classe);
                actuel.setMatiere(matiere);
            }
            actuel.setCoefficient(e.getValue());
            coefficientRepository.save(actuel);
            changements.put(matiere.getLibelle(), avant.toPlainString() + " → " + e.getValue().toPlainString());
        }
        if (!changements.isEmpty()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("classeId", classeId);
            details.put("changements", changements);
            auditService.enregistrer("MODIFICATION_COEFFICIENTS", "classe", classeId, details);
        }
        coefficientRepository.flush();
        return lister(classeId);
    }

    private Classe trouverClasse(Long classeId) {
        return classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
    }
}
