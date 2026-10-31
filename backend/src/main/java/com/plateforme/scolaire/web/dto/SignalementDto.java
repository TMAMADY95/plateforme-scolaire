package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.SignalementSecurite;
import java.time.Instant;

public record SignalementDto(Long id, String categorie, String gravite, String description, String statut,
                             String signalePar, Instant createdAt, Instant resoluAt) {

    public static SignalementDto de(SignalementSecurite s) {
        String nom = ((s.getSignalePar().getPrenom() == null ? "" : s.getSignalePar().getPrenom()) + " "
                + (s.getSignalePar().getNom() == null ? "" : s.getSignalePar().getNom())).trim();
        return new SignalementDto(s.getId(), s.getCategorie(), s.getGravite(), s.getDescription(), s.getStatut(),
                nom, s.getCreatedAt(), s.getResoluAt());
    }
}
