package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.JournalAudit;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.JournalAuditRepository;
import com.plateforme.scolaire.web.dto.JournalAuditDto;
import com.plateforme.scolaire.web.dto.PageAuditDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture du journal d'audit (ministère uniquement : voir le contrôleur). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationAuditService {

    static final int TAILLE_PAR_DEFAUT = 25;
    static final int TAILLE_MAX = 100;
    static final int ELEVES_MAX = 50;

    private final JournalAuditRepository journalAuditRepository;
    private final ResolveurContexteAudit resolveur;
    private final EleveRepository eleveRepository;

    public PageAuditDto rechercher(String action, String entite, int page, int taille) {
        return rechercher(action, entite, null, page, taille);
    }

    /**
     * [eleve] : nom, prénom ou matricule d'un élève ; ne garde que les événements qui le concernent. Une recherche
     * trop vague (plus de {@value #ELEVES_MAX} élèves) est tronquée aux premiers trouvés.
     */
    public PageAuditDto rechercher(String action, String entite, String eleve, int page, int taille) {
        int t = taille <= 0 ? TAILLE_PAR_DEFAUT : Math.min(taille, TAILLE_MAX);
        int p = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(p, t, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        String texte = nettoyer(eleve);
        Page<JournalAudit> resultat;
        if (texte.isEmpty()) {
            resultat = journalAuditRepository.rechercher(nettoyer(action), nettoyer(entite), pageable);
        } else {
            List<Long> eleves = eleveRepository.idsParTexte(texte, PageRequest.of(0, ELEVES_MAX));
            if (eleves.isEmpty()) {
                return new PageAuditDto(List.of(), 0, p, t);
            }
            resultat = journalAuditRepository.rechercherPourEleves(
                    nettoyer(action), nettoyer(entite), eleves, pageable);
        }
        return new PageAuditDto(resultat.getContent().stream().map(this::versDto).toList(),
                resultat.getTotalElements(), p, t);
    }

    private JournalAuditDto versDto(JournalAudit j) {
        ResolveurContexteAudit.Description description = resolveur.decrire(j);
        return JournalAuditDto.de(j, description.resume(), description.contexte());
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }
}
