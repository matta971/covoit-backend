package com.nc.sinpase.poc.modulith.covoit;

import org.springframework.boot.artemis.autoconfigure.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Désactive la sécurité JAAS du broker Artemis embarqué.
 *
 * Sur Java 17+, Subject.getSubject(AccessControlContext) lève
 * UnsupportedOperationException car le SecurityManager est supprimé.
 * Inutile de maintenir la sécurité broker quand il tourne dans la même JVM.
 */
@Configuration
class MessagingConfig {

    @Bean
    ArtemisConfigurationCustomizer artemisSecurityDisabler() {
        return config -> {
            config.setSecurityEnabled(false);
            config.setJMXManagementEnabled(false);
        };
    }
}
