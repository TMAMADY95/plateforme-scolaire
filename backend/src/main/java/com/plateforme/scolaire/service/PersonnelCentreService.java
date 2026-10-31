package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CentreExamen;
import com.plateforme.scolaire.domain.PersonnelCentre;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.CentreExamenRepository;
import com.plateforme.scolaire.repository.PersonnelCentreRepository;
import com.plateforme.scolaire.web.dto.PersonnelCentreDto;
import com.plateforme.scolaire.web.dto.PersonnelCentreRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Personnel d'un centre d'examen (règlement général) : un président de la commission de surveillance, un secrétariat,
 * deux surveillants par salle, des chefs de salle, la sécurité et le personnel de santé. Le centre est « complet » quand
 * les besoins obligatoires sont couverts.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonnelCentreService {

    static final Set<String> ROLES = Set.of("PRESIDENT", "CHEF_CENTRE", "SECRETAIRE", "SURVEILLANT", "CHEF_SALLE", "SECURITE", "SANTE");
    static final Set<String> UNIQUES = Set.of("PRESIDENT", "CHEF_CENTRE");
    static final int SURVEILLANTS_PAR_SALLE = 2;

    private final PersonnelCentreRepository personnel;
    private final CentreExamenRepository centres;
    private final CandidatExamenRepository candidats;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PersonnelCentreDto personnel(Long examenId, Long centreId) {
        CentreExamen centre = centre(examenId, centreId);
        return dto(centre, personnel.findByCentreIdOrderByRoleAscSalleAscNomAsc(centreId));
    }

    public PersonnelCentreDto ajouter(Long examenId, Long centreId, PersonnelCentreRequest r) {
        CentreExamen centre = centre(examenId, centreId);
        if (!ROLES.contains(r.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fonction inconnue : " + r.role());
        }
        List<PersonnelCentre> existants = personnel.findByCentreIdOrderByRoleAscSalleAscNomAsc(centreId);
        boolean parSalle = "SURVEILLANT".equals(r.role()) || "CHEF_SALLE".equals(r.role());
        if (parSalle) {
            int salles = salles(centreId);
            if (r.salle() == null || r.salle() < 1 || (salles > 0 && r.salle() > salles)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Indiquez la salle (1 à " + Math.max(salles, 1) + ") : générez d'abord la répartition en salles du centre");
            }
        } else if (r.salle() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette fonction n'est pas rattachée à une salle");
        }
        if (UNIQUES.contains(r.role()) && existants.stream().anyMatch(p -> p.getRole().equals(r.role()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette fonction est déjà attribuée dans ce centre");
        }
        String nom = r.nom().trim();
        if (existants.stream().anyMatch(p -> p.getNom().equalsIgnoreCase(nom) && p.getRole().equals(r.role())
                && java.util.Objects.equals(p.getSalle(), r.salle()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, nom + " est déjà désigné(e) à cette fonction");
        }
        PersonnelCentre p = new PersonnelCentre();
        p.setCentre(centre);
        p.setRole(r.role());
        p.setNom(nom);
        p.setTelephone(r.telephone() == null || r.telephone().isBlank() ? null : r.telephone().trim());
        p.setSalle(r.salle());
        p.setCreatedAt(Instant.now(clock));
        personnel.save(p);
        audit.enregistrer("PERSONNEL_CENTRE_AJOUTE", "CentreExamen", centreId, Map.of("role", r.role(), "nom", nom));
        return dto(centre, personnel.findByCentreIdOrderByRoleAscSalleAscNomAsc(centreId));
    }

    public PersonnelCentreDto retirer(Long examenId, Long centreId, Long personneId) {
        CentreExamen centre = centre(examenId, centreId);
        PersonnelCentre p = personnel.findById(personneId).filter(x -> x.getCentre().getId().equals(centreId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personne introuvable dans ce centre"));
        personnel.delete(p);
        personnel.flush();
        audit.enregistrer("PERSONNEL_CENTRE_RETIRE", "CentreExamen", centreId, Map.of("role", p.getRole(), "nom", p.getNom()));
        return dto(centre, personnel.findByCentreIdOrderByRoleAscSalleAscNomAsc(centreId));
    }

    /** Nombre de salles utilisées par les candidats du centre (répartition en salles faite), 0 sinon. */
    private int salles(Long centreId) {
        return candidats.duCentre(centreId).stream().map(c -> c.getSalle() == null ? 0 : c.getSalle()).max(Integer::compare).orElse(0);
    }

    private PersonnelCentreDto dto(CentreExamen centre, List<PersonnelCentre> liste) {
        int salles = salles(centre.getId());
        List<PersonnelCentreDto.Besoin> besoins = besoins(salles, liste.stream()
                .map(p -> new Affectation(p.getRole(), p.getSalle())).toList());
        return new PersonnelCentreDto(centre.getId(), centre.getNom(), salles,
                liste.stream().map(p -> new PersonnelCentreDto.Personne(p.getId(), p.getRole(), p.getNom(), p.getTelephone(), p.getSalle())).toList(),
                besoins, besoins.stream().allMatch(PersonnelCentreDto.Besoin::satisfait));
    }

    record Affectation(String role, Integer salle) {
    }

    /** Besoins obligatoires : président, chef de centre, secrétariat, puis deux surveillants dans chaque salle. */
    static List<PersonnelCentreDto.Besoin> besoins(int salles, List<Affectation> affectations) {
        List<PersonnelCentreDto.Besoin> besoins = new ArrayList<>();
        besoins.add(new PersonnelCentreDto.Besoin("Président de la commission de surveillance", 1, compter(affectations, "PRESIDENT", null)));
        besoins.add(new PersonnelCentreDto.Besoin("Chef de centre", 1, compter(affectations, "CHEF_CENTRE", null)));
        besoins.add(new PersonnelCentreDto.Besoin("Secrétariat", 1, compter(affectations, "SECRETAIRE", null)));
        for (int s = 1; s <= salles; s++) {
            besoins.add(new PersonnelCentreDto.Besoin("Surveillants de la salle " + s, SURVEILLANTS_PAR_SALLE, compter(affectations, "SURVEILLANT", s)));
        }
        return besoins;
    }

    private static int compter(List<Affectation> liste, String role, Integer salle) {
        return (int) liste.stream().filter(a -> a.role().equals(role) && (salle == null || salle.equals(a.salle()))).count();
    }

    private CentreExamen centre(Long examenId, Long centreId) {
        return centres.findById(centreId).filter(c -> c.getExamen().getId().equals(examenId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centre inconnu pour cet examen"));
    }
}
