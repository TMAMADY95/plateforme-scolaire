package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.AlerteEnvoyee;
import java.time.Instant;

public record AlerteEnvoyeeDto(Long id, Instant envoyeeLe, String type, String canal, String destinataire,
                               String resume, boolean reussie) {

    public static AlerteEnvoyeeDto de(AlerteEnvoyee a) {
        return new AlerteEnvoyeeDto(a.getId(), a.getEnvoyeeLe(), a.getType(), a.getCanal(), a.getDestinataire(),
                a.getResume(), a.isReussie());
    }
}
