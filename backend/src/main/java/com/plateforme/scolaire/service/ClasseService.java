package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Etablissement;
import com.plateforme.scolaire.domain.Niveau;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.web.dto.ClasseDto;
import com.plateforme.scolaire.web.dto.ClasseModification;
import com.plateforme.scolaire.web.dto.ClasseRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Les autorisations (@PreAuthorize) sont vérifiées dans les contrôleurs ;
 * ce service applique les règles métier.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClasseService {

    private final ClasseRepository classeRepository;
    private final EtablissementRepository etablissementRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final NiveauRepository niveauRepository;

    @Transactional(readOnly = true)
    public List<ClasseDto> lister(Long etablissementId, Long anneeScolaireId) {
        if (!etablissementRepository.existsById(etablissementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable");
        }
        List<Classe> classes = anneeScolaireId == null
                ? classeRepository.findByEtablissementIdOrderByNom(etablissementId)
                : classeRepository.findByEtablissementIdAndAnneeScolaireIdOrderByNom(etablissementId, anneeScolaireId);
        return classes.stream().map(ClasseDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ClasseDto trouver(Long id) {
        return ClasseDto.de(charger(id));
    }

    public ClasseDto creer(Long etablissementId, ClasseRequest r) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        if (!"ACTIF".equals(etablissement.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'établissement n'est pas actif");
        }
        AnneeScolaire annee = anneeRepository.findById(r.anneeScolaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année scolaire inconnue"));
        Niveau niveau = niveauRepository.findById(r.niveauId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu"));
        if (classeRepository.findByEtablissementIdAndAnneeScolaireIdAndNom(
                etablissementId, annee.getId(), r.nom()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une classe de cet établissement porte déjà ce nom pour cette année");
        }
        Classe classe = new Classe();
        classe.setEtablissement(etablissement);
        classe.setAnneeScolaire(annee);
        classe.setNiveau(niveau);
        classe.setNom(r.nom());
        classe.setCapacite(r.capacite());
        return ClasseDto.de(classeRepository.save(classe));
    }

    public ClasseDto modifier(Long id, ClasseModification r) {
        Classe classe = charger(id);
        Niveau niveau = niveauRepository.findById(r.niveauId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu"));
        classeRepository.findByEtablissementIdAndAnneeScolaireIdAndNom(
                        classe.getEtablissement().getId(), classe.getAnneeScolaire().getId(), r.nom())
                .filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Une classe de cet établissement porte déjà ce nom pour cette année");
                });
        classe.setNiveau(niveau);
        classe.setNom(r.nom());
        classe.setCapacite(r.capacite());
        return ClasseDto.de(classe);
    }

    private Classe charger(Long id) {
        return classeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
    }
}
