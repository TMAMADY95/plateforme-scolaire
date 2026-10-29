package com.plateforme.scolaire.service;

import com.plateforme.scolaire.repository.PaiementRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Clôture les paiements en ligne restés EN_ATTENTE. Le délai est largement supérieur à la fenêtre de 30 minutes
 * (qui, elle, ne fait que libérer le montant) pour laisser au prestataire le temps d'envoyer une notification tardive.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirationPaiementsTache {

    private final PaiementRepository paiementRepository;
    private final PaiementService paiementService;
    private final Clock clock;

    @Value("${app.paiement.expiration-heures:24}")
    private long expirationHeures;

    @Scheduled(cron = "${app.paiement.expiration-cron:0 5 * * * *}")
    public void executer() {
        int clotures = expirerMaintenant();
        if (clotures > 0) {
            log.info("{} paiement(s) en ligne expiré(s)", clotures);
        }
    }

    /** Chaque paiement est traité dans sa propre transaction : une erreur n'en bloque pas les autres. */
    public int expirerMaintenant() {
        Instant avant = Instant.now(clock).minus(Duration.ofHours(expirationHeures));
        int clotures = 0;
        for (Long id : paiementRepository.idsEnAttenteAvant(avant)) {
            try {
                if (paiementService.expirer(id, avant)) {
                    clotures++;
                }
            } catch (RuntimeException e) {
                log.warn("Expiration du paiement {} impossible : {}", id, e.getMessage());
            }
        }
        return clotures;
    }
}
