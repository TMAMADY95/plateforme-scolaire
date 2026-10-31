package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CycleScolaire;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.InspectionRepository;
import com.plateforme.scolaire.repository.SignalementSecuriteRepository;
import com.plateforme.scolaire.repository.StatistiquesRepository;
import com.plateforme.scolaire.web.dto.ConformiteDto;
import com.plateforme.scolaire.web.dto.CycleDto;
import com.plateforme.scolaire.web.dto.LigneConformiteDto;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableau de bord « conformité et inspections » du ministère (tout le pays, ou une zone) et de la direction
 * régionale (ses zones).
 *
 * <p>Règles : « inspecté » = inspection datant de moins d'un an ; « non conforme » = résultat de la dernière
 * inspection ; « problème de sécurité » = au moins un signalement ouvert ; « données non transmises » = établissement
 * actif sans présence saisie depuis 30 jours. Une inspection est <b>urgente</b> à partir de 3 points : signalement
 * grave ouvert (3), fonctionnement sans autorisation (2), dernière inspection non conforme (2), autre signalement
 * ouvert (1), pas d'inspection depuis un an (1), données non transmises (1). Les établissements fermés sont exclus.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConformiteService {

    static final int JOURS_SANS_DONNEES = 30;
    static final int JOURS_VALIDITE_INSPECTION = 365;
    static final int SEUIL_URGENCE = 3;

    private final EtablissementRepository etablissementRepository;
    private final InspectionRepository inspectionRepository;
    private final SignalementSecuriteRepository signalementRepository;
    private final StatistiquesRepository statistiquesRepository;
    private final RegionService regionService;
    private final Clock clock;

    /** Ministère : tout le pays, ou une zone (région, préfecture, commune) et ses sous-zones. */
    public ConformiteDto nationale(Long zoneId, String statut, String secteur, Long cycleId) {
        List<Etablissement> etablissements = zoneId == null
                ? etablissementRepository.findAll()
                : etablissementRepository.findByZoneEtSousZones(zoneId);
        return rapport(etablissements, statut, secteur, cycleId);
    }

    /** Direction régionale : ses zones uniquement. */
    public ConformiteDto regionale(String statut, String secteur, Long cycleId) {
        return rapport(new ArrayList<>(regionService.etablissementsDeLaRegion().values()), statut, secteur, cycleId);
    }

    private ConformiteDto rapport(List<Etablissement> tous, String statut, String secteur, Long cycleId) {
        List<Etablissement> etablissements = tous.stream()
                .filter(e -> statut == null || statut.isBlank() || statut.equals(e.getStatut()))
                .filter(e -> secteur == null || secteur.isBlank() || secteur.equals(e.getSecteur()))
                .filter(e -> cycleId == null || e.getCycles().stream().anyMatch(c -> cycleId.equals(c.getId())))
                .sorted(Comparator.comparing(Etablissement::getNom, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (etablissements.isEmpty()) {
            return new ConformiteDto(new ConformiteDto.Indicateurs(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), List.of());
        }
        List<Long> ids = etablissements.stream().map(Etablissement::getId).toList();
        LocalDate aujourdhui = LocalDate.now(clock);

        // Dernière inspection : les lignes arrivent de la plus récente à la plus ancienne.
        Map<Long, LocalDate> dateInspection = new HashMap<>();
        Map<Long, String> resultatInspection = new HashMap<>();
        for (Object[] ligne : inspectionRepository.resumes(ids)) {
            Long id = (Long) ligne[0];
            if (!dateInspection.containsKey(id)) {
                dateInspection.put(id, (LocalDate) ligne[1]);
                resultatInspection.put(id, (String) ligne[2]);
            }
        }
        Map<Long, Long> ouverts = new HashMap<>();
        Map<Long, Long> graves = new HashMap<>();
        for (Object[] ligne : signalementRepository.ouverts(ids)) {
            Long id = (Long) ligne[0];
            long n = (Long) ligne[2];
            ouverts.merge(id, n, Long::sum);
            if ("ELEVEE".equals(ligne[1])) {
                graves.merge(id, n, Long::sum);
            }
        }
        Map<Long, LocalDate> donnees = new HashMap<>();
        for (Object[] ligne : statistiquesRepository.dernieresPresences(ids)) {
            donnees.put((Long) ligne[0], date(ligne[1]));
        }

        List<LigneConformiteDto> lignes = new ArrayList<>();
        for (Etablissement e : etablissements) {
            lignes.add(ligne(e, aujourdhui, dateInspection.get(e.getId()), resultatInspection.get(e.getId()),
                    ouverts.getOrDefault(e.getId(), 0L), graves.getOrDefault(e.getId(), 0L), donnees.get(e.getId())));
        }
        return new ConformiteDto(indicateurs(lignes), lignes);
    }

    static LigneConformiteDto ligne(Etablissement e, LocalDate aujourdhui, LocalDate derniereInspection,
                                    String resultat, long signalementsOuverts, long signalementsGraves,
                                    LocalDate derniereDonnee) {
        boolean inspecte = derniereInspection != null
                && ChronoUnit.DAYS.between(derniereInspection, aujourdhui) < JOURS_VALIDITE_INSPECTION;
        boolean nonConforme = "NON_CONFORME".equals(resultat);
        boolean actif = "ACTIF".equals(e.getStatut());
        boolean sansDonnees = actif && (derniereDonnee == null
                || ChronoUnit.DAYS.between(derniereDonnee, aujourdhui) > JOURS_SANS_DONNEES);
        boolean sansAutorisation = "NON_AUTORISE".equals(e.getAutorisationStatut())
                || "RETIRE".equals(e.getAutorisationStatut());

        int score = 0;
        List<String> motifs = new ArrayList<>();
        if (!"FERME".equals(e.getStatut())) {
            if (signalementsGraves > 0) {
                score += 3;
                motifs.add("Problème de sécurité grave non résolu");
            } else if (signalementsOuverts > 0) {
                score += 1;
                motifs.add("Problème de sécurité non résolu");
            }
            if (actif && sansAutorisation) {
                score += 2;
                motifs.add("Fonctionne sans autorisation");
            }
            if (nonConforme) {
                score += 2;
                motifs.add("Dernière inspection non conforme");
            }
            if (!inspecte) {
                score += 1;
                motifs.add(derniereInspection == null ? "Jamais inspecté" : "Pas inspecté depuis plus d'un an");
            }
            if (sansDonnees) {
                score += 1;
                motifs.add("Données non transmises");
            }
        }
        boolean urgent = score >= SEUIL_URGENCE;

        return new LigneConformiteDto(e.getId(), e.getCodeOfficiel(), e.getNom(), e.getSecteur(),
                e.getCycles().stream().sorted(Comparator.comparingInt(CycleScolaire::getOrdre)).map(CycleDto::de).toList(),
                e.getStatut(), e.getZone() == null ? null : e.getZone().getId(),
                e.getZone() == null ? null : e.getZone().getNom(),
                e.getAutorisationStatut(), e.getAutorisationReference(), e.getMotifStatut(),
                derniereInspection, resultat, inspecte, nonConforme, signalementsOuverts, signalementsGraves,
                derniereDonnee, sansDonnees, urgent, score, motifs);
    }

    static ConformiteDto.Indicateurs indicateurs(List<LigneConformiteDto> l) {
        return new ConformiteDto.Indicateurs(
                l.size(),
                l.stream().filter(x -> "PUBLIC".equals(x.secteur())).count(),
                l.stream().filter(x -> "PRIVE".equals(x.secteur())).count(),
                l.stream().filter(x -> "AUTORISE".equals(x.autorisation())).count(),
                l.stream().filter(x -> "NON_AUTORISE".equals(x.autorisation()) || "RETIRE".equals(x.autorisation())).count(),
                l.stream().filter(x -> "EN_ATTENTE".equals(x.autorisation())).count(),
                l.stream().filter(LigneConformiteDto::nonConforme).count(),
                l.stream().filter(LigneConformiteDto::inspecte).count(),
                l.stream().filter(x -> x.signalementsOuverts() > 0).count(),
                l.stream().filter(LigneConformiteDto::sansDonnees).count(),
                l.stream().filter(x -> "SUSPENDU".equals(x.statut()) || "FERME".equals(x.statut())).count(),
                l.stream().filter(LigneConformiteDto::urgent).count());
    }

    private static LocalDate date(Object valeur) {
        if (valeur instanceof LocalDate d) {
            return d;
        }
        return valeur instanceof Date d ? d.toLocalDate() : null;
    }
}
