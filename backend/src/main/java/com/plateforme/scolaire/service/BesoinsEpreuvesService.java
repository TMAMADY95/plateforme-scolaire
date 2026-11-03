package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.BesoinPersonnelEpreuve;
import com.plateforme.scolaire.repository.BesoinPersonnelEpreuveRepository;
import com.plateforme.scolaire.web.dto.BesoinsEpreuvesDto;
import com.plateforme.scolaire.web.dto.PersonnelNationalDto;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Besoins en personnel par épreuve. Le personnel étant mobilisé pour tout l'examen, le besoin à couvrir pour une fonction est le pic
 * observé sur l'ensemble des épreuves.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BesoinsEpreuvesService {

    private final BesoinPersonnelEpreuveRepository besoins;
    private final PersonnelNationalService national;
    private final EntityManager em;
    private final AuditService audit;

    /** Pur : plus grand besoin ; null si aucun. */
    static Integer pic(Collection<Integer> valeurs) {
        return valeurs.stream().filter(v -> v != null).max(Integer::compare).orElse(null);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public BesoinsEpreuvesDto suivi(Long examenId) {
        PersonnelNationalDto synthese = national.synthese(examenId);
        List<Object[]> rows = em.createNativeQuery("SELECT e.id, coalesce(m.libelle, e.libelle, 'Épreuve'), e.date_epreuve::text, e.heure_debut::text "
                + "FROM epreuve_examen e LEFT JOIN matiere m ON m.id = e.matiere_id WHERE e.examen_id = :e ORDER BY e.date_epreuve, e.heure_debut, e.id")
                .setParameter("e", examenId).getResultList();
        List<Long> ids = rows.stream().map(r -> ((Number) r[0]).longValue()).toList();
        Map<Long, Map<String, Integer>> parEpreuve = new HashMap<>();
        if (!ids.isEmpty()) {
            besoins.desEpreuves(ids).forEach(b -> parEpreuve.computeIfAbsent(b.getEpreuveId(), k -> new HashMap<>()).put(b.getFonction(), b.getRequis()));
        }
        List<BesoinsEpreuvesDto.Epreuve> epreuves = new ArrayList<>();
        for (Object[] r : rows) {
            Long id = ((Number) r[0]).longValue();
            epreuves.add(new BesoinsEpreuvesDto.Epreuve(id, (String) r[1], (String) r[2], (String) r[3], parEpreuve.getOrDefault(id, Map.of())));
        }
        List<BesoinsEpreuvesDto.Fonction> fonctions = new ArrayList<>();
        for (PersonnelNationalDto.Ligne l : synthese.fonctions()) {
            if (!PersonnelNationalService.BESOINS_DECLARABLES.contains(l.fonction())) {
                continue;
            }
            Integer pic = pic(epreuves.stream().map(e -> e.requis().get(l.fonction())).toList());
            fonctions.add(new BesoinsEpreuvesDto.Fonction(l.fonction(), l.libelle(), l.disponibles(), pic, PersonnelNationalService.manque(pic, l.disponibles())));
        }
        return new BesoinsEpreuvesDto(examenId, fonctions, epreuves);
    }

    public BesoinsEpreuvesDto definir(Long examenId, Long epreuveId, BesoinsEpreuvesDto.Requete q) {
        Number ok = (Number) em.createNativeQuery("SELECT count(*) FROM epreuve_examen WHERE id = :p AND examen_id = :e")
                .setParameter("p", epreuveId).setParameter("e", examenId).getSingleResult();
        if (ok.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Épreuve introuvable pour cet examen");
        }
        String fonction = q.fonction().trim().toUpperCase();
        if (!PersonnelNationalService.BESOINS_DECLARABLES.contains(fonction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fonction inconnue : " + q.fonction());
        }
        BesoinPersonnelEpreuve b = besoins.findByEpreuveIdAndFonction(epreuveId, fonction).orElseGet(() -> {
            BesoinPersonnelEpreuve n = new BesoinPersonnelEpreuve();
            n.setEpreuveId(epreuveId);
            n.setFonction(fonction);
            return n;
        });
        b.setRequis(q.requis());
        besoins.save(b);
        audit.enregistrer("BESOIN_PERSONNEL_EPREUVE_DEFINI", "ExamenNational", examenId, Map.of("epreuve", epreuveId, "fonction", fonction, "requis", q.requis()));
        return suivi(examenId);
    }
}
