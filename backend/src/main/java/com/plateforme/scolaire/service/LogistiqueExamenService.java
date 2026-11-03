package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.LivraisonCentre;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.LivraisonCentreRepository;
import com.plateforme.scolaire.service.PersonnelCentreService.Affectation;
import com.plateforme.scolaire.web.dto.LivraisonCentreRequest;
import com.plateforme.scolaire.web.dto.LogistiqueExamenDto;
import com.plateforme.scolaire.web.dto.LogistiqueExamenDto.LigneCentre;
import com.plateforme.scolaire.web.dto.LogistiqueExamenDto.Livraison;
import com.plateforme.scolaire.web.dto.PersonnelCentreDto;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Suivi national de la préparation des centres : personnel requis/pourvu et fournitures livrées (DGECS). */
@Service
@RequiredArgsConstructor
@Transactional
public class LogistiqueExamenService {

    static final List<String> ARTICLES = List.of("SUJETS", "COPIES", "ENVELOPPES", "FOURNITURES");
    static final Set<String> STATUTS = Set.of("PREVUE", "EXPEDIEE", "RECUE");

    private final EntityManager em;
    private final CentreExamenRepository centres;
    private final LivraisonCentreRepository livraisons;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : postes pourvus, sans compter les surnombres (un surveillant en trop ne comble pas un autre poste). */
    static int pourvus(List<PersonnelCentreDto.Besoin> besoins) {
        return besoins.stream().mapToInt(b -> Math.min(b.requis(), b.affectes())).sum();
    }

    /** Pur : un centre est approvisionné quand chaque article est reçu. */
    static boolean approvisionne(List<Livraison> l) {
        return ARTICLES.stream().allMatch(a -> l.stream().anyMatch(x -> x.article().equals(a) && "RECUE".equals(x.statut())));
    }

    /** Pur : écart entre la quantité reçue et la quantité prévue ; null tant que rien n'est reçu. */
    static Integer ecart(int prevue, Integer livree) {
        return livree == null ? null : livree - prevue;
    }

    /** Pur : un centre est en retard s'il n'est pas prêt et que la date de début des épreuves est atteinte ou passée. */
    static boolean enRetard(boolean pret, LocalDate debut, LocalDate aujourdhui) {
        return !pret && debut != null && !aujourdhui.isBefore(debut);
    }

    /** Pur : cumule les lignes de centres. */
    static LogistiqueExamenDto.Resume resume(List<LigneCentre> l) {
        int requis = l.stream().mapToInt(LigneCentre::postesRequis).sum();
        int pourvus = l.stream().mapToInt(LigneCentre::postesPourvus).sum();
        int complets = (int) l.stream().filter(LigneCentre::personnelComplet).count();
        int appro = (int) l.stream().filter(LigneCentre::approvisionne).count();
        int prets = (int) l.stream().filter(LigneCentre::pret).count();
        return new LogistiqueExamenDto.Resume(l.size(), complets, appro, prets, (int) l.stream().filter(LigneCentre::enRetard).count(), requis, pourvus,
                StatistiquesCandidatsService.pct(pourvus, requis), StatistiquesCandidatsService.pct(appro, l.size()),
                StatistiquesCandidatsService.pct(prets, l.size()));
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public LogistiqueExamenDto suivi(Long examenId) {
        var examen = em.find(com.plateforme.scolaire.domain.ExamenNational.class, examenId);
        if (examen == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable");
        }
        Map<Long, Integer> salles = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createQuery("select c.centre.id, max(c.salle) from CandidatExamen c where c.examen.id = :e and c.centre is not null "
                + "group by c.centre.id").setParameter("e", examenId).getResultList()) {
            salles.put(((Number) r[0]).longValue(), r[1] == null ? 0 : ((Number) r[1]).intValue());
        }
        Map<Long, List<Affectation>> personnel = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createQuery("select p.centre.id, p.role, p.salle from PersonnelCentre p where p.centre.examen.id = :e")
                .setParameter("e", examenId).getResultList()) {
            personnel.computeIfAbsent(((Number) r[0]).longValue(), k -> new ArrayList<>()).add(new Affectation((String) r[1], (Integer) r[2]));
        }
        Map<Long, List<LivraisonCentre>> parCentre = new HashMap<>();
        for (LivraisonCentre l : livraisons.findByExamenId(examenId)) {
            parCentre.computeIfAbsent(l.getCentre().getId(), k -> new ArrayList<>()).add(l);
        }
        LocalDate aujourdhui = LocalDate.now(clock);
        List<LigneCentre> lignes = new ArrayList<>();
        for (CentreExamen c : centres.deLExamen(examenId)) {
            int nbSalles = salles.getOrDefault(c.getId(), 0);
            var besoins = PersonnelCentreService.besoins(nbSalles, personnel.getOrDefault(c.getId(), List.of()));
            int requis = besoins.stream().mapToInt(PersonnelCentreDto.Besoin::requis).sum();
            int pourvus = pourvus(besoins);
            boolean complet = besoins.stream().allMatch(PersonnelCentreDto.Besoin::satisfait);
            List<Livraison> liv = new ArrayList<>();
            for (String article : ARTICLES) {
                LivraisonCentre l = parCentre.getOrDefault(c.getId(), List.of()).stream().filter(x -> x.getArticle().equals(article)).findFirst().orElse(null);
                liv.add(l == null ? new Livraison(article, "NON_PREVUE", 0, null, null, null)
                        : new Livraison(article, l.getStatut(), l.getQuantitePrevue(), l.getQuantiteLivree(), ecart(l.getQuantitePrevue(), l.getQuantiteLivree()),
                                l.getObservation()));
            }
            boolean appro = approvisionne(liv);
            boolean pret = complet && appro;
            LocalDate debut = c.getDateDebut() != null ? c.getDateDebut() : examen.getDateDebut();
            lignes.add(new LigneCentre(c.getId(), c.getNom(), c.getZone() == null ? null : c.getZone().getNom(), nbSalles, requis, pourvus, complet, liv, appro,
                    pret, enRetard(pret, debut, aujourdhui)));
        }
        return new LogistiqueExamenDto(examenId, resume(lignes), lignes);
    }

    public LogistiqueExamenDto enregistrer(Long examenId, Long centreId, LivraisonCentreRequest q) {
        CentreExamen centre = centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
        String article = q.article().trim().toUpperCase();
        if (!ARTICLES.contains(article)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article inconnu : " + q.article());
        }
        String statut = q.statut().trim().toUpperCase();
        if (!STATUTS.contains(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu : " + q.statut());
        }
        LivraisonCentre l = livraisons.findByCentreIdAndArticle(centreId, article).orElseGet(() -> {
            LivraisonCentre n = new LivraisonCentre();
            n.setExamen(centre.getExamen());
            n.setCentre(centre);
            n.setArticle(article);
            return n;
        });
        Instant maintenant = Instant.now(clock);
        l.setQuantitePrevue(q.quantitePrevue());
        l.setStatut(statut);
        l.setObservation(q.observation() == null || q.observation().isBlank() ? null : q.observation().trim());
        if (!"PREVUE".equals(statut) && l.getExpedieeLe() == null) {
            l.setExpedieeLe(maintenant);
        }
        if ("RECUE".equals(statut)) {
            l.setQuantiteLivree(q.quantiteLivree() == null ? q.quantitePrevue() : q.quantiteLivree());
            if (l.getRecueLe() == null) {
                l.setRecueLe(maintenant);
            }
        } else {
            l.setQuantiteLivree(null);
            l.setRecueLe(null);
        }
        livraisons.save(l);
        audit.enregistrer("LIVRAISON_CENTRE_ENREGISTREE", "CentreExamen", centreId, Map.of("article", article, "statut", statut));
        return suivi(examenId);
    }
}
