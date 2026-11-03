package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.ListeCandidats;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.ListeCandidatsRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.SuiviListesDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suivi des listes de candidats par établissement : celles transmises attendent la vérification de la direction
 * préfectorale ou communale (ou de l'inspection régionale), celles vérifiées attendent la validation de la DGECS.
 * Chaque utilisateur ne voit que les établissements de son périmètre.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuiviListesService {

    private final CandidatExamenRepository candidats;
    private final ListeCandidatsRepository listes;
    private final PerimetreService perimetre;
    private final Clock clock;

    /** Pur : qui doit agir sur une liste dans cet état. */
    static String enAttenteDe(String statut) {
        return switch (statut) {
            case "TRANSMISE" -> "VERIFICATION";
            case "VALIDEE_REGION" -> "VALIDATION";
            default -> null;
        };
    }

    /** Pur : jours écoulés depuis le dépôt à l'étape courante ; null si rien n'est attendu. */
    static Long joursAttente(String statut, Instant depuis, Instant maintenant) {
        return enAttenteDe(statut) == null || depuis == null ? null : Duration.between(depuis, maintenant).toDays();
    }

    /** Pur : date de début de l'attente selon l'étape. */
    static Instant depuis(ListeCandidats l) {
        return switch (l.getStatut()) {
            case "TRANSMISE" -> l.getTransmiseLe();
            case "VALIDEE_REGION" -> l.getValideeRegionLe();
            case "VALIDEE" -> l.getValideeLe();
            default -> null;
        };
    }

    public SuiviListesDto suivi(Long examenId) {
        Map<Long, ListeCandidats> parEtab = listes.deLExamen(examenId).stream()
                .collect(Collectors.toMap(l -> l.getEtablissement().getId(), Function.identity(), (a, b) -> a));
        Instant maintenant = Instant.now(clock);
        List<SuiviListesDto.Ligne> lignes = candidats.effectifsParEtablissement(examenId).stream()
                .filter(r -> perimetre.peutAccederEtablissement(((Etablissement) r[0]).getId()))
                .map(r -> {
                    Etablissement e = (Etablissement) r[0];
                    ListeCandidats l = parEtab.get(e.getId());
                    String statut = l == null ? "BROUILLON" : l.getStatut();
                    Instant depuis = l == null ? null : depuis(l);
                    return new SuiviListesDto.Ligne(e.getId(), e.getNom(), e.getZone() == null ? null : e.getZone().getNom(),
                            ((Long) r[1]).intValue(), statut, depuis, joursAttente(statut, depuis, maintenant), enAttenteDe(statut));
                })
                .sorted(Comparator.comparing((SuiviListesDto.Ligne x) -> x.joursAttente() == null ? -1L : x.joursAttente()).reversed()
                        .thenComparing(SuiviListesDto.Ligne::etablissement))
                .toList();
        return new SuiviListesDto(examenId, compter(lignes, "BROUILLON"), compter(lignes, "TRANSMISE"),
                compter(lignes, "VALIDEE_REGION"), compter(lignes, "VALIDEE"), lignes);
    }

    private static int compter(List<SuiviListesDto.Ligne> lignes, String statut) {
        return (int) lignes.stream().filter(l -> statut.equals(l.statut())).count();
    }
}
