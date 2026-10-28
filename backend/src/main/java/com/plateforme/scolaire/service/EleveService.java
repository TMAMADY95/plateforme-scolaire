package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.web.dto.EleveDto;
import com.plateforme.scolaire.web.dto.EleveRequest;
import com.plateforme.scolaire.web.dto.MonProfilEleveDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class EleveService {

    private final EleveRepository eleveRepository;
    private final InscriptionRepository inscriptionRepository;

    @Transactional(readOnly = true)
    public EleveDto trouver(Long id) {
        return EleveDto.de(charger(id));
    }

    /** Profil de l'élève connecté, avec sa classe actuelle. */
    @Transactional(readOnly = true)
    public MonProfilEleveDto monProfil(Long utilisateurId) {
        Eleve eleve = eleveRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun dossier élève n'est rattaché à ce compte"));
        Inscription active = inscriptionRepository
                .findByEleveIdAndStatutOrderByAnneeScolaireDateDebutDesc(eleve.getId(), "ACCEPTEE")
                .stream().findFirst().orElse(null);
        return MonProfilEleveDto.de(eleve, active);
    }

    /** Correction de l'identité d'un élève (le matricule ne change jamais). */
    public EleveDto modifier(Long id, EleveRequest r) {
        Eleve eleve = charger(id);
        String nom = r.nom().trim();
        String prenom = r.prenom().trim();
        boolean doublon = eleveRepository.findParIdentite(nom, prenom, r.dateNaissance()).stream()
                .anyMatch(autre -> !autre.getId().equals(id));
        if (doublon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un autre élève a déjà cette identité");
        }
        eleve.setNom(nom);
        eleve.setPrenom(prenom);
        eleve.setDateNaissance(r.dateNaissance());
        eleve.setSexe(r.sexe());
        return EleveDto.de(eleve);
    }

    private Eleve charger(Long id) {
        return eleveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable"));
    }
}
