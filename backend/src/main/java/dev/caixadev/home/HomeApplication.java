package dev.caixadev.home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Point d'entrée de l'application.
 * - scanBasePackages : component scan sur tout dev.caixadev (architecture hexagonale)
 * - EntityScan : indique à JPA où trouver les entités @Entity
 * - EnableJpaRepositories : indique à Spring Data où trouver les repositories
 */
@SpringBootApplication(
        exclude = {UserDetailsServiceAutoConfiguration.class},
        scanBasePackages = "dev.caixadev"
)
@EntityScan(basePackages = "dev.caixadev.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "dev.caixadev.infrastructure.persistence.repository")
public class HomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeApplication.class, args);
    }
}
