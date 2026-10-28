package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.JournalAudit;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Une entrée du journal, avec une phrase lisible et le contexte (qui est concerné, dans quel établissement).
 * {@code details} garde les données brutes pour un contrôle technique.
 */
public record JournalAuditDto(Long id, Instant date, String utilisateurEmail, String utilisateurNom,
                              String action, String resume, String entite, Long entiteId,
                              List<ElementContexte> contexte, Map<String, Object> details, String adresseIp) {

    public static JournalAuditDto de(JournalAudit j, String resume, List<ElementContexte> contexte) {
        var u = j.getUtilisateur();
        return new JournalAuditDto(j.getId(), j.getCreatedAt(),
                u == null ? null : u.getEmail(),
                u == null ? null : u.getPrenom() + " " + u.getNom(),
                j.getAction(), resume, j.getEntite(), j.getEntiteId(), contexte, j.getDetails(), j.getAdresseIp());
    }
}
