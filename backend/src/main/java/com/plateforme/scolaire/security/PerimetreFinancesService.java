package com.plateforme.scolaire.security;

import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
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

    /**
     * Les factures nomment des élèves et révèlent des situations sociales (bourses, aides) :
     * seuls la direction et le personnel administratif de l'établissement les consultent.
     * Un enseignant, qui a pourtant accès à l'établissement, ne les voit pas.
     */
    @Transactional(readOnly = true)
    public boolean peutConsulterFactures(Long etablissementId) {
        return perimetreService.peutGererEtablissement(etablissementId);
    }

    @Transactional(readOnly = true)
    public boolean peutAccederFacture(Long factureId) {
        if (factureId == null) {
            return false;
        }
        return factureRepository.findEtablissementIdById(factureId)
                .map(this::peutConsulterFactures)
                .orElse(false);
    }

    /**
     * Le bilan est une synthèse sans nom d'élève : en plus de la direction et du personnel
     * administratif, le ministère et les directions régionales le consultent pour leur périmètre.
     */
    @Transactional(readOnly = true)
    public boolean peutConsulterBilan(Long etablissementId) {
        if (peutConsulterFactures(etablissementId)) {
            return true;
        }
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        if (p == null || etablissementId == null) {
            return false;
        }
        boolean superviseur = p.getAffectations().stream().anyMatch(a ->
                (a.role() == Role.MINISTERE || a.role() == Role.DIRECTION_REGIONALE)
                        && (a.perimetre() == Perimetre.NATIONAL || a.perimetre() == Perimetre.ZONE));
        return superviseur && perimetreService.peutAccederEtablissement(etablissementId);
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
