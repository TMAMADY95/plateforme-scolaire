package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.JournalAudit;
import com.plateforme.scolaire.repository.JournalAuditRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.security.UtilisateurPrincipal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Journal des modifications sensibles. S'exécute dans la transaction de l'appelant : tout ou rien. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final JournalAuditRepository journalAuditRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PerimetreService perimetreService;

    public void enregistrer(String action, String entite, Long entiteId, Map<String, Object> details) {
        JournalAudit journal = new JournalAudit();
        UtilisateurPrincipal courant = perimetreService.utilisateurCourant();
        if (courant != null) {
            journal.setUtilisateur(utilisateurRepository.getReferenceById(courant.getId()));
        }
        journal.setAction(action);
        journal.setEntite(entite);
        journal.setEntiteId(entiteId);
        journal.setDetails(details);
        journal.setAdresseIp(adresseIp());
        journalAuditRepository.save(journal);
    }

    /** Derrière un proxy ou un répartiteur de charge, il faudra lire l'en-tête X-Forwarded-For. */
    private static String adresseIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs) {
            return attributs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
