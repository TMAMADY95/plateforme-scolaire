package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.BesoinPersonnelExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.PersonnelExamen;
import com.plateforme.scolaire.repository.BesoinPersonnelExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.PersonnelExamenRepository;
import com.plateforme.scolaire.web.dto.PersonnelNationalDto;
import com.plateforme.scolaire.web.dto.PersonnelNationalDto.Ligne;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
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

/**
 * Vue nationale du personnel mobilisé pour un examen : surveillants, chefs de centre, secrétaires, sécurité et santé (saisis dans les centres),
 * correcteurs (commissions de correction), puis jurys, superviseurs, inspecteurs et agents informatiques (saisis ici). Les besoins en surveillants,
 * chefs de centre et secrétaires sont calculés ; les autres sont déclarés par la DGECS.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonnelNationalService {

    static final Set<String> FONCTIONS_NATIONALES = Set.of("JURY", "SUPERVISEUR", "INSPECTEUR", "INFORMATIQUE");
    static final Set<String> BESOINS_DECLARABLES = Set.of("SECURITE", "SANTE", "CORRECTEUR", "JURY", "SUPERVISEUR", "INSPECTEUR", "INFORMATIQUE");

    private final PersonnelExamenRepository personnel;
    private final BesoinPersonnelExamenRepository besoins;
    private final ExamenNationalRepository examens;
    private final EntityManager em;
    private final AuditService audit;
    private final Clock clock;

    /** Pur : personnes manquantes ; null sans besoin connu. */
    static Integer manque(Integer requis, int disponibles) {
        return requis == null ? null : Math.max(0, requis - disponibles);
    }

    private static Ligne ligne(String fonction, String libelle, Integer requis, int dispo, boolean modifiable, boolean saisies, String source) {
        return new Ligne(fonction, libelle, requis, dispo, manque(requis, dispo), modifiable, saisies, source);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public PersonnelNationalDto synthese(Long examenId) {
        examen(examenId);
        Map<String, Integer> decl = new HashMap<>();
        besoins.findByExamenId(examenId).forEach(b -> decl.put(b.getFonction(), b.getRequis()));
        Map<String, Integer> centres = new HashMap<>();
        for (Object[] r : (List<Object[]>) em.createQuery("select p.role, count(p.id) from PersonnelCentre p where p.centre.examen.id = :e group by p.role")
                .setParameter("e", examenId).getResultList()) {
            centres.put((String) r[0], ((Number) r[1]).intValue());
        }
        int nbCentres = ((Number) em.createQuery("select count(c.id) from CentreExamen c where c.examen.id = :e").setParameter("e", examenId).getSingleResult()).intValue();
        int sallesTotal = 0;
        for (Object[] r : (List<Object[]>) em.createQuery("select c.centre.id, max(c.salle) from CandidatExamen c where c.examen.id = :e and c.centre is not null "
                + "group by c.centre.id").setParameter("e", examenId).getResultList()) {
            sallesTotal += r[1] == null ? 0 : ((Number) r[1]).intValue();
        }
        int correcteurs = ((Number) em.createQuery("select count(m.id) from MembreCommissionCorrection m where m.commission.examen.id = :e and m.role = 'CORRECTEUR'")
                .setParameter("e", examenId).getSingleResult()).intValue();
        List<PersonnelExamen> liste = personnel.findByExamenIdOrderByFonctionAscNomAsc(examenId);
        Map<String, Integer> nationaux = new HashMap<>();
        liste.forEach(p -> nationaux.merge(p.getFonction(), 1, Integer::sum));

        List<Ligne> lignes = new ArrayList<>();
        lignes.add(ligne("CHEF_CENTRE", "Chefs de centre", nbCentres, centres.getOrDefault("CHEF_CENTRE", 0), false, false, "CENTRES"));
        lignes.add(ligne("SECRETAIRE", "Secrétaires", nbCentres, centres.getOrDefault("SECRETAIRE", 0), false, false, "CENTRES"));
        lignes.add(ligne("SURVEILLANT", "Surveillants (2 par salle)", sallesTotal * PersonnelCentreService.SURVEILLANTS_PAR_SALLE,
                centres.getOrDefault("SURVEILLANT", 0), false, false, "CENTRES"));
        lignes.add(ligne("SECURITE", "Agents de sécurité", decl.get("SECURITE"), centres.getOrDefault("SECURITE", 0), true, false, "CENTRES"));
        lignes.add(ligne("SANTE", "Personnels de santé", decl.get("SANTE"), centres.getOrDefault("SANTE", 0), true, false, "CENTRES"));
        lignes.add(ligne("CORRECTEUR", "Correcteurs", decl.get("CORRECTEUR"), correcteurs, true, false, "COMMISSIONS"));
        lignes.add(ligne("JURY", "Membres des jurys", decl.get("JURY"), nationaux.getOrDefault("JURY", 0), true, true, "NATIONAL"));
        lignes.add(ligne("SUPERVISEUR", "Superviseurs", decl.get("SUPERVISEUR"), nationaux.getOrDefault("SUPERVISEUR", 0), true, true, "NATIONAL"));
        lignes.add(ligne("INSPECTEUR", "Inspecteurs", decl.get("INSPECTEUR"), nationaux.getOrDefault("INSPECTEUR", 0), true, true, "NATIONAL"));
        lignes.add(ligne("INFORMATIQUE", "Agents informatiques", decl.get("INFORMATIQUE"), nationaux.getOrDefault("INFORMATIQUE", 0), true, true, "NATIONAL"));
        return new PersonnelNationalDto(examenId, lignes,
                liste.stream().map(p -> new PersonnelNationalDto.Personne(p.getId(), p.getFonction(), p.getNom(), p.getTelephone())).toList());
    }

    public PersonnelNationalDto ajouter(Long examenId, PersonnelNationalDto.AjoutRequest q) {
        ExamenNational examen = examen(examenId);
        String fonction = q.fonction().trim().toUpperCase();
        if (!FONCTIONS_NATIONALES.contains(fonction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fonction inconnue : " + q.fonction());
        }
        String nom = q.nom().trim();
        if (personnel.existsByExamenIdAndFonctionAndNomIgnoreCase(examenId, fonction, nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette personne est déjà enregistrée pour cette fonction");
        }
        PersonnelExamen p = new PersonnelExamen();
        p.setExamen(examen);
        p.setFonction(fonction);
        p.setNom(nom);
        p.setTelephone(q.telephone() == null || q.telephone().isBlank() ? null : q.telephone().trim());
        p.setCreatedAt(Instant.now(clock));
        personnel.save(p);
        audit.enregistrer("PERSONNEL_NATIONAL_AJOUTE", "ExamenNational", examenId, Map.of("fonction", fonction, "nom", nom));
        return synthese(examenId);
    }

    public PersonnelNationalDto retirer(Long examenId, Long id) {
        PersonnelExamen p = personnel.findById(id).filter(x -> x.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personne introuvable"));
        personnel.delete(p);
        audit.enregistrer("PERSONNEL_NATIONAL_RETIRE", "ExamenNational", examenId, Map.of("fonction", p.getFonction(), "nom", p.getNom()));
        return synthese(examenId);
    }

    public PersonnelNationalDto definirBesoin(Long examenId, PersonnelNationalDto.BesoinRequest q) {
        ExamenNational examen = examen(examenId);
        String fonction = q.fonction().trim().toUpperCase();
        if (!BESOINS_DECLARABLES.contains(fonction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le besoin de cette fonction est calculé : " + q.fonction());
        }
        BesoinPersonnelExamen b = besoins.findByExamenIdAndFonction(examenId, fonction).orElseGet(() -> {
            BesoinPersonnelExamen n = new BesoinPersonnelExamen();
            n.setExamen(examen);
            n.setFonction(fonction);
            return n;
        });
        b.setRequis(q.requis());
        besoins.save(b);
        audit.enregistrer("BESOIN_PERSONNEL_DEFINI", "ExamenNational", examenId, Map.of("fonction", fonction, "requis", q.requis()));
        return synthese(examenId);
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
