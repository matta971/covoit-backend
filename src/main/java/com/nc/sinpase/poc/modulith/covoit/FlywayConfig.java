package com.nc.sinpase.poc.modulith.covoit;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway n'est plus auto-configuré par Spring Boot 4.x.
 * On crée le bean manuellement et on force entityManagerFactory à dépendre de flyway
 * pour que les migrations tournent avant la validation du schéma JPA.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    /**
     * BeanFactoryPostProcessor static = exécuté très tôt, avant l'instanciation des beans.
     * Ajoute "flyway" dans le dependsOn de entityManagerFactory.
     */
    @Bean
    public static BeanFactoryPostProcessor flywayDependsOnEntityManagerFactory() {
        return beanFactory -> {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            try {
                BeanDefinition bd = registry.getBeanDefinition("entityManagerFactory");
                String[] current = bd.getDependsOn();
                if (current == null) {
                    bd.setDependsOn("flyway");
                } else {
                    String[] updated = new String[current.length + 1];
                    System.arraycopy(current, 0, updated, 0, current.length);
                    updated[current.length] = "flyway";
                    bd.setDependsOn(updated);
                }
            } catch (Exception ignored) {
                // entityManagerFactory pas encore enregistré — sera résolu au refresh
            }
        };
    }
}
