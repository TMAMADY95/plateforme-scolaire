package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Devoir;
import com.plateforme.scolaire.domain.DevoirFait;
import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.repository.DevoirFaitRepository;
import com.plateforme.scolaire.repository.DevoirRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.DevoirDto;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Suivi « fait / pas fait » des devoirs. Un élève coche ses propres devoirs, un parent ceux de ses enfants ;
 * l'équipe pédagogique voit seulement combien d'élèves ont terminé, pas lesquels.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DevoirFaitService {

    private final DevoirFaitRepository devoirFaitRepository;
    private final DevoirRepository devoirRepository;
    private final DevoirService devoirService;
    private final EleveRepository eleveRepository;
    private final LienParentalRepository lienParentalRepository;
    private final InscriptionRepository inscriptionRepository;
    private final PerimetreService perimetreService;
    private final Clock clock;

    /** Ajoute la progression (terminés / élèves de la classe) pour l'équipe ; les autres reçoivent la liste telle quelle. */
    @Transactional(readOnly = true)
    public List<DevoirDto> avecProgres(Long classeId, List<DevoirDto> devoirs) {
        if (devoirs.isEmpty() || !perimetreService.peutConsulterPresences(classeId)) {
            return devoirs;
        }
        Map<Long, Integer> termines = new HashMap<>();
        devoirFaitRepository.compter(devoirs.stream().map(DevoirDto::id).toList())
                .forEach(l -> termines.put(((Number) l[0]).longValue(), ((Number) l[1]).intValue()));
        int total = (int) inscriptionRepository.countByClasseIdAndStatut(classeId, "ACCEPTEE");
        return devoirs.stream().map(d -> d.avecProgres(termines.getOrDefault(d.id(), 0), total)).toList();
    }

    /** Les devoirs de la classe actuelle de l'élève, chacun avec son état « fait ». */
    @Transactional(readOnly = true)
    public List<DevoirDto> deLEleve(Long eleveId) {
        Long classeId = inscriptionRepository
                .findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(eleveId, "ACCEPTEE").stream()
                .filter(i -> i.getClasse() != null).map(i -> i.getClasse().getId()).findFirst().orElse(null);
        if (classeId == null) {
            return List.of();
        }
        List<DevoirDto> devoirs = devoirService.lister(classeId, null);
        if (devoirs.isEmpty()) {
            return devoirs;
        }
        Set<Long> faits = Set.copyOf(devoirFaitRepository.idsTermines(eleveId, devoirs.stream().map(DevoirDto::id).toList()));
        return devoirs.stream().map(d -> d.avecFait(faits.contains(d.id()))).collect(Collectors.toList());
    }

    /** Coche ou décoche un devoir pour un élève. Idempotent. */
    public DevoirDto marquer(Long devoirId, Long eleveIdDemande, boolean fait) {
        UtilisateurPrincipal moi = perimetreService.utilisateurCourant();
        if (moi == null) {
            throw new AccessDeniedException("Authentification requise");
        }
        Eleve eleve = eleveConcerne(moi.getId(), eleveIdDemande);
        Devoir devoir = devoirRepository.findById(devoirId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable"));
        if (!inscriptionRepository.existsByClasseIdAndEleveIdAndStatut(devoir.getClasse().getId(), eleve.getId(), "ACCEPTEE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet élève n'est pas dans la classe de ce devoir");
        }

        var existant = devoirFaitRepository.findByDevoirIdAndEleveId(devoirId, eleve.getId());
        if (fait && existant.isEmpty()) {
            DevoirFait f = new DevoirFait();
            f.setDevoir(devoir);
            f.setEleve(eleve);
            f.setFaitLe(Instant.now(clock));
            f.setParUtilisateurId(moi.getId());
            devoirFaitRepository.save(f);
        } else if (!fait && existant.isPresent()) {
            devoirFaitRepository.delete(existant.get());
        }
        return DevoirDto.de(devoir).avecFait(fait);
    }

    /** L'élève demandé doit être le dossier de l'utilisateur, ou l'un de ses enfants. */
    private Eleve eleveConcerne(Long utilisateurId, Long eleveIdDemande) {
        Eleve proprieDossier = eleveRepository.findByUtilisateurId(utilisateurId).orElse(null);
        if (eleveIdDemande == null) {
            if (proprieDossier == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Précisez l'élève concerné");
            }
            return proprieDossier;
        }
        if (proprieDossier != null && proprieDossier.getId().equals(eleveIdDemande)) {
            return proprieDossier;
        }
        if (lienParentalRepository.existsByEleveIdAndUtilisateurId(eleveIdDemande, utilisateurId)) {
            return eleveRepository.findById(eleveIdDemande)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable"));
        }
        throw new AccessDeniedException("Cet élève n'est pas rattaché à votre compte");
    }
}
