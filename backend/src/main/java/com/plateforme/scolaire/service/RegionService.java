package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Perimetre;
import com.plateforme.scolaire.domain.Role;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import com.plateforme.scolaire.web.dto.SyntheseNationaleDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chiffres clés de la direction régionale : uniquement les établissements des zones (et sous-zones)
 * qui lui sont confiées par un rôle DIRECTION_REGIONALE. Les autres rôles de l'utilisateur
 * n'élargissent pas ce périmètre.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final EtablissementRepository etablissementRepository;
    private final ClasseRepository classeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final PerimetreService perimetreService;

    public SyntheseNationaleDto synthese(Long anneeScolaireId) {
        UtilisateurPrincipal p = perimetreService.utilisateurCourant();
        Map<Long, Etablissement> etablissements = new LinkedHashMap<>();
        if (p != null) {
            for (UtilisateurPrincipal.AffectationInfo a : p.getAffectations()) {
                if (a.role() == Role.DIRECTION_REGIONALE && a.perimetre() == Perimetre.ZONE && a.zoneId() != null) {
                    etablissementRepository.findByZoneEtSousZones(a.zoneId())
                            .forEach(e -> etablissements.put(e.getId(), e));
                }
            }
        }
        if (etablissements.isEmpty()) {
            return new SyntheseNationaleDto(anneeScolaireId, 0, 0, 0, 0, 0, 0);
        }
        List<Long> ids = List.copyOf(etablissements.keySet());
        return new SyntheseNationaleDto(anneeScolaireId,
                etablissements.size(),
                compter(etablissements, "ACTIF"),
                compter(etablissements, "SUSPENDU"),
                compter(etablissements, "FERME"),
                classeRepository.countByEtablissementIdInAndAnneeScolaireId(ids, anneeScolaireId),
                inscriptionRepository.countByEtablissementIdInAndAnneeScolaireIdAndStatut(
                        ids, anneeScolaireId, "ACCEPTEE"));
    }

    private static long compter(Map<Long, Etablissement> etablissements, String statut) {
        return etablissements.values().stream().filter(e -> statut.equals(e.getStatut())).count();
    }
}
