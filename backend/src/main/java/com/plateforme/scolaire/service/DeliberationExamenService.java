package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.JuryMembre;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.JuryMembreRepository;
import com.plateforme.scolaire.repository.ResultatExamenRepository;
import com.plateforme.scolaire.web.dto.JuryDto;
import com.plateforme.scolaire.web.dto.JuryMembreRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Jury, délibération et proclamation (règlement général de 2022). Le jury siège à un niveau qui dépend de l'examen
 * (préfectoral ou communal pour le CEE, régional pour le BEPC, national pour le baccalauréat), délibère si au moins
 * deux tiers de ses membres sont présents, puis le ministère proclame officiellement les résultats : la saisie des
 * résultats est alors close.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeliberationExamenService {

    static final String EN_COURS = "EN_COURS";
    static final String DELIBERE = "DELIBERE";
    static final String PROCLAME = "PROCLAME";

    private final ExamenNationalRepository examens;
    private final JuryMembreRepository membres;
    private final ResultatExamenRepository resultats;
    private final AuditService audit;
    private final Clock clock;

    /** Fonctions des membres du jury selon le niveau de l'examen (composition prévue ; les noms sont nommés à chaque session). */
    static List<String> composition(String niveauOrganisation) {
        return switch (niveauOrganisation == null ? "" : niveauOrganisation.toUpperCase()) {
            case "NATIONAL" -> List.of("Directeur national de l'enseignement secondaire général et technique (président)",
                    "Représentant du ministère de l'Enseignement supérieur (premier vice-président)",
                    "Représentant du ministère de l'Enseignement technique (deuxième vice-président)",
                    "Directeur général des examens et du contrôle scolaire (rapporteur)",
                    "Proviseur de lycée public", "Proviseur de lycée privé", "Professeur de lycée",
                    "Membre de la FEGUIPAE", "Fondateur d'établissement privé");
            case "REGION" -> List.of("Inspecteur régional de l'éducation (président)", "Chef de la section pédagogique",
                    "Chargé des examens de l'IRE", "Principal de collège public", "Principal de collège privé", "Enseignant",
                    "Membre de l'APEAE régionale");
            default -> List.of("Directeur préfectoral ou communal de l'éducation (président)", "Chef de la section pédagogique",
                    "Chargé des examens", "Délégué scolaire de l'enseignement élémentaire", "Directeur d'école publique",
                    "Directeur d'école privée", "Enseignant", "Membre de l'APEAE préfectorale");
        };
    }

    /** Présents nécessaires pour délibérer valablement : au moins les deux tiers des membres. */
    static int quorum(int membresTotal) {
        return (2 * membresTotal + 2) / 3;
    }

    @Transactional(readOnly = true)
    public JuryDto jury(Long examenId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen, false);
        return dto(examen, membres.findByExamenIdOrderByOrdre(examenId));
    }

    /** Crée le jury avec la composition prévue pour ce niveau d'examen (sans effet s'il existe déjà). */
    public JuryDto initialiser(Long examenId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen, false);
        verifierModifiable(examen);
        if (membres.countByExamenId(examenId) == 0) {
            List<String> fonctions = composition(examen.getNiveauOrganisation());
            for (int i = 0; i < fonctions.size(); i++) {
                JuryMembre m = new JuryMembre();
                m.setExamen(examen);
                m.setOrdre(i + 1);
                m.setFonction(fonctions.get(i));
                membres.save(m);
            }
            audit.enregistrer("JURY_CREE", "ExamenNational", examenId, Map.of("membres", fonctions.size()));
        }
        return dto(examen, membres.findByExamenIdOrderByOrdre(examenId));
    }

    public JuryDto modifierMembre(Long examenId, Long membreId, JuryMembreRequest r) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen, false);
        verifierModifiable(examen);
        JuryMembre m = membres.findById(membreId).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));
        String nom = r.nom() == null || r.nom().isBlank() ? null : r.nom().trim();
        if (r.present() && nom == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez le nom d'un membre avant de le marquer présent");
        }
        m.setNom(nom);
        m.setPresent(r.present());
        membres.flush();
        return dto(examen, membres.findByExamenIdOrderByOrdre(examenId));
    }

    /** Le jury délibère : quorum atteint et au moins un établissement ayant transmis ses résultats. */
    public JuryDto deliberer(Long examenId) {
        ExamenNational examen = examen(examenId);
        verifierDroit(examen, false);
        if (!EN_COURS.equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La délibération a déjà eu lieu");
        }
        List<JuryMembre> liste = membres.findByExamenIdOrderByOrdre(examenId);
        int presents = (int) liste.stream().filter(JuryMembre::isPresent).count();
        if (liste.isEmpty() || presents < quorum(liste.size())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quorum non atteint : " + presents + " présent(s) sur "
                    + liste.size() + ", il en faut au moins " + quorum(liste.size()) + " (deux tiers des membres)");
        }
        if (resultats.countByExamenId(examenId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun résultat n'a encore été transmis pour cet examen");
        }
        examen.setStatutResultats(DELIBERE);
        examen.setDateDeliberation(LocalDate.now(clock));
        audit.enregistrer("EXAMEN_DELIBERE", "ExamenNational", examenId, Map.of("presents", presents, "membres", liste.size()));
        return dto(examen, liste);
    }

    /** Réservé au ministère : proclamation officielle, qui clôt la saisie des résultats. */
    public JuryDto proclamer(Long examenId) {
        ExamenNational examen = examen(examenId);
        if (!estMinistere()) {
            throw new AccessDeniedException("Seul le ministère proclame officiellement les résultats");
        }
        if (!DELIBERE.equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le jury doit avoir délibéré avant la proclamation");
        }
        examen.setStatutResultats(PROCLAME);
        examen.setDateProclamation(LocalDate.now(clock));
        audit.enregistrer("EXAMEN_PROCLAME", "ExamenNational", examenId, Map.of("libelle", examen.getLibelle()));
        return dto(examen, membres.findByExamenIdOrderByOrdre(examenId));
    }

    /** Réservé au ministère : annule une délibération (par exemple après une erreur de saisie) tant que rien n'est proclamé. */
    public JuryDto rouvrir(Long examenId) {
        ExamenNational examen = examen(examenId);
        if (!estMinistere()) {
            throw new AccessDeniedException("Seul le ministère peut rouvrir une délibération");
        }
        if (!DELIBERE.equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une délibération non proclamée peut être rouverte");
        }
        examen.setStatutResultats(EN_COURS);
        examen.setDateDeliberation(null);
        audit.enregistrer("DELIBERATION_ROUVERTE", "ExamenNational", examenId, Map.of("libelle", examen.getLibelle()));
        return dto(examen, membres.findByExamenIdOrderByOrdre(examenId));
    }

    private JuryDto dto(ExamenNational examen, List<JuryMembre> liste) {
        int presents = (int) liste.stream().filter(JuryMembre::isPresent).count();
        int requis = liste.isEmpty() ? 0 : quorum(liste.size());
        return new JuryDto(examen.getId(), examen.getStatutResultats(), examen.getDateDeliberation(), examen.getDateProclamation(),
                liste.stream().map(m -> new JuryDto.Membre(m.getId(), m.getOrdre(), m.getFonction(), m.getNom(), m.isPresent())).toList(),
                presents, requis, !liste.isEmpty() && presents >= requis, DELIBERE.equals(examen.getStatutResultats()) && estMinistere());
    }

    private void verifierModifiable(ExamenNational examen) {
        if (!EN_COURS.equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le jury a déjà délibéré : il n'est plus modifiable");
        }
    }

    /** Le ministère gère tous les jurys ; une direction régionale seulement ceux des examens qui ne sont pas nationaux. */
    private void verifierDroit(ExamenNational examen, boolean ministereSeul) {
        if (estMinistere()) {
            return;
        }
        if (ministereSeul || "NATIONAL".equalsIgnoreCase(examen.getNiveauOrganisation())) {
            throw new AccessDeniedException("Le jury d'un examen national relève du ministère");
        }
    }

    private static boolean estMinistere() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_MINISTERE".equals(a.getAuthority()));
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
