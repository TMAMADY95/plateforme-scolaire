package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Annonce;
import com.plateforme.scolaire.repository.AnnonceRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.AnnonceDto;
import com.plateforme.scolaire.web.dto.AnnonceRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Annonces à une classe. Publier : l'enseignant de la classe ou la direction de l'établissement (contrôlé par le
 * contrôleur). Supprimer : l'auteur, ou la direction. Les 50 plus récentes sont listées.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnnonceService {

    static final int MAX_LISTE = 50;

    private final AnnonceRepository annonceRepository;
    private final ClasseRepository classeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PerimetreService perimetreService;
    private final Clock clock;
    private final ApplicationEventPublisher evenements;

    @Transactional(readOnly = true)
    public List<AnnonceDto> lister(Long classeId) {
        if (!classeRepository.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable");
        }
        Long moi = moiId();
        boolean direction = perimetreService.peutGererClasse(classeId);
        return annonceRepository.recentes(classeId, PageRequest.of(0, MAX_LISTE)).stream()
                .map(a -> AnnonceDto.de(a, direction || a.getAuteur().getId().equals(moi))).toList();
    }

    public AnnonceDto publier(Long classeId, AnnonceRequest r) {
        var classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        Long moi = moiId();
        Annonce a = new Annonce();
        a.setClasse(classe);
        a.setAuteur(utilisateurRepository.getReferenceById(moi));
        a.setTitre(r.titre().trim());
        a.setContenu(r.contenu().trim());
        a.setCreatedAt(Instant.now(clock));
        Annonce enregistree = annonceRepository.save(a);
        var u = a.getAuteur();
        // Traité après la validation de la transaction, en tâche de fond : voir NotificationAnnonces.
        evenements.publishEvent(new AnnoncePubliee(classeId, classe.getNom(), enregistree.getTitre(),
                ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim()));
        return AnnonceDto.de(enregistree, true);
    }

    public void supprimer(Long id) {
        Annonce a = annonceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce introuvable"));
        boolean auteur = a.getAuteur().getId().equals(moiId());
        if (!auteur && !perimetreService.peutGererClasse(a.getClasse().getId())) {
            throw new AccessDeniedException("Vous ne pouvez pas supprimer cette annonce");
        }
        annonceRepository.delete(a);
    }

    private Long moiId() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null) {
            throw new AccessDeniedException("Authentification requise");
        }
        return p.getId();
    }
}
