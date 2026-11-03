package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.VerificationDiplome;
import com.plateforme.scolaire.repository.VerificationDiplomeRepository;
import com.plateforme.scolaire.web.dto.VerificationsDiplomeDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Journal des vérifications publiques de diplômes et synthèse pour la DGECS (taux de codes inconnus, tentatives répétées). */
@Service
@RequiredArgsConstructor
public class VerificationsDiplomeService {

    private static final Logger LOG = LoggerFactory.getLogger(VerificationsDiplomeService.class);
    /** Nombre de saisies d'un même code inconnu à partir duquel la tentative est jugée suspecte (hypothèse à valider avec la DGECS). */
    static final int SEUIL_SUSPECT = 3;
    static final int LONGUEUR_MAX = 20;

    private final VerificationDiplomeRepository verifications;
    private final Clock clock;

    /** Pur : code normalisé (espaces retirés, majuscules, tronqué) ; vide si rien n'est saisi. */
    static String normaliser(String saisie) {
        String s = saisie == null ? "" : saisie.trim().toUpperCase();
        return s.length() > LONGUEUR_MAX ? s.substring(0, LONGUEUR_MAX) : s;
    }

    /** Pur : une tentative est suspecte quand le même code inconnu est saisi au moins SEUIL_SUSPECT fois. */
    static boolean suspect(long tentatives) {
        return tentatives >= SEUIL_SUSPECT;
    }

    /** Enregistre une vérification sans jamais faire échouer le contrôle public. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String saisie, String resultat) {
        try {
            String code = normaliser(saisie);
            if (code.isEmpty()) {
                return;
            }
            VerificationDiplome v = new VerificationDiplome();
            v.setCodeSaisi(code);
            v.setResultat(resultat);
            v.setVerifieLe(Instant.now(clock));
            verifications.save(v);
        } catch (RuntimeException e) {
            LOG.warn("Vérification de diplôme non journalisée : {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public VerificationsDiplomeDto synthese() {
        Instant depuis = Instant.now(clock).minus(Duration.ofDays(30));
        long auth = verifications.countByResultat("AUTHENTIQUE");
        long rev = verifications.countByResultat("REVOQUE");
        long inc = verifications.countByResultat("INCONNU");
        long total = auth + rev + inc;
        List<VerificationsDiplomeDto.Suspect> suspects = verifications.inconnusRepetes(depuis, SEUIL_SUSPECT).stream()
                .limit(10).map(r -> new VerificationsDiplomeDto.Suspect((String) r[0], ((Number) r[1]).longValue(), (Instant) r[2])).toList();
        return new VerificationsDiplomeDto(total, auth, rev, inc, verifications.countByVerifieLeAfter(depuis),
                StatistiquesCandidatsService.pct((int) inc, (int) total), SEUIL_SUSPECT, suspects);
    }
}
