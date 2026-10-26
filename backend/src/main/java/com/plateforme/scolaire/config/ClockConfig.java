package com.plateforme.scolaire.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Horloge unique de l'application : remplaçable par une horloge fixe dans les tests. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
