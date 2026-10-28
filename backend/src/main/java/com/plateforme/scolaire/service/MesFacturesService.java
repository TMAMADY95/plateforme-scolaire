package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.LienParentalRepository;
import com.plateforme.scolaire.web.dto.FactureDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Accès d'un élève ou d'un parent à ses propres factures (Étape 3a).
 *
 * <p>Le contrôle est volontairement ici et non dans {@code @PreAuthorize} : il faut
 * vérifier que l'élève visé est bien le dossier de l'utilisateur connecté, ou l'un
 * de ses enfants rattachés.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MesFacturesService {

    private final FacturationService facturationService;
    private final EleveRepository eleveRepository;
    private final LienParentalRepository lienParentalRepository;

    /** Les factures de l'élève connecté. */
    public List<FactureDto> facturesDeMonDossier(Long utilisateurId) {
        Eleve eleve = eleveRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun dossier élève n'est rattaché à ce compte"));
        return facturationService.facturesDeLEleve(eleve.getId());
    }

    /** Les factures d'un enfant du parent connecté, et seulement dans ce cas. */
    public List<FactureDto> facturesDeMonEnfant(Long utilisateurId, Long eleveId) {
        if (!lienParentalRepository.existsByEleveIdAndUtilisateurId(eleveId, utilisateurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cet élève n'est pas rattaché à votre compte");
        }
        return facturationService.facturesDeLEleve(eleveId);
    }
}
