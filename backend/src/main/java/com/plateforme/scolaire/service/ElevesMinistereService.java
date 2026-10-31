package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ElevesMinistereRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.web.dto.EleveListeDto;
import com.plateforme.scolaire.web.dto.ElevesPageDto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Liste nationale des élèves pour le ministère : filtres combinables, pagination et export PDF. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ElevesMinistereService {

    static final int TAILLE_MAX = 100;
    static final int LIGNES_PDF_MAX = 5000;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ElevesMinistereRepository repository;
    private final EtablissementRepository etablissements;
    private final AnneeScolaireRepository annees;
    private final com.plateforme.scolaire.repository.ZoneGeographiqueRepository zones;
    private final GenerateurPdf pdf;
    private final Clock clock;

    /** Filtres facultatifs, sauf l'année. [statut] vaut ACCEPTEE par défaut ; TOUS pour ne pas filtrer. */
    public record Filtres(Long anneeScolaireId, String q, Long zoneId, Long etablissementId, String secteur,
                          Long typeEtablissementId, Long cycleId, Long niveauId, String sexe, String statut) {
    }

    public ElevesPageDto lister(Filtres f, int page, int taille) {
        int t = Math.max(1, Math.min(taille, TAILLE_MAX));
        Page<Inscription> resultat = repository.findAll(specification(f),
                PageRequest.of(Math.max(0, page), t, tri()));
        return new ElevesPageDto(resultat.getContent().stream().map(ElevesMinistereService::ligne).toList(),
                resultat.getTotalElements(), resultat.getNumber(), t, resultat.getTotalPages());
    }

    public byte[] exporterPdf(Filtres f) {
        var annee = annees.findById(f.anneeScolaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année scolaire inconnue"));
        Page<Inscription> resultat = repository.findAll(specification(f), PageRequest.of(0, LIGNES_PDF_MAX, tri()));
        List<List<String>> lignes = resultat.getContent().stream().map(ElevesMinistereService::ligne).map(l -> List.of(
                l.matricule(), (l.nom() + " " + l.prenom()).trim(), "F".equals(l.sexe()) ? "F" : "M".equals(l.sexe()) ? "M" : "—",
                l.dateNaissance() == null ? "—" : l.dateNaissance().format(JOUR), l.etablissement(),
                l.zone() == null ? "—" : l.zone(), l.classe() == null ? "—" : l.classe(), libelleStatut(l.statut()))).toList();
        List<String[]> chiffres = new ArrayList<>();
        chiffres.add(new String[] {"Élèves correspondant aux filtres", String.valueOf(resultat.getTotalElements())});
        if (resultat.getTotalElements() > LIGNES_PDF_MAX) {
            chiffres.add(new String[] {"Lignes imprimées (limite)", String.valueOf(LIGNES_PDF_MAX)});
        }
        return pdf.rapport(new GenerateurPdf.DonneesRapport("Ministère de l'Éducation", "République de Guinée",
                "Liste des élèves", description(f), annee.getLibelle(), LocalDate.now(clock).format(JOUR), chiffres,
                List.of(new GenerateurPdf.SectionRapport("Élèves",
                        List.of("Matricule", "Élève", "Sexe", "Naissance", "Établissement", "Zone", "Classe", "Statut"), lignes))));
    }

    // ---------- Filtres ----------

    private Specification<Inscription> specification(Filtres f) {
        List<Long> idsZone = f.zoneId() == null ? null
                : etablissements.findByZoneEtSousZones(f.zoneId()).stream().map(Etablissement::getId).toList();
        String statut = f.statut() == null || f.statut().isBlank() ? "ACCEPTEE" : f.statut();
        return (root, query, cb) -> {
            boolean lecture = query.getResultType() != Long.class && query.getResultType() != long.class;
            Join<Inscription, Eleve> eleve;
            Join<Inscription, Etablissement> etab;
            Join<?, ?> classe;
            Join<?, ?> niveau;
            if (lecture) {
                eleve = cast(root.fetch("eleve", JoinType.INNER));
                etab = cast(root.fetch("etablissement", JoinType.INNER));
                classe = cast(root.fetch("classe", JoinType.LEFT));
                niveau = cast(classe.fetch("niveau", JoinType.LEFT));
                niveau.fetch("cycle", JoinType.LEFT);
                etab.fetch("zone", JoinType.LEFT);
            } else {
                eleve = root.join("eleve", JoinType.INNER);
                etab = root.join("etablissement", JoinType.INNER);
                classe = root.join("classe", JoinType.LEFT);
                niveau = classe.join("niveau", JoinType.LEFT);
            }
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("anneeScolaire").get("id"), f.anneeScolaireId()));
            if (!"TOUS".equalsIgnoreCase(statut)) {
                p.add(cb.equal(root.get("statut"), statut));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String motif = "%" + f.q().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(eleve.get("nom")), motif), cb.like(cb.lower(eleve.get("prenom")), motif),
                        cb.like(cb.lower(eleve.get("matricule")), motif)));
            }
            if (idsZone != null) {
                p.add(idsZone.isEmpty() ? cb.disjunction() : etab.get("id").in(idsZone));
            }
            if (f.etablissementId() != null) {
                p.add(cb.equal(etab.get("id"), f.etablissementId()));
            }
            if (f.secteur() != null && !f.secteur().isBlank()) {
                p.add(cb.equal(etab.get("secteur"), f.secteur()));
            }
            if (f.typeEtablissementId() != null) {
                p.add(cb.equal(etab.get("typeEtablissement").get("id"), f.typeEtablissementId()));
            }
            if (f.niveauId() != null) {
                p.add(cb.equal(niveau.get("id"), f.niveauId()));
            }
            if (f.cycleId() != null) {
                p.add(cb.equal(niveau.get("cycle").get("id"), f.cycleId()));
            }
            if (f.sexe() != null && !f.sexe().isBlank()) {
                p.add("N".equalsIgnoreCase(f.sexe()) ? cb.isNull(eleve.get("sexe")) : cb.equal(eleve.get("sexe"), f.sexe()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings("unchecked")
    private static <T extends Join<?, ?>> T cast(Object fetch) {
        return (T) fetch;
    }

    private static Sort tri() {
        return Sort.by("eleve.nom", "eleve.prenom", "id");
    }

    private static EleveListeDto ligne(Inscription i) {
        Eleve e = i.getEleve();
        Etablissement etab = i.getEtablissement();
        var classe = i.getClasse();
        var niveau = classe == null ? null : classe.getNiveau();
        return new EleveListeDto(i.getId(), e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(), e.getSexe(),
                e.getDateNaissance(), i.getStatut(), etab.getId(), etab.getNom(), etab.getSecteur(),
                etab.getTypeEtablissement() == null ? null : etab.getTypeEtablissement().getLibelle(),
                etab.getZone() == null ? null : etab.getZone().getNom(), classe == null ? null : classe.getNom(),
                niveau == null ? null : niveau.getLibelle(),
                niveau == null || niveau.getCycle() == null ? null : niveau.getCycle().getLibelle());
    }

    static String libelleStatut(String statut) {
        return switch (statut) {
            case "ACCEPTEE" -> "Inscrit";
            case "EN_ATTENTE" -> "En attente";
            case "REFUSEE" -> "Refusée";
            case "ANNULEE" -> "Annulée";
            case "TRANSFEREE" -> "Transféré";
            default -> statut;
        };
    }

    /** Rappel des filtres actifs, imprimé en tête du PDF. */
    private String description(Filtres f) {
        List<String> parts = new ArrayList<>();
        if (f.zoneId() != null) {
            zones.findById(f.zoneId()).ifPresent(z -> parts.add(z.getNom()));
        }
        if (f.etablissementId() != null) {
            etablissements.findById(f.etablissementId()).ifPresent(e -> parts.add(e.getNom()));
        }
        if (f.secteur() != null && !f.secteur().isBlank()) {
            parts.add("PRIVE".equals(f.secteur()) ? "privé" : "public");
        }
        if (f.sexe() != null && !f.sexe().isBlank()) {
            parts.add("F".equals(f.sexe()) ? "filles" : "M".equals(f.sexe()) ? "garçons" : "sexe non précisé");
        }
        if (f.q() != null && !f.q().isBlank()) {
            parts.add("recherche « " + f.q().trim() + " »");
        }
        return parts.isEmpty() ? "Tout le pays" : String.join(", ", parts);
    }
}
