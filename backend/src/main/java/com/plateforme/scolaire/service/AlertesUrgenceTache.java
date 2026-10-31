package com.plateforme.scolaire.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Lance chaque jour les alertes d'inspection urgente et les rappels de plans d'action (app.alertes.cron). */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertesUrgenceTache {

    private final AlertesUrgenceService alertes;
    private final RappelsPlansActionService rappels;
    private final RappelsResultatsExamenService rappelsExamens;

    @Value("${app.alertes.actif:true}")
    private boolean actif;

    @Scheduled(cron = "${app.alertes.cron:0 0 7 * * *}")
    public void executer() {
        if (!actif) {
            return;
        }
        try {
            int n = alertes.alerter();
            if (n > 0) {
                log.info("{} établissement(s) signalé(s) pour inspection urgente", n);
            }
        } catch (RuntimeException e) {
            log.error("Alertes d'inspection urgente impossibles", e);
        }
        try {
            int n = rappels.rappeler();
            if (n > 0) {
                log.info("{} plan(s) d'action rappelé(s)", n);
            }
        } catch (RuntimeException e) {
            log.error("Rappels de plans d'action impossibles", e);
        }
        try {
            int n = rappelsExamens.rappeler();
            if (n > 0) {
                log.info("{} établissement(s) rappelé(s) pour des résultats d'examen non transmis", n);
            }
        } catch (RuntimeException e) {
            log.error("Rappels de résultats d'examen impossibles", e);
        }
    }
}
