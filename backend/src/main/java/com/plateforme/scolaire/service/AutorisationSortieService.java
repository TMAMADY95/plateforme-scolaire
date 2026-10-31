package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AutorisationSortie;
import com.plateforme.scolaire.repository.AutorisationSortieRepository;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.ReponseRequest;
import com.plateforme.scolaire.web.dto.SortieDto;
import com.plateforme.scolaire.web.dto.SortieRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Autorisations de sortie anticipée : la famille demande, la direction de l'école décide. */
@Service
@RequiredArgsConstructor
@Transactional
public class AutorisationSortieService {

    private final AutorisationSortieRepository sorties;
    private final EleveRepository eleves;
    private final EtablissementRepository etablissements;
    private final InscriptionRepository inscriptions;
    private final UtilisateurRepository utilisateurs;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<SortieDto> deLEleve(Long eleveId) {
        return sorties.deLEleve(eleveId).stream().map(SortieDto::de).toList();
    }

    /** Demandes en attente, et sorties accordées ou refusées à partir d'aujourd'hui. */
    @Transactional(readOnly = true)
    public List<SortieDto> deLEtablissement(Long etablissementId) {
        return sorties.deLEtablissement(etablissementId, LocalDate.now(clock)).stream().map(SortieDto::de).toList();
    }

    public SortieDto demander(Long eleveId, SortieRequest r) {
        Long etablissementId = inscriptions.findEtablissementIdsActifsByEleveId(eleveId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet élève n'a pas d'inscription active"));
        if (r.dateSortie().isBefore(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de sortie ne peut pas être dans le passé");
        }
        AutorisationSortie s = new AutorisationSortie();
        s.setEleve(eleves.getReferenceById(eleveId));
        s.setEtablissement(etablissements.getReferenceById(etablissementId));
        s.setDemandeePar(utilisateurs.getReferenceById(perimetre.utilisateurCourant().getId()));
        s.setDateSortie(r.dateSortie());
        s.setHeureSortie(r.heureSortie());
        s.setMotif(r.motif().trim());
        s.setAccompagnateur(r.accompagnateur() == null || r.accompagnateur().isBlank() ? null : r.accompagnateur().trim());
        s.setCreatedAt(Instant.now(clock));
        Long id = sorties.save(s).getId();
        return sorties.deLEleve(eleveId).stream().filter(x -> x.getId().equals(id)).findFirst().map(SortieDto::de).orElseThrow();
    }

    public SortieDto decider(Long id, ReponseRequest r) {
        AutorisationSortie s = sorties.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
        if (!perimetre.peutGererEtablissement(s.getEtablissement().getId())) {
            throw new AccessDeniedException("Demande hors de votre établissement");
        }
        if (!"DEMANDEE".equals(s.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée");
        }
        s.setStatut(r.accepter() ? "ACCEPTEE" : "REFUSEE");
        s.setReponse(r.reponse() == null || r.reponse().isBlank() ? null : r.reponse().trim());
        s.setTraiteeLe(Instant.now(clock));
        s.setTraiteePar(utilisateurs.getReferenceById(perimetre.utilisateurCourant().getId()));
        audit.enregistrer("SORTIE_" + s.getStatut(), "AutorisationSortie", s.getId(), Map.of("eleve", s.getEleve().getId()));
        return SortieDto.de(s);
    }
}
