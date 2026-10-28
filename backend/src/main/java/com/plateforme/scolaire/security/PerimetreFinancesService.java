package com.plateforme.scolaire.security;

import com.plateforme.scolaire.repository.FactureRepository;
import com.plateforme.scolaire.repository.FraisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autorisations des frais et des factures, utilisables dans les annotations :
 * <pre>@PreAuthorize("@perimetreFinances.peutGererFacture(#id)")</pre>
 *
 * <p>Même découpage que {@link PerimetreNotesService} et {@link PerimetreEmploiDuTempsService} :
 * les règles propres aux finances ne chargent pas {@link PerimetreService} de dépendances
 * supplémentaires, ce qui laisse ses tests unitaires intacts.</p>
 */
@Service("perimetreFinances")
@RequiredArgsConstructor
public class PerimetreFinancesService {

    private final PerimetreService perimetreService;
    private final FraisRepository fraisRepository;
    private final FactureRepository factureRepository;

    @Transactional(readOnly = true)
    public boolean peutAccederFrais(Long fraisId) {
        if (fraisId == null) {
            return false;
        }
        return fraisRepository.findEtablissementIdById(fraisId)
                .map(perimetreService::peutAccederEtablissement)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean peutGererFrais(Long fraisId) {
        if (fraisId == null) {
            return false;
        }
        return fraisRepository.findEtablissementIdById(fraisId)
                .map(perimetreService::peutGererEtablissement)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean peutAccederFacture(Long factureId) {
        if (factureId == null) {
            return false;
        }
        return factureRepository.findEtablissementIdById(factureId)
                .map(perimetreService::peutAccederEtablissement)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean peutGererFacture(Long factureId) {
        if (factureId == null) {
            return false;
        }
        return factureRepository.findEtablissementIdById(factureId)
                .map(perimetreService::peutGererEtablissement)
                .orElse(false);
    }
}
