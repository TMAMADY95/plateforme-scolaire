package com.plateforme.scolaire.service;

import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.EtablissementDto;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtablissementService {

    private final EtablissementRepository etablissementRepository;
    private final PerimetreService perimetreService;

    /** Uniquement les établissements du périmètre de l'utilisateur connecté. */
    public List<EtablissementDto> listerAccessibles() {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null) {
            return List.of();
        }
        return perimetreService.etablissementsAccessibles(p).stream().map(EtablissementDto::de).toList();
    }

    /** L'autorisation est vérifiée en amont (@PreAuthorize sur le contrôleur). */
    public Optional<EtablissementDto> trouver(Long id) {
        return etablissementRepository.findById(id).map(EtablissementDto::de);
    }
}
